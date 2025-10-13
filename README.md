# s3-application
A desktop application for interacting with S3 for uploads and downloads using Java

<!-- <img src="./img/ScreenShot01.PNG"> -->

## Features

- Manage multiple S3/S3-compatiable accounts
- Upload/Download from buckets

## Technical Overview

s3-application is built using JavaFX for a modern, responsive UI and integrates technologies like:

- **Jackson**: For JSON parsing, enabling fast processing and manipulation of JSON data.
 
## How to Use

1. Download the latest release JAR file from [Releases](https://github.com/talaatharb/s3-application/releases).
 
2. Run the application using Java:

```java
 java -jar s3-application-<version>. jar
```


## Requirements

- **Java 25+** for running the project
- **Maven 3.x** (for building the project only)
  
## How to Build

1. Clone the repository:
   ```bash
   git clone https://github.com/talaatharb/s3-application.git
   ```
2. Navigate to the project directory:
   ```bash
   cd s3-application
   ```
3. Build the project using Maven:
   ```bash
   mvn clean compile package
   ```
4. Run the application:
   ```bash
   mvn javafx:run
   ```

## Contributions
Contributions are welcome! Feel free to open issues for bugs, feature requests, or submit pull requests. Make sure to follow the contribution guidelines.
