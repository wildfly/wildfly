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
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.InputStream;

import org.jboss.staxmapper.XMLMapper;

/**
 * Parser for a patch generation configuration document.
 *
 * @author Brian Stansberry (c) 2012 Red Hat Inc.
 */
public class PatchConfigXml {

    private static final XMLMapper MAPPER = XMLMapper.Factory.create();
    private static final PatchConfigXml_1_0 INSTANCE = new PatchConfigXml_1_0();
    private static final XMLInputFactory INPUT_FACTORY = XMLInputFactory.newInstance();
    private static final QName ROOT_ELEMENT = new QName(Namespace.PATCH_1_0.getNamespace(), PatchConfigXml_1_0.Element.PATCH_CONFIG.name);

    static {
        MAPPER.registerRootElement(ROOT_ELEMENT, INSTANCE);
    }

    enum Namespace {

        PATCH_1_0("urn:jboss:patch-config:1.0"),
        UNKNOWN(null),;

        private final String namespace;

        Namespace(String namespace) {
            this.namespace = namespace;
        }

        public String getNamespace() {
            return namespace;
        }

    }

    public static PatchConfig parse(final InputStream stream) throws XMLStreamException {
        try {
            final XMLInputFactory inputFactory = INPUT_FACTORY;
            setIfSupported(inputFactory, XMLInputFactory.IS_VALIDATING, Boolean.FALSE);
            setIfSupported(inputFactory, XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
            final XMLStreamReader streamReader = inputFactory.createXMLStreamReader(stream);
            //
            final PatchConfigBuilder builder = new PatchConfigBuilder();
            MAPPER.parseDocument(builder, streamReader);
            return builder.build();
        } finally {
            safeClose(stream);
        }
    }

    private static void setIfSupported(final XMLInputFactory inputFactory, final String property, final Object value) {
        if (inputFactory.isPropertySupported(property)) {
            inputFactory.setProperty(property, value);
        }
    }

    private PatchConfigXml() {
        //
    }
}
