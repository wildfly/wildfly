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

package org.jboss.as.server.manager;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.jboss.as.model.Element;
import org.jboss.as.model.HostModel;
import org.jboss.logging.Logger;
import org.jboss.staxmapper.XMLContentWriter;
import org.jboss.staxmapper.XMLExtendedStreamWriter;
import org.jboss.staxmapper.XMLMapper;

/**
 * Default implementation of {@link HostConfigurationPersister}.
 *
 * @author Brian Stansberry
 */
public class HostConfigurationPersisterImpl implements HostConfigurationPersister {

    /** The expected name of the host configuration file */
    public static final String CONFIG_FILE = "host.xml";

    /** Suffix to append to the config file name to create the backup copy. */
    public static final String BACKUP_SUFFIX = ".last-known-good";

    private static Logger logger = Logger.getLogger("org.jboss.as.server.manager");

    private final File configFile;

    public HostConfigurationPersisterImpl(File configDir) {

        if (configDir == null)
            throw new IllegalArgumentException("Domain configuration directory is null");
        File configFile = new File(configDir, CONFIG_FILE);
        if (configFile.exists()) {
            if (configFile.isDirectory()) {
                throw new IllegalArgumentException(configFile.getAbsolutePath() + " is a directory");
            }
        }
        else {
            if (configFile.isDirectory() || !configDir.canWrite()) {
                throw new IllegalArgumentException(configFile.getAbsolutePath() + " is not a writable");
            }
        }
        this.configFile = configFile;
    }

    @Override
    public InputStream getConfigurationInputStream() throws IOException {
        return new FileInputStream(configFile);
    }

    @Override
    public void persistConfiguration(final HostModel hostModel) {

        FileOutputStream fos = null;
        BufferedOutputStream bos = null;
        XMLStreamWriter writer = null;
        try {
            backupConfigFile();
            configFile.createNewFile();
            fos = new FileOutputStream(configFile);
            bos = new BufferedOutputStream(fos);
            writer = XMLOutputFactory.newInstance().createXMLStreamWriter(bos);
            final XMLMapper mapper = XMLMapper.Factory.create();
            mapper.deparseDocument(new RootElementWriter(hostModel), writer);
        }
        catch (Exception e) {
            logger.errorf(e, "Failed persisting configuration file %s" , configFile.getAbsolutePath());
        }
        finally {
             if (writer != null) {
                 try {
                    writer.close();
                } catch (XMLStreamException e) {
                    logger.warnf(e, "Failed closing writer to configuration file %s" , configFile.getAbsolutePath());
                }
             }
             if (fos != null) {
                 try {
                    fos.close();
                } catch (IOException e) {
                    logger.warnf(e, "Failed closing output stream to configuration file %s" , configFile.getAbsolutePath());
                }
             }
        }

    }

    private void backupConfigFile() throws IOException {
        File backup = new File(configFile.getParent(), configFile.getName() + BACKUP_SUFFIX);
        copyFile(configFile, backup);
    }

    private static void copyFile(File file, File backup) throws IOException {

        if (backup.exists())
            backup.delete();

        if (!file.renameTo(backup)) {
            FileOutputStream fos = null;
            BufferedOutputStream bos = null;
            FileInputStream fis = null;
            try {
                fos = new FileOutputStream(backup);
                bos = new BufferedOutputStream(fos);
                fis = new FileInputStream(file);
                BufferedInputStream bis = new BufferedInputStream(fis);
                byte[] bytes = new byte[8192];
                int read;
                while ((read = bis.read(bytes)) > -1) {
                    bos.write(bytes, 0, read);
                }
            }
            finally {
                try {
                    if (bos != null) {
                        bos.close();
                    }
                 } catch (Exception ignored) {}
                 try {
                     if (fos != null) {
                         fos.close();
                     }
                  } catch (Exception ignored) {}
                  try {
                      if (fis != null) {
                          fis.close();
                      }
                   } catch (Exception ignored) {}
            }
        }
    }

    private static class RootElementWriter implements XMLContentWriter {

        private final HostModel hostModel;

        private RootElementWriter(final HostModel hostModel) {
            this.hostModel = hostModel;
        }

        @Override
        public void writeContent(XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
            streamWriter.writeStartDocument();
            streamWriter.writeStartElement(Element.DOMAIN.getLocalName());
            hostModel.writeContent(streamWriter);
            streamWriter.writeEndDocument();
        }

    }

}
