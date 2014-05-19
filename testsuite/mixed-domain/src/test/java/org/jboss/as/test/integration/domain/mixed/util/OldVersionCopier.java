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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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

import org.jboss.as.test.integration.domain.mixed.Version;
import org.xnio.IoUtils;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class OldVersionCopier {

    private static String OLD_VERSIONS_DIR = "jboss.test.mixed.domain.dir";

    private final Version.AsVersion version;
    private final File oldVersionsBaseDir;
    private final File targetOldVersions = new File("target/old-versions/");


    private OldVersionCopier(Version.AsVersion version, File oldVersionsBaseDir) {
        this.version = version;
        this.oldVersionsBaseDir = oldVersionsBaseDir;
    }

    static File expandOldVersion(Version.AsVersion version) {
        OldVersionCopier copier = new OldVersionCopier(version, obtainOldVersionsDir());
        return copier.expandAsInstance(version);
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


    private File expandAsInstance(Version.AsVersion version) {
        createIfNotExists(targetOldVersions);

        File file = new File(oldVersionsBaseDir, version.getZipFileName());
        if (!file.exists()) {
            throw new RuntimeException("Old version not found in " + file.getAbsolutePath());
        }
        try {
            File expanded = expandAsInstance(file);

//                    if (file.getName().equals("jboss-as-7.1.2.Final.zip")) {
//                        patchBadRemoting("jboss-as-7.1.2.Final");
//                    } else if (file.getName().equals("jboss-as-7.1.3.Final.zip")) {
//                        patchBadRemoting("jboss-as-7.1.3.Final");
//                    }
            return expanded;
        } catch(Exception e) {
            throw new RuntimeException(e);
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

    private File expandAsInstance(final File file) throws Exception {
        File versionDir = new File(targetOldVersions, version.getFullVersionName());
        createIfNotExists(versionDir);

        final ZipFile zipFile = new ZipFile(file);
        try {
            for (Enumeration<? extends ZipEntry> en = zipFile.entries() ; en.hasMoreElements() ; ) {
                final ZipEntry entry = en.nextElement();
                final File output = new File(versionDir, entry.getName());
                if (entry.isDirectory()) {
                    createIfNotExists(output);
                } else {
                    inputStreamToFile(zipFile.getInputStream(entry), output);
                }

            }
        } finally {
            IoUtils.safeClose(zipFile);
        }

        File[] files = versionDir.listFiles();
        if (files.length != 1) {
            //If this really becomes a problem, inspect the directory structures
            throw new RuntimeException("The unzipped file contains more than one file in " + versionDir.getAbsolutePath() + ". Unable to determine the true distribution");
        }
        return files[0];
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

    private void createIfNotExists(File file) {
        if (!file.exists()) {
            if (!file.mkdirs() && file.exists()) {
                throw new RuntimeException("Could not create " + targetOldVersions);
            }
        }
    }
}
