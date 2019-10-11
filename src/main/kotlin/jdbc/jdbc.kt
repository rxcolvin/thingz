package jdbc

import java.sql.Connection as JdbcConnection
import java.sql.Driver as JdbcDriver
import java.sql.DriverManager as JdbcDriverManager
import java.sql.PreparedStatement as JdbcPreparedStatement
import java.sql.ResultSet as JdbcResultSet
typealias ColName = String
typealias DbMap = Map<ColName, DbValue>
typealias DbMapBuilder = MutableMap<ColName, DbValue>

class DbValue private constructor(
    val value: Any?
) {
    constructor(value: String?) : this(value as Any?)
    constructor(value: Int?) : this(value as Any?)
    constructor() : this(null as Any?)

    fun asString() = value as String?
    fun asInt() = value as Int?
}

fun dbValue(value: Any?)  = when(value){
    is String -> DbValue(value)
    is Int -> DbValue(value)
    else -> throw Exception()
}

interface Connection {
    fun prepareStatement(sql: String): PreparedStatement
}

interface ResultSet {
    fun asMapSequence(): Sequence<DbMap>
    fun asMap() : DbMap?
}

interface PreparedStatement {
    fun executeQuery(values: List<DbValue>) : ResultSet
    fun executeQuery(value: DbValue) : ResultSet
    fun execute(values: List<DbValue> = emptyList())
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
    override fun asMapSequence(): Sequence<DbMap> =
            ResultSetMapIterator(jdbcResultSet).asSequence()

    override fun asMap(): DbMap? = asMapSequence().firstOrNull()
}

class ResultSetMapIterator(
    val jdbcResultSet: JdbcResultSet
) : Iterator<DbMap> {

    val colNames = columnsNames(jdbcResultSet)

    override fun hasNext(): Boolean = jdbcResultSet.next()


    override fun next(): DbMap =
        colNames.mapIndexed { index, it -> it to dbValue(jdbcResultSet.getObject(index + 1)) }.associate { it }

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
    override fun executeQuery(values: List<DbValue>): ResultSet {
        values.forEachIndexed { i, _ ->
            jdbcPreparedStatement.setObject(i + 1, values[i].value)
        }
        return ResultSetImpl(jdbcPreparedStatement.executeQuery())
    }

    override fun executeQuery(value: DbValue) = executeQuery(listOf(value))

    override fun execute(values: List<DbValue>) {
        values.forEachIndexed { i, _ ->
            jdbcPreparedStatement.setObject(i + 1, values[i].value)
        }
        jdbcPreparedStatement.execute()
    }
}




