/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.jdr.util;

import org.jboss.as.jdr.commands.JdrEnvironment;
import org.jboss.vfs.VirtualFile;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipOutputStream;

import static org.jboss.as.jdr.logger.JdrLogger.ROOT_LOGGER;

/**
 * Abstracts the zipfile used for packaging the JDR Report.
 */
public class JdrZipFile {

    ZipOutputStream zos;
    String jbossHome;
    JdrEnvironment env;
    String name;
    String baseName;

    public JdrZipFile(JdrEnvironment env) throws Exception {
        this.env = env;
        this.jbossHome = this.env.getJbossHome();
        SimpleDateFormat fmt = new SimpleDateFormat("yy-MM-dd_hh-mm-ss");
        baseName = "jdr_" + fmt.format(new Date());

        if (this.env.getHostControllerName() != null) {
            this.baseName += "." + this.env.getHostControllerName();
        }

        if (this.env.getServerName() != null) {
            this.baseName += "_" + this.env.getServerName();
        }

        this.name = this.env.getOutputDirectory() +
                java.io.File.separator +
                baseName + ".zip";

        zos = new ZipOutputStream(new FileOutputStream(this.name));
    }

    /**
     * @return the full pathname to the zipfile on disk
     */
    public String name() {
        return this.name;
    }

    /**
     * Adds the contents of the {@link InputStream} to the path in the zip.
     *
     * This method allows for absolute control of the destination of the content to be stored.
     * It is not common to use this method.
     * @param is content to write
     * @param path destination to write to in the zip file
     */
    public void add(InputStream is, String path) {
        byte [] buffer = new byte[1024];

        try {
            String entryName = this.baseName + "/" + path;
            ZipEntry ze = new ZipEntry(entryName);
            zos.putNextEntry(ze);
            int bytesRead = is.read(buffer);
            while( bytesRead > -1 ) {
                zos.write(buffer, 0, bytesRead);
                bytesRead = is.read(buffer);
            }
        }
        catch (ZipException ze) {
            ROOT_LOGGER.debugf(ze, "%s is already in the zip", path);
        }
        catch (Exception e) {
            ROOT_LOGGER.debugf(e, "Error when adding %s", path);
        }
        finally {
            try {
                zos.closeEntry();
            }
            catch (Exception e) {
                ROOT_LOGGER.debugf(e, "Error when closing entry for %s", path);
            }
        }
    }

    /**
     * Adds the content of the {@link InputStream} to the zip in a location that mirrors where {@link VirtualFile file} is located.
     *
     * For example if {@code file} is at {@code /tmp/foo/bar} and {@code $JBOSS_HOME} is {@code tmp} then the destination will be {@code JBOSSHOME/foo/bar}
     *
     * @param file {@link VirtualFile} where metadata is read from
     * @param is content to write to the zip file
     * @throws Exception
     */
    public void add(VirtualFile file, InputStream is) throws Exception {
        String name = "JBOSS_HOME" + file.getPhysicalFile().getAbsolutePath().substring(this.jbossHome.length());
        this.add(is, name);
    }

    /**
     * Adds content to the zipfile at path
     *
     * path is prepended with the directory reserved for generated text files in JDR
     *
     * @param content
     * @param path
     * @throws Exception
     */
    public void add(String content, String path) throws Exception {
        StringBuilder name = new StringBuilder("sos_strings/");

        name.append(this.env.getProductName().replace(" ", "_").toLowerCase());
        name.append("-");
        name.append(this.env.getProductVersion().split("\\.")[0]);
        name.append("/");
        name.append(path);

        this.add(new ByteArrayInputStream(content.getBytes()), name.toString());
    }

    /**
     * Adds content to the zipfile at path
     *
     * path is prepended with the directory reserved for generated text files in JDR
     *
     * @param stream
     * @param path
     * @throws Exception
     */
    public void addAsString(InputStream stream, String path) throws Exception {
        StringBuilder name = new StringBuilder("sos_strings/");

        name.append(this.env.getProductName().replace(" ", "_").toLowerCase());
        name.append("-");
        name.append(this.env.getProductVersion().split("\\.")[0]);
        name.append("/");
        name.append(path);

        this.add(stream, name.toString());
    }

    /**
     * Adds content to the zipfile in a file named logName
     *
     * path is prepended with the directory reserved for JDR log files
     *
     * @param content
     * @param logName
     * @throws Exception
     */
    public void addLog(String content, String logName) throws Exception {
        String name = "sos_logs/" + logName;
        this.add(new ByteArrayInputStream(content.getBytes()), name);
    }

    public void close() throws Exception {
        this.zos.close();
    }
}
