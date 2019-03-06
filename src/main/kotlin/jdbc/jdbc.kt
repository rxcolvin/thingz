package jdbc

import java.sql.Connection as JdbcConnection
import java.sql.Driver as JdbcDriver
import java.sql.DriverManager as JdbcDriverManager
import java.sql.PreparedStatement as JdbcPreparedStatement
import java.sql.ResultSet as JdbcResultSet


interface Connection {
    fun prepareStatement(sql: String): PreparedStatement
}

interface ResultSet {
    fun asMapSequence(): Sequence<Map<String, Any>>
}

interface PreparedStatement {
    fun executeQuery(values: List<Any>): ResultSet
    fun execute(values: List<*> = emptyList<Any>()): Unit
}

interface Driver {
    fun connect(
        url: String,
        info: Map<String, String>
    ): Connection
}

class DriverImpl(val jdbcDriver: JdbcDriver) : Driver {
    override fun connect(
        url: String,
        info: Map<String, String>
    ): Connection {
        return ConnectionImpl(jdbcDriver.connect(url, info.toProperties()))
    }
}

object DriverManager {
    fun getConnection(url: String) : Connection = ConnectionImpl(
        JdbcDriverManager.getConnection(url)
    )

}


class ConnectionImpl(
    val jdbcConnection: JdbcConnection
) : Connection {

    override fun prepareStatement(sql: String): PreparedStatement {
        val stmt = jdbcConnection.prepareStatement(sql)
        return PreparedStatementImpl(stmt)
    }
}

class ResultSetImpl(
    private val jdbcResultSet: JdbcResultSet
) : ResultSet {
    override fun asMapSequence(): Sequence<Map<String, Any>> =
            ResultSetMapSequence(jdbcResultSet)
}

class ResultSetMapSequence (
    private val jdbcResultSet: JdbcResultSet
): Sequence<Map<String, Any>> {
    override fun iterator(): Iterator<Map<String, Any>> =
            ResultSetMapIterator(jdbcResultSet)

}

class ResultSetMapIterator(
    val jdbcResultSet: JdbcResultSet
) : Iterator<Map<String, Any>> {

    val colNames = columnsNames(jdbcResultSet)

    override fun hasNext(): Boolean = jdbcResultSet.next()


    override fun next(): Map<String, Any> =
        colNames.mapIndexed { index, it -> it to jdbcResultSet.getObject(index + 1) }.associate { it }

}

private fun columnsNames(jdbcResultSet: JdbcResultSet): List<String> {
    val ret = mutableListOf<String>()
    for (i in 1..jdbcResultSet.metaData.columnCount) {
        ret.add(jdbcResultSet.metaData.getColumnName(i))
    }
    return ret.toList()
}

class PreparedStatementImpl(
    val jdbcPreparedStatement: JdbcPreparedStatement
) : PreparedStatement {
    override fun executeQuery(values: List<Any>): ResultSet {
        values.forEachIndexed { i, it ->
            jdbcPreparedStatement.setObject(i + 1, values[i])
        }
        return ResultSetImpl(jdbcPreparedStatement.executeQuery())
    }

    override fun execute(values: List<*>) {
        values.forEachIndexed { i, it ->
            jdbcPreparedStatement.setObject(i + 1, values[i])
        }
        jdbcPreparedStatement.execute()
    }
}




