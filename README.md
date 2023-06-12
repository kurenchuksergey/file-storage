# File System in a Single File Container

## Overview
This project demonstrates the implementation of a custom file system that operates within a single file container.

## Current Limitations
- Maximum data volume: Approximately 2GB

## Usage Example
```
// Create a new file system instance
Storage storage = Storage.newStorage()
        .efficient()
        .path(Files.createTempFile("storage", ".data"))
        .size(10 * 1024 * 1024)
        .smallHierarchy()
        .build();

        File sourceFile = new File("/testFolder/test.jpg", sourceData);
        storage.createFile(sourceFile);
        //localStorage.list("/testFolder") - will be available soon
        storage.delete("/testFolder/test.jpg");

```

## File System Architecture

The file system is composed of three main spaces: **Meta Space**, **File Hierarchy Space**, and **Blocks Space**.


- **Meta Space** is a fixed-size space that contains information about the sizes of the other two spaces.

- **Hierarchy Space** consists of a prefix tree, specifically a trie, which represents the hierarchy of files and directories in the file system. This structure provide effective way for move operation

- **Blocks Space** is divided into two areas: the **Free Blocks Map** and the **Raw data blocks**.
    -  **Free Blocks Map** is an area that keeps track of the availability of blocks in the file system. It maintains a map of free blocks, indicating which blocks are available for use.
    -  **Raw data blocks** stores the actual data content of the files. These blocks have a fixed length and hold the file contents.
    By using fixed-length data blocks, the file system addresses file fragmentation without the need for defragmentation algorithms.

```

+-----------------------------------+
|            Meta Space             |
|-----------------------------------|
|       File Hierarchy Space        |
|-----------------------------------|
|   Free Blocks Map |   Data Blocks |
|               ...                 |
|-----------------------------------|
```
