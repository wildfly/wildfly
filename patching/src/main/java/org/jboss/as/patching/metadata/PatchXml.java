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

import org.jboss.as.patching.runner.PatchUtils;
import org.jboss.staxmapper.XMLExtendedStreamReader;

import javax.xml.stream.Location;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Set;

/**
 * @author Emanuel Muckenhuber
 */
public class PatchXml implements XMLStreamConstants {

    public static final String PATCH_XML = "patch.xml";
    private static final XMLInputFactory INPUT_FACTORY = XMLInputFactory.newInstance();

    public static Patch parse(final InputStream stream) throws XMLStreamException {
        try {
            final XMLInputFactory inputFactory = INPUT_FACTORY;
            setIfSupported(inputFactory, XMLInputFactory.IS_VALIDATING, Boolean.FALSE);
            setIfSupported(inputFactory, XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
            final XMLStreamReader streamReader = inputFactory.createXMLStreamReader(stream);
            return parseDocument(streamReader);
        } finally {
            PatchUtils.safeClose(stream);
        }
    }

    static Patch parseDocument(final XMLStreamReader reader) throws XMLStreamException {
        while(reader.hasNext()) {
            switch (reader.nextTag()) {
                case START_DOCUMENT:
                    return parsePatch(reader);
                default:
                    throw unexpectedElement(reader);
            }
        }
        throw endOfDocument(reader.getLocation());
    }

    static Patch parsePatch(final XMLStreamReader reader) throws XMLStreamException {
        while(reader.hasNext()) {
            switch (reader.nextTag()) {
                case START_ELEMENT:
                    break;
                default:
                    throw unexpectedElement(reader);
            }
        }
        throw endOfDocument(reader.getLocation());
    }


    private static XMLStreamException unexpectedElement(final XMLStreamReader reader) {
        return new XMLStreamException("Unexpected element '" + reader.getName() + "' encountered", reader.getLocation());
    }

    private static XMLStreamException unexpectedEndElement(final XMLStreamReader reader) {
        return new XMLStreamException("Unexpected end of element '" + reader.getName() + "' encountered", reader.getLocation());
    }

    private static XMLStreamException unexpectedAttribute(final XMLStreamReader reader, final int index) {
        return new XMLStreamException("Unexpected attribute '" + reader.getAttributeName(index) + "' encountered",
                reader.getLocation());
    }

    private static XMLStreamException endOfDocument(final Location location) {
        return new XMLStreamException("Unexpected end of document", location);
    }

    private static XMLStreamException missingRequired(final XMLExtendedStreamReader reader, final Set<?> required) {
        final StringBuilder b = new StringBuilder();
        Iterator<?> iterator = required.iterator();
        while (iterator.hasNext()) {
            final Object o = iterator.next();
            b.append(o.toString());
            if (iterator.hasNext()) {
                b.append(", ");
            }
        }
        return new XMLStreamException("Missing required attribute(s): " + b, reader.getLocation());
    }

    private static void setIfSupported(final XMLInputFactory inputFactory, final String property, final Object value) {
        if (inputFactory.isPropertySupported(property)) {
            inputFactory.setProperty(property, value);
        }
    }

}