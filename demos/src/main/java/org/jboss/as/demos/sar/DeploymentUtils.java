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
package org.jboss.as.demos.sar;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;

import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

/**
 * Used to deploy/undeploy deployments to a running <b>standalone</b> application server
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class DeploymentUtils {
    final String archiveName;
    final Package pkg;
    final JavaArchive archive;
    final File realArchive;

    File deployedArchive;

    public DeploymentUtils(String archiveName, Package pkg) {
        this.archiveName = archiveName;
        this.pkg = pkg;

        ArchivePath metaInf = ArchivePaths.create("META-INF");

        archive = ShrinkWrap.create(JavaArchive.class, archiveName);
        archive.addPackage(pkg);

        File sourceMetaInf = getSourceMetaInfDir();
        addFiles(archive, sourceMetaInf, metaInf);

        System.out.println(archive.toString(true));

        realArchive = new File(getOutputDir(), archive.getName());
        archive.as(ZipExporter.class).exportZip(realArchive, true);
    }

    private void addFiles(JavaArchive archive, File dir, ArchivePath dest) {
        for (String name : dir.list()) {
            File file = new File(dir, name);
            if (file.isDirectory()) {
                addFiles(archive, file, ArchivePaths.create(dest, name));
            } else {
                archive.addResource(file, ArchivePaths.create(dest, name));
            }
        }
    }

    public synchronized void deploy() throws IOException {
        File file = realArchive.getParentFile().getParentFile().getParentFile().getParentFile();
        file = new File(file, "build");
        file = new File(file, "target");
        file = new File(file, "standalone");
        file = new File(file, "deployments");

        if (!file.exists()) {
            throw new IllegalStateException("Deploy directory " + file + " does not exist");
        }

        deployedArchive = new File(file, archiveName);
        if (deployedArchive.exists()) {
            deployedArchive.delete();
        }

        InputStream in = null;
        OutputStream out = null;

        try {
            in = new BufferedInputStream(new FileInputStream(realArchive));
            out = new BufferedOutputStream(new FileOutputStream(deployedArchive));

            int i = in.read();
            while (i != -1) {
                out.write(i);
                i = in.read();
            }
            out.flush();
        } finally {
            close(in);
            close(out);
        }
    }

    public synchronized void undeploy() {
        if (deployedArchive == null) {
            throw new IllegalStateException(archiveName + " is not deployed");
        }
        if (deployedArchive.exists()) {
            deployedArchive.delete();
        }
        deployedArchive = null;
    }

    private File getSourceMetaInfDir() {
        String name = "archives/" + archiveName + "/META-INF/MANIFEST.MF";

        URL url = Thread.currentThread().getContextClassLoader().getResource(name);
        if (url == null) {
            throw new IllegalArgumentException("No resource called " + name);
        }
        try {
            File file = new File(url.toURI());
            return file.getParentFile();
        } catch (URISyntaxException e) {
            throw new RuntimeException("Could not get file for " + url);
        }
    }

    private File getOutputDir() {
        File file = new File("target");
        if (!file.exists()) {
            throw new IllegalStateException("target/ does not exist");
        }
        if (!file.isDirectory()) {
            throw new IllegalStateException("target/ is not a directory");
        }
        file = new File(file, "archives");
        if (file.exists()) {
            if (!file.isDirectory()) {
                throw new IllegalStateException("target/archives/ already exists and is not a directory");
            }
        } else {
            file.mkdir();
        }
        return file.getAbsoluteFile();
    }

    private void close(Closeable c) {
        try {
            if (c != null) {
                c.close();
            }
        } catch (IOException ignore) {
        }
    }

}
