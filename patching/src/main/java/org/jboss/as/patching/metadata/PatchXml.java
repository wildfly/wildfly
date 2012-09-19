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

package org.jboss.as.patching.metadata;

import static org.jboss.as.patching.IoUtils.safeClose;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.jboss.staxmapper.XMLMapper;

/**
 * @author Emanuel Muckenhuber
 */
public class PatchXml {

    public static final String PATCH_XML = "patch.xml";

    private static final XMLMapper MAPPER = XMLMapper.Factory.create();
    private static final PatchXml_1_0 INSTANCE = new PatchXml_1_0();
    private static final XMLInputFactory INPUT_FACTORY = XMLInputFactory.newInstance();
    private static final XMLOutputFactory OUTPUT_FACTORY = XMLOutputFactory.newFactory();
    private static final QName ROOT_ELEMENT = new QName(Namespace.PATCH_1_0.getNamespace(), PatchXml_1_0.Element.PATCH.name);

    static {
        MAPPER.registerRootElement(ROOT_ELEMENT, INSTANCE);
    }

    enum Namespace {

        PATCH_1_0("urn:jboss:patch:1.0"),
        UNKNOWN(null),
        ;

        private final String namespace;
        Namespace(String namespace) {
            this.namespace = namespace;
        }

        public String getNamespace() {
            return namespace;
        }

    }

    private PatchXml() {
        //
    }

    public static void marshal(final Writer writer, final Patch patch) throws XMLStreamException {
        final XMLOutputFactory outputFactory = OUTPUT_FACTORY;
        final XMLStreamWriter streamWriter = outputFactory.createXMLStreamWriter(writer);
        MAPPER.deparseDocument(INSTANCE, patch, streamWriter);
        streamWriter.close();
    }

    public static void marshal(final OutputStream os, final Patch patch) throws XMLStreamException {
        final XMLOutputFactory outputFactory = OUTPUT_FACTORY;
        final XMLStreamWriter streamWriter = outputFactory.createXMLStreamWriter(os);
        MAPPER.deparseDocument(INSTANCE, patch, streamWriter);
        streamWriter.close();
    }

    public static Patch parse(final InputStream stream) throws XMLStreamException {
        try {
            final XMLInputFactory inputFactory = INPUT_FACTORY;
            setIfSupported(inputFactory, XMLInputFactory.IS_VALIDATING, Boolean.FALSE);
            setIfSupported(inputFactory, XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
            final XMLStreamReader streamReader = inputFactory.createXMLStreamReader(stream);
            //
            final PatchBuilder builder = PatchBuilder.create();
            MAPPER.parseDocument(builder, streamReader);
            return builder.build();
        } finally {
            safeClose(stream);
        }
    }

    public static Patch parse(final File patchXml) throws IOException, XMLStreamException {
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
