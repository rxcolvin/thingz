package jdbcstorage

import expression.Eq
import expression.Field
import expression.Parameter
import jdbc.Connection
import jdbc.DbMap
import jdbc.DbMapBuilder
import common.meta.*
import common.query.FilterQueryDef
import common.query.QueryDef
import sql.SqlHelper
import sql.SqlStatement
import sql.Table
import common.storage.StorageManager


/**
 *
 */
private class SqlMapper<K : Any, E : Any, E_ : Any>(
    schemaName: String,
    val entityType: EntityType<E, E_, K>
) {
    val identitySqlMeta = sqlFieldMeta(entityType.identityField)
    val sqlFieldMetas =
        (entityType.fields.map { it to sqlFieldMeta(it) } + (entityType.identityField to identitySqlMeta))

    val table = Table(
        tableName = entityType.typeName,
        schemaName = schemaName,
        columnDefs = columnDefsFromSqlFieldMetas(sqlFieldMetas.map { it.second })
    )

    fun fromMap(map: DbMap): E {
        val builder = entityType.buildNew()
        sqlFieldMetas.forEach {
            //Ugly Cast Alert - is there a construct that can hide this better? A big switch that knows all the types
            // perhaps as part of sqlFieldMeta
            (it.first.setter_ as (E_, Any) -> Unit)(builder, it.second.fromDbMap(map))
        }
        return entityType.build(builder)
    }

    fun toMap(item: E): DbMap {
        val dbMapBuilder = DbMapBuilder()
        sqlFieldMetas.forEach {
            val value = (it.first.getter)(item)
            val x = (it.second.toDbMap as (Any) -> DbMap)
            val dbMap = x(value)
            dbMap.entries.forEach {
                dbMapBuilder.put(it.key, it.value)
            }
        }
        return dbMapBuilder.toMap()
    }

    val identityFieldName: String =
        entityType.identityField.field.fieldName

    fun identityValue(key: K): DbMap {
        val x = identitySqlMeta.toDbMap as (Any) -> DbMap
        return x(key)
    }
}


class SimpleJdbcStorageManager<K : Any, E : Any, E_ : Any>(
    schemaName: String,
    val entityType: EntityType<E, E_, K>,
    val sqlHelper: SqlHelper,
    val connection: Connection
) : StorageManager<K, E> {

    private val sqlMapper = SqlMapper(schemaName, entityType)

    inner class PreparedStatementHolder(
        val sqlStatement: SqlStatement
    ) {
        val preparedStatement by lazy {
            connection.prepareStatement(sqlStatement.sql)
        }

        fun execute() {
            preparedStatement.execute()
        }

        //TODO: Proper exception
        fun execute(dbMap: DbMap) =
            preparedStatement.execute(sqlStatement.parameters.map { dbMap.getOrElse(it, { throw Exception() }) })


        //TODO: Proper exception
        fun executeQuery(dbMap: DbMap = emptyMap()): Sequence<DbMap> =
            preparedStatement.executeQuery(
                sqlStatement.parameters.map {
                    dbMap.getOrElse(
                        it,
                        { throw Exception() })
                }
            ).asMapSequence()
    }

    val createStatement = PreparedStatementHolder(
        sqlHelper.createTableSql(sqlMapper.table)
    )

    //Schema may not have been created deferred until first usage
    val insertStatement = PreparedStatementHolder(
        sqlHelper.insertSql(sqlMapper.table)
    )

    //TOOD: Needs to work for compound Key type amd multi field primary keys
    val getByIdStatement = PreparedStatementHolder(
        sqlHelper.selectSql(
            table = sqlMapper.table,
            where = Eq<K>(
                Field(sqlMapper.identitySqlMeta.columnDefs[0].name),
                Parameter(sqlMapper.identitySqlMeta.columnDefs[0].name)
            )
        )
    )

    val queryAllStatement = PreparedStatementHolder(
        sqlHelper.selectSql(
            table = sqlMapper.table
        )
    )


    override fun describeSchema(): String = "???"

    override fun createSchema() {
        createStatement.execute()
    }

    override fun dropSchema(silent: Boolean) {
        try {
            connection.prepareStatement(
                sqlHelper.dropTableSql(sqlMapper.table).sql
            ).execute()
        } catch (ex: java.sql.SQLNonTransientException) {
            if (silent) {
                if (!sqlHelper.validateDropTableException(sqlMapper.table, ex)) {
                    throw ex
                }
            }
        }
    }

    //TODO: Deal with Complex Keys
    override fun getById(id: K): E =
        sqlMapper.fromMap(
            getByIdStatement.executeQuery(
                sqlMapper.identityValue(id)
            ).firstOrNull() ?: throw Exception("Not Found")
        )


    override fun create(item: E) =
        insertStatement.execute(sqlMapper.toMap(item))

    override fun update(item: E) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun query(queryDef: QueryDef<E>): Sequence<E> {
        if (queryDef is FilterQueryDef<E>) {
            return queryAll().filter(queryDef.filter)
        }
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


    override fun queryAll(): Sequence<E> =
        queryAllStatement.executeQuery().map {
            sqlMapper.fromMap(it)
        }


    override fun deleteQuery(filter: QueryDef<E>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun deleteById(id: K) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


}










