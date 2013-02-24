/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.process.support;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Class to access the files output by the various processes. They will go in
 * the target/process-files directory and will have the name of the process
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public abstract class TestFileUtils {
    private static final File DIR = new File(new File("target"),
            "process-files");
    static {
        // Check that target/process-files is a directory
        if (DIR.exists() && !DIR.isDirectory())
            throw new IllegalStateException(DIR.getAbsolutePath()
                    + " exists and is not a directory");
        DIR.mkdir();
    }

    public static TestFile getOutputFile(String processName) {
        return new TestFile(new File(DIR, processName));
    }

    public static void cleanFiles() {
        for (File file : getOutputFiles()) {
            if (file.isFile()) {
                if (!file.delete())
                    throw new IllegalStateException("Could not delete " + file);
            }
        }
    }

    static File[] getOutputFiles() {
        String[] list = DIR.list();
        List<File> result = new ArrayList<File>();

        for (String name : list) {
            File file = new File(DIR, name);
            result.add(file);
        }
        return result.toArray(new File[result.size()]);
    }

    public static int getNumberOutputFiles() {
        return getOutputFiles().length;
    }

    public static void assertNumberOutputFiles(int expected) {
        File[] files = getOutputFiles();
        assertEquals(Arrays.toString(files), expected, files.length);
    }

    public static void close(Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (Exception e) {
            }
        }
    }

    public static class TestFile {
        private File file;

        public TestFile(File file) {
            this.file = file;
        }

        public void writeToFile(String s) {
            FileWriter writer = null;
            try {
                writer = new FileWriter(file, true);
                writer.append(s);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                close(writer);
            }
        }

        public List<String> readFile() {
            List<String> lines = new ArrayList<String>();
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new FileReader(file));
                String line = reader.readLine();
                while (line != null) {
                    lines.add(line);
                    line = reader.readLine();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                close(reader);
            }
            return lines;
        }

        public void checkFile(String... expected) {
            List<String> lines = readFile();
            assertEquals(Arrays.toString(expected) + ":" + lines, + expected.length, lines.size());

            for (int i = 0; i < expected.length; i++) {
                assertEquals(expected[i], lines.get(i));
            }

        }

        public boolean exists() {
            return file.exists();
        }
    }

}
