package api

import logging.Logger
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.path
import org.http4k.server.KtorCIO
import org.http4k.server.asServer

// JVM only

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

    val app = org.http4k.routing.routes(*appRoutes.toTypedArray())

    val appServer = app.asServer(KtorCIO(port = portNo))

    val lifecycleRoutes = listOf(
        "start" bind Method.POST to { request -> startOp(request) },
        "stop" bind Method.POST to { request -> stopOp(request) },
        "exit" bind Method.POST to { request -> exitOp(request) },
        "status" bind Method.GET to { request -> status(request) }
    )

    val adminApp = "/_admin" bind org.http4k.routing.routes(
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
        Response(Status.OK).body("OK")
    } catch (ex: Exception) {
        Response(Status.BAD_REQUEST).body(ex.message ?: "No message!")
    }

    fun stopOp(
        request: Request
    ): Response = try {
        appServer.stop()
        Response(Status.OK).body("OK")
    } catch (ex: Exception) {
        Response(Status.BAD_REQUEST).body(ex.message ?: "No message!")
    }

    fun exitOp(
        request: Request
    ): Response = try {
        stop()
        exiting()
        Response(Status.OK).body("OK")
    } catch (ex: Exception) {
        exiting()
        Response(Status.BAD_REQUEST).body(ex.message ?: "No message!")
    }

    fun status(
        request: Request
    ): Response = try {
        Response(Status.OK).body(if (started) "STARTED" else "STOPPED")
    } catch (ex: Exception) {
        Response(Status.BAD_REQUEST).body(ex.message ?: "No message!")
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
            return Response(Status.BAD_REQUEST).body(ex.message ?: "No message!")
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
            return Response(Status.BAD_REQUEST).body(ex.message ?: "No message!")
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
            return Response(Status.BAD_REQUEST).body(ex.message ?: "No message!")
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
            Response(Status.BAD_REQUEST).body(ex.message ?: "No message!")
        }

    fun <E : Any, E_ : Any, K : Any> dropSchema(
        assetAPI: AssetAPI<E, E_, K>,
        request: Request
    ): Response =
        try {
            val resp = assetAPI.dropSchema()
            Response(statusFromInt(resp.status)).body(resp.body)
        } catch (ex: Exception) {
            Response(Status.BAD_REQUEST).body(ex.message ?: "No message!")
        }

    fun statusFromInt(n: Int) =
        when (n) {
            200 -> Status.OK
            else -> Status.INTERNAL_SERVER_ERROR
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

fun routes(list: List<RoutingHttpHandler>): RoutingHttpHandler = org.http4k.routing.routes(*list.toTypedArray())
