/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

import static org.jboss.as.patching.IoUtils.safeClose;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;

import org.jboss.staxmapper.XMLMapper;

/**
 * Xml model for {@code Distribution}.
 *
 * @author Emanuel Muckenhuber
 */
class DistributionXml {

    public static final String DISTRIBUTION_XML = "distribution.xml";

    private static final XMLMapper MAPPER = XMLMapper.Factory.create();
    private static final DistributionXml_1_0 INSTANCE = new DistributionXml_1_0();
    private static final XMLInputFactory INPUT_FACTORY = XMLInputFactory.newInstance();
    private static final XMLOutputFactory OUTPUT_FACTORY = XMLOutputFactory.newFactory();
    private static final QName ROOT_ELEMENT = new QName(Namespace.DISTRIBUTION_1_0.getNamespace(), "distribution");

    static {
        MAPPER.registerRootElement(ROOT_ELEMENT, INSTANCE);
    }

    enum Namespace {

        DISTRIBUTION_1_0("urn:jboss:distribution:1.0"),
        UNKNOWN(null),;

        private final String namespace;

        Namespace(String namespace) {
            this.namespace = namespace;
        }

        public String getNamespace() {
            return namespace;
        }

    }

    private DistributionXml() {
        //
    }

    public static void marshal(final Writer writer, final Distribution root) throws XMLStreamException {
        final XMLOutputFactory outputFactory = OUTPUT_FACTORY;
        final XMLStreamWriter streamWriter = outputFactory.createXMLStreamWriter(writer);
        MAPPER.deparseDocument(INSTANCE, root, streamWriter);
        streamWriter.close();
    }

    public static void marshal(final OutputStream os, final Distribution root) throws XMLStreamException {
        final XMLOutputFactory outputFactory = OUTPUT_FACTORY;
        final XMLStreamWriter streamWriter = outputFactory.createXMLStreamWriter(os);
        MAPPER.deparseDocument(INSTANCE, root, streamWriter);
        streamWriter.close();
    }

    public static Distribution parse(final InputStream stream) throws XMLStreamException {
        try {
            final XMLInputFactory inputFactory = INPUT_FACTORY;
            setIfSupported(inputFactory, XMLInputFactory.IS_VALIDATING, Boolean.FALSE);
            setIfSupported(inputFactory, XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
            final XMLStreamReader streamReader = inputFactory.createXMLStreamReader(stream);
            //
            final Distribution root = new Distribution();
            MAPPER.parseDocument(root, streamReader);
            return root;
        } finally {
            safeClose(stream);
        }
    }

    public static Distribution parse(final File patchXml) throws IOException, XMLStreamException {
        final InputStream is = new FileInputStream(patchXml);
        try {
            return parse(is);
        } finally {
            safeClose(is);
        }
    }

    private static void setIfSupported(final XMLInputFactory inputFactory, final String property, final Object value) {
        if (inputFactory.isPropertySupported(property)) {
            inputFactory.setProperty(property, value);
        }
    }
}