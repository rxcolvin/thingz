package storage

interface StorageManager<K, E> {
  fun createSchema()
  fun getById(id: K): E
  fun put(item: E)
}



