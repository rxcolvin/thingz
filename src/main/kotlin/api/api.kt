package api

import common.meta.EntityType
import common.meta.entityFromJSon
import common.meta.entityListToJson
import common.meta.entityToJson
import common.storage.StorageManager

//TODO Figure out context info - user id etc

//Allplatforms - pure koltin
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







