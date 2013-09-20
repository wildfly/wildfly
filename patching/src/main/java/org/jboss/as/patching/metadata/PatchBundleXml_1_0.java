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

package org.jboss.as.patching.metadata;

import static org.jboss.as.controller.parsing.ParseUtils.missingRequired;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoContent;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * @author Emanuel Muckenhuber
 */
public class PatchBundleXml_1_0 implements XMLStreamConstants, XMLElementReader<PatchXml.Result<BundledPatch>>, XMLElementWriter<BundledPatch> {

    enum Element {

        PATCHES("patches"),
        PATCH_ELEMENT("element"),

        // default unknown element
        UNKNOWN(null),
        ;

        public final String name;
        Element(String name) {
            this.name = name;
        }

        static Map<String, Element> elements = new HashMap<String, Element>();
        static {
            for(Element element : Element.values()) {
                if(element != UNKNOWN) {
                    elements.put(element.name, element);
                }
            }
        }

        static Element forName(String name) {
            final Element element = elements.get(name);
            return element == null ? UNKNOWN : element;
        }

    }

    enum Attribute {

        ID("id"),
        PATH("path"),

        // default unknown attribute
        UNKNOWN(null);

        private final String name;
        Attribute(String name) {
            this.name = name;
        }

        static Map<String, Attribute> attributes = new HashMap<String, Attribute>();
        static {
            for(Attribute attribute : Attribute.values()) {
                if(attribute != UNKNOWN) {
                    attributes.put(attribute.name, attribute);
                }
            }
        }

        static Attribute forName(String name) {
            final Attribute attribute = attributes.get(name);
            return attribute == null ? UNKNOWN : attribute;
        }
    }

    @Override
    public void readElement(final XMLExtendedStreamReader reader, final PatchXml.Result<BundledPatch> result) throws XMLStreamException {

        final List<BundledPatch.BundledPatchEntry> patches = new ArrayList<BundledPatch.BundledPatchEntry>();
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final String localName = reader.getLocalName();
            final Element element = Element.forName(localName);
            switch (element) {
                case PATCH_ELEMENT:
                    parseElement(reader, patches);
                    break;
                default:
                    throw unexpectedElement(reader);
            }
        }
        result.setResult(new BundledPatch() {
            @Override
            public List<BundledPatchEntry> getPatches() {
                return patches;
            }
        });
    }

    private void parseElement(XMLExtendedStreamReader reader, List<BundledPatch.BundledPatchEntry> patches) throws XMLStreamException {

        String id = null;
        String path = null;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case ID:
                    id = value;
                    break;
                case PATH:
                    path = value;
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }

        if (id == null) {
            throw missingRequired(reader, Attribute.ID.name);
        }
        if (path == null) {
            throw missingRequired(reader, Attribute.PATH.name);
        }
        //
        requireNoContent(reader);
        patches.add(new BundledPatch.BundledPatchEntry(id, path));
    }

    @Override
    public void writeContent(XMLExtendedStreamWriter writer, BundledPatch bundledPatch) throws XMLStreamException {

        writer.writeStartElement(Element.PATCHES.name);
        writer.writeDefaultNamespace(PatchXml.Namespace.PATCH_BUNDLE_1_0.getNamespace());

        for (final BundledPatch.BundledPatchEntry entry : bundledPatch.getPatches()) {
            writer.writeEmptyElement(Element.PATCH_ELEMENT.name);
            writer.writeAttribute(Attribute.ID.name, entry.getPatchId());
            writer.writeAttribute(Attribute.PATH.name, entry.getPatchPath());
        }

        writer.writeEndElement();
    }
}
