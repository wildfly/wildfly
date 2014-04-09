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

import static org.jboss.as.controller.parsing.ParseUtils.requireNoContent;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import org.jboss.as.patching.Constants;
import org.jboss.as.patching.DirectoryStructure;
import org.jboss.as.patching.installation.AddOn;
import org.jboss.as.patching.installation.InstalledIdentity;
import org.jboss.as.patching.installation.InstalledIdentityImpl;
import org.jboss.as.patching.installation.Layer;
import org.jboss.as.patching.installation.LayerInfo;
import org.jboss.as.patching.installation.PatchableTarget;
import org.jboss.as.patching.runner.PatchUtils;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * @author Emanuel Muckenhuber
 */
class RollbackPatchXml_1_0 extends PatchXmlUtils implements XMLStreamConstants, XMLElementReader<PatchXml.Result<PatchMetadataResolver>>, XMLElementWriter<RollbackPatch> {

    static enum Element {

        ADD_ON("add-on"),
        IDENTITY("identity"),
        INSTALLATION("installation"),
        LAYER("layer"),
        PATCH("patch"),

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

    static enum Attribute {

        NAME("name"),
        PATCHES("patches"),
        RELEASE_ID("release-id"),

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
    public void readElement(XMLExtendedStreamReader reader, PatchXml.Result<PatchMetadataResolver> factory) throws XMLStreamException {
        final RollbackPatchBuilder builder = new RollbackPatchBuilder();
        doReadElement(reader, builder, factory.getOriginalIdentity());
        factory.setResult(builder);
    }

    @Override
    protected void handleRootElement(String localName, XMLExtendedStreamReader reader, PatchBuilder patch, InstalledIdentity originalIdentity) throws XMLStreamException {
        final RollbackPatchBuilder builder = (RollbackPatchBuilder) patch;
        final Element element = Element.forName(localName);
        if (element == Element.INSTALLATION) {
            final InstalledIdentity identity = processInstallation(reader, originalIdentity);
            builder.setIdentity(identity);
        } else {
            throw unexpectedElement(reader);
        }
    }

    @Override
    public void writeContent(final XMLExtendedStreamWriter writer, final RollbackPatch rollbackPatch) throws XMLStreamException {

        // Get started ...
        writer.writeStartDocument();
        writer.writeStartElement(Element.PATCH.name);
        writer.writeDefaultNamespace(PatchXml.Namespace.ROLLBACK_1_0.getNamespace());

        writePatch(writer, rollbackPatch);
        writeInstallation(writer, rollbackPatch.getIdentityState());

        // Done
        writer.writeEndElement();
        writer.writeEndDocument();
    }

    static InstalledIdentity processInstallation(final XMLExtendedStreamReader reader, InstalledIdentity originalIdentity) throws XMLStreamException {

        LayerInfo identity = null;
        final Map<String, LayerInfo> layers = new LinkedHashMap<String, LayerInfo>();
        final Map<String, LayerInfo> addOns = new LinkedHashMap<String, LayerInfo>();

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case IDENTITY:
                    identity = parseTargetInfo(reader, originalIdentity, element);
                    break;
                case LAYER: {
                    final LayerInfo info = parseTargetInfo(reader, originalIdentity, element);
                    layers.put(info.getName(), info);
                    break;
                } case ADD_ON:
                    final LayerInfo info = parseTargetInfo(reader, originalIdentity, element);
                    addOns.put(info.getName(), info);
                    break;
                default:
                    throw unexpectedElement(reader);
            }
        }
        //
        final DirectoryStructure structure = identity.getDirectoryStructure();
        final WrappedIdentity installation = new WrappedIdentity(identity, structure);
        for (final Map.Entry<String, LayerInfo> entry : layers.entrySet()) {
            installation.putLayer(entry.getKey(), entry.getValue());
        }
        for (final Map.Entry<String, LayerInfo> entry : addOns.entrySet()) {
            installation.putAddOn(entry.getKey(), entry.getValue());
        }
        return installation;
    }

    static LayerInfo parseTargetInfo(final XMLExtendedStreamReader reader, InstalledIdentity originalIdentity, Element target) throws XMLStreamException {

        String name = null;
        final Properties properties = new Properties();
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case NAME:
                    name = value;
                    break;
                case PATCHES:
                    properties.put(Constants.PATCHES, value);
                    break;
                case RELEASE_ID:
                    properties.put(Constants.CUMULATIVE, value);
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }

        requireNoContent(reader);
        final DirectoryStructure dirStructure;
        if(originalIdentity != null) {
            switch(target) {
                case LAYER:
                    dirStructure = originalIdentity.getLayer(name).getDirectoryStructure();
                    break;
                case ADD_ON:
                    dirStructure = originalIdentity.getAddOn(name).getDirectoryStructure();
                    break;
                default:
                    dirStructure = originalIdentity.getIdentity().getDirectoryStructure();
            }
        } else {
            dirStructure = null;
        }
        final LayerInfo.TargetInfo info = LayerInfo.loadTargetInfo(properties, dirStructure);
        return new LayerInfo(name, info, dirStructure);
    }

    static void writeInstallation(final XMLExtendedStreamWriter writer, final InstalledIdentity identity) throws XMLStreamException {

        writer.writeStartElement(Element.INSTALLATION.name);

        // identity
        writeTargetInfo(writer, Element.IDENTITY, identity.getIdentity());
        // layers
        for (final Layer layer : identity.getLayers()) {
            writeTargetInfo(writer, Element.LAYER, layer);
        }
        // addons
        for (final AddOn addOn : identity.getAddOns()) {
            writeTargetInfo(writer, Element.ADD_ON, addOn);
        }

        writer.writeEndElement();
    }

    static void writeTargetInfo(final XMLExtendedStreamWriter writer, final Element element, final PatchableTarget target) throws XMLStreamException {
        try {
            final PatchableTarget.TargetInfo info = target.loadTargetInfo();
            //
            writer.writeEmptyElement(element.name);
            writer.writeAttribute(Attribute.NAME.name, target.getName());
            writer.writeAttribute(Attribute.RELEASE_ID.name, info.getCumulativePatchID());
            if (! info.getPatchIDs().isEmpty()) {
                writer.writeAttribute(Attribute.PATCHES.name, PatchUtils.asString(info.getPatchIDs()));
            }

        } catch (IOException e) {
            throw new XMLStreamException(e);
        }
    }

    static class WrappedIdentity extends InstalledIdentityImpl {

        final PatchableTarget identity;
        WrappedIdentity(final PatchableTarget identity, final DirectoryStructure structure) {
            super(new org.jboss.as.patching.installation.Identity() {

                @Override
                public String getVersion() {
                    return null;
                }

                @Override
                public String getName() {
                    return identity.getName();
                }

                @Override
                public TargetInfo loadTargetInfo() throws IOException {
                    return identity.loadTargetInfo();
                }

                @Override
                public DirectoryStructure getDirectoryStructure() {
                    return identity.getDirectoryStructure();
                }
            }, Collections.<String>emptyList(), structure == null ? null : structure.getInstalledImage());
            this.identity = identity;
        }

        @Override
        protected Layer putLayer(String name, Layer layer) {
            return super.putLayer(name, layer);
        }

        @Override
        protected AddOn putAddOn(String name, AddOn addOn) {
            return super.putAddOn(name, addOn);
        }
    }

    static class RollbackPatchBuilder extends PatchBuilder {

        protected InstalledIdentity identity;
        void setIdentity(InstalledIdentity identity) {
            this.identity = identity;
        }

        @Override
        public Patch build() {
            final Patch patch = super.build();
            return new PatchImpl.RollbackPatchImpl(patch, identity);
        }
    }
}
