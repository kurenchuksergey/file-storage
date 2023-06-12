package com.ks.storage.file.api.exceptions

enum class Space {
    STORAGE_META, HIERARCHY, BLOCKS
}

open class StorageException(message: String) : RuntimeException(message)

class NoEmptySpace(space: Space) : StorageException("No empty space in $space")