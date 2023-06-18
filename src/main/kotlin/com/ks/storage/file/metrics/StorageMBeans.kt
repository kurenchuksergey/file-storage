package com.ks.storage.file.metrics

import java.io.Closeable
import java.lang.management.ManagementFactory
import javax.management.ObjectName
import javax.management.StandardMBean

interface StorageMBeans {
    fun getSize(): Long

    fun getFreeSpace(): Long

    fun getWritesOperation(): Int

    fun getReadsOperation(): Int
}

internal class StorageStat(
        id: String,
        private val size: Long
) : Closeable, StorageMBeans {
    private val name = ObjectName("com.ks.storage.file.metrics", "id", id)

    @JvmField var freeSpace = 0L
    @JvmField var writesOperation = 0
    @JvmField var readsOperations = 0

    init {
        ManagementFactory.getPlatformMBeanServer()
                .registerMBean(
                        StandardMBean(this, StorageMBeans::class.java, false),
                        name
                )
    }

    override fun close() {
        ManagementFactory.getPlatformMBeanServer().unregisterMBean(name)
    }

    override fun getSize(): Long = size

    override fun getFreeSpace() = freeSpace

    override fun getWritesOperation() = writesOperation

    override fun getReadsOperation() = readsOperations
}