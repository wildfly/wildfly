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

import static org.jboss.as.controller.parsing.ParseUtils.missingOneOf;
import static org.jboss.as.controller.parsing.ParseUtils.missingRequired;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoAttributes;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoContent;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jboss.as.patching.IoUtils;
import org.jboss.as.patching.metadata.BundleItem;
import org.jboss.as.patching.metadata.MiscContentItem;
import org.jboss.as.patching.metadata.ModificationType;
import org.jboss.as.patching.metadata.ModuleItem;
import org.jboss.as.patching.metadata.Patch;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * Parser for the 1.0 version of the patch-config xsd
 *
 * @author Brian Stansberry (c) 2012 Red Hat Inc.
 */
class PatchConfigXml_1_0 implements XMLStreamConstants, XMLElementReader<PatchConfigBuilder> {

    enum Element {

        ADDED("added"),
        APPLIES_TO_VERSION("applies-to-version"),
        BUNDLES("bundles"),
        DESCRIPTION("description"),
        ELEMENT("element"),
        GENERATE_BY_DIFF("generate-by-diff"),
        IN_RUNTIME_USE("in-runtime-use"),
        MISC_FILES("misc-files"),
        MODULES("modules"),
        NAME("name"),
        ONE_OFF("one-off"),
        PATCH_CONFIG("patch-config"),
        REMOVED("removed"),
        SPECIFIED_CONTENT("specified-content"),
        UPDATED("updated"),
        UPGRADE("cumulative"),

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
        APPLIES_TO_VERSION("applies-to-version"),
        DIRECTORY("directory"),
        IN_RUNTIME_USE("in-runtime-use"),
        NAME("name"),
        PATCH_ID("patch-id"),
        PATH("path"),
        RESULTING_VERSION("resulting-version"),
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

    private boolean patchTypeConfigured;

    @Override
    public void readElement(final XMLExtendedStreamReader reader, final PatchConfigBuilder patchConfigBuilder) throws XMLStreamException {
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case NAME:
                    patchConfigBuilder.setPatchId(reader.getElementText());
                    break;
                case DESCRIPTION:
                    patchConfigBuilder.setDescription(reader.getElementText());
                    break;
                case ELEMENT:
                    parseElement(reader, patchConfigBuilder);
                    break;
                case UPGRADE:
                    parseCumulativePatchType(reader, patchConfigBuilder);
                    break;
                case ONE_OFF:
                    parseOneOffPatchType(reader, patchConfigBuilder);
                    break;
                case GENERATE_BY_DIFF:
                    parseGenerateByDiff(reader, patchConfigBuilder);
                    break;
                case SPECIFIED_CONTENT:
                    parseSpecifiedContent(reader, patchConfigBuilder);
                    break;
                case MISC_FILES:
                    parseMiscFiles(reader, patchConfigBuilder);
                    break;
                default:
                    throw unexpectedElement(reader);
            }
        }
    }

    private void parseElement(XMLExtendedStreamReader reader, PatchConfigBuilder patchConfigBuilder) throws XMLStreamException {

        String patchID = null;

        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case PATCH_ID:
                    patchID = value;
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }

        if (patchID == null) {
            throw missingRequired(reader, EnumSet.of(Attribute.PATCH_ID));
        }

        PatchElementConfigBuilder builder = null;
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case UPGRADE:
                    builder = createBuilder(reader, patchConfigBuilder, patchID, Patch.PatchType.CUMULATIVE);
                    break;
                case ONE_OFF:
                    builder = createBuilder(reader, patchConfigBuilder, patchID, Patch.PatchType.ONE_OFF);
                    break;
                case DESCRIPTION:
                    if (builder == null) {
                        throw missingRequired(reader, "cumulative", "one-off");
                    }
                    builder.setDescription(reader.getElementText());
                    break;
                case SPECIFIED_CONTENT:
                    if (builder == null) {
                        throw missingRequired(reader, "cumulative", "one-off");
                    }
                    parseSpecifiedContent(reader, builder);
                    break;
                default:
                    throw unexpectedElement(reader);
            }
        }

    }

    private PatchElementConfigBuilder createBuilder(XMLExtendedStreamReader reader, PatchConfigBuilder parent, final String patchID, Patch.PatchType type) throws XMLStreamException {

        String layerName = null;

        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case NAME:
                    layerName = value;
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }

        if (layerName == null) {
            throw missingRequired(reader, "name");
        }

        requireNoContent(reader);

        final PatchElementConfigBuilder builder = parent.addElement(layerName);
        builder.setPatchId(patchID);
        builder.setPatchType(type);

        return builder;
    }

    private void parseDistributionStructure(XMLExtendedStreamReader reader, PatchConfigBuilder patchConfigBuilder) throws XMLStreamException {

        if (!patchTypeConfigured) {
            throw missingOneOf(reader, EnumSet.of(Element.UPGRADE, Element.ONE_OFF));
        }

        requireNoAttributes(reader);

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                default:
                    throw unexpectedElement(reader);
            }
        }
    }

    private void parseSlottedContentSearchPaths(XMLExtendedStreamReader reader, PatchConfigBuilder patchConfigBuilder, Element type) throws XMLStreamException {

        requireNoAttributes(reader);

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            if (element == type) {
                parseSlottedContentSearchPath(reader, patchConfigBuilder, type);
            } else {
                throw unexpectedElement(reader);
            }
        }
    }

    private void parseSlottedContentSearchPath(XMLExtendedStreamReader reader, PatchConfigBuilder patchConfigBuilder, Element type) throws XMLStreamException {

        String name = null;
        String path = null;
        PatchConfigBuilder.AffectsType affectsType = PatchConfigBuilder.AffectsType.BOTH;

        Set<Attribute> required = EnumSet.of(Attribute.NAME, Attribute.PATH);

        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            required.remove(attribute);
            switch (attribute) {
                case NAME:
                    name = value;
                    break;
                case PATH:
                    path = value;
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }

        requireNoContent(reader);

        if (!required.isEmpty()) {
            throw missingRequired(reader, required);
        }

    }

    private void parseSlottedContentDefaultExclusion(XMLExtendedStreamReader reader, PatchConfigBuilder patchConfigBuilder, Element type) throws XMLStreamException {

        PatchConfigBuilder.AffectsType affectsType = null;

        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            throw unexpectedAttribute(reader, i);
        }

        requireNoContent(reader);

    }

    private void parseIgnoredPath(XMLExtendedStreamReader reader, PatchConfigBuilder patchConfigBuilder) throws XMLStreamException {

        String path = null;

        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case PATH:
                    path = value;
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }

        requireNoContent(reader);

        if (path == null) {
            throw missingRequired(reader, EnumSet.of(Attribute.PATH));
        }

        // patchConfigBuilder.addIgnoredPath(path, affectsType);
    }

    private void parseCumulativePatchType(final XMLExtendedStreamReader reader, final PatchConfigBuilder builder) throws XMLStreamException {

        String name = null;
        String appliesTo = null;
        String resulting = null;

        Set<Attribute> required = Collections.emptySet(); // EnumSet.of(Attribute.APPLIES_TO_VERSION, Attribute.RESULTING_VERSION);

        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            required.remove(attribute);
            switch (attribute) {
                case NAME:
                    name = value;
                    break;
                case APPLIES_TO_VERSION:
                    appliesTo = value;
                    break;
                case RESULTING_VERSION:
                    resulting = value;
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }

        requireNoContent(reader);

        if (!required.isEmpty()) {
            throw missingRequired(reader, required);
        }

        builder.setAppliesToName(name);
        builder.setCumulativeType(appliesTo, resulting);

        patchTypeConfigured = true;
    }

    private void parseOneOffPatchType(final XMLExtendedStreamReader reader, final PatchConfigBuilder builder) throws XMLStreamException {

        String name = null;
        String appliesTo = null;

        Set<Attribute> required = Collections.emptySet(); // EnumSet.of(Attribute.APPLIES_TO_VERSION, Attribute.RESULTING_VERSION);

        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            required.remove(attribute);
            switch (attribute) {
                case NAME:
                    name = value;
                    break;
                case APPLIES_TO_VERSION:
                    appliesTo = value;
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }

        requireNoContent(reader);

        if (!required.isEmpty()) {
            throw missingRequired(reader, required);
        }

        builder.setOneOffType(appliesTo);
        builder.setAppliesToName(name);

        patchTypeConfigured = true;
    }

    private static void parseGenerateByDiff(XMLExtendedStreamReader reader, PatchConfigBuilder patchConfigBuilder) throws XMLStreamException {

        patchConfigBuilder.setGenerateByDiff(true);
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case IN_RUNTIME_USE:
                    parseInRuntimeUse(reader, patchConfigBuilder);
                    break;
                default:
                    throw unexpectedElement(reader);
            }
        }
    }

    private static void parseInRuntimeUse(XMLExtendedStreamReader reader, PatchConfigBuilder patchConfigBuilder) throws XMLStreamException {

        String path = null;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case PATH:
                    path = value;
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }

        if (path == null) {
            throw missingRequired(reader, Collections.singleton(Attribute.PATH.name));
        }

        requireNoContent(reader);
        patchConfigBuilder.addRuntimeUseItem(path);
    }

    private void parseSpecifiedContent(XMLExtendedStreamReader reader, PatchElementConfigBuilder patchConfigBuilder) throws XMLStreamException {

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case MODULES:
                    parseModules(reader, patchConfigBuilder);
                    break;
                case BUNDLES:
                    parseBundles(reader, patchConfigBuilder);
                    break;
                default:
                    throw unexpectedElement(reader);
            }
        }
    }

    private void parseSpecifiedContent(XMLExtendedStreamReader reader, PatchConfigBuilder patchConfigBuilder) throws XMLStreamException {

        patchConfigBuilder.setGenerateByDiff(false);

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case MISC_FILES:
                    parseMiscFiles(reader, patchConfigBuilder);
                    break;
                default:
                    throw unexpectedElement(reader);
            }
        }
    }

    private void parseModules(final XMLExtendedStreamReader reader, final PatchElementConfigBuilder builder) throws XMLStreamException {
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case ADDED:
                    parseSlottedItem(reader, ModificationType.ADD, false, builder);
                    break;
                case UPDATED:
                    parseSlottedItem(reader, ModificationType.MODIFY, false, builder);
                    break;
                case REMOVED:
                    parseSlottedItem(reader, ModificationType.REMOVE, false, builder);
                    break;
                default:
                    throw unexpectedElement(reader);
            }
        }
    }

    private void parseMiscFiles(final XMLExtendedStreamReader reader, final PatchConfigBuilder builder) throws XMLStreamException {
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case ADDED:
                    parseMiscModification(reader, ModificationType.ADD, builder);
                    break;
                case UPDATED:
                    parseMiscModification(reader, ModificationType.MODIFY, builder);
                    break;
                case REMOVED:
                    parseMiscModification(reader, ModificationType.REMOVE, builder);
                    break;
                default:
                    throw unexpectedElement(reader);
            }
        }
    }

    private void parseBundles(final XMLExtendedStreamReader reader, final PatchElementConfigBuilder builder) throws XMLStreamException {
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case ADDED:
                    parseSlottedItem(reader, ModificationType.ADD, true, builder);
                    break;
                case UPDATED:
                    parseSlottedItem(reader, ModificationType.MODIFY, true, builder);
                    break;
                case REMOVED:
                    parseSlottedItem(reader, ModificationType.REMOVE, true, builder);
                    break;
                default:
                    throw unexpectedElement(reader);
            }
        }
    }

    private void parseSlottedItem(final XMLExtendedStreamReader reader, ModificationType modificationType,
                                  final boolean bundle, final PatchElementConfigBuilder builder) throws XMLStreamException {

        String name = null;
        String slot = "main";

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
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        requireNoContent(reader);

        final ModuleItem item;
        if (bundle) {
            item = new BundleItem(name, slot, IoUtils.NO_CONTENT);
        } else {
            item = new ModuleItem(name, slot, IoUtils.NO_CONTENT);
        }
        builder.getSpecifiedContent().add(item);
    }

    private void parseMiscModification(final XMLExtendedStreamReader reader, final ModificationType modificationType,
                                       final PatchConfigBuilder builder) throws XMLStreamException {

        String path = null;
        boolean directory = false;
        boolean affectsRuntime = false;

        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case DIRECTORY:
                    directory = Boolean.parseBoolean(value);
                    break;
                case PATH:
                    path = value;
                    break;
                case IN_RUNTIME_USE:
                    affectsRuntime = Boolean.parseBoolean(value);
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        requireNoContent(reader);

        if (affectsRuntime) {
            builder.addRuntimeUseItem(path);
        }

        final String[] parsed = path.split("/");
        final MiscContentItem item;
        if (parsed.length > 0) {
            item = new MiscContentItem(parsed[parsed.length -1], Arrays.copyOf(parsed, parsed.length -1), IoUtils.NO_CONTENT);
        } else {
            item = new MiscContentItem(path, new String[0], IoUtils.NO_CONTENT);
        }
        builder.getSpecifiedContent().add(item);
    }



}
