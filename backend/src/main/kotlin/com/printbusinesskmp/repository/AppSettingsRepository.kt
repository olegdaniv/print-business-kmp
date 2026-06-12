package com.printbusinesskmp.repository

import com.printbusinesskmp.database.DatabaseFactory.dbQuery
import com.printbusinesskmp.database.tables.AppSettingsTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update

class AppSettingsRepository {

    suspend fun get(key: String): String? = dbQuery {
        AppSettingsTable.selectAll()
            .where { AppSettingsTable.settingKey eq key }
            .map { it[AppSettingsTable.settingValue] }
            .singleOrNull()
    }

    suspend fun set(key: String, value: String): Unit = dbQuery {
        val updated = AppSettingsTable.update({ AppSettingsTable.settingKey eq key }) {
            it[settingValue] = value
        }
        if (updated == 0) {
            AppSettingsTable.insert {
                it[settingKey] = key
                it[settingValue] = value
            }
        }
    }
}
