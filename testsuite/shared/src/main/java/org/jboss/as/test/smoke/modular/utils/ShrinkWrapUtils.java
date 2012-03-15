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
package org.jboss.as.test.smoke.modular.utils;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.container.ClassContainer;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class ShrinkWrapUtils {

    public static JavaArchive createJavaArchive(String archiveName) {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, getBaseArchiveName(archiveName));
        addResources(archiveName, archive);
        return archive;
    }

    public static JavaArchive createJavaArchive(String archiveName, Package...packages) {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, getBaseArchiveName(archiveName));
        addPackages(archive, packages);
        addResources(archiveName, archive);
        return archive;
    }

    public static JavaArchive createEmptyJavaArchive(String archiveName) {

        int index = archiveName.lastIndexOf("/");
        String baseName = index < 0 ? archiveName : archiveName.substring(index + 1);

        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, baseName);
        //Shrinkwrap does not like empty archives
        archive.addClass(ShrinkWrapUtils.class);
        return archive;
    }

    public static File getResources(String archiveName) {
        String name = archiveName;

        URL url = Thread.currentThread().getContextClassLoader().getResource(name);
        if (url == null) {
            System.err.println("No resources for " + archiveName);
            return null;
        }
        try {
            File file = new File(url.toURI());
            if (!file.exists()) {
                throw new IllegalArgumentException("Could not find " + file.getAbsolutePath());
            }
            if (!file.isDirectory()) {
                throw new IllegalArgumentException(file.getAbsolutePath() + " is not a directory");
            }
            return file;

        } catch (URISyntaxException e) {
            throw new RuntimeException("Could not get file for " + url);
        }
    }


    public static void addFiles(Archive<?> archive, File dir, ArchivePath dest) {
        for (String name : dir.list()) {
            File file = new File(dir, name);
            if (file.isDirectory()) {
                addFiles(archive, file, ArchivePaths.create(dest, name));
            } else {
                archive.add(new FileAsset(file), ArchivePaths.create(dest, name));
            }
        }
    }

    private static String getBaseArchiveName(String archiveName) {
        int index = archiveName.lastIndexOf("/");
        return index < 0 ? archiveName : archiveName.substring(index + 1);
    }

    private static void addPackages(ClassContainer<?> archive, Package...packages) {
        for (Package pkg : packages) {
            archive.addPackage(pkg);
        }
    }

    private static void addResources(String archiveName, Archive<?> archive) {
        File resourcesDir = getResources(archiveName);
        if (resourcesDir != null) {
            addFiles(archive, resourcesDir, ArchivePaths.create("/"));
        }

    }
}
