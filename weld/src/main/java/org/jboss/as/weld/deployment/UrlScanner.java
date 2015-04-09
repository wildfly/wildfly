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

package org.jboss.as.weld.deployment;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.jboss.as.weld.logging.WeldLogger;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VirtualFile;

public class UrlScanner {


    public boolean handleBeansXml(final URL url, final List<String> discoveredClasses) {
        String urlPath = url.toExternalForm();

        // determin resource type (eg: jar, file, bundle)
        String urlType = "file";
        int colonIndex = urlPath.indexOf(":");
        if (colonIndex != -1) {
            urlType = urlPath.substring(0, colonIndex);
        }

        // Extra built-in support for simple file-based resources
        if ("file".equals(urlType) || "jar".equals(urlType)) {
            // switch to using getPath() instead of toExternalForm()
            urlPath = url.getPath();

            if (urlPath.indexOf('!') > 0) {
                urlPath = urlPath.substring(0, urlPath.indexOf('!'));
            } else {
                // hack for /META-INF/beans.xml
                File dirOrArchive = new File(urlPath);
                dirOrArchive = dirOrArchive.getParentFile();
                urlPath = dirOrArchive.getParent();
            }

            try {
                urlPath = URLDecoder.decode(urlPath, "UTF-8");
            } catch (UnsupportedEncodingException ex) {
                throw new RuntimeException(ex);
            }
            handle(urlPath, discoveredClasses);
            return true;
        } else if ("vfs".equals(urlType)) {
            try {
                VirtualFile vfsRoot = VFS.getChild(url).getParent().getParent();
                handle(vfsRoot, discoveredClasses);
                return true;
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        } else {
            WeldLogger.DEPLOYMENT_LOGGER.doNotUnderstandProtocol(url);
            return false;
        }
    }

    private void handle(VirtualFile urlPath, List<String> discoveredClasses) {
        WeldLogger.DEPLOYMENT_LOGGER.tracef("scanning: %s", urlPath);
        handleDirectory(urlPath, null, discoveredClasses);
    }

    private void handle(String urlPath, List<String> discoveredClasses) {
        try {
            WeldLogger.DEPLOYMENT_LOGGER.tracef("scanning: %s", urlPath);

            if (urlPath.startsWith("file:")) {
                urlPath = urlPath.substring(5);
            }
            if (urlPath.indexOf('!') > 0) {
                urlPath = urlPath.substring(0, urlPath.indexOf('!'));
            }

            File file = new File(urlPath);
            if (file.isDirectory()) {
                handleDirectory(file, null, discoveredClasses);
            } else {
                handleArchiveByFile(file, discoveredClasses);
            }
        } catch (IOException ioe) {
            WeldLogger.DEPLOYMENT_LOGGER.couldNotReadEntries(ioe);
        }
    }

    private void handleArchiveByFile(File file, List<String> discoveredClasses) throws IOException {
        try {
            WeldLogger.DEPLOYMENT_LOGGER.trace("archive: " + file);

            try (ZipFile zip = new ZipFile(file)) {
                Enumeration<? extends ZipEntry> entries = zip.entries();

                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    String name = entry.getName();
                    handleFile(name, discoveredClasses);
                }
            }
        } catch (ZipException e) {
            throw new RuntimeException("Error handling file " + file, e);
        }
    }

    private void handleDirectory(File file, String path,  List<String> discoveredClasses) {

        WeldLogger.DEPLOYMENT_LOGGER.tracef("handling directory: %s", file);

        for (File child : file.listFiles()) {
            String newPath = (path == null) ? child.getName() : (path + '/' + child.getName());

            if (child.isDirectory()) {
                handleDirectory(child, newPath,  discoveredClasses);
            } else {
                handleFile(newPath, discoveredClasses);
            }
        }
    }

    private void handleDirectory(VirtualFile file, String path, List<String> discoveredClasses) {


        WeldLogger.DEPLOYMENT_LOGGER.tracef("handling directory: %s", file);

        for (VirtualFile child : file.getChildren()) {
            String newPath = (path == null) ? child.getName() : (path + '/' + child.getName());

            if (child.isDirectory()) {
                handleDirectory(child, newPath, discoveredClasses);
            } else {
                handleFile(newPath, discoveredClasses);
            }
        }
    }

    protected void handleFile(String name, List<String> discoveredClasses) {
        if (name.endsWith(".class")) {
            discoveredClasses.add(filenameToClassname(name));
        }
    }

    /**
     * Convert a path to a class file to a class name
     */
    public static String filenameToClassname(String filename) {
        return filename.substring(0, filename.lastIndexOf(".class")).replace('/', '.').replace('\\', '.');
    }
}
