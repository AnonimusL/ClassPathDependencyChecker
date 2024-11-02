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

## How It Works

The **ClassPathDependencyChecker** operates by analyzing the bytecode of the specified main class and recursively identifying its dependencies. Here's a detailed breakdown of the process:

1. **Input Handling**:
   - The program accepts the name of the main class and an array of paths to JAR files containing class definitions. It begins by validating these inputs and initializing necessary data structures for tracking dependencies.

2. **Dependency Collection**:
   - The `checkDependencies` method initiates the dependency check by calling the `collectAllDependencies` method, passing the main class name and the paths to the JAR files. This method uses a `Set` to keep track of all dependencies found and another `Set` to track visited classes to avoid redundant checks.

3. **Recursive Dependency Resolution**:
   - The `collectAllDependencies` method recursively collects dependencies for the given class. It does this by:
     - Normalizing the class name to remove any array type prefixes.
     - Checking if the class has already been visited or if it belongs to the JDK. If either condition is true, it returns early to prevent unnecessary processing.
     - Using the `getClassDependencies` method to find direct dependencies of the current class. These dependencies are then added to the set of all dependencies.

4. **Asynchronous Processing**:
   - For each direct dependency found, a new task is created to collect its dependencies asynchronously using a cached thread pool. The method waits for all tasks to complete and checks their results to determine if all dependencies can be resolved.

5. **Bytecode Analysis**:
   - The `getClassDependencies` method retrieves the bytecode of the specified class from the provided JAR files. It uses the ASM library to analyze the class structure and collect references to other classes:
     - It visits method instructions, field instructions, and type instructions to identify dependencies.
     - The `addDependencyIfNotJdk` method ensures that only non-JDK classes are added to the dependency set, preventing unnecessary checks against standard Java libraries.

6. **JAR File Exploration**:
   - The `getClassBytes` method handles the exploration of the JAR files to locate the bytecode of the specified class. It constructs the class file path from the class name, reads the contents of the JAR, and returns the byte array representing the class.

7. **Final Result**:
   - After recursively collecting all dependencies for the main class, the program checks if all necessary dependencies are provided in the supplied JAR paths. It returns `true` if all dependencies are resolved or `false` otherwise. Any errors encountered during the analysis (e.g., missing classes) will also result in a `false` output.

## Example Workflow

1. The user specifies a main class and a list of JAR file paths as command-line arguments.
2. The program checks for dependencies, recursively resolving all classes required by the main class.
3. After completing the checks, it outputs `true` if all dependencies are present or `false` if any are missing.

## Test cases
Some test cases available at https://github.com/zulkar/internship2024_1492
