# База даних: де живуть дані та як їх дивитися

> Документ створено після розбору проблеми «зберіг клієнта — він зник».
> Дата: 2026-06-06.

---

## 1. Висновок: де насправді зберігаються дані

**Дані НЕ зберігаються на твоєму комп'ютері.** Desktop- і web-застосунки не мають
власної бази — вони лише клієнти, що ходять по HTTP на бекенд.

| Що | Куди налаштовано |
|----|------------------|
| Адреса бекенда (desktop/web) | `https://print-business-kmp.onrender.com` |
| Де це задано | `gradle.properties` → `printbusiness.api.host/port/scheme` |
| База даних | Керована **PostgreSQL на Render** (`render.yaml` → `print-business-db`) |
| План | **free** (і веб-сервіс, і Postgres) |

Бекенд обирає базу так (`backend/.../DatabaseFactory.kt`):
- якщо задано env `DB_HOST` (як на Render/у Docker) → **PostgreSQL**;
- інакше (локальний запуск без Docker) → **H2-файл** у
  `~/Library/Application Support/PrintBusiness/data/printbusiness.mv.db` (macOS).

> На цій машині локальної H2-бази **немає** — бо ти не піднімаєш локальний бекенд,
> а працюєш проти Render.

---

## 2. Чому клієнт «зник» — ймовірні причини

1. **Стара збірка desktop маскувала помилки.**
   Фікс обробки помилок API (коміт `cf2ae7d`) ще не потрапив у запущений у тебе
   застосунок. У старій версії помилка збереження/завантаження показувалась як
   незрозуміла крипта або ковталась. → Перезбери й перезапусти desktop, тоді
   побачиш справжню причину:
   ```bash
   ./gradlew :desktopApp:run
   ```

2. **Render free засинає після 15 хв простою.**
   Перший запит після сну = холодний старт (~50 с) або 502 під час редеплою.
   Якщо збереження/завантаження списку потрапило в це вікно — запит падає.

3. **Free Postgres на Render видаляється приблизно через 30 днів.**
   Якщо колись зникнуть **усі** дані разом — це майже напевно воно.
   На момент написання БД жива (`/health` → 200), тож поки не цей випадок,
   але для реального використання це ризик: потрібен платний план або регулярні
   бекапи (див. §6).

---

## 3. Як отримати доступ до бази (реквізити)

Усі способи нижче потребують **зовнішнього рядка підключення** до Render Postgres.

1. Зайди на <https://dashboard.render.com> → база **`print-business-db`**.
2. Вкладка **Connect** → секція **External Connection**.
3. Скопіюй **External Database URL** (виглядає так):
   ```
   postgresql://printbusiness:ПАРОЛЬ@dpg-xxxx.frankfurt-postgres.render.com/printbusiness_db
   ```
   > Важливо: бери саме **External** (зовнішній). Internal URL працює лише
   > всередині Render. Зовнішній доступ до free-Postgres дозволений.

⚠️ Цей URL містить пароль — **не комітити** його і не вставляти в код.

---

## 4. Чим дивитися базу

### Варіант A — GUI-клієнт (найзручніше, рекомендовано)

Будь-який підтримує PostgreSQL:

- **DBeaver** (безкоштовний, кросплатформний) — <https://dbeaver.io>
- **TablePlus** (зручний, є безкоштовна версія) — <https://tableplus.com>
- **pgAdmin** (офіційний для Postgres) — <https://www.pgadmin.org>

Налаштування підключення: тип **PostgreSQL**, поля Host / Port / Database / User /
Password взяти з External URL (§3). У DBeaver можна навіть вставити цілий URL.
Після підключення видно дерево таблиць, дані редагуються кліком, є редактор SQL.

### Варіант B — Android Studio / IntelliJ

- **Android Studio НЕ має** вбудованого Database-інструмента — це фіча
  **IntelliJ IDEA Ultimate** / **DataGrip** (платні).
- Безкоштовна альтернатива — плагін **Database Navigator**:
  `Settings → Plugins → Marketplace → "Database Navigator"` → Install → Restart.
  Підтримує PostgreSQL, дерево таблиць і SQL-консоль прямо в IDE.
- Якщо є JetBrains-підписка — простіше поставити **DataGrip** (найкращий UX для БД).

### Варіант C — термінал (`psql`)

```bash
# встановити (macOS)
brew install libpq && brew link --force libpq    # дає psql без усього Postgres

# підключитися (вставити свій External URL у лапках)
psql "postgresql://printbusiness:ПАРОЛЬ@dpg-xxxx...render.com/printbusiness_db"
```

### Варіант D — Render Dashboard (без встановлення нічого)

На сторінці бази **`print-business-db`** є кнопка з вебконсоллю **psql** —
відкриває термінал у браузері, підключений до цієї бази. Зручно для разового
перегляду.

---

## 5. Корисні SQL-запити

Назви таблиць у базі:
`clients`, `orders`, `order_items`, `invoices`, `invoice_lines`, `layouts`,
`partners`, `outsource_jobs`, `business_profiles`, `saved_items`,
`allowed_emails`, `schema_version`.

```sql
-- усі таблиці
\dt                      -- у psql
-- або універсально:
SELECT table_name FROM information_schema.tables WHERE table_schema = 'public';

-- подивитися всіх клієнтів
SELECT id, display_name, phone, address, created_at FROM clients ORDER BY created_at DESC;

-- скільки чого в базі
SELECT
  (SELECT count(*) FROM clients) AS clients,
  (SELECT count(*) FROM orders)  AS orders,
  (SELECT count(*) FROM invoices) AS invoices;

-- знайти конкретного клієнта
SELECT * FROM clients WHERE display_name ILIKE '%петро%';

-- замовлення конкретного клієнта
SELECT o.* FROM orders o WHERE o.client_id = 'CLIENT_ID_ТУТ';
```

---

## 6. Видалення / очищення даних (обережно)

### Що треба знати про зв'язки
- Застосунок **забороняє** видаляти клієнта, у якого є замовлення
  (`ClientRepository.deleteClient` → HTTP 409). У SQL такого захисту немає —
  видаляючи напряму, можна лишити «осиротілі» замовлення.
- `layouts.client_id` і `invoices.order_id` — це FK (зовнішні ключі).
  Видалення «батька» може впасти, поки існують «діти».

### Безпечний порядок видалення (приклад: повністю прибрати клієнта)
```sql
-- 1. знайти id клієнта
SELECT id FROM clients WHERE display_name = 'Тест';

-- 2. видалити пов'язані сутності (макети, позиції замовлень, замовлення)
DELETE FROM layouts      WHERE client_id = 'CLIENT_ID';
DELETE FROM order_items  WHERE order_id IN (SELECT id FROM orders WHERE client_id = 'CLIENT_ID');
DELETE FROM orders       WHERE client_id = 'CLIENT_ID';

-- 3. видалити самого клієнта
DELETE FROM clients      WHERE id = 'CLIENT_ID';
```

### Видалити лише тестові дублі клієнтів
```sql
DELETE FROM clients
WHERE display_name ILIKE 'test%'
  AND id NOT IN (SELECT DISTINCT client_id FROM orders);  -- не чіпати тих, у кого є замовлення
```

> 🔴 **Завжди роби бекап перед DELETE** (див. нижче). `DELETE` без `WHERE`
> зітре всю таблицю.

---

## 7. Бекап і відновлення

```bash
# повний бекап усієї бази у файл (зробити ПЕРЕД будь-яким видаленням)
pg_dump "postgresql://printbusiness:ПАРОЛЬ@dpg-xxxx...render.com/printbusiness_db" \
  > backup_$(date +%Y%m%d_%H%M).sql

# відновлення з бекапу
psql "postgresql://...тойсамийURL..." < backup_20260606_1200.sql
```

> Локальна H2-база (якщо колись піднімеш локальний бекенд) бекапиться
> автоматично при старті — копії в `~/Library/Application Support/PrintBusiness/backups/`
> (див. `LocalH2BackupManager`).

---

## 8. Рекомендації, щоб дані більше не «зникали»

- **Перезібрати desktop** із фіксом помилок (`cf2ae7d`), щоб бачити реальні збої,
  а не тишу.
- Памʼятати про **сон free-сервісу** (15 хв): перший запит після паузи може
  «тупити» — це не втрата даних.
- Для продакшену — **платний Postgres** на Render (free видаляється ~через 30 днів)
  або регулярний `pg_dump` за розкладом.





