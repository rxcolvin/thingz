package myapplication

import application.Application
import common.entities.PersonType
import jdbc.DerbyJdbcHelper


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
