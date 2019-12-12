package api

import logging.Logger
import common.meta.EntityType
import common.meta.entityFromJSon
import common.meta.entityListToJson
import common.meta.entityToJson
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.*
import org.http4k.routing.routes
import org.http4k.server.KtorCIO
import org.http4k.server.asServer
import common.storage.StorageManager

//TODO Figure out context info - user id etc

/**
 * Represents an Engine independent interaction with an asset API API
 */
class AssetAPI<E : Any, E_ : Any, K : Any>(
    val entityType: EntityType<E, E_, K>,
    val storageManager: StorageManager<K, E>
) {
    fun getById(keyString: String): APIResponse {
        val key = entityType.identityField.field.type.fromString(keyString)
        return try {
            val item = storageManager.getById(key)
            APIResponse(entityToJson(item, entityType))

        } catch (ex: Exception) {
            APIResponse(ex.message ?: "ERROR", 400)
        }
    }

    fun query(paramGetter: (String) -> String?): APIResponse {
        return try {
            val item = storageManager.queryAll()
            APIResponse(entityListToJson(item.toList() as List<Any>, entityType as EntityType<Any, Any, Any>))

        } catch (ex: Exception) {
            APIResponse(ex.message ?: "ERROR", 400)
        }
    }


    fun create(payload: String): APIResponse {
        return try {
            val entity = entityFromJSon(payload, entityType)
            storageManager.create(entity)
            APIResponse("OK")

        } catch (ex: Exception) {
            ex.printStackTrace()
            APIResponse(ex.message ?: "ERROR", 400)
        }
    }


    fun createSchema(): APIResponse =
        try {
            storageManager.createSchema()
            APIResponse("OK")
        } catch (ex: Exception) {
            APIResponse(ex.message ?: "ERROR", 400)
        }

    fun dropSchema(): APIResponse =
        try {
            storageManager.dropSchema()
            APIResponse("OK")
        } catch (ex: Exception) {
            APIResponse(ex.message ?: "ERROR", 400)
        }


}


data class APIResponse(
    val body: String,
    val status: Int = 200
)

class APIException(
    val code: Int,
    override val message: String,
    val token: String
) : Exception(
)

class ViewAPI


class APIHttp4KEngine(
    val assetAPIs: List<AssetAPI<*, *, *>>,
    val portNo: Int = 8080,
    val adminPortNo: Int = 8001,
    val startApp: Boolean = true,
    val logger: Logger = Logger(name = "APIHttp4KEngine"),
    private val exiting: () -> Unit
) {

    var started = false

    val appRoutes = assetAPIs.map { api ->
        listOf(
            api.entityType.typeName.toLowerCase() + "/{id}"
                    bind Method.GET to { request -> getById(api, request) },
            api.entityType.typeName.toLowerCase()
                    bind Method.GET to { request -> query(api, request) },
            api.entityType.typeName.toLowerCase()
                    bind Method.POST to { request -> create(api, request) }
        )
    }.flatten()

    val createSchemaRoutes = assetAPIs.map { api ->
        api.entityType.typeName.toLowerCase() bind Method.POST to { request -> createSchema(api, request) }
    }

    val dropSchemaRoutes = assetAPIs.map { api ->
        api.entityType.typeName.toLowerCase() bind Method.GET to { request -> dropSchema(api, request) }
    }

    val app = routes(*appRoutes.toTypedArray())

    val appServer = app.asServer(KtorCIO(port = portNo))

    val lifecycleRoutes = listOf(
        "start" bind Method.POST to { request -> startOp(request) },
        "stop" bind Method.POST to { request -> stopOp(request) },
        "exit" bind Method.POST to { request -> exitOp(request) },
        "status" bind Method.GET to { request -> status(request) }
    )

    val adminApp = "/_admin" bind routes(
        "/createschema" bind routes(createSchemaRoutes),
        "/dropschema" bind routes(dropSchemaRoutes),
        "/lifecycle" bind routes(lifecycleRoutes)
    )

    val adminAppServer = adminApp.asServer(KtorCIO(port = adminPortNo))


    fun startOp(
        request: Request
    ): Response = try {
        appServer.start()
        started = true
        Response(OK).body("OK")
    } catch (ex: Exception) {
        Response(BAD_REQUEST).body(ex.message ?: "No message!")
    }

    fun stopOp(
        request: Request
    ): Response = try {
        appServer.stop()
        Response(OK).body("OK")
    } catch (ex: Exception) {
        Response(BAD_REQUEST).body(ex.message ?: "No message!")
    }

    fun exitOp(
        request: Request
    ): Response = try {
        stop()
        exiting()
        Response(OK).body("OK")
    } catch (ex: Exception) {
        exiting()
        Response(BAD_REQUEST).body(ex.message ?: "No message!")
    }

    fun status(
        request: Request
    ): Response = try {
        Response(OK).body(if (started) "STARTED" else "STOPPED")
    } catch (ex: Exception) {
        Response(BAD_REQUEST).body(ex.message ?: "No message!")
    }

    fun <E : Any, E_ : Any, K : Any> create(
        assetAPI: AssetAPI<E, E_, K>,
        request: Request
    ): Response {
        try {
            val payload = request.bodyString() as String
            val resp = assetAPI.create(payload)
            return Response(statusFromInt(resp.status)).body(resp.body)
        } catch (ex: Exception) {
            return Response(BAD_REQUEST).body(ex.message ?: "No message!")
        }
    }

    fun <E : Any, E_ : Any, K : Any> getById(
        assetAPI: AssetAPI<E, E_, K>,
        request: Request
    ): Response {
        try {
            val idString = request.path("id") as String
            val resp = assetAPI.getById(idString)
            return Response(statusFromInt(resp.status)).body(resp.body)
        } catch (ex: Exception) {
            return Response(BAD_REQUEST).body(ex.message ?: "No message!")
        }
    }

    fun <E : Any, E_ : Any, K : Any> query(
        assetAPI: AssetAPI<E, E_, K>,
        request: Request
    ): Response {
        try {
            val resp = assetAPI.query(paramGetter =  {request.query(it)})
            return Response(statusFromInt(resp.status)).body(resp.body)
        } catch (ex: Exception) {
            return Response(BAD_REQUEST).body(ex.message ?: "No message!")
        }
    }



    fun <E : Any, E_ : Any, K : Any> createSchema(
        assetAPI: AssetAPI<E, E_, K>,
        request: Request
    ): Response =
        try {
            val resp = assetAPI.createSchema()
            Response(statusFromInt(resp.status)).body(resp.body)
        } catch (ex: Exception) {
            Response(BAD_REQUEST).body(ex.message ?: "No message!")
        }

    fun <E : Any, E_ : Any, K : Any> dropSchema(
        assetAPI: AssetAPI<E, E_, K>,
        request: Request
    ): Response =
        try {
            val resp = assetAPI.dropSchema()
            Response(statusFromInt(resp.status)).body(resp.body)
        } catch (ex: Exception) {
            Response(BAD_REQUEST).body(ex.message ?: "No message!")
        }

    fun statusFromInt(n: Int) =
        when (n) {
            200 -> OK
            else -> INTERNAL_SERVER_ERROR
        }

    fun start() {
        adminAppServer.start()
        if (startApp) {
            appServer.start()
            started = true
        } else {
            started = false
        }
    }

    fun stop() {
        appServer.stop()
        adminAppServer.start()
    }
}

fun routes(list: List<RoutingHttpHandler>): RoutingHttpHandler = routes(*list.toTypedArray())





