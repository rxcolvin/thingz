package jdbcstorage

import expression.Eq
import expression.Field
import expression.Parameter
import jdbc.Connection
import jdbc.DbMap
import jdbc.DbValue
import jdbc.PreparedStatement
import meta.*
import query.QueryDef
import sql.SqlHelper
import sql.SqlStatement
import sql.Table
import storage.StorageManager



//TODO: Replace
class SimpleDbMapper<K : Any, E : Any, E_ : Any>(
    schemaName: String,
    val entityType: EntityType<E, E_, K>
 )  {
    private val sqlFieldMetas = entityType.fields.map { it to sqlFieldMeta(it) }
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
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    val identityFieldName: String =
        entityType.identityField.field.fieldName

    fun identityValue(key: K): DbValue {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}


class SimpleJdbcStorageManager<K : Any, E : Any, E_ : Any>(
    val dbMapper: SimpleDbMapper<K, E, E_>,
    val sqlHelper: SqlHelper,
    val connection: Connection
) : StorageManager<K, E> {

    inner class PreparedStatementHolder(
        val sqlStatement: SqlStatement
    ) {
        val preparedStatement by lazy {
            connection.prepareStatement(sqlStatement.sql)
        }

        fun execute(dbMap: DbMap) =
            preparedStatement.execute(sqlStatement.parameters.map { dbMap.getOrElse(it, { throw Exception() }) })


        fun executeQuery(dbMap: DbMap): Sequence<DbMap> =
            preparedStatement.executeQuery(sqlStatement.parameters.map {
                dbMap.getOrElse(
                    it,
                    { throw Exception() })
            }).asMapSequence()
    }


    //Schema may not have been created deferred until first usage
    val insertStatement = PreparedStatementHolder(
        sqlHelper.insertSql(dbMapper.table)
    )

    val getByIdStatement = PreparedStatementHolder(
        sqlHelper.selectSql(
            table = dbMapper.table,
            where = Eq<K>(
                Field(dbMapper.identityFieldName),
                Parameter(dbMapper.identityFieldName)
            )
        )
    )


    override fun describeSchema(): String = "???"

    override fun createSchema() {
        connection.prepareStatement(
            sqlHelper.createTableSql(dbMapper.table).sql
        ).execute()
    }

    override fun dropSchema() {
        connection.prepareStatement(
            sqlHelper.dropTableSql(dbMapper.table).sql
        ).execute()
    }

    //TODO: Deal with Complex Keys
    override fun getById(id: K): E =
        dbMapper.fromMap(
            getByIdStatement.executeQuery(
                mapOf(dbMapper.identityFieldName to dbMapper.identityValue(id))
            ).firstOrNull() ?: throw Exception()
        )


    override fun create(item: E) =
        insertStatement.execute(dbMapper.toMap(item))

    override fun update(item: E) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun query(filter: QueryDef): Sequence<E> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun deleteQuery(filter: QueryDef) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun deleteById(id: K) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


}










