/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.patching.generator;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @author Brian Stansberry (c) 2012 Red Hat Inc.
 */
public class ZipUtils {

    public static void zip(File sourceDir, File zipFile) {
        try {
            ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile));

            for (File file : sourceDir.listFiles()) {
                if (file.isDirectory()) {
                    addDirectoryToZip(file, file.getName(), zos);
                } else {
                    addFileToZip(file, null, zos);
                }
            }

            zos.close();

        } catch (IOException e) {
            throw new RuntimeException("Failed creating patch file " + zipFile, e);
        }

        System.out.println("\nPrepared " + zipFile.getName() + " at " + zipFile.getAbsolutePath());
    }

    private static void addDirectoryToZip(File dir, String dirName, ZipOutputStream zos) throws IOException {

        ZipEntry dirEntry = new ZipEntry(dirName + "/");
        zos.putNextEntry(dirEntry);
        zos.closeEntry();

        File[] children = dir.listFiles();
        if (children != null) {
            for (File file : children) {
                if (file.isDirectory()) {
                    addDirectoryToZip(file, dirName + "/" + file.getName(), zos);
                } else {
                    addFileToZip(file, dirName, zos);
                }
            }
        }
    }

    private static void addFileToZip(File file, String parent, ZipOutputStream zos) throws IOException {

        String entryName = parent == null ? file.getName() : parent + "/" + file.getName();
        zos.putNextEntry(new ZipEntry(entryName));

        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));

        byte[] bytesIn = new byte[4096];
        int read;
        while ((read = bis.read(bytesIn)) != -1) {
            zos.write(bytesIn, 0, read);
        }

        zos.closeEntry();
    }

}
