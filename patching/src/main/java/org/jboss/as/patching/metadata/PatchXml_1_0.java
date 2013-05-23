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

import static org.jboss.as.controller.parsing.ParseUtils.missingRequired;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoAttributes;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoContent;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;
import static org.jboss.as.patching.HashUtils.bytesToHexString;
import static org.jboss.as.patching.HashUtils.hexStringToByteArray;
import static org.jboss.as.patching.IoUtils.NO_CONTENT;
import static org.jboss.as.patching.metadata.ModuleItem.MAIN_SLOT;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * @author Emanuel Muckenhuber
 */
class PatchXml_1_0 implements XMLStreamConstants, XMLElementReader<PatchBuilder>, XMLElementWriter<Patch> {

    private static final String PATH_DELIMITER = "/";

    enum Element {

        ADDED_BUNDLE("added-bundle"),
        ADDED_MISC_CONTENT("added-misc-content"),
        ADDED_MODULE("added-module"),
        APPLIES_TO_VERSION("applies-to-version"),
        BUNDLES("bundles"),
        CUMULATIVE("cumulative"),
        DESCRIPTION("description"),
        MISC_FILES("misc-files"),
        MODULES("modules"),
        NAME("name"),
        ONE_OFF("one-off"),
        PATCH("patch"),
        REMOVED_BUNDLE("removed-bundle"),
        REMOVED_MISC_CONTENT("removed-misc-content"),
        REMOVED_MODULE("removed-module"),
        UPDATED_BUNDLE("updated-bundle"),
        UPDATED_MISC_CONTENT("updated-misc-content"),
        UPDATED_MODULE("updated-module"),

        // default unknown element
        UNKNOWN(null),
        ;

        final String name;
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

        APPLIES_TO_VERSION("applies-to-version"),
        DIRECTORY("directory"),
        EXISTING_HASH("existing-hash"),
        EXISTING_PATH("existing-path"),
        HASH("hash"),
        IN_RUNTIME_USE("in-runtime-use"),
        NAME("name"),
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
    public void writeContent(final XMLExtendedStreamWriter writer, final Patch patch) throws XMLStreamException {

        // Get started ...
        writer.writeStartDocument();
        writer.writeStartElement(Element.PATCH.name);
        writer.writeDefaultNamespace(PatchXml.Namespace.PATCH_1_0.getNamespace());

        // Name
        writer.writeStartElement(Element.NAME.name);
        writer.writeCharacters(patch.getPatchId());
        writer.writeEndElement();

        // Description
        writer.writeStartElement(Element.DESCRIPTION.name);
        writer.writeCharacters(patch.getDescription());
        writer.writeEndElement();

        // Type
        final Patch.PatchType type = patch.getPatchType();
        if(type == Patch.PatchType.ONE_OFF) {
            writer.writeStartElement(Element.ONE_OFF.name);
            writeAppliesToVersions(writer, patch.getAppliesTo());
            writer.writeEndElement();
        } else {
            writer.writeEmptyElement(Element.CUMULATIVE.name);
            writer.writeAttribute(Attribute.APPLIES_TO_VERSION.name, patch.getAppliesTo().iterator().next());
            writer.writeAttribute(Attribute.RESULTING_VERSION.name, patch.getResultingVersion());
        }

        // Sort by content and modification type
        final List<ContentModification> bundlesAdd  = new ArrayList<ContentModification>();
        final List<ContentModification> bundlesUpdate  = new ArrayList<ContentModification>();
        final List<ContentModification> bundlesRemove  = new ArrayList<ContentModification>();

        final List<ContentModification> miscAdd = new ArrayList<ContentModification>();
        final List<ContentModification> miscUpdate = new ArrayList<ContentModification>();
        final List<ContentModification> miscRemove = new ArrayList<ContentModification>();

        final List<ContentModification> modulesAdd = new ArrayList<ContentModification>();
        final List<ContentModification> modulesUpdate = new ArrayList<ContentModification>();
        final List<ContentModification> modulesRemove = new ArrayList<ContentModification>();

        for(final ContentModification mod : patch.getModifications()) {
            final ModificationType modificationType = mod.getType();
            final ContentType contentType = mod.getItem().getContentType();
            switch (contentType) {
                case BUNDLE:
                    switch (modificationType) {
                        case ADD:
                            bundlesAdd.add(mod);
                            break;
                        case MODIFY:
                            bundlesUpdate.add(mod);
                            break;
                        case REMOVE:
                            bundlesRemove.add(mod);
                            break;
                    }
                    break;
                case MODULE:
                    switch (modificationType) {
                        case ADD:
                            modulesAdd.add(mod);
                            break;
                        case MODIFY:
                            modulesUpdate.add(mod);
                            break;
                        case REMOVE:
                            modulesRemove.add(mod);
                            break;
                    }
                    break;
                case MISC:
                    switch (modificationType) {
                        case ADD:
                            miscAdd.add(mod);
                            break;
                        case MODIFY:
                            miscUpdate.add(mod);
                            break;
                        case REMOVE:
                            miscRemove.add(mod);
                            break;
                    }
                    break;
            }
        }

        // Modules
        if (!modulesAdd.isEmpty() ||
                !modulesUpdate.isEmpty() ||
                !modulesRemove.isEmpty()) {
            writer.writeStartElement(Element.MODULES.name);
            writeSlottedItems(writer, Element.ADDED_MODULE, modulesAdd);
            writeSlottedItems(writer, Element.UPDATED_MODULE, modulesUpdate);
            writeSlottedItems(writer, Element.REMOVED_MODULE, modulesRemove);
            writer.writeEndElement();
        }

        // Bundles
        if (!bundlesAdd.isEmpty() ||
                !bundlesUpdate.isEmpty() ||
                !bundlesRemove.isEmpty()) {
            writer.writeStartElement(Element.BUNDLES.name);
            writeSlottedItems(writer, Element.ADDED_BUNDLE, bundlesAdd);
            writeSlottedItems(writer, Element.UPDATED_BUNDLE, bundlesUpdate);
            writeSlottedItems(writer, Element.REMOVED_BUNDLE, bundlesRemove);
            writer.writeEndElement();
        }

        // Misc
        if (!miscAdd.isEmpty() ||
                !miscUpdate.isEmpty() ||
                !miscRemove.isEmpty()) {
            writer.writeStartElement(Element.MISC_FILES.name);
            writeMiscItems(writer, Element.ADDED_MISC_CONTENT, miscAdd);
            writeMiscItems(writer, Element.UPDATED_MISC_CONTENT, miscUpdate);
            writeMiscItems(writer, Element.REMOVED_MISC_CONTENT, miscRemove);
            writer.writeEndElement();
        }

        // Done
        writer.writeEndElement();
        writer.writeEndDocument();
    }

    @Override
    public void readElement(final XMLExtendedStreamReader reader, final PatchBuilder patch) throws XMLStreamException {
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case NAME:
                    patch.setPatchId(reader.getElementText());
                    break;
                case DESCRIPTION:
                    patch.setDescription(reader.getElementText());
                    break;
                case CUMULATIVE:
                    parseCumulativePatchType(reader, patch);
                    break;
                case ONE_OFF:
                    parseOneOffPatchType(reader, patch);
                    break;
                case MODULES:
                    parseModules(reader, patch);
                    break;
                case BUNDLES:
                    parseBundles(reader, patch);
                    break;
                case MISC_FILES:
                    parseMiscFiles(reader, patch);
                    break;
                default:
                    throw unexpectedElement(reader);
            }
        }
    }

    static void parseCumulativePatchType(final XMLExtendedStreamReader reader, final PatchBuilder builder) throws XMLStreamException {

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
    }

    static void parseOneOffPatchType(final XMLExtendedStreamReader reader, final PatchBuilder builder) throws XMLStreamException {

        final List<String> appliesTo = new ArrayList<String>();

        requireNoAttributes(reader);

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case APPLIES_TO_VERSION:
                    appliesTo.add(reader.getElementText());
                    break;
                default:
                    throw unexpectedElement(reader);
            }
        }

        if (appliesTo.isEmpty()) {
            throw missingRequired(reader, Collections.singleton(Element.APPLIES_TO_VERSION));
        }

        builder.setOneOffType(appliesTo);
    }

    static void parseModules(final XMLExtendedStreamReader reader, final PatchBuilder builder) throws XMLStreamException {
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case ADDED_MODULE:
                    builder.addContentModification(parseModuleModification(reader, ModificationType.ADD));
                    break;
                case UPDATED_MODULE:
                    builder.addContentModification(parseModuleModification(reader, ModificationType.MODIFY));
                    break;
                case REMOVED_MODULE:
                    builder.addContentModification(parseModuleModification(reader, ModificationType.REMOVE));
                    break;
                default:
                    throw unexpectedElement(reader);
            }
        }
    }

    static void parseMiscFiles(final XMLExtendedStreamReader reader, final PatchBuilder builder) throws XMLStreamException {
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case ADDED_MISC_CONTENT:
                    builder.addContentModification(parseMiscModification(reader, ModificationType.ADD));
                    break;
                case UPDATED_MISC_CONTENT:
                    builder.addContentModification(parseMiscModification(reader, ModificationType.MODIFY));
                    break;
                case REMOVED_MISC_CONTENT:
                    builder.addContentModification(parseMiscModification(reader, ModificationType.REMOVE));
                    break;
                default:
                    throw unexpectedElement(reader);
            }
        }
    }

    static void parseBundles(final XMLExtendedStreamReader reader, final PatchBuilder builder) throws XMLStreamException {
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case ADDED_BUNDLE:
                    builder.addContentModification(parseBundleModification(reader, ModificationType.ADD));
                    break;
                case UPDATED_BUNDLE:
                    builder.addContentModification(parseBundleModification(reader, ModificationType.MODIFY));
                    break;
                case REMOVED_BUNDLE:
                    builder.addContentModification(parseBundleModification(reader, ModificationType.REMOVE));
                    break;
                default:
                    throw unexpectedElement(reader);
            }
        }
    }

    static ContentModification parseBundleModification(final XMLExtendedStreamReader reader, final ModificationType type) throws XMLStreamException {
        return parseSlottedItem(reader, type, ContentType.BUNDLE);
    }

    static ContentModification parseModuleModification(final XMLExtendedStreamReader reader, final ModificationType type) throws XMLStreamException {
        return parseSlottedItem(reader, type, ContentType.MODULE);
    }

    static ContentModification parseSlottedItem(final XMLExtendedStreamReader reader, ModificationType modificationType, ContentType contentType) throws XMLStreamException {

        String moduleName = null;
        String slot = "main";
        byte[] hash = NO_CONTENT;
        byte[] targetHash = NO_CONTENT;

        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case NAME:
                    moduleName = value;
                    break;
                case SLOT:
                    slot = value;
                    break;
                case HASH:
                    hash = hexStringToByteArray(value);
                    break;
                case EXISTING_HASH:
                    targetHash = hexStringToByteArray(value);
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        requireNoContent(reader);

        final ModuleItem item = contentType == ContentType.MODULE ? new ModuleItem(moduleName, slot, hash) : new BundleItem(moduleName, slot, hash);
        return new ContentModification(item, targetHash, modificationType);
    }

    static ContentModification parseMiscModification(final XMLExtendedStreamReader reader, ModificationType type) throws XMLStreamException {

        String path = null;
        byte[] hash = NO_CONTENT;
        boolean directory = false;
        boolean affectsRuntime = false;
        byte[] targetHash = NO_CONTENT;

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
                case HASH:
                    hash = hexStringToByteArray(value);
                    break;
                case EXISTING_HASH:
                    targetHash = hexStringToByteArray(value);
                    break;
                case IN_RUNTIME_USE:
                    affectsRuntime = Boolean.parseBoolean(value);
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        requireNoContent(reader);

        // process path
        final String[] s = path.split(PATH_DELIMITER);
        final int length = s.length;
        final String name = s[length - 1];
        final String[] itemPath = Arrays.copyOf(s, length - 1);

        final MiscContentItem item = new MiscContentItem(name, itemPath, hash, directory, affectsRuntime);
        return new ContentModification(item, targetHash, type);
    }

    protected void writeAppliesToVersions(XMLExtendedStreamWriter writer, List<String> appliesTo) throws XMLStreamException {
        for (String version : appliesTo) {
            writer.writeStartElement(Element.APPLIES_TO_VERSION.name);
            writer.writeCharacters(version);
            writer.writeEndElement();
        }
    }

    protected void writeSlottedItems(final XMLExtendedStreamWriter writer, final Element element, final List<ContentModification> modifications) throws XMLStreamException {
        for(final ContentModification modification : modifications) {
            writeSlottedItem(writer, element, modification);
        }
    }

    protected void writeSlottedItem(final XMLExtendedStreamWriter writer, Element element, ContentModification modification) throws XMLStreamException {

        writer.writeEmptyElement(element.name);

        final ModuleItem item = (ModuleItem) modification.getItem();
        final ModificationType type = modification.getType();

        writer.writeAttribute(Attribute.NAME.name, item.getName());
        if (!MAIN_SLOT.equals(item.getSlot())) {
            writer.writeAttribute(Attribute.SLOT.name, item.getSlot());
        }
        if(type != ModificationType.REMOVE) {
            byte[] hash = item.getContentHash();
            if (hash.length > 0) {
                writer.writeAttribute(Attribute.HASH.name,  bytesToHexString(hash));
            }
        }
        if(type != ModificationType.ADD) {
            final byte[] existingHash = modification.getTargetHash();
            if (existingHash.length > 0) {
                writer.writeAttribute(Attribute.EXISTING_HASH.name, bytesToHexString(existingHash));
            }
        }
    }

    protected void writeMiscItems(final XMLExtendedStreamWriter writer, final Element element, final List<ContentModification> modifications) throws XMLStreamException {
        for(final ContentModification modification : modifications) {
            writeMiscItem(writer, element, modification);
        }
    }

    protected void writeMiscItem(final XMLExtendedStreamWriter writer, final Element element, final ContentModification modification) throws XMLStreamException {

        writer.writeEmptyElement(element.name);

        final MiscContentItem item = (MiscContentItem) modification.getItem();
        final ModificationType type = modification.getType();

        final StringBuilder path = new StringBuilder();
        for(final String p : item.getPath()) {
            path.append(p).append(PATH_DELIMITER);
        }
        path.append(item.getName());

        writer.writeAttribute(Attribute.PATH.name, path.toString());
        if (item.isDirectory()) {
            writer.writeAttribute(Attribute.DIRECTORY.name, "true");
        }

        if(type != ModificationType.REMOVE) {
            writer.writeAttribute(Attribute.HASH.name, bytesToHexString(item.getContentHash()));
        }
        if(type != ModificationType.ADD) {
            writer.writeAttribute(Attribute.EXISTING_HASH.name, bytesToHexString(modification.getTargetHash()));
            if (item.isAffectsRuntime()) {
                writer.writeAttribute(Attribute.IN_RUNTIME_USE.name, "true");
            }
        }
    }
}
