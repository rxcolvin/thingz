package myapplication

import application.Application
import jdbc.DerbyJdbcHelper
import sql.DerbySqlHelper


fun main() {
    try {
        Application(
            jdbcHelper = DerbyJdbcHelper(),
            assetTypes = listOf(PersonType)
        ).start()
    } catch (ex: Exception) {
        println(ex)
    }
}
