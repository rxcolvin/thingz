package common.storage

import common.query.QueryDef

interface QueryManager<K, E> {
  fun getById(id: K): E
  fun query(filter: QueryDef<E>) : Sequence<E>
  fun queryAll() : Sequence<E>
}

interface UpdateManager<K, E> {
  fun create(item: E)
  fun update(item: E)
  fun deleteQuery(filter: QueryDef<E>)
  fun deleteById(id: K)
}

interface DataManager<K,E> : QueryManager<K,E>, UpdateManager<K,E>

interface StorageManager<K, E> : DataManager<K,E> {
  fun createSchema()
  fun describeSchema(): String
  fun dropSchema(silent: Boolean = false)
}



