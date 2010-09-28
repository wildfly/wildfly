/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.jboss.as.Extension;
import org.jboss.as.ExtensionContext;
import org.jboss.as.SubsystemFactory;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoadException;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLMapper;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import static javax.xml.stream.XMLStreamConstants.*;
import static org.jboss.as.model.ParseUtils.*;

/**
 * A set of parsers for the JBoss Application Server XML schema, which result in lists of updates to apply to new models.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ModelXmlParsers {

    private ModelXmlParsers() {
    }

    public static void registerAll(final XMLMapper xmlMapper) {
        xmlMapper.registerRootElement(new QName(Namespace.DOMAIN_1_0.getUriString(), Element.DOMAIN.getLocalName()), DOMAIN_XML_READER);
        xmlMapper.registerRootElement(new QName(Namespace.DOMAIN_1_0.getUriString(), Element.HOST.getLocalName()), HOST_XML_READER);
        xmlMapper.registerRootElement(new QName(Namespace.DOMAIN_1_0.getUriString(), Element.SERVER.getLocalName()), SERVER_XML_READER);
    }

    /**
     * The XML reader for the {@code &lt;domain&gt;} root element.
     */
    public static final XMLElementReader<List<? super AbstractDomainModelUpdate<?>>> DOMAIN_XML_READER = new XMLElementReader<List<? super AbstractDomainModelUpdate<?>>>() {
        public void readElement(final XMLExtendedStreamReader reader, final List<? super AbstractDomainModelUpdate<?>> objects) throws XMLStreamException {
            parseDomainRootElement(reader, objects);
        }
    };

    /**
     * The XML reader for the {@code &lt;host&gt;} root element.
     */
    public static final XMLElementReader<List<? super AbstractHostModelUpdate<?>>> HOST_XML_READER = new XMLElementReader<List<? super AbstractHostModelUpdate<?>>>() {
        public void readElement(final XMLExtendedStreamReader reader, final List<? super AbstractHostModelUpdate<?>> objects) throws XMLStreamException {
            parseHostRootElement(reader, objects);
        }
    };

    /**
     * The XML reader for the {@code &lt;server&gt;} root element.
     */
    public static final XMLElementReader<List<? super AbstractServerModelUpdate<?>>> SERVER_XML_READER = new XMLElementReader<List<? super AbstractServerModelUpdate<?>>>() {
        public void readElement(final XMLExtendedStreamReader reader, final List<? super AbstractServerModelUpdate<?>> objects) throws XMLStreamException {
            parseServerRootElement(reader, objects);
        }
    };

    public static void parseServerRootElement(final XMLExtendedStreamReader reader, final List<? super AbstractServerModelUpdate<?>> list) throws XMLStreamException {
        final List<NamespacePrefix> prefixes = readNamespaces(reader);
        if (! prefixes.isEmpty()) list.add(new ServerNamespaceUpdate(prefixes));
    }

    public static void parseHostRootElement(final XMLExtendedStreamReader reader, final List<? super AbstractHostModelUpdate<?>> list) throws XMLStreamException {
        final List<NamespacePrefix> prefixes = readNamespaces(reader);
        if (! prefixes.isEmpty()) list.add(new HostNamespaceUpdate(prefixes));
    }


    public static void parseDomainRootElement(final XMLExtendedStreamReader reader, final List<? super AbstractDomainModelUpdate<?>> list) throws XMLStreamException {

        // Read namespaces
        final List<NamespacePrefix> prefixes = readNamespaces(reader);
        if (! prefixes.isEmpty()) list.add(new DomainNamespaceUpdate(prefixes));

        // Read attributes
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            switch (Namespace.forUri(reader.getAttributeNamespace(i))) {
                case DOMAIN_1_0: {
                    throw unexpectedAttribute(reader, i);
                }
                case XML_SCHEMA_INSTANCE: {
                    switch (Attribute.forName(reader.getAttributeLocalName(i))) {
                        case SCHEMA_LOCATION: {
                            final List<SchemaLocation> locationList = readSchemaLocations(reader, i);
                            list.add(new DomainSchemaLocationUpdate(locationList));
                            break;
                        }
                        case NO_NAMESPACE_SCHEMA_LOCATION: {
                            // todo, jeez
                            break;
                        }
                        default: {
                            throw unexpectedAttribute(reader, i);
                        }
                    }
                }
            }
        }

        // Content
        // Handle elements: sequence

        Element element = nextElement(reader);
        if (element == Element.EXTENSIONS) {
            parseExtensions(reader, list);
            element = nextElement(reader);
        }
        if (element == Element.PROFILES) {
            parseProfiles(reader, list);
            element = nextElement(reader);
        }
        if (element == Element.INTERFACES) {
            parseInterfaces(reader, list);
            element = nextElement(reader);
        }
        if (element == Element.SOCKET_BINDING_GROUPS) {
            parseSocketBindingGroups(reader, list);
            element = nextElement(reader);
        }
        if (element == Element.DEPLOYMENTS) {
            parseDeployments(reader, list);
            element = nextElement(reader);
        }
        if (element == Element.SERVER_GROUPS) {
            parseServerGroups(reader, list);
            element = nextElement(reader);
        }
        if (element == Element.SYSTEM_PROPERTIES) {
            parseSystemProperties(reader, list);
            element = nextElement(reader);
        }
        if (element != null) {
            throw unexpectedElement(reader);
        }
    }

    static void parseExtensions(final XMLExtendedStreamReader reader, final List<? super AbstractDomainModelUpdate<?>> list) throws XMLStreamException {
        requireNoAttributes(reader);

        final ExtensionContext extensionContext = new ExtensionContextImpl(reader);

        final Set<String> found = new HashSet<String>();

        while (reader.nextTag() != END_ELEMENT) {
            // Attributes
            final String moduleName = readStringAttributeElement(reader, Attribute.MODULE.getLocalName());

            // Content
            requireNoContent(reader);

            if (! found.add(moduleName)) {
                // duplicate module name
                throw invalidAttributeValue(reader, 0);
            }

            // Register element handlers for this extension
            try {
                for (Extension extension : Module.loadService(moduleName, Extension.class)) {
                    extension.initialize(extensionContext);
                }
            } catch (ModuleLoadException e) {
                throw new XMLStreamException("Failed to load module", e);
            }

            // Create the update
            list.add(new DomainExtensionAdd(moduleName));
        }
    }

    static void parseProfiles(final XMLExtendedStreamReader reader, final List<? super AbstractDomainModelUpdate<?>> list) throws XMLStreamException {
        requireNoAttributes(reader);

        final Set<String> names = new HashSet<String>();

        while (reader.nextTag() != END_ELEMENT) {
            // Attributes
            final String name = readStringAttributeElement(reader, Attribute.NAME.getLocalName());
            if (! names.add(name)) {
                throw new XMLStreamException("Duplicate profile declaration", reader.getLocation());
            }
            final Set<String> includes = new LinkedHashSet<String>();

            // Content
            // Sequence
            OUT: while (reader.nextTag() != END_ELEMENT) {
                switch (Namespace.forUri(reader.getNamespaceURI())) {
                    case DOMAIN_1_0: {
                        if (Element.forName(reader.getLocalName()) != Element.INCLUDE) {
                            throw unexpectedElement(reader);
                        }
                        final String includedName = readStringAttributeElement(reader, Attribute.NAME.getLocalName());
                        if (! names.contains(includedName)) {
                            throw new XMLStreamException("No profile found for inclusion", reader.getLocation());
                        }
                        if (! includes.add(includedName)) {
                            throw new XMLStreamException("Duplicate profile include", reader.getLocation());
                        }
                        break;
                    }
                    default: {
                        break OUT;
                    }
                }
            }
            final Set<String> configuredSubsystemTypes = new HashSet<String>();
            while (reader.nextTag() != END_ELEMENT) {
                switch (Namespace.forUri(reader.getNamespaceURI())) {
                    case UNKNOWN: {
                        if (Element.forName(reader.getLocalName()) != Element.SUBSYSTEM) {
                            throw unexpectedElement(reader);
                        }
                        if (configuredSubsystemTypes.add(reader.getNamespaceURI())) {
                            throw new XMLStreamException("Duplicate subsystem declaration", reader.getLocation());
                        }
                        // parse content
                        final List<AbstractSubsystemUpdate<?, ?>> resultList = new ArrayList<AbstractSubsystemUpdate<?, ?>>();
                        reader.handleAny(resultList);
                        for (AbstractSubsystemUpdate<?, ?> update : resultList) {
                            // I don't think this is really an unchecked cast (even though ? is bounded by Object, the class
                            // specifies an additional bound for E so it should be considered safe), but IDEA thinks it is...
                            //noinspection unchecked
                            list.add(DomainProfileUpdate.create(name, update));
                        }
                        break;
                    }
                    default: {
                        throw unexpectedElement(reader);
                    }
                }
            }
        }
    }

    static void parseInterfaces(final XMLExtendedStreamReader reader, final List<? super AbstractDomainModelUpdate<?>> list) throws XMLStreamException {
        requireNoAttributes(reader);

        final Set<String> names = new HashSet<String>();

        while (reader.nextTag() != END_ELEMENT) {
            // Attributes
            final String name = readStringAttributeElement(reader, Attribute.NAME.getLocalName());
            if (! names.add(name)) {
                throw new XMLStreamException("Duplicate interface declaration", reader.getLocation());
            }

            // Content
            // nested choices
            if (reader.nextTag() == END_ELEMENT) {
                throw unexpectedEndElement(reader);
            }
            boolean first = true;
            do {
                if (Namespace.forUri(reader.getNamespaceURI()) != Namespace.DOMAIN_1_0) {
                    throw unexpectedElement(reader);
                }
                switch (Element.forName(reader.getLocalName())) {
                    case ANY_ADDRESS: {
                        if (! first) {
                            throw unexpectedElement(reader);
                        }
                        requireNoAttributes(reader);
                        requireNoContent(reader);

                        // no others allowed
                        if (reader.nextTag() != END_ELEMENT) {
                            throw unexpectedElement(reader);
                        }
                    }
                }
                first = false;
            } while (reader.nextTag() != END_ELEMENT);
        }
    }

    static void parseSocketBindingGroups(final XMLExtendedStreamReader reader, final List<? super AbstractDomainModelUpdate<?>> list) throws XMLStreamException {

    }

    static void parseDeployments(final XMLExtendedStreamReader reader, final List<? super AbstractDomainModelUpdate<?>> list) throws XMLStreamException {

    }

    static void parseServerGroups(final XMLExtendedStreamReader reader, final List<? super AbstractDomainModelUpdate<?>> list) throws XMLStreamException {

    }

    static void parseSystemProperties(final XMLExtendedStreamReader reader, final List<? super AbstractDomainModelUpdate<?>> list) throws XMLStreamException {

    }

    private static Element nextElement(XMLExtendedStreamReader reader) throws XMLStreamException {
        if (reader.nextTag() == END_ELEMENT) {
            return null;
        }
        if (Namespace.forUri(reader.getNamespaceURI()) != Namespace.DOMAIN_1_0) {
            throw unexpectedElement(reader);
        }
        return Element.forName(reader.getLocalName());
    }

    private static class ExtensionContextImpl implements ExtensionContext {

        private final XMLExtendedStreamReader reader;

        public ExtensionContextImpl(final XMLExtendedStreamReader reader) {
            this.reader = reader;
        }

        public <E extends AbstractSubsystemElement<E>> void registerSubsystem(final String namespaceUri, final SubsystemFactory<E> factory, final XMLElementReader<List<? super AbstractSubsystemUpdate<E, ?>>> elementReader) {
            final XMLMapper mapper = reader.getXMLMapper();
            mapper.registerRootElement(new QName(namespaceUri, Element.SUBSYSTEM.getLocalName()), elementReader);
        }
    }
}
