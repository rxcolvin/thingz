package application

import api.APIHttpEngine
import api.AssetAPI
import jdbc.JdbcHelper
import jdbcstorage.SimpleJdbcStorageManager
import meta.EntityType

class Application(
    val jdbcHelper: JdbcHelper,
    val assetTypes: List<EntityType<*, *, *>>
) {
    val cxn = jdbcHelper.connection(databasename = "testone")
    val sqlHelper = jdbcHelper.sqlHelper()


    fun <E : Any, E_ : Any, K : Any> buildOne(entityType: EntityType<E, E_, K>): AssetAPI<E, E_, K> {
        val storageManager = SimpleJdbcStorageManager(
            connection = cxn,
            entityType = entityType,
            sqlHelper = sqlHelper,
            schemaName = ""
        )

        return AssetAPI(
            entityType = entityType,
            storageManager = storageManager
        )
    }

    val assetAPIs = assetTypes.map {
        buildOne(it)
    }

    val engine = APIHttpEngine(
        assetAPIs = assetAPIs
    )

    fun start() {
        engine.start()
    }
}


