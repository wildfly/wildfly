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

package org.jboss.as.domain.controller;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.model.DomainModel;
import org.jboss.as.model.Element;
import org.jboss.logging.Logger;
import org.jboss.staxmapper.XMLContentWriter;
import org.jboss.staxmapper.XMLExtendedStreamWriter;
import org.jboss.staxmapper.XMLMapper;

/**
 * Default implementation of {@link DomainConfigurationPersister}.
 *
 * @author Brian Stansberry
 */
public class DomainConfigurationPersisterImpl implements DomainConfigurationPersister {

    /** The expected name of the domain configuration file */
    public static final String CONFIG_FILE = "domain.xml";

    /** Suffix to append to the config file name to create the backup copy. */
    public static final String BACKUP_SUFFIX = ".last-known-good";

    private static Logger logger = Logger.getLogger("org.jboss.as.domain");

    private final File configFile;

    public DomainConfigurationPersisterImpl(File configDir) {

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
    public Reader getConfigurationReader() throws IOException {
        return new FileReader(configFile);
    }

    @Override
    public void persistConfiguration(final DomainModel domainModel) {

        FileOutputStream fos = null;
        BufferedOutputStream bos = null;
        OutputStreamWriter writer = null;
        try {
            backupConfigFile();
            configFile.createNewFile();
            fos = new FileOutputStream(configFile);
            bos = new BufferedOutputStream(fos);
            writer = new OutputStreamWriter(bos);
            final XMLMapper mapper = XMLMapper.Factory.create();
            mapper.deparseDocument(new RootElementWriter(domainModel), XMLOutputFactory.newInstance().createXMLStreamWriter(writer));
        }
        catch (Exception e) {
            logger.errorf(e, "Failed persisting configuration file %s" , configFile.getAbsolutePath());
        }
        finally {
             if (writer != null) {
                 try {
                    writer.close();
                } catch (IOException e) {
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

        private final DomainModel domainModel;

        private RootElementWriter(final DomainModel domainModel) {
            this.domainModel = domainModel;
        }

        @Override
        public void writeContent(XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
            streamWriter.writeStartDocument();
            streamWriter.writeStartElement(Element.DOMAIN.getLocalName());
            domainModel.writeContent(streamWriter);
            streamWriter.writeEndDocument();
        }

    }

}
