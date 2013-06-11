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

import static org.jboss.as.controller.parsing.ParseUtils.missingRequired;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoContent;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;
import static org.jboss.as.patching.HashUtils.hexStringToByteArray;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jboss.as.patching.HashUtils;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * @author Emanuel Muckenhuber
 */
class DistributionXml_1_0 implements XMLStreamConstants, XMLElementReader<Distribution>, XMLElementWriter<Distribution> {

    enum Element {

        ADD_ON("add-on"),
        ADD_ONS("add-ons"),
        BUNDLE("bundle"),
        BUNDLES("bundles"),
        DISTRIBUTION("distribution"),
        FILES("files"),
        LAYER("layer"),
        LAYERS("layers"),
        MODULE("module"),
        MODULES("modules"),
        NODE("node"),

        // default unknown element
        UNKNOWN(null),;

        final String name;

        Element(String name) {
            this.name = name;
        }

        static Map<String, Element> elements = new HashMap<String, Element>();

        static {
            for (Element element : Element.values()) {
                if (element != UNKNOWN) {
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

        DIRECTORY("directory"),
        COMPARISON_HASH("comparison-hash"),
        METADATA_HASH("metadata-hash"),
        NAME("name"),
        SLOT("slot"),

        // default unknown attribute
        UNKNOWN(null);

        private final String name;

        Attribute(String name) {
            this.name = name;
        }

        static Map<String, Attribute> attributes = new HashMap<String, Attribute>();

        static {
            for (Attribute attribute : Attribute.values()) {
                if (attribute != UNKNOWN) {
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
    public void writeContent(final XMLExtendedStreamWriter writer, final Distribution distribution) throws XMLStreamException {

        // Get started ...
        writer.writeStartDocument();
        writer.writeStartElement(Element.DISTRIBUTION.name);
        writer.writeDefaultNamespace(DistributionXml.Namespace.DISTRIBUTION_1_0.getNamespace());

        final DistributionContentItem root = distribution.getRoot();
        final Collection<DistributionContentItem> children = root.getChildren();

        final Set<String> layers = distribution.getLayers();
        if (!layers.isEmpty()) {
            writer.writeStartElement(Element.LAYERS.name);
            for (final String layerName : layers) {
                final Distribution.ProcessedLayer layer = distribution.getLayer(layerName);
                writeLayer(writer, layer, Element.LAYER);
            }
            writer.writeEndElement();
        }
        final Set<String> addOns = distribution.getAddOns();
        if (!addOns.isEmpty()) {
            writer.writeStartElement(Element.ADD_ONS.name);
            for (final String addOnName : addOns) {
                final Distribution.ProcessedLayer addOn = distribution.getAddOn(addOnName);
                writeLayer(writer, addOn, Element.ADD_ON);
            }
            writer.writeEndElement();
        }

        // The misc tree
        writer.writeStartElement(Element.FILES.name);
        writeChildren(writer, children);
        writer.writeEndElement();

        // Done
        writer.writeEndElement();
        writer.writeEndDocument();
    }

    private static void writeLayer(final XMLExtendedStreamWriter writer, final Distribution.ProcessedLayer layer, final Element element) throws XMLStreamException {

        writer.writeStartElement(element.name);
        writer.writeAttribute(Attribute.NAME.name, layer.getName());

        final Set<DistributionModuleItem> bundles = layer.getBundles();
        final Set<DistributionModuleItem> modules = layer.getModules();

        if (!bundles.isEmpty()) {
            writer.writeStartElement(Element.BUNDLES.name);
            for (final DistributionModuleItem item : bundles) {
                writeModuleItem(writer, item, Element.BUNDLE);
            }
            writer.writeEndElement();
        }
        if (!modules.isEmpty()) {
            writer.writeStartElement(Element.MODULES.name);
            for (final DistributionModuleItem item : modules) {
                writeModuleItem(writer, item, Element.MODULE);
            }
            writer.writeEndElement();
        }
        writer.writeEndElement();
    }

    private static void writeModuleItem(final XMLExtendedStreamWriter writer, final DistributionModuleItem item, final Element element) throws XMLStreamException {
        writer.writeEmptyElement(element.name);
        writer.writeAttribute(Attribute.NAME.name, item.getName());
        writer.writeAttribute(Attribute.SLOT.name, item.getSlot());
        writer.writeAttribute(Attribute.COMPARISON_HASH.name, HashUtils.bytesToHexString(item.getComparisonHash()));
        writer.writeAttribute(Attribute.METADATA_HASH.name, HashUtils.bytesToHexString(item.getMetadataHash()));
    }

    private static void writeChildren(final XMLExtendedStreamWriter writer, final Collection<DistributionContentItem> items) throws XMLStreamException {
        if (items == null || items.size() == 0) {
            return;
        }

        for (final DistributionContentItem item : items) {

            writer.writeStartElement(Element.NODE.name);
            writer.writeAttribute(Attribute.NAME.name, item.getName());
            if (item.isLeaf()) {
                writer.writeAttribute(Attribute.COMPARISON_HASH.name, HashUtils.bytesToHexString(item.getComparisonHash()));
                writer.writeAttribute(Attribute.METADATA_HASH.name, HashUtils.bytesToHexString(item.getMetadataHash()));
            }
            writer.writeAttribute(Attribute.DIRECTORY.name, String.valueOf(!item.isLeaf()));

            // Recurse
            final Collection<DistributionContentItem> children = item.getChildren();
            writeChildren(writer, children);

            writer.writeEndElement();
        }

    }

    @Override
    public void readElement(final XMLExtendedStreamReader reader, final Distribution distribution) throws XMLStreamException {
        final DistributionContentItem root = distribution.getRoot();
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case LAYERS:
                case ADD_ONS:
                    readLayers(reader, distribution);
                    break;
                case FILES:
                    readNodes(reader, root);
                    break;
                default:
                    throw unexpectedElement(reader);
            }
        }
    }

    protected static void readLayers(XMLExtendedStreamReader reader, final Distribution distribution) throws XMLStreamException {
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case LAYER:
                    readLayer(reader, distribution, false);
                    break;
                case ADD_ON:
                    readLayer(reader, distribution, true);
                    break;
                default:
                    throw unexpectedElement(reader);
            }
        }
    }

    protected static void readLayer(XMLExtendedStreamReader reader, final Distribution distribution, boolean addOn) throws XMLStreamException {
        String name = null;

        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case NAME:
                    name = value;
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }

        if (name == null) {
            throw missingRequired(reader, "name");
        }

        final Distribution.ProcessedLayer layer = addOn ? distribution.addAddOn(name) : distribution.addLayer(name);
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case MODULES:
                    readModuleItems(reader, layer);
                    break;
                case BUNDLES:
                    readModuleItems(reader, layer);
                    break;
                default:
                    throw unexpectedElement(reader);
            }
        }
    }

    protected static void readModuleItems(final XMLExtendedStreamReader reader, final Distribution.ProcessedLayer layer) throws XMLStreamException {
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case BUNDLE: {
                    final DistributionModuleItem item = readModuleItem(reader);
                    layer.getBundles().add(item);
                    break;
                }
                case MODULE: {
                    final DistributionModuleItem item = readModuleItem(reader);
                    layer.getModules().add(item);
                    break;
                }
                default:
                    throw unexpectedElement(reader);
            }
        }
    }

    protected static DistributionModuleItem readModuleItem(final XMLExtendedStreamReader reader) throws XMLStreamException {

        String name = null;
        String slot = null;
        String metadata = null;
        String comparison = null;

        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case NAME:
                    name = value;
                    break;
                case SLOT:
                    slot = value;
                    break;
                case COMPARISON_HASH:
                    comparison = value;
                    break;
                case METADATA_HASH:
                    metadata = value;
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        if (name == null || slot == null || comparison == null || metadata == null) {
            throw missingRequired(reader, "name", "slot", "comparison", "metadata");
        }
        requireNoContent(reader);
        return new DistributionModuleItem(name, slot, hexStringToByteArray(comparison), hexStringToByteArray(metadata));
    }

    protected static void readNodes(XMLExtendedStreamReader reader, DistributionContentItem parent) throws XMLStreamException {
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case NODE:
                    readNode(reader, parent);
                    break;
                default:
                    throw unexpectedElement(reader);
            }
        }
    }

    protected static void readNode(XMLExtendedStreamReader reader, DistributionContentItem parent) throws XMLStreamException {

        String name = null;
        String directory = null;
        String comparison = "";
        String metadata = "";

        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case NAME:
                    name = value;
                    break;
                case DIRECTORY:
                    directory = value;
                    break;
                case COMPARISON_HASH:
                    comparison = value;
                    break;
                case METADATA_HASH:
                    metadata = value;
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }

        if (name == null) {
            throw missingRequired(reader, "name");
        }
        final DistributionItemImpl item = new DistributionItemImpl(parent, name, hexStringToByteArray(comparison), hexStringToByteArray(metadata), !Boolean.valueOf(directory));
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case NODE:
                    readNode(reader, item);
                    break;
                default:
                    throw unexpectedElement(reader);
            }
        }
        parent.getChildren().add(item);
    }

}

