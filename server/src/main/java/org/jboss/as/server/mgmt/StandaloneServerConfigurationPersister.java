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

package org.jboss.as.server.mgmt;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import java.util.ArrayList;
import java.util.List;
import org.jboss.as.model.AbstractServerModelUpdate;
import org.jboss.as.model.Element;
import org.jboss.as.model.ModelXmlParsers;
import org.jboss.as.model.Namespace;
import org.jboss.as.model.ServerModel;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.as.server.ServerController;
import org.jboss.logging.Logger;
import org.jboss.staxmapper.XMLContentWriter;
import org.jboss.staxmapper.XMLExtendedStreamWriter;
import org.jboss.staxmapper.XMLMapper;
import org.jboss.vfs.VFSUtils;

/**
 * Implementation of {@link ServerConfigurationPersister} for standalone server instances.
 *
 * @author Brian Stansberry
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class StandaloneServerConfigurationPersister implements ServerConfigurationPersister {

    private static Logger logger = Logger.getLogger("org.jboss.as.server");

    private static final String STANDALONE_XML = "standalone.xml";

    public StandaloneServerConfigurationPersister() {
    }

    public void persist(final ServerController serverController, final ServerModel serverModel) {
        final File configFile = getConfigFile(serverController);
        try {
            backupConfigFile(configFile);
            configFile.createNewFile();
            final FileOutputStream fos = new FileOutputStream(configFile);
            try {
                final BufferedOutputStream bos = new BufferedOutputStream(fos);
                try {
                    final XMLStreamWriter writer = XMLOutputFactory.newInstance().createXMLStreamWriter(bos);
                    try {
                        final XMLMapper mapper = XMLMapper.Factory.create();
                        mapper.deparseDocument(new RootElementWriter(serverModel), writer);
                    } finally {
                        StreamUtils.safeClose(writer);
                    }
                } finally {
                    StreamUtils.safeClose(bos);
                }
            } finally {
                StreamUtils.safeClose(fos);
            }
        } catch (Exception e) {
            logger.errorf(e, "Failed to persist configuration file \"%s\"" , configFile.getAbsolutePath());
        }
    }

    private File getConfigFile(final ServerController serverController) {
        return new File(serverController.getServerEnvironment().getServerConfigurationDir(), STANDALONE_XML);
    }

    public List<AbstractServerModelUpdate<?>> load(final ServerController serverController) throws Exception {
        final File configFile = getConfigFile(serverController);
        final List<AbstractServerModelUpdate<?>> updates = new ArrayList<AbstractServerModelUpdate<?>>();
        final XMLMapper mapper = XMLMapper.Factory.create();
        mapper.registerRootElement(new QName(Namespace.DOMAIN_1_0.getUriString(), Element.SERVER.getLocalName()), ModelXmlParsers.SERVER_XML_READER);
        final FileInputStream fis = new FileInputStream(configFile);
        try {
            BufferedInputStream input = new BufferedInputStream(fis);
            XMLStreamReader streamReader = XMLInputFactory.newInstance().createXMLStreamReader(input);
            mapper.parseDocument(updates, streamReader);
            streamReader.close();
            input.close();
            fis.close();
        } finally {
            StreamUtils.safeClose(fis);
        }
        return updates;
    }

    private void backupConfigFile(final File configFile) throws IOException {
        File backup = new File(configFile.getParent(), configFile.getName() + ".last-known-good");
        moveFile(configFile, backup);
    }

    private void moveFile(File file, File backup) throws IOException {

        if (backup.exists())
            backup.delete();

        if (!file.renameTo(backup) && file.exists()) {
            final FileInputStream fis = new FileInputStream(file);
            try {
                final FileOutputStream fos = new FileOutputStream(backup);
                try {
                    VFSUtils.copyStreamAndClose(fis, fos);
                } finally {
                    VFSUtils.safeClose(fos);
                }
            } finally {
                VFSUtils.safeClose(fis);
            }
        }
    }

    private class RootElementWriter implements XMLContentWriter {

        private final ServerModel configuration;

        private RootElementWriter(final ServerModel configuration) {
            this.configuration = configuration;
        }

        @Override
        public void writeContent(XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
            streamWriter.writeStartDocument();
            streamWriter.writeStartElement(Element.SERVER.getLocalName());
            configuration.writeContent(streamWriter);
            streamWriter.writeEndDocument();
        }

    }

}
