package com.ks.storage.file

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class FolderNodeTest {
    @Test
    fun `wr-test with small hierarchy`() {
        val root = FolderNode()
        val testSubFolder = root.addNode("testFolder", FolderNode())
        val testSubFolder2 = root.addNode("testFolder2", FolderNode())
        val testFile = testSubFolder2.addNode("testFile", FileNode(1))

        val rawData = root.serialize()
        val serialised = FolderNode.deserialize(rawData)

        assertEquals(root, serialised, "hierarchies should be equal")
        //if not trust equals
        assertEquals(testSubFolder, serialised.getNode("testFolder"), "first empty folder should be the same")
        assertEquals(testSubFolder2, serialised.getNode("testFolder2"), "second non-empty folder should be the same")
        assertEquals(
            testFile,
            serialised.getNode<FolderNode>("testFolder2")?.getNode("testFile"),
            "file should be the same"
        )
    }


    @Test
    fun `file list should contain new node after add operation`() {
        val root = FolderNode()
        assertTrue(root.list().isEmpty())

        root.addNode("testFile", FileNode(2))
        assertIterableEquals(listOf("testFile" to FileNode(2)), root.list())
    }

    @Test
    fun `should be any exception on the same node path`(){
        val root = FolderNode()
        assertDoesNotThrow{ root.addNode("test", FileNode(1)) }
        assertThrows<Throwable> { root.addNode("test", FolderNode()) }
        assertThrows<Throwable> { root.addNode("test", FileNode(2)) }
        root.delete("test")
        assertDoesNotThrow{ root.addNode("test", FolderNode()) }
    }

    @Test
    fun `node shouldn't be present in list after delete operation`() {
        val root = FolderNode()
        root.addNode("test", FileNode(1))
        assertFalse(root.list().isEmpty())
        root.delete("test")
    }
}