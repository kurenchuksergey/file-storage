package com.ks.storage.file

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.RandomAccessFile
import java.nio.file.Files

class HierarchySpaceTest {

    @Test
    fun `validate minimum space size`() {
        val tempFile = Files.createTempFile("validate-minimum-space-size", "txt")
        RandomAccessFile(tempFile.toFile(), "rw").use {
            assertDoesNotThrow {
                HierarchySpace.initNew(
                    spaceSize = 1024 * 1024,
                    offset = 0,
                    separator = "/",
                    it.channel
                )
            }

            assertThrows<Throwable> {
                HierarchySpace.initNew(
                    spaceSize = 1024,
                    offset = 0,
                    separator = "/",
                    it.channel
                )
            }
        }
    }

    @Test
    fun `create chain folders by creating one file`() {
        val tempFile = Files.createTempFile("folder-chain", "txt")
        RandomAccessFile(tempFile.toFile(), "rw").use {
            val space = HierarchySpace.initNew(
                spaceSize = 1024 * 1024,
                offset = 0,
                separator = "/",
                it.channel
            )

            space.createNode("/test/folder1/folder2/my-file.txt", FileNode(1))
            assertEquals(
                1,
                space.getNode<FolderNode>("test")
                    ?.getNode<FolderNode>("folder1")
                    ?.getNode<FolderNode>("folder2")
                    ?.getNode<FileNode>("my-file.txt")?.startBlock
            )
        }
    }

    @Test
    fun `successful add file into root folder`() {
        val tempFile = Files.createTempFile("file-into-root", "txt")
        RandomAccessFile(tempFile.toFile(), "rw").use {
            val space = HierarchySpace.initNew(
                spaceSize = 1024 * 1024,
                offset = 0,
                separator = "/",
                it.channel
            )

            space.createNode("/my-file.txt", FileNode(1))
            assertEquals(
                1,
                space.getNode<FileNode>("my-file.txt")?.startBlock
            )

            space.createNode("my-file2.txt", FileNode(2))
            assertEquals(
                2,
                space.getNode<FileNode>("my-file2.txt")?.startBlock
            )
            assertEquals(
                2,
                space.getNode<FileNode>("/my-file2.txt")?.startBlock
            )
        }
    }

    @Test
    fun `delete folder with children`() {
        val tempFile = Files.createTempFile("delete folder with children", "txt")
        RandomAccessFile(tempFile.toFile(), "rw").use {
            val space = HierarchySpace.initNew(
                spaceSize = 1024 * 1024,
                offset = 0,
                separator = "/",
                it.channel
            )

            space.createNode("/testFolder/my-file.txt", FileNode(1))
            space.deleteNode("/testFolder")
            assertNull(space.getNode<FileNode>("/testFolder/my-file.txt"))
        }
    }

    @Test
    fun `read hierarchy after close-open source file`() {
        val tempFile = Files.createTempFile("file-into-root", "txt")
        RandomAccessFile(tempFile.toFile(), "rw").use {
            HierarchySpace.initNew(
                spaceSize = 1024 * 1024,
                offset = 0,
                separator = "/",
                it.channel
            ).use { space -> space.createNode("/my-file.txt", FileNode(23)) }
        }

        RandomAccessFile(tempFile.toFile(), "rw").use {
            HierarchySpace.load(
                spaceSize = 1024 * 1024,
                offset = 0,
                separator = "/",
                it.channel
            ).use { space ->
                assertEquals(23, space.getNode<FileNode>("my-file.txt")?.startBlock)
            }
        }
    }
}
