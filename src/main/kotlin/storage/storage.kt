package storage

import query.QueryDef

interface StorageManager<K, E> {
  fun createSchema()
  fun describeSchema(): String
  fun dropSchema()
  fun getById(id: K): E
  fun create(item: E)
  fun update(item: E)
  fun query(filter: QueryDef) : Sequence<E>
  fun deleteQuery(filter: QueryDef)
  fun deleteById(id: K)
}



