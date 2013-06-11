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
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.as.patching.PatchMessages;
import org.jboss.as.patching.metadata.ModificationType;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * Parser for the 1.0 version of the patch-config xsd
 *
 * @author Brian Stansberry (c) 2012 Red Hat Inc.
 */
class PatchConfigXml_1_0 implements XMLStreamConstants, XMLElementReader<PatchConfigBuilder> {

    enum Element {

        ADDED_BUNDLE("added-bundle"),
        ADDED_MISC_CONTENT("added-misc-content"),
        ADDED_MODULE("added-module"),
        APPLIES_TO_VERSION("applies-to-version"),
        BUNDLES("bundles"),
        BUNDLE_SEARCH_PATH("bundle-search-path"),
        BUNDLE_SEARCH_PATHS("bundle-search-paths"),
        CUMULATIVE("cumulative"),
        DESCRIPTION("description"),
        DISTRIBUTION_STRUCTURE("distribution-structure"),
        EXCLUDE_DEFAULT("exclude-default"),
        GENERATE_BY_DIFF("generate-by-diff"),
        IGNORED_PATH("ignored-path"),
        IN_RUNTIME_USE("in-runtime-use"),
        MISC_FILES("misc-files"),
        MODULES("modules"),
        MODULE_SEARCH_PATH("module-search-path"),
        MODULE_SEARCH_PATHS("module-search-paths"),
        NAME("name"),
        ONE_OFF("one-off"),
        PATCH_CONFIG("patch-config"),
        REMOVED_BUNDLE("removed-bundle"),
        REMOVED_MISC_CONTENT("removed-misc-content"),
        REMOVED_MODULE("removed-module"),
        SPECIFIED_CONTENT("specified-content"),
        UPDATED_BUNDLE("updated-bundle"),
        UPDATED_MISC_CONTENT("updated-misc-content"),
        UPDATED_MODULE("updated-module"),

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
        AFFECTS_TYPE("affects-type"),
        APPLIES_TO_VERSION("applies-to-version"),
        DIRECTORY("directory"),
        EXISTING_PATH("existing-path"),
        IN_RUNTIME_USE("in-runtime-use"),
        NAME("name"),
        PATH("path"),
        RESULTING_VERSION("resulting-version"),
        SEARCH_PATH("search-path"),
        SLOT("slot"),

        // default unknown attribute
        UNKNOWN(null),;

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
                case DISTRIBUTION_STRUCTURE:
                    parseDistributionStructure(reader, patchConfigBuilder);
                    break;
                case CUMULATIVE:
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
                case MODULES:
                    parseModules(reader, patchConfigBuilder);
                    break;
                case BUNDLES:
                    parseBundles(reader, patchConfigBuilder);
                    break;
                case MISC_FILES:
                    parseMiscFiles(reader, patchConfigBuilder);
                    break;
                default:
                    throw unexpectedElement(reader);
            }
        }
    }

    private void parseDistributionStructure(XMLExtendedStreamReader reader, PatchConfigBuilder patchConfigBuilder) throws XMLStreamException {

        if (!patchTypeConfigured) {
            throw missingOneOf(reader, EnumSet.of(Element.CUMULATIVE, Element.ONE_OFF));
        }

        requireNoAttributes(reader);

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case MODULE_SEARCH_PATHS:
                    parseSlottedContentSearchPaths(reader, patchConfigBuilder, Element.MODULE_SEARCH_PATH);
                    break;
                case BUNDLE_SEARCH_PATHS:
                    parseSlottedContentSearchPaths(reader, patchConfigBuilder, Element.BUNDLE_SEARCH_PATH);
                    break;
                case IGNORED_PATH:
                    parseIgnoredPath(reader, patchConfigBuilder);
                    break;
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
            } else if (element == Element.EXCLUDE_DEFAULT) {
                parseSlottedContentDefaultExclusion(reader, patchConfigBuilder, type);
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
                case AFFECTS_TYPE:
                    try {
                        affectsType = Enum.valueOf(PatchConfigBuilder.AffectsType.class, value.toUpperCase());
                    } catch (RuntimeException e) {
                        throw PatchMessages.MESSAGES.illegalAffectsType(value, reader.getLocation());
                    }
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }

        requireNoContent(reader);

        if (!required.isEmpty()) {
            throw missingRequired(reader, required);
        }

        if (type == Element.MODULE_SEARCH_PATH) {
            patchConfigBuilder.addModuleSearchPath(name, path, affectsType); // TODO affectsType
        } else {
            patchConfigBuilder.addBundleSearchPath(name, path, affectsType); // TODO affectsType
        }
    }

    private void parseSlottedContentDefaultExclusion(XMLExtendedStreamReader reader, PatchConfigBuilder patchConfigBuilder, Element type) throws XMLStreamException {

        PatchConfigBuilder.AffectsType affectsType = null;

        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            if (attribute == Attribute.AFFECTS_TYPE) {
                try {
                    affectsType = Enum.valueOf(PatchConfigBuilder.AffectsType.class, value.toUpperCase());
                } catch (RuntimeException e) {
                    throw PatchMessages.MESSAGES.illegalAffectsType(value, reader.getLocation());
                }
            } else {
                throw unexpectedAttribute(reader, i);
            }
        }

        requireNoContent(reader);

        if (affectsType == null) {
            throw missingRequired(reader, EnumSet.of(Attribute.AFFECTS_TYPE));
        }

        if (type == Element.MODULE_SEARCH_PATH) {
            patchConfigBuilder.setDefaultModuleSearchPathExclusion(affectsType);
        } else {
            patchConfigBuilder.setDefaultBundleSearchPathExclusion(affectsType);
        }
    }

    private void parseIgnoredPath(XMLExtendedStreamReader reader, PatchConfigBuilder patchConfigBuilder) throws XMLStreamException {

        String path = null;
        PatchConfigBuilder.AffectsType affectsType = PatchConfigBuilder.AffectsType.BOTH;

        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case PATH:
                    path = value;
                    break;
                case AFFECTS_TYPE:
                    try {
                        affectsType = Enum.valueOf(PatchConfigBuilder.AffectsType.class, value.toUpperCase());
                    } catch (RuntimeException e) {
                        throw PatchMessages.MESSAGES.illegalAffectsType(value, reader.getLocation());
                    }
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }

        requireNoContent(reader);

        if (path == null) {
            throw missingRequired(reader, EnumSet.of(Attribute.PATH));
        }

        patchConfigBuilder.addIgnoredPath(path, affectsType);
    }

    private void parseCumulativePatchType(final XMLExtendedStreamReader reader, final PatchConfigBuilder builder) throws XMLStreamException {

        String appliesTo = null;
        String resulting = null;

        Set<Attribute> required = EnumSet.of(Attribute.APPLIES_TO_VERSION, Attribute.RESULTING_VERSION);

        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            required.remove(attribute);
            switch (attribute) {
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

        builder.setCumulativeType(appliesTo, resulting);

        patchTypeConfigured = true;
    }

    private void parseOneOffPatchType(final XMLExtendedStreamReader reader, final PatchConfigBuilder builder) throws XMLStreamException {

        final List<String> appliesTo = new ArrayList<String>();

        requireNoAttributes(reader);

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case APPLIES_TO_VERSION:
                    String value = reader.getElementText();
                    appliesTo.add(value);
                    break;
                default:
                    throw unexpectedElement(reader);
            }
        }

        if (appliesTo.isEmpty()) {
            throw missingRequired(reader, Collections.singleton(Element.APPLIES_TO_VERSION));
        }

        builder.setOneOffType(appliesTo);

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

        // patchConfigBuilder.addRuntimeUseItem(DistributionContentItem.createMiscItemForPath(path));
    }

    private void parseSpecifiedContent(XMLExtendedStreamReader reader, PatchConfigBuilder patchConfigBuilder) throws XMLStreamException {

        patchConfigBuilder.setGenerateByDiff(false);

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case MODULES:
                    parseModules(reader, patchConfigBuilder);
                    break;
                case BUNDLES:
                    parseBundles(reader, patchConfigBuilder);
                    break;
                case MISC_FILES:
                    parseMiscFiles(reader, patchConfigBuilder);
                    break;
                default:
                    throw unexpectedElement(reader);
            }
        }
    }

    private void parseModules(final XMLExtendedStreamReader reader, final PatchConfigBuilder builder) throws XMLStreamException {
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case ADDED_MODULE:
                    parseSlottedItem(reader, ModificationType.ADD, false, builder);
                    break;
                case UPDATED_MODULE:
                    parseSlottedItem(reader, ModificationType.MODIFY, false, builder);
                    break;
                case REMOVED_MODULE:
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
                case ADDED_MISC_CONTENT:
                    parseMiscModification(reader, ModificationType.ADD, builder);
                    break;
                case UPDATED_MISC_CONTENT:
                    parseMiscModification(reader, ModificationType.MODIFY, builder);
                    break;
                case REMOVED_MISC_CONTENT:
                    parseMiscModification(reader, ModificationType.REMOVE, builder);
                    break;
                default:
                    throw unexpectedElement(reader);
            }
        }
    }

    private void parseBundles(final XMLExtendedStreamReader reader, final PatchConfigBuilder builder) throws XMLStreamException {
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case ADDED_BUNDLE:
                    parseSlottedItem(reader, ModificationType.ADD, true, builder);
                    break;
                case UPDATED_BUNDLE:
                    parseSlottedItem(reader, ModificationType.MODIFY, true, builder);
                    break;
                case REMOVED_BUNDLE:
                    parseSlottedItem(reader, ModificationType.REMOVE, true, builder);
                    break;
                default:
                    throw unexpectedElement(reader);
            }
        }
    }

    private void parseSlottedItem(final XMLExtendedStreamReader reader, ModificationType modificationType,
                                  final boolean bundle, final PatchConfigBuilder builder) throws XMLStreamException {

        String name = null;
        String slot = "main";
        String searchPath = null;

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
                case SEARCH_PATH:
                    searchPath = value;
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        requireNoContent(reader);

//        if (bundle) {
//            builder.addBundleModification(name, slot, searchPath, modificationType);
//        } else {
//            builder.addModuleModification(name, slot, searchPath, modificationType);
//        }
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

        // builder.addMiscModification(path, directory, affectsRuntime, modificationType);
    }
}
