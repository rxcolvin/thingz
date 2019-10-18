package api

import meta.EntityType
import meta.toJson
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.Status.Companion.OK
import org.http4k.lens.Path
import org.http4k.routing.bind
import org.http4k.routing.path
import org.http4k.routing.routes
import org.http4k.server.KtorCIO
import org.http4k.server.asServer
import storage.StorageManager

//TODO Figure out context info - user id etc

class AssetAPI<E : Any, E_ : Any, K : Any>(
    val entityType: EntityType<E, E_, K>,
    val storageManager: StorageManager<K, E>
) {
    fun getById(keyString: String): APIResponse {
        val key = entityType.identityField.field.type.fromString(keyString)
        return try {
            val item = storageManager.getById(key)
            APIResponse(toJson(item, entityType))

        } catch (ex: Exception) {
            APIResponse(ex.message ?: "ERROR", 200)
        }

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


class APIHttpEngine(
    val assetAPIs: List<AssetAPI<*, *, *>>,
    val portNo: Int = 8080
) {
    val idLens = Path.fixed("id")

    val list = assetAPIs.map { api ->
        listOf(
            api.entityType.typeName.toLowerCase() + "/{id}" bind Method.GET to { request -> getById(api, request) }
        )
    }.flatMap { it }.toTypedArray()

    val app = routes(*list)

    val appServer = app.asServer(KtorCIO(port = portNo))

    fun <E:Any, E_:Any, K:Any> getById(assetAPI: AssetAPI<E, E_, K>, request: Request): Response {
        try {
            val idString = request.path("id") as String
             val resp = assetAPI.getById(idString)
            return Response(statusFromInt(resp.status)).body(resp.body)
        } catch(ex: Exception) {
            return Response(BAD_REQUEST).body(ex.message ?: "No message!")
        }
    }

    fun statusFromInt(n: Int) =
        when (n) {
            200 -> OK
            else -> INTERNAL_SERVER_ERROR
        }

    fun start(block: Boolean = true) {
        appServer.start()
        if (block) {
            appServer.block()
        }
    }
    fun stop() {
        appServer.stop()
    }
}




