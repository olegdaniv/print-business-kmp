package com.printbusinesskmp.database.tables

import org.jetbrains.exposed.sql.Table

object AppSettingsTable : Table("app_settings") {
    val settingKey = varchar("setting_key", 100)
    val settingValue = varchar("setting_value", 500)

    override val primaryKey = PrimaryKey(settingKey)
}
