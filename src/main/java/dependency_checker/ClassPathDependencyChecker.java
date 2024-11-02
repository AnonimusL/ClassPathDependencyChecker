package dependency_checker;

import org.objectweb.asm.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

public class ClassPathDependencyChecker {

    private final ExecutorService executorService = Executors.newCachedThreadPool();

    /**
     * Checks dependencies for the specified main class
     *
     * @param mainClassName The fully qualified name of the main class
     * @param jarPaths An array of paths to JAR files to search for class files
     * @return true if all needed jars are provided, otherwise false
     * @throws IOException
     */
    public boolean checkDependencies(String mainClassName, String[] jarPaths) throws IOException {
        Set<String> allDependencies = new HashSet<>();
        Set<String> visitedClasses = new HashSet<>();

        try {
            return collectAllDependencies(mainClassName, jarPaths, allDependencies, visitedClasses);
        } finally {
            executorService.shutdown();
        }
    }

    /**
     * Helper method recursively collects dependencies for a given class
     *
     * @param className The current class name being analyzed
     * @param jarPaths An array of paths to JAR files
     * @param allDependencies A set to collect all found dependencies
     * @param visitedClasses A set to track classes that have already been visited
     * @throws IOException
     */
    private boolean collectAllDependencies(String className, String[] jarPaths, Set<String> allDependencies, Set<String> visitedClasses) throws IOException {
        className = removeArrayPrefix(className);

        if (visitedClasses.contains(className) || isJdkClass(className)) {
            return true;
        }
        visitedClasses.add(className);

        Set<String> directDependencies = getClassDependencies(className, jarPaths);
        allDependencies.addAll(directDependencies);

        List<Callable<Boolean>> tasks = new ArrayList<>();
        for (String dependency : directDependencies) {
            if (!visitedClasses.contains(dependency)) {
                tasks.add(() -> collectAllDependencies(dependency, jarPaths, allDependencies, visitedClasses));
            }
        }

        try {
            for (Future<Boolean> result : executorService.invokeAll(tasks)) {
                if (!result.get()) {
                    return false;
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            return false;
        }
        return true;
    }

    /**
     *  Method analyzes a single class and returns its dependencies
     *
     * @param className The name of the class being analyzed
     * @param jarPaths An array of paths to JAR files
     * @return The collected dependencies are returned
     * @throws IOException
     */
    private static Set<String> getClassDependencies(String className, String[] jarPaths) throws IOException {
        Set<String> dependencies = new HashSet<>();
        byte[] classBytes = getClassBytes(className, jarPaths);

        if (classBytes == null) {
            return null;
        }

        ClassReader classReader = new ClassReader(classBytes);
        classReader.accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                        addDependencyIfNotJdk(owner);
                    }

                    @Override
                    public void visitTypeInsn(int opcode, String type) {
                        addDependencyIfNotJdk(type);
                    }

                    @Override
                    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
                        addDependencyIfNotJdk(owner);
                        addDependencyIfNotJdk(descriptor);
                    }

                    private void addDependencyIfNotJdk(String internalName) {
                        String dependency = internalName.replace('/', '.');
                        dependency = removeArrayPrefix(dependency);
                        if (!isJdkClass(dependency)) {
                            dependencies.add(dependency);
                        }
                    }
                };
            }
        }, 0);
        return dependencies;
    }

    /**
     * The method removes prefixes that indicate an array type and formats the class name correctly
     *
     * @param typeName The name of the class, which may include array and class prefixes
     * @return The cleaned-up name is returned
     */
    public static String removeArrayPrefix(String typeName) {
        while (typeName.startsWith("[")) {
            typeName = typeName.substring(1);
        }
        if (typeName.startsWith("L")) {
            typeName = typeName.substring(1);
        }
        if (typeName.endsWith(";")) {
            typeName = typeName.substring(0, typeName.length() - 1);
        }
        return typeName;
    }

    /**
     * The method retrieves the bytecode of a specified class from the provided JAR files
     *
     * @param className The name of the class for which to find the bytecode
     * @param jarPaths An array of paths to JAR files
     * @return The byte array containing the class bytes or null, if not found
     * @throws IOException
     */
    private static byte[] getClassBytes(String className, String[] jarPaths) throws IOException {
        String classFilePath = className.replace('.', '/') + ".class";
        for (String jarPath : jarPaths) {
            try (JarFile jarFile = new JarFile(jarPath)) {
                ZipEntry entry = jarFile.getEntry(classFilePath);
                if (entry != null) {
                    try (InputStream inputStream = jarFile.getInputStream(entry);
                         ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {

                        byte[] buffer = new byte[1024];
                        int bytesRead;
                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            byteArrayOutputStream.write(buffer, 0, bytesRead);
                        }
                        return byteArrayOutputStream.toByteArray();
                    }
                }
            }
        }
        return null;
    }

    /**
     * The method checks whether a class is part of the JDK
     *
     * @param className The name of the class to check
     * @return true if the class name starts with any of the specified JDK prefixes or if it’s a primitive type
     */
    private static boolean isJdkClass(String className) {
        return className.startsWith("java.")
                || className.startsWith("javax.")
                || className.startsWith("jdk.")
                || className.startsWith("sun.")
                || isPrimitiveType(className);
    }

    /**
     * Method that checks for primitive types based on their bytecode representation
     *
     * @param internalName The internal (or bytecode) representation of a class name
     * @return true if it is a primitive type
     */
    private static boolean isPrimitiveType(String internalName) {
        return internalName.length() == 1 && "ZBCSIFDJ".indexOf(internalName.charAt(0)) != -1;
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: java ClassPathDependencyChecker <mainClassName> <jarPath1> <jarPath2> ...");
            return;
        }

        String mainClassName = args[0];
        String[] jarPaths = new String[args.length - 1];
        System.arraycopy(args, 1, jarPaths, 0, args.length - 1);

        ClassPathDependencyChecker checker = new ClassPathDependencyChecker();
        try {
            boolean allDependenciesResolved = checker.checkDependencies(mainClassName, jarPaths);
            System.out.println(allDependenciesResolved ? true : false);
        } catch (IOException e) {
            System.out.println(false);
            System.err.println("Error analyzing class dependencies: " + e.getMessage());
        }
    }
}
