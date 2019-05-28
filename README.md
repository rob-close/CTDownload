# CTDownload

A utility to download the first 4 chunks of a file from an HTTP server.  The default chunk size is 1 Mib.

## Building
On the command line, in the root of the project:

```
./gradlew build
```

Two jar files are built:
  * *CTDownload-1.0-SNAPSHOT.jar* is just the classes from this project, and no dependencies.
  * *download-1.0-SNAPSHOT-all.jar* has the classes from this project, and any dependencies.

## Running

```
java -JAR download [OPTIONS]
```

### OPTIONS
|Option|Meaning|
|:------------|:---|
|-source| This option is required.  It must be followed by a complete URL, including the scheme, for the file to download.|
|-destionation| This option is optional.   If used, it must be followed by a path and file name for the location to download to.  The path must exist.  If not used the file is downloaded to the current directory and named download.dat.|
|-parallel| When used, chunks are downloaded in parallel.  Otherwise, chunks are downloaded sequentially.|
|-help|Describes options to user.|

