package com.ks.storage.file.space.hierarchy

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.RandomAccessFile
import java.nio.file.Files

class HierarchyOpsTest {
    @Test
    fun `mv with children`() {
        val tempFile = Files.createTempFile("mv-file-to-non-exist-folder", "txt")
        RandomAccessFile(tempFile.toFile(), "rw").use {
            val hierarchyOps = HierarchyOps(
                HierarchySpace.initNew(
                    spaceSize = 1024 * 1024,
                    offset = 0,
                    separator = "/",
                    it.channel
                )
            )

            hierarchyOps.createFile("/testFolder/testFolder3/file1.txt", 1)
            hierarchyOps.createFile("/testFolder/testFolder3/file2.txt", 2)

            hierarchyOps.move("/testFolder", "/testFolder2")
            assertEquals(1, hierarchyOps.getFile("/testFolder2/testFolder3/file1.txt").startBlock)
            assertEquals(2, hierarchyOps.getFile("/testFolder2/testFolder3/file2.txt").startBlock)
            assertThrows<Throwable> {
                hierarchyOps.getFile("testFolder/testFolder3/file1.txt")
            }
            assertThrows<Throwable> {
                hierarchyOps.getFile("testFolder/testFolder3/file2.txt")
            }
        }
    }

    @Test
    fun `mv files to exist folder`() {
        val tempFile = Files.createTempFile("mv-file-to-exist-folder", "txt")
        RandomAccessFile(tempFile.toFile(), "rw").use {
            val hierarchyOps = HierarchyOps(
                HierarchySpace.initNew(
                    spaceSize = 1024 * 1024,
                    offset = 0,
                    separator = "/",
                    it.channel
                )
            )

            hierarchyOps.createFile("/testFolder/testFolder3/file1.txt", 1)

            hierarchyOps.createFolder("/testFolder2")
            hierarchyOps.move("/testFolder", "/testFolder2")
            assertEquals(1, hierarchyOps.getFile("/testFolder2/testFolder3/file1.txt").startBlock)
            assertThrows<Throwable> {
                hierarchyOps.getFile("testFolder/testFolder3/file1.txt")
            }
        }
    }

    @Test
    fun `not found exception`() {
        val tempFile = Files.createTempFile("delete-file-twice", "txt")
        RandomAccessFile(tempFile.toFile(), "rw").use {
            val hierarchyOps = HierarchyOps(
                HierarchySpace.initNew(
                    spaceSize = 1024 * 1024,
                    offset = 0,
                    separator = "/",
                    it.channel
                )
            )

            hierarchyOps.createFile("/file1.txt", 1)
            assertDoesNotThrow { hierarchyOps.delete("/file1.txt") }
            assertThrows<NodeNotFound> { hierarchyOps.delete("/file1.txt") }
        }
    }
}