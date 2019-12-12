package application

import api.APIHttp4KEngine
import api.AssetAPI
import jdbc.JdbcHelper
import jdbcstorage.SimpleJdbcStorageManager
import common.meta.EntityType
import kotlin.concurrent.thread

class Application(
    val jdbcHelper: JdbcHelper,
    val assetTypes: List<EntityType<*, *, *>>
) {
    val cxn = jdbcHelper.connection(databasename = "testone")
    val sqlHelper = jdbcHelper.sqlHelper()


    private fun <E : Any, E_ : Any, K : Any> buildOne(entityType: EntityType<E, E_, K>): AssetAPI<E, E_, K> {
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

    val engine = APIHttp4KEngine(
        assetAPIs = assetAPIs,
        exiting = {
            thread(start = true) {
                Thread.sleep(500)
                System.exit(0)}
            }
     )

    fun start() {
        engine.start()
        Thread.currentThread().join()
    }
}


