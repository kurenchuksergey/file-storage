package com.ks.storage.file

import java.nio.ByteBuffer

internal fun ByteBuffer.fromStart() = this.position(0)
