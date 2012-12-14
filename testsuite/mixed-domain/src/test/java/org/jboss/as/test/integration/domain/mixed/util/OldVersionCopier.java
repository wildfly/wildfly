/*
* JBoss, Home of Professional Open Source.
* Copyright 2012, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.test.integration.domain.mixed.util;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.xnio.IoUtils;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class OldVersionCopier {

    private static String OLD_VERSIONS_DIR = "jboss.test.mixed.domain.dir";

    private final File oldVersionsBaseDir;
    private final File targetOldVersions = new File("target/old-versions/");


    private OldVersionCopier(File oldVersionsBaseDir) {
        this.oldVersionsBaseDir = oldVersionsBaseDir;
    }

    static OldVersionCopier expandOldVersions() {
        OldVersionCopier copier = new OldVersionCopier(obtainOldVersionsDir());
        copier.expandAsInstances();
        return copier;
    }

    File getVersionDir(String version) {
        File file = new File(targetOldVersions, "jboss-as-" + version);
        if (!file.exists() || !file.isDirectory()) {
            throw new IllegalStateException("Could not find " + file.getAbsolutePath());
        }
        return file;
    }

    private static File obtainOldVersionsDir() {
        String error = "System property '" + OLD_VERSIONS_DIR + "' must be set to a directory containing old versions";
        String oldVersionsDir = System.getProperty(OLD_VERSIONS_DIR);
        if (oldVersionsDir == null) {
            throw new RuntimeException(error);
        }
        File file = new File(oldVersionsDir);
        if (!file.exists() || !file.isDirectory()) {
            throw new RuntimeException(error);
        }
        return file;
    }

    private void expandAsInstances() {
        if (targetOldVersions.exists()) {
            return;
        }
        if (!targetOldVersions.mkdirs() && targetOldVersions.exists()) {
            throw new RuntimeException("Could not create " + targetOldVersions);
        }

        for (File file : oldVersionsBaseDir.listFiles()) {
            if (file.getName().endsWith(".zip")) {
                try {
                    expandAsInstance(file);

                    if (file.getName().equals("jboss-as-7.1.2.Final.zip")) {
                        patchBadRemoting("jboss-as-7.1.2.Final");
                    } else if (file.getName().equals("jboss-as-7.1.3.Final.zip")) {
                        patchBadRemoting("jboss-as-7.1.3.Final");
                    }
                } catch(Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private void patchBadRemoting(String name) throws Exception {
        File file = new File(targetOldVersions, name);
        File modulesDir = new File(file, "modules");
        if (!modulesDir.exists()) {
            throw new RuntimeException("No modules dir " + modulesDir);
        }
//        File modulesZip = new File(file, "modules.zip");
//        if (!modulesZip.exists()) {
//            throw new RuntimeException("No modules zip " + modulesZip);
//        }

        //org/jboss/xnio/main/
        File xnioDir = new File(modulesDir, "org");
        xnioDir = new File(xnioDir, "jboss");
        xnioDir = new File(xnioDir, "xnio");
        xnioDir = new File(xnioDir, "main");
        URL patchedXnioUrl = getClass().getResource("/patched-jars/xnio-api-3.0.7.GA.jar");
        patchModule(xnioDir, patchedXnioUrl);

        //org/jboss/remoting3/main
        File remotingDir = new File(modulesDir, "org");
        remotingDir = new File(remotingDir, "jboss");
        remotingDir = new File(remotingDir, "remoting3");
        remotingDir = new File(remotingDir, "main");
        URL patchedRemotingUrl = getClass().getResource("/patched-jars/jboss-remoting-3.2.13.GA.jar");
        patchModule(remotingDir, patchedRemotingUrl);
    }

    private void patchModule(File moduleMainDir, URL patchedJar) throws Exception {
        assertTrue(moduleMainDir.exists());
        assertNotNull(patchedJar);
        if (patchedJar == null) {
            throw new RuntimeException("Null url");
        }
        String oldJarName = null;
        for (String fileName : moduleMainDir.list()) {
            if (fileName.endsWith(".jar")) {
                if (oldJarName != null) {
                    throw new RuntimeException("Could not determine old jar in " + moduleMainDir);
                }
                oldJarName = fileName;
            }
        }

        if (oldJarName == null) {
            throw new RuntimeException("Could not determine old jar in " + moduleMainDir);
        }

        File patchedJarFile = new File(patchedJar.toURI());

        //Copy the patched jar to the module main directory
        inputStreamToFile(new FileInputStream(patchedJarFile), new File(moduleMainDir, patchedJarFile.getName()));

        //Replace the name of the jar with the patched jar in module.xml
        File moduleXml = new File(moduleMainDir, "module.xml");
        StringBuffer moduleXmlContents = new StringBuffer();
        BufferedReader reader = new BufferedReader(new FileReader(moduleXml));
        try {
            String line = reader.readLine();
            while (line != null) {
                moduleXmlContents.append(line);
                moduleXmlContents.append("\n");
                line = reader.readLine();
            }
        } finally {
            IoUtils.safeClose(reader);
        }
        int index = moduleXmlContents.indexOf(oldJarName);
        moduleXmlContents.replace(index, index + oldJarName.length(), patchedJarFile.getName());
        moduleXml.delete();
        BufferedWriter writer = new BufferedWriter(new FileWriter(moduleXml));
        try {
            writer.write(moduleXmlContents.toString());
        } finally {
            IoUtils.safeClose(writer);
        }

    }

    private void expandAsInstance(final File file) throws Exception {

        ZipFile zipFile = null;
        try {
            zipFile = new ZipFile(file);
            for (Enumeration<? extends ZipEntry> en = zipFile.entries() ; en.hasMoreElements() ; ) {
                final ZipEntry entry = en.nextElement();
                final File output = new File(targetOldVersions, entry.getName());
                if (entry.isDirectory()) {
                    if (!output.exists()) {
                        if (!output.mkdirs() && !output.exists()) {
                            throw new RuntimeException("Could not make dir " + output.getAbsolutePath());
                        }
                    }
                } else {
                    inputStreamToFile(zipFile.getInputStream(entry), output);
                }

            }
        } finally {
            IoUtils.safeClose(zipFile);
        }
    }

    private void inputStreamToFile(InputStream input, File output) throws Exception {
        final InputStream in = new BufferedInputStream(input);
        try {
            final OutputStream out = new BufferedOutputStream(new FileOutputStream(output));
            try {
                byte[] buf = new byte[1024];
                int len = in.read(buf);
                while (len != -1) {
                    out.write(buf, 0, len);
                    len = in.read(buf);
                }
            } finally {
                IoUtils.safeClose(out);
            }
        } finally {
            IoUtils.safeClose(in);
        }
    }

    public static void main(String[] args) {
        System.setProperty(OLD_VERSIONS_DIR, "/Users/kabir/old-as7-releases/");
        OldVersionCopier.expandOldVersions();
    }
}
