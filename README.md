# File System in a Single File Container

## Source Task

Please design and implement a library to emulate the typical file system API, which will actually store files and
folders in one single container file. Do not compress the data, minimize used RAM, minimize disk operations.

Create-write-read-append-delete-rename-move operations should be supported.
Keep balance between container file size and used resources.
Provide a metric and routine for container file maintenance - if required.

Provide unit tests for all features.

Include at least one complete functional test: store all project tree, erase 70% of all files, compact container, add
all files again into new subfolder. Close container file, reopen and verify all content.
...
Do not use any libs except unit test or logging libs.

Time is not limited, but you need to follow your own estimate or provide new.
Keep your approach simple enough to finish the entire task in a total of about 8 hours of work.

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

- **Hierarchy Space** consists of a prefix tree, specifically a trie, which represents the hierarchy of files and
  directories in the file system. This structure provide effective way for move operation

- **Blocks Space** is divided into two areas: the **Use Blocks Map** and the **Raw data blocks**.
    - **Used Blocks Map** is an area that keeps track of the availability of blocks in the file system. It maintains a
      map of used/free blocks, indicating which blocks are available for use.
    - **Raw data blocks** stores the actual data content of the files. These blocks have a fixed length and hold the
      file contents.
      By using fixed-length data blocks, the file system addresses file fragmentation without the need for
      defragmentation algorithms.

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
