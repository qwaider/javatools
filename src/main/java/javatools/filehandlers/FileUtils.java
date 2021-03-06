package javatools.filehandlers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.LinkedList;
import java.util.zip.GZIPInputStream;

/** 
=======
import javatools.filehandlers.FileLines;

/**
>>>>>>> 89c9b59d43bed12e9804a55921af206f188b0603:src/main/java/javatools/util/FileUtils.java
Copyright 2016 Johannes Hoffart

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

Some utility methods for files
*/
public class FileUtils {

  /** Buffer size for the readers */
  private static final int bufferSize = 16777216; // 16 MiB

  /**
   * Creates a BufferedReader for UTF-8-encoded files
   *
   * @param file  File in UTF-8 encoding
   * @return      BufferedReader for file
   * @throws FileNotFoundException
   */
  public static BufferedReader getBufferedUTF8Reader(File file) throws IOException {
    InputStream is = new FileInputStream(file);
    if (file.getName().endsWith(".gz")) {
      is = new GZIPInputStream(is, bufferSize);
    }
    return getBufferedUTF8Reader(is);
  }

  /**
   * Creates a BufferedReader for UTF-8-encoded files
   *
   * @param fileName  Path to file in UTF-8 encoding
   * @return      BufferedReader for file
   * @throws FileNotFoundException
   */
  public static BufferedReader getBufferedUTF8Reader(String fileName) throws IOException {
    return getBufferedUTF8Reader(new File(fileName));
  }

  /**
   * Creates a BufferedReader the UTF-8-encoded InputStream
   *
   * @param inputStream  InputStream in UTF-8 encoding
   * @return      BufferedReader for inputStream
   */
  public static BufferedReader getBufferedUTF8Reader(InputStream inputStream) {
    return new BufferedReader(new InputStreamReader(inputStream, Charset.forName("UTF-8")), bufferSize);
  }

  /**
   * Creates a BufferedWriter for UTF-8-encoded files
   *
   * @param file  File in UTF-8 encoding
   * @return      BufferedWriter for file
   * @throws FileNotFoundException
   */
  public static BufferedWriter getBufferedUTF8Writer(File file) throws FileNotFoundException {
    return new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), Charset.forName("UTF-8")), bufferSize);
  }

  /**
   * Creates a BufferedWriter for UTF-8-encoded files
   *
   * @param fileName  Path to file in UTF-8 encoding
   * @return      BufferedWriter for file
   * @throws FileNotFoundException
   */
  public static BufferedWriter getBufferedUTF8Writer(String fileName) throws FileNotFoundException {
    return getBufferedUTF8Writer(new File(fileName));
  }

  /**
   * Returns the content of the (UTF-8 encoded) file as string. Linebreaks
   * are encoded as unix newlines (\n)
   *
   * @param file  File to get String content from
   * @return      String content of file.
   * @throws IOException
   */
  public static String getFileContent(File file) throws IOException {
    StringBuilder sb = new StringBuilder();
    BufferedReader reader = getBufferedUTF8Reader(file);
    for (String line = reader.readLine(); line != null; line = reader.readLine()) {
      sb.append(line);
      sb.append('\n');
    }
    reader.close();
    return sb.toString();
  }

  /**
   * Writes the content of the string to the (UTF-8 encoded) file.
   *
   * @param file  File to write String content to.
   * @return      Content of file.
   * @throws IOException
   */
  public static void writeFileContent(File file, String content) throws IOException {
    BufferedWriter writer = getBufferedUTF8Writer(file);
    writer.write(content);
    writer.close();
  }

  /**
   * Verifies that a file is lexicographically order (ascending or descending)
   *
   * @param check         File to check
   * @param descending    true if ordering should be descending, false if it should be ascending
   * @return              true if file is order, false otherwise
   * @throws IOException
   */
  public static boolean verifyOrderedFile(File check, boolean descending) throws IOException {
    if (check == null || check.isDirectory() || !check.canRead()) {
      System.out.println("Unable to verify sort order of file, make sure it is readable and not a directory");
    }

    boolean first = true;

    String one = "";
    String two = "";

    long lineCount = 0;

    FileLines lines = new FileLines(check, "UTF-8", "Verifying that '" + check + "' is sorted");
    for (String line : lines) {
      lineCount++;

      if (first) {
        one = line;
        two = line;
        first = false;
        continue;
      }
      lines.close();

      one = two;
      two = line;

      int comp = two.compareTo(one);

      if (!descending && comp < 0) {
        System.out.println("Ascending order violated in line " + lineCount + ": '" + one + "' vs. '" + two + "'");
        return false;
      }

      if (descending && comp > 0) {
        System.out.println("Descending order violated in line " + lineCount + ": '" + one + "' vs. '" + two + "'");
        return false;
      }
    }

    return true;
  }

  /**
   * Collects all non-directory files in the given input directory
   * (recursively).
   *
   * @param directory Input directory.
   * @return          All non-directory files, recursively.
   */
  public static Collection<File> getAllFiles(File directory) {
    Collection<File> files = new LinkedList<File>();
    getAllFilesRecursively(directory, files);
    return files;
  }

  /**
   * Helper for getAllSubdirectories(directory).
   */
  private static void getAllFilesRecursively(File directory, Collection<File> files) {

    for (File file : directory.listFiles()) {
      if (file.isDirectory()) {
        getAllFilesRecursively(file, files);
      } else {
        files.add(file);
      }
    }
  }

  public static void main(String[] args) throws IOException {
    verifyOrderedFile(new File(args[0]), false);
  }
}
