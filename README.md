# ClassPathDependencyChecker

## Project Description
This project is a task for the JetBrains Internship program, aimed at developing a tool to verify if a specified main class can be executed with a given set of JAR dependencies. The tool, **ClassPathDependencyChecker**, checks whether the provided classpath, composed of various JAR files, contains all necessary dependencies to successfully run the main class.

## Features
- **Input**: Accepts the name of the main class and a list of paths to JAR files.
- **Output**: Outputs a result to `stdout`, indicating whether the classpath is sufficient for executing the specified main class.
- **Assumptions**:
  - Ignores dependencies introduced via runtime reflection.
  - Ignores binary class incompatibilities (e.g., method/type mismatches).
  - Only compile-time dependencies are considered.

## Technical Requirements
- **Language**: Java or Kotlin
- **Libraries**: Use of third-party libraries is permitted if needed to facilitate dependency resolution.

## Getting Started
1. **Clone the Repository**:
   ```bash
   git clone https://github.com/AnonimusL/ClassPathDependencyChecker.git
