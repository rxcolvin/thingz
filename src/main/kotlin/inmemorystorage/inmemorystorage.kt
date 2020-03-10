package inmemorystorage

import common.query.FilterQueryDef
import common.query.QueryDef
import common.storage.StorageManager

class InMemoryStorage<K, E>(
    private val keyExtractor: (E) -> K,
    private val sync: Boolean = false

) : StorageManager<K, E> {

    private val map = if (sync) java.util.concurrent.ConcurrentHashMap<K, E>() else mutableMapOf<K,E>()

    override fun getById(id: K): E = map.getOrElse(id) {
        throw Exception("Not Found")
    }

    override fun query(queryDef: QueryDef<E>): Sequence<E> {
        if (queryDef is FilterQueryDef<E>) {
            return map.values.asSequence().filter(queryDef.filter)
        }
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun queryAll(): Sequence<E> = map.values.asSequence()

    override fun create(item: E) {
        map.putIfAbsentElse(keyExtractor(item), item) {
            throw Exception("Duplicate Key: ${keyExtractor(item)}")
        }
    }

    override fun update(item: E) {
        map.putIfNotAbsentElse(keyExtractor(item), item) {
            throw Exception("Key Not Found: ${keyExtractor(item)}")
        }
    }

    override fun deleteQuery(queryDef: QueryDef<E>) {
        if (queryDef is FilterQueryDef<E>) {
            return map.values.asSequence().filter(queryDef.filter).forEach {
                deleteById(keyExtractor(it))
            }
        }
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun deleteById(id: K) {
        map.remove(id)
    }

    override fun createSchema() {
    }

    override fun describeSchema(): String {
        return ""
    }

    override fun dropSchema(silent: Boolean) {
    }

}

fun <K, V> MutableMap<K, V>.putIfAbsentElse(
    key: K,
    value: V,
    elseFunc: (K) -> Unit
) {
    if (containsKey(key)) {
        put(key, value)
    } else {
        elseFunc(key)
    }
}

fun <K, V> MutableMap<K, V>.putIfNotAbsentElse(
    key: K,
    value: V,
    elseFunc: (K) -> Unit
) {
    if (!containsKey(key)) {
        put(key, value)
    } else {
        elseFunc(key)
    }
}