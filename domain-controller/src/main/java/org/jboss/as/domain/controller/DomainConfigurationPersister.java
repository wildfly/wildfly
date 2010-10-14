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
import java.io.IOException;
import java.io.OutputStreamWriter;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.model.DomainModel;
import org.jboss.as.model.Element;
import org.jboss.logging.Logger;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.staxmapper.XMLContentWriter;
import org.jboss.staxmapper.XMLExtendedStreamWriter;
import org.jboss.staxmapper.XMLMapper;

/**
 * Handles persistence of the {@link DomainModel}.
 *
 * @author Brian Stansberry
 */
public class DomainConfigurationPersister implements Service<DomainConfigurationPersister> {

    /**
     * ServiceName under which an instance of this service should be registered.
     */
    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("domain-configuration-persister");

    private static Logger logger = Logger.getLogger("org.jboss.as.domain");

    private final InjectedValue<DomainController> injectedDomainController = new InjectedValue<DomainController>();
    private File configFile;

    public static void addService(BatchBuilder batchBuilder) {
        DomainConfigurationPersister service = new DomainConfigurationPersister();
        batchBuilder.addService(SERVICE_NAME, service)
        .addDependency(DomainController.SERVICE_NAME, DomainController.class, service.injectedDomainController);
    }

    public void configurationModified() {

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
            mapper.deparseDocument(new RootElementWriter(), XMLOutputFactory.newInstance().createXMLStreamWriter(writer));
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
        File backup = new File(configFile.getParent(), configFile.getName() + ".last-known-good");
        copyFile(configFile, backup);
    }

    private void copyFile(File file, File backup) throws IOException {

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

    @Override
    public void start(StartContext context) throws StartException {
        // Validate injections
        DomainController dc = null;
        try {
            dc = injectedDomainController.getValue();
        }
        catch (IllegalStateException ise) {
            throw new StartException(DomainController.class.getSimpleName() + " was not injected");
        }

        File configDir = dc.getDomainConfigDir();
        if (configDir == null)
            throw new StartException("Domain configuration directory is null");
        File configFile = new File(configDir, "domain.xml");
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
    public void stop(StopContext context) {
        // no-op;
    }

    @Override
    public DomainConfigurationPersister getValue() throws IllegalStateException {
        return this;
    }

    private class RootElementWriter implements XMLContentWriter {

        @Override
        public void writeContent(XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
            streamWriter.writeStartDocument();
            streamWriter.writeStartElement(Element.DOMAIN.getLocalName());
            injectedDomainController.getValue().getDomainModel().writeContent(streamWriter);
            streamWriter.writeEndDocument();
        }

    }

}
