package com.ks.storage.file.api

import com.ks.storage.file.space.hierarchy.NodeNotFound
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path

class StorageTest {

    @Test
    fun `file import and export`() {
        val sourceData = FileTest::class.java.classLoader.getResourceAsStream("fixture.jpg").use {
            readResource(it!!)
        }
        val file = File("testFolder/testFile", sourceData)
        testStorage.createFile(file)
        val storageFile = testStorage.getFile("testFolder/testFile")

        val output = Files.createTempFile("fixture-import-export-to-storage", ".jpg")
        storageFile.export(output)
        assertTrue(sourceData.contentEquals(readFile(output)))
    }

    @Test
    fun `move file from one folder to another and compare`() {
        val sourceData = FileTest::class.java.classLoader.getResourceAsStream("fixture.jpg").use {
            readResource(it!!)
        }
        val file = File("testFolder/testFile", sourceData)
        testStorage.createFile(file)
        testStorage.move("testFolder/testFile", "test2/testFolder2/test.jpg")
        val storageFile = testStorage.getFile("test2/testFolder2/test.jpg")

        assertTrue(sourceData.contentEquals(storageFile.content))
    }

    @Test
    fun `read after delete`() {
        val sourceData = FileTest::class.java.classLoader.getResourceAsStream("fixture.jpg").use {
            readResource(it!!)
        }
        val file = File("testFolder/testFile", sourceData)
        testStorage.createFile(file)
        testStorage.delete(file.path)
        assertThrows<NodeNotFound> { testStorage.getFile(file.path) }
    }

    @Test
    fun `init - fill - close - load - check`(){
        val path = Files.createTempFile("storage", ".data")
        val localStorage = Storage.new()
            .efficient()
            .path(path)
            .size(10 * 1024 * 1024)
            .smallHierarchy()
            .build()

        val sourceData = FileTest::class.java.classLoader.getResourceAsStream("fixture.jpg").use {
            readResource(it!!)
        }
        localStorage.use {
            val file = File("/testFolder/testFile", sourceData)
            localStorage.createFile(file)
        }

        val loadedStorage = Storage(path)
        val loadedFile = loadedStorage.getFile("/testFolder/testFile")
        assertTrue(sourceData.contentEquals(loadedFile.content))
    }

    private fun readFile(path: Path): ByteArray {
        val file = path.toFile()
        return FileInputStream(file).use {
            val fileData = ByteArray(file.length().toInt())
            it.read(fileData)
            it.close()
            fileData
        }
    }

    private fun readResource(stream: InputStream): ByteArray {
        val buffer = ByteArrayOutputStream()

        var nRead: Int
        val data = ByteArray(16384)

        while (stream.read(data, 0, data.size).also { nRead = it } != -1) {
            buffer.write(data, 0, nRead)
        }

        return buffer.toByteArray()
    }

    companion object {
        val testStorage = Storage.new()
            .efficient()
            .path(Files.createTempFile("storage", ".data"))
            .size(10 * 1024 * 1024)
            .smallHierarchy()
            .build()
    }
}