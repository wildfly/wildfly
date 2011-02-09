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

package org.jboss.as.controller;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLMapper;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

/**
 * A configuration persister which uses an XML file for backing storage.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public class XmlConfigurationPersister implements ConfigurationPersister {
    private final File fileName;
    private final QName rootElement;
    private final XMLElementReader<ModelNode> rootParser;
    private final XMLElementWriter<ModelNode> rootDeparser;

    /**
     * Construct a new instance.
     *
     * @param fileName the configuration base file name
     * @param rootElement the root element of the configuration file
     * @param rootParser the root model parser
     * @param rootDeparser the root model deparser
     */
    public XmlConfigurationPersister(final File fileName, final QName rootElement, final XMLElementReader<ModelNode> rootParser, final XMLElementWriter<ModelNode> rootDeparser) {
        this.fileName = fileName;
        this.rootElement = rootElement;
        this.rootParser = rootParser;
        this.rootDeparser = rootDeparser;
    }

    /**
     * Back up an old configuration file before overwriting it.
     *
     * @param fileName the file name being overwritten
     * @throws ConfigurationPersistenceException if the backup fails
     */
    protected void backup(File fileName) throws ConfigurationPersistenceException {
        // todo - either provide a default impl or keep this pluggable
    }

    /** {@inheritDoc} */
    public void store(final ModelNode model) throws ConfigurationPersistenceException {
        final XMLMapper mapper = XMLMapper.Factory.create();
        try {
            final FileOutputStream fos = new FileOutputStream(fileName);
            try {
                BufferedOutputStream output = new BufferedOutputStream(fos);
                final XMLStreamWriter streamWriter = XMLOutputFactory.newInstance().createXMLStreamWriter(output);
                mapper.deparseDocument(rootDeparser, model, streamWriter);
                streamWriter.close();
                output.close();
                fos.close();
            } finally {
                StreamUtils.safeClose(fos);
            }
        } catch (Exception e) {
            throw new ConfigurationPersistenceException("Failed to store configuration", e);
        }
    }

    /** {@inheritDoc} */
    public List<ModelNode> load() throws ConfigurationPersistenceException {
        final XMLMapper mapper = XMLMapper.Factory.create();
        mapper.registerRootElement(rootElement, rootParser);
        final List<ModelNode> updates = new ArrayList<ModelNode>();
        try {
            final FileInputStream fis = new FileInputStream(fileName);
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
        } catch (Exception e) {
            throw new ConfigurationPersistenceException("Failed to parse configuration", e);
        }
        return updates;
    }
}
