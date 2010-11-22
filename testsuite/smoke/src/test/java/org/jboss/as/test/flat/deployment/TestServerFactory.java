/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.flat.deployment;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.StandaloneServer;
import org.jboss.as.server.StandaloneServerFactory;
import org.junit.Assert;

/**
 * Utility class to create a standalone server within a unit test case.
 *
 * TODO tests shouldn't use standalone server directly.
 *
 * @author Emanuel Muckenhuber
 */
class TestServerFactory {

    /**
     * Helper method to create a standalone server. This copies the
     * configuration/ and data/system-content/ to the "target" directory.
     *
     * @param serverName the server name
     * @return the standalone server
     * @throws IOException
     */
    static StandaloneServer createServer(String serverName) throws IOException {

        // Get the AS home in "build"
        final File buildServerHome = getASHome();

        // Use the module root from build
        final File modulesRoot = new File(buildServerHome, "modules");
        System.setProperty("module.path", modulesRoot.getAbsolutePath());

        // Get the standalone directory
        final File standalone = new File(buildServerHome, "standalone");
        if (!standalone.exists()) {
            throw new IOException(standalone.getAbsolutePath());
        }
        // Copy required stuff into the "target" directory
        final File tempServerDir = createTarget(serverName);
        copyFiles("configuration", standalone, tempServerDir);
        copyFiles("data/system-content", standalone, tempServerDir);

        // Create the server environment
        final Properties props = new Properties();
        props.put(ServerEnvironment.HOME_DIR, tempServerDir.getAbsolutePath());
        props.put(ServerEnvironment.SERVER_BASE_DIR, tempServerDir.getAbsolutePath());
        props.put(ServerEnvironment.MODULES_DIR, modulesRoot.getAbsolutePath());

        ServerEnvironment env = new ServerEnvironment(props, serverName, true);
        return StandaloneServerFactory.create(env);
    }

    static File createTarget(final String serverName) throws IOException {
        File f = new File(".");
        f = f.getAbsoluteFile();
        File tempServerHome = new File(f, "target/server/" + serverName);
        if (tempServerHome.exists()) {
            if (!recursiveDelete(tempServerHome)) {
                throw new IOException("failed to delete" + tempServerHome);
            }
        }
        if (!tempServerHome.mkdirs()) {
            throw new IOException("failed to create " + tempServerHome);
        }
        return tempServerHome;
    }

    static File getASHome() {
        File f = new File(".");
        f = f.getAbsoluteFile();
        while (f.getParentFile() != null) {
            if ("testsuite".equals(f.getName())) {
                Assert.assertNotNull("Expected to find a parent directory for " + f.getAbsolutePath(), f.getParentFile());
                f = f.getParentFile();
                f = new File(f, "build");
                Assert.assertTrue("The server 'build' dir exists", f.exists());
                f = new File(f, "target");
                File[] children = f.listFiles();
                f = null;
                if (children != null) {
                    for (File child : children) {
                        if (child.getName().startsWith("jboss-")) {
                            f = child;
                        }
                    }
                }
                if (f == null || !f.exists()) {
                    Assert.fail("The server hasn't been built yet.");
                }
                Assert.assertTrue("The server 'build/target' dir exists", f.exists());
                return f.getAbsoluteFile();
            } else {
                f = f.getParentFile();
            }
        }
        return null;
    }

    static void copyFiles(String path, File src, File dest) throws IOException {
        final File srcDir = new File(src, path);
        if (!srcDir.exists()) {
            throw new IOException(srcDir.getAbsolutePath());
        }
        final File destDir = new File(dest, path);
        if (!destDir.mkdirs()) {
            throw new IOException(destDir.getAbsolutePath());
        }
        copyFiles(srcDir, destDir);
    }

    static void copyFiles(File srcDir, File destDir) throws IOException {
        if (!destDir.exists()) {
            if (!destDir.mkdirs()) {
                throw new IOException("failed to create " + destDir);
            }
        }
        File[] files = srcDir.listFiles();
        for (File file : files) {
            if (file.isFile()) {
                final File dest = new File(destDir, file.getName());
                copyFile(file, dest);
            }
        }
    }

    static void copyFile(File src, File dest) throws IOException {
        final FileInputStream is = new FileInputStream(src);
        try {
            final FileOutputStream os = new FileOutputStream(dest);
            try {
                byte[] bytes = new byte[8192];
                int read;
                while ((read = is.read(bytes)) > -1) {
                    os.write(bytes, 0, read);
                }
                os.close();
            } finally {
                safeClose(os);
            }
            is.close();
        } finally {
            safeClose(is);
        }
    }

    static boolean recursiveDelete(File root) {
        boolean ok = true;
        if (root.isDirectory()) {
            final File[] files = root.listFiles();
            for (File file : files) {
                ok &= recursiveDelete(file);
            }
            return ok && (root.delete() || !root.exists());
        } else {
            ok &= root.delete() || !root.exists();
        }
        return ok;
    }

    static void safeClose(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException ignore) {
                //
            }
        }
    }

}
