package jdbcstorage

import expression.Eq
import expression.Field
import expression.Parameter
import jdbc.Connection
import meta.Builder
import meta.EntityMapper
import meta.SqlEntityMeta
import sql.ColumnDef
import sql.SqlHelper
import sql.SqlType
import sql.Table
import storage.StorageManager
import java.lang.Exception


class JdbcStorageManager<K, E:Any, E_: Builder<E>>(
    val sqlHelperFactory: SqlHelper.Factory,
    val sqlEntityMeta: SqlEntityMeta<E>,
    val entityMapper: EntityMapper<E, E_>,
    val connection: Connection
) : StorageManager<K, E> {
    val sqlHelper = sqlHelperFactory.create(
        Table(
            tableName = sqlEntityMeta.entityType.typeName,
            schemaName = "",
            columnDefs = sqlEntityMeta.fields.flatMap { it.columnDefs }
        )
    )
    val insertStatement = connection.prepareStatement(
        sqlHelper.insertSql()
    )

    override fun createSchema() {
        connection.prepareStatement(
            sqlHelper.createTableSql()
        ).execute()
    }

    override fun getById(id: K): E {
        val sql = sqlHelper.selectSql(
            where = Eq<K>(
                Field(sqlEntityMeta.identitySqlField.columnDefs.first().name),
                Parameter(sqlEntityMeta.identitySqlField.columnDefs.first().name)
            )
        )
        val ps = connection.prepareStatement(sql.first)
        val rs = ps.executeQuery(sql.second)
        val dbMap = rs.asMapSequence().firstOrNull() ?: throw Exception()
        val entityMap = sqlEntityMeta.fields.map {
            Pair(it.field.fieldName, it.fromDbMap(dbMap))
        }.toMap()
        val builder = entityMapper.builder()
        entityMapper.fieldMappers.forEach { (it.setter_ as (E_, Any?)->Unit)(builder, entityMap[it.field.fieldName]) }
        return builder.create()
    }

    override fun put(item: E) {
        val map = entityMapper.fieldMappers.map { Pair(it.field.fieldName, it.getter(item)) }.toMap()
        val dbMap = sqlEntityMeta.fields.flatMap {
            (it.toDbMap as (Any?) -> List<Pair<String, Any>>)(map.getOrElse(it.field.fieldName) { throw Exception() })
        }.associate { it }
        insertStatement.execute(sqlHelper.table.columnDefs.map { dbMap.getOrElse(it.name) { throw Exception() } })
    }

}



