package jdbcstorage

import expression.Eq
import expression.Field
import expression.Parameter
import jdbc.Connection
import meta.Builder
import meta.EntityMapper
import meta.SqlEntityMeta
import query.QueryDef
import sql.SqlHelper
import sql.Table
import storage.StorageManager


class JdbcStorageManager<K: Any, E:Any, E_: Builder<E>>(
    val sqlHelperFactory: SqlHelper.Factory,
    val sqlEntityMeta: SqlEntityMeta<E, K>,
    val entityMapper: EntityMapper<E, E_>,
    val connection: Connection
) : StorageManager<K, E> {
    val sqlHelper = sqlHelperFactory.create(
        Table(
            tableName = sqlEntityMeta.entityType.typeName,
            schemaName = "",
            columnDefs = sqlEntityMeta.fields.flatMap { it.columnDefs },
            primaryColumnDefs = sqlEntityMeta.identitySqlField.columnDefs
        )
    )
    //Schema may not have been created deferred until first usage
    val insertStatement by lazy { connection.prepareStatement(
        sqlHelper.insertSql()
    )}

    override fun describeSchema(): String = sqlHelper.createTableSql()

    override fun createSchema() {
        connection.prepareStatement(
            sqlHelper.createTableSql()
        ).execute()
    }

    override fun dropSchema() {
        connection.prepareStatement(
            sqlHelper.dropTableSql()
        ).execute()
    }

    //TODO: Deal with Complex Keys
    override fun getById(id: K): E {
        val (sql, params) = sqlHelper.selectSql(
            where = Eq<K>(
                Field(sqlEntityMeta.identitySqlField.columnDefs.first().name),
                Parameter(sqlEntityMeta.identitySqlField.columnDefs.first().name)
            )
        )
        val ps = connection.prepareStatement(sql)
        val rs = ps.executeQuery(listOf(sqlEntityMeta.identitySqlField.toDbMap(id)[0].second))
        val dbMap = rs.asMapSequence().firstOrNull() ?: throw Exception()
        val entityMap = sqlEntityMeta.allFields.map {
            Pair(it.field.fieldName, it.fromDbMap(dbMap))
        }.toMap()
        val builder = entityMapper.builder()
        entityMapper.fieldMappers.forEach { (it.setter_ as (E_, Any?)->Unit)(builder, entityMap[it.field.fieldName]) }
        return builder.create()
    }

    override fun create(item: E) {
        val map = entityMapper.fieldMappers.map { Pair(it.field.fieldName, it.getter(item)) }.toMap()
        val dbMap = sqlEntityMeta.allFields.flatMap {
            (it.toDbMap as (Any?) -> List<Pair<String, Any>>)(map.getOrElse(it.field.fieldName) { throw Exception() })
        }.associate { it }
        insertStatement.execute(sqlHelper.table.allColumnDefs.map {
            dbMap.getOrElse(it.name) {
                throw Exception() }
        })
    }

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



