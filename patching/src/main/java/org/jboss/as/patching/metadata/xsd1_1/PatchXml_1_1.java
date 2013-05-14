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

package org.jboss.as.patching.metadata.xsd1_1;

import static org.jboss.as.controller.parsing.ParseUtils.requireNoAttributes;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoContent;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;
import static org.jboss.as.patching.HashUtils.bytesToHexString;
import static org.jboss.as.patching.HashUtils.hexStringToByteArray;
import static org.jboss.as.patching.IoUtils.NO_CONTENT;
import static org.jboss.as.patching.metadata.ModuleItem.MAIN_SLOT;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.patching.metadata.BundleItem;
import org.jboss.as.patching.metadata.ContentModification;
import org.jboss.as.patching.metadata.ContentType;
import org.jboss.as.patching.metadata.MiscContentItem;
import org.jboss.as.patching.metadata.ModificationType;
import org.jboss.as.patching.metadata.ModuleItem;
import org.jboss.as.patching.metadata.Patch;
import org.jboss.as.patching.metadata.Patch.PatchType;
import org.jboss.as.patching.metadata.PatchBuilderFactory;
import org.jboss.as.patching.metadata.PatchXml;
import org.jboss.as.patching.metadata.xsd1_1.impl.IdentityImpl;
import org.jboss.as.patching.metadata.xsd1_1.impl.RequiresCallback;
import org.jboss.as.patching.metadata.xsd1_1.impl.PatchElementImpl;
import org.jboss.as.patching.metadata.xsd1_1.impl.PatchElementProviderImpl;
import org.jboss.as.patching.metadata.xsd1_1.impl.UpgradeCallback;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * @author Emanuel Muckenhuber
 */
public class PatchXml_1_1 implements XMLStreamConstants, XMLElementReader<PatchBuilderFactory>, XMLElementWriter<Patch1_1> {

    private static final String PATH_DELIMITER = "/";

    public enum Element {

        ADD_ON("add-on"),
        ADDED("added"),
        BUNDLES("bundles"),
        DESCRIPTION("description"),
        ELEMENT("element"),
        IDENTITY("identity"),
        REQUIRES("requires"),
        LAYER("layer"),
        MISC_FILES("misc-files"),
        MODULES("modules"),
        NO_UPGRADE("no-upgrade"),
        PATCH("patch"),
        REMOVED("removed"),
        UPDATED("updated"),
        UPGRADE("upgrade"),

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

        DIRECTORY("directory"),
        EXISTING_PATH("existing-path"),
        HASH("hash"),
        ID("id"),
        IN_RUNTIME_USE("in-runtime-use"),
        NAME("name"),
        NEW_HASH("new-hash"),
        PATH("path"),
        SLOT("slot"),
        TO_VERSION("to-version"),
        VERSION("version"),


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
    public void writeContent(final XMLExtendedStreamWriter writer, final Patch1_1 patch) throws XMLStreamException {

        // Get started ...
        writer.writeStartDocument();
        writer.writeStartElement(Element.PATCH.name);
        writer.writeDefaultNamespace(PatchXml.Namespace.PATCH_1_1.getNamespace());

        // id
        writer.writeAttribute(Attribute.ID.name, patch.getPatchId());

        // Description
        writer.writeStartElement(Element.DESCRIPTION.name);
        writer.writeCharacters(patch.getDescription());
        writer.writeEndElement(); // description

        // identity
        final Identity identity = patch.getIdentity();
        writer.writeStartElement(Element.IDENTITY.name);
        writer.writeAttribute(Attribute.NAME.name, identity.getName());
        writer.writeAttribute(Attribute.VERSION.name, identity.getVersion());
        if(!identity.getRequires().isEmpty()) {
            writer.writeStartElement(Element.REQUIRES.name);
            for(String patchId : identity.getRequires()) {
                writer.writeStartElement(Element.PATCH.name);
                writer.writeAttribute(Attribute.ID.name, patchId);
                writer.writeEndElement(); // patch
            }
            writer.writeEndElement(); // includes
        }
        writer.writeEndElement(); // identity

        // upgrade / no-upgrade
        final PatchType type = patch.getPatchType();
        if(type == PatchType.ONE_OFF) {
            writer.writeEmptyElement(Element.NO_UPGRADE.name);
        } else {
            writer.writeStartElement(Element.UPGRADE.name);
            writer.writeAttribute(Attribute.TO_VERSION.name, patch.getResultingVersion());
            writer.writeEndElement();
        }

        // elements
        final List<PatchElement> elements = patch.getElements();
        for(PatchElement element : elements) {
            writer.writeStartElement(Element.ELEMENT.name);
            writer.writeAttribute(Attribute.ID.name, element.getId());

            if(element.getDescription() != null) {
                writer.writeStartElement(Element.DESCRIPTION.name);
                writer.writeCharacters(element.getDescription());
                writer.writeEndElement(); // description
            }

            // layer / add-on
            final PatchElementProvider provider = element.getProvider();
            if(provider == null) {
                throw new XMLStreamException("Provider is missing for patch element " + element.getId());
            }
            if(provider.isAddOn()) {
                writer.writeStartElement(Element.ADD_ON.name);
            } else {
                writer.writeStartElement(Element.LAYER.name);
            }
            writer.writeAttribute(Attribute.NAME.name, provider.getName());
            writer.writeAttribute(Attribute.VERSION.name, provider.getVersion());
            if(!provider.getRequires().isEmpty()) {
                writer.writeStartElement(Element.REQUIRES.name);
                for(String elementId : provider.getRequires()) {
                    writer.writeStartElement(Element.ELEMENT.name);
                    writer.writeAttribute(Attribute.ID.name, elementId);
                    writer.writeEndElement(); // element
                }
                writer.writeEndElement(); // includes
            }
            writer.writeEndElement(); // add-on / layer

            // upgrade / no-upgrade
            final Patch.PatchType upgrade = element.getPatchType();
            if(upgrade == Patch.PatchType.ONE_OFF) {
                writer.writeEmptyElement(Element.NO_UPGRADE.name);
            } else {
                writer.writeEmptyElement(Element.UPGRADE.name);
                writer.writeAttribute(Attribute.TO_VERSION.name, element.getResultingVersion());
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
                writeSlottedItems(writer, Element.ADDED, modulesAdd);
                writeSlottedItems(writer, Element.UPDATED, modulesUpdate);
                writeSlottedItems(writer, Element.REMOVED, modulesRemove);
                writer.writeEndElement(); // modules
            }

            // Bundles
            if (!bundlesAdd.isEmpty() ||
                    !bundlesUpdate.isEmpty() ||
                    !bundlesRemove.isEmpty()) {
                writer.writeStartElement(Element.BUNDLES.name);
                writeSlottedItems(writer, Element.ADDED, bundlesAdd);
                writeSlottedItems(writer, Element.UPDATED, bundlesUpdate);
                writeSlottedItems(writer, Element.REMOVED, bundlesRemove);
                writer.writeEndElement(); // bundles
            }

            // Misc
            if (!miscAdd.isEmpty() ||
                    !miscUpdate.isEmpty() ||
                    !miscRemove.isEmpty()) {
                writer.writeStartElement(Element.MISC_FILES.name);
                writeMiscItems(writer, Element.ADDED, miscAdd);
                writeMiscItems(writer, Element.UPDATED, miscUpdate);
                writeMiscItems(writer, Element.REMOVED, miscRemove);
                writer.writeEndElement(); // misc-files
            }

            writer.writeEndElement(); // element
        }

        // Done
        writer.writeEndElement();
        writer.writeEndDocument();
    }

    @Override
    public void readElement(final XMLExtendedStreamReader reader, final PatchBuilderFactory factory) throws XMLStreamException {
        PatchBuilder1_1 patch = PatchBuilder1_1.create();
        factory.setBuilder(patch);

        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            if(Attribute.ID == attribute) {
                patch.setPatchId(value);
            } else {
                throw unexpectedAttribute(reader, i);
            }
        }

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case DESCRIPTION:
                    patch.setDescription(reader.getElementText());
                    break;
                case UPGRADE:
                    parseUpgrade(reader, patch);
                    break;
                case NO_UPGRADE:
                    parseNoUpgrade(reader, patch);
                    break;
                case IDENTITY:
                    parseIdentity(reader, patch);
                    break;
                case ELEMENT:
                    parseElement(reader, patch);
                    break;
                default:
                    throw unexpectedElement(reader);
            }
        }
    }

    static void parseElement(final XMLExtendedStreamReader reader, final PatchBuilder1_1 builder) throws XMLStreamException {

        String id = null;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            if(Attribute.ID == attribute) {
                id = value;
            } else {
                throw unexpectedAttribute(reader, i);
            }
        }
        final PatchElementImpl patchElement = new PatchElementImpl(id);
        builder.addElement(patchElement);

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case DESCRIPTION:
                    patchElement.setDescription(reader.getElementText());
                    break;
                case LAYER:
                    parseElementProvider(reader, patchElement, false);
                    break;
                case ADD_ON:
                    parseElementProvider(reader, patchElement, true);
                    break;
                case UPGRADE:
                    parseUpgrade(reader, patchElement);
                    break;
                case NO_UPGRADE:
                    parseNoUpgrade(reader, patchElement);
                    break;
                case MODULES:
                    parseModules(reader, patchElement);
                    break;
                case BUNDLES:
                    parseBundles(reader, patchElement);
                    break;
                case MISC_FILES:
                    parseMiscFiles(reader, patchElement);
                    break;
                default:
                    throw unexpectedElement(reader);
            }
        }
    }

    static void parseElementProvider(final XMLExtendedStreamReader reader, final PatchElementImpl patchElement, boolean isAddOn) throws XMLStreamException {

        String name = null;
        String version = null;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            if(Attribute.VERSION == attribute) {
                version = value;
            } else if(Attribute.NAME == attribute) {
                name = value;
            } else {
                throw unexpectedAttribute(reader, i);
            }
        }

        final PatchElementProviderImpl provider = new PatchElementProviderImpl(name, version, isAddOn);
        patchElement.setProvider(provider);

        int level = 0;
        while (reader.hasNext()) {
            if(reader.nextTag() == END_ELEMENT) {
                if(level == 0) {
                    break;
                } else {
                    --level;
                }
            }
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case REQUIRES:
                    break;
                case ELEMENT:
                    level = 1;
                    parseIncluded(reader, provider);
                    break;
                default:
                    throw unexpectedElement(reader);
            }
        }
    }

    static void parseIdentity(final XMLExtendedStreamReader reader, final PatchBuilder1_1 builder) throws XMLStreamException {

        String name = null;
        String version = null;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            if(Attribute.VERSION == attribute) {
                version = value;
            } else if(Attribute.NAME == attribute) {
                name = value;
            } else {
                throw unexpectedAttribute(reader, i);
            }
        }

        final IdentityImpl identity = new IdentityImpl(name, version);
        builder.setIdentity(identity);

        int level = 0;
        while (reader.hasNext()) {
            if(reader.nextTag() == END_ELEMENT) {
                if(level == 0) {
                    break;
                } else {
                    --level;
                }
            }

            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case REQUIRES:
                    break;
                case PATCH:
                    level = 1;
                    parseIncluded(reader, identity);
                    break;
                default:
                    throw unexpectedElement(reader);
            }
        }
    }

    static void parseIncluded(final XMLExtendedStreamReader reader, final RequiresCallback includes) throws XMLStreamException {

        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            if(Attribute.ID == attribute) {
                includes.require(value);
            } else {
                throw unexpectedAttribute(reader, i);
            }
        }
        requireNoContent(reader);
    }

    static void parseUpgrade(final XMLExtendedStreamReader reader, final UpgradeCallback builder) throws XMLStreamException {

        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            if(Attribute.TO_VERSION == attribute) {
                builder.setUpgrade(value);
            } else {
                throw unexpectedAttribute(reader, i);
            }
        }
        requireNoContent(reader);
    }

    static void parseNoUpgrade(final XMLExtendedStreamReader reader, final UpgradeCallback builder) throws XMLStreamException {
        requireNoAttributes(reader);
        requireNoContent(reader);
        builder.setNoUpgrade();
    }

    static void parseModules(final XMLExtendedStreamReader reader, final PatchElementImpl patchElement) throws XMLStreamException {
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case ADDED:
                    patchElement.addContentModification(parseModuleModification(reader, ModificationType.ADD));
                    break;
                case UPDATED:
                    patchElement.addContentModification(parseModuleModification(reader, ModificationType.MODIFY));
                    break;
                case REMOVED:
                    patchElement.addContentModification(parseModuleModification(reader, ModificationType.REMOVE));
                    break;
                default:
                    throw unexpectedElement(reader);
            }
        }
    }

    static void parseMiscFiles(final XMLExtendedStreamReader reader, final PatchElementImpl patchElement) throws XMLStreamException {
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case ADDED:
                    patchElement.addContentModification(parseMiscModification(reader, ModificationType.ADD));
                    break;
                case UPDATED:
                    patchElement.addContentModification(parseMiscModification(reader, ModificationType.MODIFY));
                    break;
                case REMOVED:
                    patchElement.addContentModification(parseMiscModification(reader, ModificationType.REMOVE));
                    break;
                default:
                    throw unexpectedElement(reader);
            }
        }
    }

    static void parseBundles(final XMLExtendedStreamReader reader, final PatchElementImpl builder) throws XMLStreamException {
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case ADDED:
                    builder.addContentModification(parseBundleModification(reader, ModificationType.ADD));
                    break;
                case UPDATED:
                    builder.addContentModification(parseBundleModification(reader, ModificationType.MODIFY));
                    break;
                case REMOVED:
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
                    if(modificationType == ModificationType.REMOVE) {
                        targetHash = hexStringToByteArray(value);
                    } else {
                        hash = hexStringToByteArray(value);
                    }
                    break;
                case NEW_HASH:
                    if(modificationType == ModificationType.REMOVE) {
                        hash = hexStringToByteArray(value);
                    } else {
                        targetHash = hexStringToByteArray(value);
                    }
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
                    if(type == ModificationType.REMOVE) {
                        targetHash = hexStringToByteArray(value);
                    } else {
                        hash = hexStringToByteArray(value);
                    }
                    break;
                case NEW_HASH:
                    if(type == ModificationType.REMOVE) {
                        hash = hexStringToByteArray(value);
                    } else {
                        targetHash = hexStringToByteArray(value);
                    }
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
//            writer.writeStartElement(Element.APPLIES_TO_VERSION.name);
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
        byte[] hash = item.getContentHash();
        if (hash.length > 0) {
            writer.writeAttribute(Attribute.HASH.name,  bytesToHexString(hash));
        }
        if(type == ModificationType.REMOVE) {
            final byte[] existingHash = modification.getTargetHash();
            if (existingHash.length > 0) {
                writer.writeAttribute(Attribute.HASH.name, bytesToHexString(existingHash));
            }
        } else if(type == ModificationType.MODIFY) {
            final byte[] existingHash = modification.getTargetHash();
            if (existingHash.length > 0) {
                writer.writeAttribute(Attribute.NEW_HASH.name, bytesToHexString(existingHash));
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

        byte[] hash = item.getContentHash();
        if(hash.length > 0) {
            writer.writeAttribute(Attribute.HASH.name, bytesToHexString(hash));
        }

        if(type == ModificationType.REMOVE) {
            final byte[] existingHash = modification.getTargetHash();
            if (existingHash.length > 0) {
                writer.writeAttribute(Attribute.HASH.name, bytesToHexString(existingHash));
            }
            if(item.isAffectsRuntime()) {
                writer.writeAttribute(Attribute.IN_RUNTIME_USE.name, "true");
            }
        } else if(type == ModificationType.MODIFY) {
            writer.writeAttribute(Attribute.NEW_HASH.name, bytesToHexString(modification.getTargetHash()));
            if (item.isAffectsRuntime()) {
                writer.writeAttribute(Attribute.IN_RUNTIME_USE.name, "true");
            }
        }
    }
}
