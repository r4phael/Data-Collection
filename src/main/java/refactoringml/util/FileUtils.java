package refactoringml.util;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;
import static refactoringml.util.FilePathUtils.*;

public class FileUtils {

	private FileUtils() {
	}

	public static String[] getAllJavaFiles(Path path) {
		return getAllJavaFiles(path, null);
	}

	public static String[] getAllJavaFiles(Path path, String regex) {
		try {
			try (var filesStream = Files.walk(path)) {
				return filesStream.filter(Files::isRegularFile)
						.filter(x -> !x.toAbsolutePath().toString().contains("/.git/"))
						.filter(x -> isJavaFile(x.toAbsolutePath().toString()))
						.filter(x -> (regex == null || x.toAbsolutePath().toString().contains(regex)))
						.map(x -> x.toAbsolutePath().toString()).toArray(String[]::new);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	// The fileName and filePath, as both is important to be considered
	// src/test is a enforced convention for test files with gradle and maven build
	// tools
	// every test file for ant has to end on Test, e.g. *Test.java
	public static boolean isTestFile(String filePathName) {
		if (!FileUtils.isJavaFile(filePathName))
			return false;

		String normalizedFilePath = enforceUnixPaths(filePathName.toLowerCase());
		return normalizedFilePath.contains("test") || normalizedFilePath.contains("/test/");
	}

	// Returns true if a file is a java file.
	public static boolean isJavaFile(String fileName) {
		return fileName.toLowerCase().endsWith(".java");
	}

	public static String createTmpDir() {
		String rawTempDir = com.google.common.io.Files.createTempDir().getAbsolutePath();
		return lastSlashDir(rawTempDir);
	}

	// Remove all existing temp dirs containing repo in their name
	public static void cleanOldTempDir() throws IOException {
		File tmpdir = new File(System.getProperty("java.io.tmpdir"));
		String[] directories = tmpdir
				.list((current, name) -> new File(current, name).isDirectory() && name.contains("repo"));
		if (directories == null)
			return;
		for (String dir : directories) {
			cleanTempDir(dir);
		}
	}

	public static void cleanTempDir(String tempDir) throws IOException {
		if (tempDir != null) {
			org.apache.commons.io.FileUtils.deleteDirectory(new File(tempDir));
		}
	}

	public static boolean newDir(String path) {
		return new File(path).mkdirs();
	}

	// Write the content to a new file at the given path. Creates a new directory at
	// the path if necessary.
	public static void writeFile(String filePath, Object content) throws FileNotFoundException {
		new File(dirsOnly(filePath)).mkdirs();
		PrintStream ps = new PrintStream(filePath);
		ps.print(content);
		ps.close();
	}

	// Write the toString of an object to a file at the given path. Creates a new
	// file and directory at the path if necessary.
	public static void appendToFile(File file, String content) throws IOException {
		try (FileWriter fr = new FileWriter(file, true)) {
			fr.write(content);
		}
	}

	// remove all matching lines from the file
	public static void removeFromFile(File file, String condition) throws IOException {
		List<String> lines = Files.readAllLines(file.toPath());
		List<String> cleanedLines = lines.stream().filter(line -> !line.contains(condition))
				.collect(Collectors.toList());
		org.apache.commons.io.FileUtils.writeLines(file, cleanedLines, false);
	}

	// Write the content to a new file at the given path. Creates a new directory at
	// the path if necessary.
	public static String readFile(String filePath) throws FileNotFoundException {
		File myObj = new File(filePath);
		Scanner myReader = new Scanner(myObj);
		String data = "";
		while (myReader.hasNextLine()) {
			data = myReader.nextLine();
		}
		myReader.close();
		return data;
	}

	public static boolean fileDoesNotExist(String fileName) {
		return fileName.equals("/dev/null");
	}

}