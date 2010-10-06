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

import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static org.jboss.as.model.ParseUtils.invalidAttributeValue;
import static org.jboss.as.model.ParseUtils.readNamespaces;
import static org.jboss.as.model.ParseUtils.readSchemaLocations;
import static org.jboss.as.model.ParseUtils.readStringAttributeElement;
import static org.jboss.as.model.ParseUtils.requireNoAttributes;
import static org.jboss.as.model.ParseUtils.requireNoContent;
import static org.jboss.as.model.ParseUtils.requireSingleAttribute;
import static org.jboss.as.model.ParseUtils.unexpectedAttribute;
import static org.jboss.as.model.ParseUtils.unexpectedElement;
import static org.jboss.as.model.ParseUtils.unexpectedEndElement;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.Extension;
import org.jboss.as.ExtensionContext;
import org.jboss.as.model.socket.SocketBindingAdd;
import org.jboss.as.model.socket.SocketBindingGroupUpdate;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoadException;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLMapper;

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
        Set<String> names = Collections.emptySet();
        if (element == Element.INTERFACES) {
            names = parseInterfaces(reader, list);
            element = nextElement(reader);
        }
        if (element == Element.SOCKET_BINDING_GROUPS) {
            parseSocketBindingGroups(reader, list, names);
            element = nextElement(reader);
        }
        if (element == Element.DEPLOYMENTS) {
            parseDeployments(reader, list, null);
            element = nextElement(reader);
        }
        if (element == Element.SERVER_GROUPS) {
            parseServerGroups(reader, list);
            element = nextElement(reader);
        }
        if (element == Element.SYSTEM_PROPERTIES) {
            parseDomainSystemProperties(reader, list);
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
            requireSingleAttribute(reader, Attribute.NAME.getLocalName());
            final String name = reader.getAttributeValue(0);
            if (! names.add(name)) {
                throw new XMLStreamException("Duplicate profile declaration " + name, reader.getLocation());
            }

            list.add(new DomainProfileAdd(name));
            final Set<String> includes = new LinkedHashSet<String>();

            // Content
            // Sequence
            final Set<String> configuredSubsystemTypes = new HashSet<String>();
            while (reader.nextTag() != END_ELEMENT) {
                switch (Namespace.forUri(reader.getNamespaceURI())) {
                    case UNKNOWN: {
                        if (Element.forName(reader.getLocalName()) != Element.SUBSYSTEM) {
                            throw unexpectedElement(reader);
                        }
                        if (!configuredSubsystemTypes.add(reader.getNamespaceURI())) {
                            throw new XMLStreamException("Duplicate subsystem declaration", reader.getLocation());
                        }
                        // parse content
                        final ParseResult<ExtensionContext.SubsystemConfiguration<?>> result = new ParseResult<ExtensionContext.SubsystemConfiguration<?>>();
                        reader.handleAny(result);
                        list.add(new DomainSubsystemAdd(name, result.getResult().getSubsystemAdd()));
                        for (AbstractSubsystemUpdate<?, ?> update : result.getResult().getUpdates()) {
                            // I don't think this is really an unchecked cast (even though ? is bounded by Object, the class
                            // specifies an additional bound for E so it should be considered safe), but IDEA thinks it is...
                            //noinspection unchecked
                            list.add(DomainSubsystemUpdate.create(name, update));
                        }
                        break;
                    }
                    case DOMAIN_1_0: {
                        // include should come first
                        if (configuredSubsystemTypes.size() > 0) {
                            throw unexpectedElement(reader);
                        }
                        if (Element.forName(reader.getLocalName()) != Element.INCLUDE) {
                            throw unexpectedElement(reader);
                        }
                        final String includedName = readStringAttributeElement(reader, Attribute.PROFILE.getLocalName());
                        if (! names.contains(includedName)) {
                            throw new XMLStreamException("No profile found for inclusion", reader.getLocation());
                        }
                        if (! includes.add(includedName)) {
                            throw new XMLStreamException("Duplicate profile include", reader.getLocation());
                        }
                        list.add(new DomainProfileIncludeAdd(name, includedName));
                        break;
                    }
                    default: {
                        throw unexpectedElement(reader);
                    }
                }
            }

            if (configuredSubsystemTypes.size() == 0) {
                throw new XMLStreamException("Profile has no subsystem configurations", reader.getLocation());
            }
        }
    }

    static Set<String> parseInterfaces(final XMLExtendedStreamReader reader, final List<? super AbstractDomainModelUpdate<?>> list) throws XMLStreamException {
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
        return names;
    }

    static void parseSocketBindingGroups(final XMLExtendedStreamReader reader, final List<? super AbstractDomainModelUpdate<?>> list, Set<String> interfaces) throws XMLStreamException {
        String name = null;
        String defIntf = null;
        Set<String> includedGroups = new HashSet<String>();
        Set<String> socketBindings = new HashSet<String>();

        List<SocketBindingAdd> bindingUpdates = new ArrayList<SocketBindingAdd>();

        // Handle attributes
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i ++) {
            final String value = reader.getAttributeValue(i);
            if (reader.getAttributeNamespace(i) != null) {
                throw ParseUtils.unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case NAME: {
                        name = value;
                        break;
                    }
                    case DEFAULT_INTERFACE: {
                        if (! interfaces.contains(value)) {
                            throw new XMLStreamException("Unknown interface " + value +
                                    " " + attribute.getLocalName() + " must be declared in element " +
                                    Element.INTERFACES.getLocalName(), reader.getLocation());
                        }
                        defIntf = value;
                        break;
                    }
                    default:
                        throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        if (name == null) {
            throw ParseUtils.missingRequired(reader, Collections.singleton(Attribute.NAME));
        }
        if (defIntf == null) {
            throw ParseUtils.missingRequired(reader, Collections.singleton(Attribute.DEFAULT_INTERFACE));
        }
        // Handle elements
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case DOMAIN_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case INCLUDE: {
                            final String includedGroup = ParseUtils.readStringAttributeElement(reader, Attribute.SOCKET_BINDING_GROUP.getLocalName());
                            if (includedGroups.contains(includedGroup)) {
                                throw new XMLStreamException("Included socket-binding-group " + includedGroup + " already declared", reader.getLocation());
                            }
                            includedGroups.add(includedGroup);
                            break;
                        }
                        case SOCKET_BINDING: {
                            final String bindingName = parseSocketBinding(reader, interfaces, bindingUpdates);
                            if (socketBindings.contains(bindingName)) {
                                throw new XMLStreamException("socket-binding " + bindingName + " already declared", reader.getLocation());
                            }
                            socketBindings.add(bindingName);
                            break;
                        }
                        default:
                            throw ParseUtils.unexpectedElement(reader);
                    }
                    break;
                }
                default:
                    throw ParseUtils.unexpectedElement(reader);
            }
        }
        final SocketBindingGroupUpdate update = new SocketBindingGroupUpdate(name, defIntf, includedGroups);
    }

    static String parseSocketBinding(final XMLExtendedStreamReader reader, Set<String> interfaces, List<SocketBindingAdd> updates) throws XMLStreamException {

        String name = null;
        String intf = null;
        Integer port = null;
        Boolean fixPort = null;
        InetAddress mcastAddr = null;
        Integer mcastPort = null;

        // Handle attributes
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i ++) {
            final String value = reader.getAttributeValue(i);
            if (reader.getAttributeNamespace(i) != null) {
                throw ParseUtils.unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case NAME: {
                        name = value;
                        break;
                    }
                    case INTERFACE: {
                        if (! interfaces.contains(value)) {
                            throw new XMLStreamException("Unknown interface " + value +
                                    " " + attribute.getLocalName() + " must be declared in element " +
                                    Element.INTERFACES.getLocalName(), reader.getLocation());
                        }
                        intf = value;
                        break;
                    }
                    case PORT: {
                        port = Integer.valueOf(parsePort(value, attribute, reader, true));
                        break;
                    }
                    case FIXED_PORT: {
                        fixPort = Boolean.valueOf(value);
                        break;
                    }
                    case MULTICAST_ADDRESS: {
                        try {
                            mcastAddr = InetAddress.getByName(value);
                            if (!mcastAddr.isMulticastAddress()) {
                                throw new XMLStreamException("Value " + value + " for attribute " +
                                        attribute.getLocalName() + " is not a valid multicast address",
                                        reader.getLocation());
                            }
                        } catch (UnknownHostException e) {
                            throw new XMLStreamException("Value " + value + " for attribute " +
                                    attribute.getLocalName() + " is not a valid multicast address",
                                    reader.getLocation(), e);
                        }
                    }
                    case MULTICAST_PORT: {
                        mcastPort = Integer.valueOf(parsePort(value, attribute, reader, false));
                        break;
                    }
                    default:
                        throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        if (name == null) {
            throw ParseUtils.missingRequired(reader, Collections.singleton(Attribute.NAME));
        }
        // Handle elements
        ParseUtils.requireNoContent(reader);

        final SocketBindingAdd update = new SocketBindingAdd(name, port == null ? 0 : port);
        update.setFixedPort(fixPort == null ? false : fixPort.booleanValue());
        update.setInterfaceName(intf);
        update.setMulticastAddress(mcastAddr);
        update.setMulticastPort(mcastAddr == null ? -1 : mcastPort != null ? mcastPort.intValue() : -1);
        updates.add(update);
        return name;
    }

    static void parseDeployments(final XMLExtendedStreamReader reader, final List<? super AbstractDomainModelUpdate<?>> list, final String serverGroupName) throws XMLStreamException {
        requireNoAttributes(reader);

        final Set<String> names = new HashSet<String>();

        while (reader.nextTag() != END_ELEMENT) {
            // Handle attributes
            String uniqueName = null;
            String runtimeName = null;
            byte[] hash = null;
            String startInput = null;
            final int count = reader.getAttributeCount();
            for (int i = 0; i < count; i ++) {
                final String value = reader.getAttributeValue(i);
                if (reader.getAttributeNamespace(i) != null) {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                } else {
                    final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                    switch (attribute) {
                        case NAME: {
                            if (!names.add(value)) {
                                throw ParseUtils.duplicateNamedElement(reader, value);
                            }
                            uniqueName = value;
                            break;
                        }
                        case RUNTIME_NAME: {
                            runtimeName = value;
                            break;
                        }
                        case SHA1: {
                            try {
                                hash = ParseUtils.hexStringToByteArray(value);
                            }
                            catch (Exception e) {
                               throw new XMLStreamException("Value " + value +
                                       " for attribute " + attribute.getLocalName() +
                                       " does not represent a properly hex-encoded SHA1 hash",
                                       reader.getLocation(), e);
                            }
                            break;
                        }
                        case ALLOWED: {
                            if (!Boolean.valueOf(value)) {
                                throw new XMLStreamException("Attribute '" + attribute.getLocalName() + "' is not allowed", reader.getLocation());
                            }
                            break;
                        }
                        case START: {
                            startInput = value;
                            break;
                        }
                        default:
                            throw ParseUtils.unexpectedAttribute(reader, i);
                    }
                }
            }
            if (uniqueName == null) {
                throw ParseUtils.missingRequired(reader, Collections.singleton(Attribute.NAME));
            }
            if (runtimeName == null) {
                throw ParseUtils.missingRequired(reader, Collections.singleton(Attribute.RUNTIME_NAME));
            }
            if (hash == null) {
                throw ParseUtils.missingRequired(reader, Collections.singleton(Attribute.SHA1));
            }
            boolean toStart = startInput == null ? true : Boolean.valueOf(startInput);

            // Handle elements
            ParseUtils.requireNoContent(reader);

            if (serverGroupName == null) {
                list.add(new DomainDeploymentAdd(uniqueName, runtimeName, hash, toStart));
            }
            else {
                list.add(DomainServerGroupUpdate.create(serverGroupName, new ServerGroupDeploymentAdd(uniqueName, runtimeName, hash, toStart)));
            }
        }
    }

    static void parseServerGroups(final XMLExtendedStreamReader reader, final List<? super AbstractDomainModelUpdate<?>> list) throws XMLStreamException {
        requireNoAttributes(reader);

        final Set<String> names = new HashSet<String>();

        while (reader.nextTag() != END_ELEMENT) {

            String name = null;
            String profile = null;
            Collection<PropertyAdd> systemProperties = null;

            // Handle attributes
            final int count = reader.getAttributeCount();
            for (int i = 0; i < count; i ++) {
                final String value = reader.getAttributeValue(i);
                if (reader.getAttributeNamespace(i) != null) {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                } else {
                    final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                    switch (attribute) {
                        case NAME: {
                            if (name != null) {
                                throw ParseUtils.duplicateAttribute(reader, attribute.getLocalName());
                            }
                            if (!names.add(value)) {
                                throw ParseUtils.duplicateNamedElement(reader, value);
                            }
                            name = value;
                            break;
                        }
                        case PROFILE: {
                            if (profile != null) {
                                throw ParseUtils.duplicateAttribute(reader, attribute.getLocalName());
                            }
                            profile = value;
                            break;
                        }
                        default:
                            throw ParseUtils.unexpectedAttribute(reader, i);
                    }
                }
            }
            if (name == null) {
                throw ParseUtils.missingRequired(reader, Collections.singleton(Attribute.NAME));
            }
            if (profile == null) {
                throw ParseUtils.missingRequired(reader, Collections.singleton(Attribute.PROFILE));
            }

            list.add(new ServerGroupAdd(name, profile));

            // Handle elements

            NamedModelUpdates<JvmElement> jvm = null;
            boolean sawBindingGroup = false;
            boolean sawDeployments = false;
            while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                switch (Namespace.forUri(reader.getNamespaceURI())) {
                    case DOMAIN_1_0: {
                        final Element element = Element.forName(reader.getLocalName());
                        switch (element) {
                            case JVM: {
                                if (jvm != null) {
                                    throw new XMLStreamException(element.getLocalName() + " already defined", reader.getLocation());
                                }
                                jvm = parseJvm(reader);
                                list.add(DomainServerGroupUpdate.create(name, new ServerGroupJvmAdd(jvm.name)));
                                for (AbstractModelUpdate<JvmElement, ?> update : jvm.updates) {
                                    list.add(DomainServerGroupUpdate.create(name, ServerGroupJvmUpdate.create(update)));
                                }
                                break;
                            }
                            case SOCKET_BINDING_GROUP: {
                                if (sawBindingGroup) {
                                    throw new XMLStreamException(element.getLocalName() + " already defined", reader.getLocation());
                                }
                                sawBindingGroup = true;
                                parseSocketBindingGroupRef(reader, name, list);
                                break;
                            }
                            case DEPLOYMENTS: {
                                if (sawDeployments) {
                                    throw new XMLStreamException(element.getLocalName() + " already defined", reader.getLocation());
                                }
                                sawDeployments = true;
                                parseDeployments(reader, list, name);
                                break;
                            }
                            case SYSTEM_PROPERTIES: {
                                if (systemProperties != null) {
                                    throw new XMLStreamException(element.getLocalName() + " already declared", reader.getLocation());
                                }
                                systemProperties = parseProperties(reader, Element.PROPERTY, true);
                                for(final PropertyAdd propertyUpdate : systemProperties) {
                                    list.add(DomainServerGroupUpdate.create(name, new ServerGroupPropertiesUpdate(name, propertyUpdate)));
                                }
                                break;
                            }
                            default:
                                throw ParseUtils.unexpectedElement(reader);
                        }
                        break;
                    }
                    default:
                        throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
    }

    static void parseDomainSystemProperties(final XMLExtendedStreamReader reader, final List<? super AbstractDomainModelUpdate<?>> list) throws XMLStreamException {
        for(final PropertyAdd propertyUpdate : parseProperties(reader, Element.PROPERTY, true)) {
            list.add(new DomainSystemPropertyUpdate(propertyUpdate));
        }
    }

    static Collection<PropertyAdd> parseProperties(final XMLExtendedStreamReader reader, final Element propertyType, final boolean allowNullValue) throws XMLStreamException {
        Map<String, PropertyAdd> properties = new HashMap<String, PropertyAdd>();
        // Handle attributes
        ParseUtils.requireNoAttributes(reader);
        // Handle elements
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case DOMAIN_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    if (element == propertyType) {
                        // Handle attributes
                        String name = null;
                        String value = null;
                        int count = reader.getAttributeCount();
                        for (int i = 0; i < count; i++) {
                            final String attrValue = reader.getAttributeValue(i);
                            if (reader.getAttributeNamespace(i) != null) {
                                throw ParseUtils.unexpectedAttribute(reader, i);
                            }
                            else {
                                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                                switch (attribute) {
                                    case NAME: {
                                        name = attrValue;
                                        if (properties.containsKey(name)) {
                                            throw new XMLStreamException("Property " + name + " already exists", reader.getLocation());
                                        }
                                        break;
                                    }
                                    case VALUE: {
                                        value = attrValue;
                                        break;
                                    }
                                    default:
                                        throw ParseUtils.unexpectedAttribute(reader, i);
                                }
                            }
                        }
                        if (name == null) {
                            throw ParseUtils.missingRequired(reader, Collections.singleton(Attribute.NAME));
                        }
                        if (value == null && !allowNullValue) {
                            throw new XMLStreamException("Value for property " + name + " is null", reader.getLocation());
                        }
                        // PropertyAdd
                        properties.put(name, new PropertyAdd(name, value));
                        // Handle elements
                        ParseUtils.requireNoContent(reader);
                    } else {
                        throw ParseUtils.unexpectedElement(reader);
                    }
                    break;
                }
                default:
                    throw ParseUtils.unexpectedElement(reader);
            }
        }
        if (properties.size() == 0) {
            throw ParseUtils.missingRequiredElement(reader, Collections.singleton(propertyType));
        }
        return properties.values();
    }

    static NamedModelUpdates<JvmElement> parseJvm(XMLExtendedStreamReader reader) throws XMLStreamException {

        List<AbstractModelUpdate<JvmElement, ?>> updates = new ArrayList<AbstractModelUpdate<JvmElement, ?>>();

        // Handle attributes
        String name = null;
        String home = null;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i ++) {
            final String value = reader.getAttributeValue(i);
            if (reader.getAttributeNamespace(i) != null) {
                throw ParseUtils.unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case NAME: {
                        if (name != null)
                            throw ParseUtils.duplicateAttribute(reader, attribute.getLocalName());
                        name = value;
                        break;
                    }
                    case JAVA_HOME: {
                        if (home != null)
                            throw ParseUtils.duplicateAttribute(reader, attribute.getLocalName());
                        updates.add(new JvmHomeUpdate(value));
                        break;
                    }
                    default:
                        throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        if (name == null) {
            // FIXME and fix xsd. A name is only required at domain and host
            // level (i.e. when wrapped in <jvms/>). At server-group and server
            // levels it can be unnamed, in which case configuration from
            // domain and host levels aren't mixed in. OR make name required in xsd always
            throw ParseUtils.missingRequired(reader, Collections.singleton(Attribute.NAME));
        }
        // Handle elements
        Collection<PropertyAdd> environmentVariables = null;
        Collection<PropertyAdd> systemProperties = null;
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case DOMAIN_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case HEAP: {
                            updates.addAll(parseHeap(reader));
                            break;
                        }
                        case ENVIRONMENT_VARIABLES: {
                            if (environmentVariables != null) {
                                throw new XMLStreamException(element.getLocalName() + " already declared", reader.getLocation());
                            }
                            environmentVariables = parseProperties(reader, Element.VARIABLE, true);
                            for (PropertyAdd propAdd : environmentVariables) {
                                updates.add(new JvmEnvironmentVariableUpdate(propAdd));
                            }
                            break;
                        }
                        case SYSTEM_PROPERTIES: {
                            if (systemProperties != null) {
                                throw new XMLStreamException(element.getLocalName() + " already declared", reader.getLocation());
                            }
                            systemProperties = parseProperties(reader, Element.PROPERTY, true);
                            for (PropertyAdd propAdd : systemProperties) {
                                updates.add(new JvmSystemPropertiesUpdate(propAdd));
                            }
                            break;
                        }
                        default:
                            throw ParseUtils.unexpectedElement(reader);
                    }
                    break;
                }
                default:
                    throw ParseUtils.unexpectedElement(reader);
            }
        }
        return new NamedModelUpdates<JvmElement>(name, updates);
    }

    static List<AbstractModelUpdate<JvmElement, ?>> parseHeap(XMLExtendedStreamReader reader) throws XMLStreamException {
        List<AbstractModelUpdate<JvmElement, ?>> updates = new ArrayList<AbstractModelUpdate<JvmElement, ?>>();
        // Handle attributes
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i ++) {
            final String value = reader.getAttributeValue(i);
            if (reader.getAttributeNamespace(i) != null) {
                throw ParseUtils.unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case SIZE: {
                        updates.add(new JvmHeapUpdate(value));
                        break;
                    }
                    case MAX_SIZE: {
                        updates.add(new JvmMaxHeapUpdate(value));
                        break;
                    }
                    default:
                        throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        // Handle elements
        ParseUtils.requireNoContent(reader);
        return updates;
    }

    static void parseSocketBindingGroupRef(final XMLExtendedStreamReader reader, final String serverGroupName, final List<? super AbstractDomainModelUpdate<?>> list) throws XMLStreamException {
        // Handle attributes
        String name = null;
        int offset = -1;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i ++) {
            final String value = reader.getAttributeValue(i);
            if (reader.getAttributeNamespace(i) != null) {
                throw ParseUtils.unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case REF: {
                        if (name != null)
                            throw ParseUtils.duplicateAttribute(reader, attribute.getLocalName());
                        name = value;
                        list.add(DomainServerGroupUpdate.create(serverGroupName, new ServerGroupSocketBindingGroupUpdate(name)));
                        break;
                    }
                    case PORT_OFFSET: {
                        try {
                            if (offset != -1)
                                throw ParseUtils.duplicateAttribute(reader, attribute.getLocalName());
                            offset = Integer.parseInt(value);
                            if (offset < 0) {
                                throw new XMLStreamException(offset + " is not a valid " +
                                        attribute.getLocalName() + " -- must be greater than zero",
                                        reader.getLocation());
                            }
                            list.add(DomainServerGroupUpdate.create(serverGroupName, new ServerGroupSocketBindingPortOffsetUpdate(offset)));
                        } catch (NumberFormatException e) {
                            throw new XMLStreamException(offset + " is not a valid " +
                                    attribute.getLocalName(), reader.getLocation(), e);
                        }
                        break;
                    }
                    default:
                        throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        if (name == null) {
            throw ParseUtils.missingRequired(reader, Collections.singleton(Attribute.REF));
        }
        // Handle elements
        ParseUtils.requireNoContent(reader);
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

        public <E extends AbstractSubsystemElement<E>> void registerSubsystem(final String namespaceUri, final XMLElementReader<ParseResult<SubsystemConfiguration<E>>> elementReader) {
            final XMLMapper mapper = reader.getXMLMapper();
            mapper.registerRootElement(new QName(namespaceUri, Element.SUBSYSTEM.getLocalName()), elementReader);
        }
    }

    private static int parsePort(String value, Attribute attribute, XMLExtendedStreamReader reader, boolean allowEphemeral) throws XMLStreamException {
        int legal;
        try {
            legal = Integer.parseInt(value);
            int min = allowEphemeral ? 0 : 1;
            if (legal < min || legal >= 65536) {
                throw new XMLStreamException("Illegal value " + value +
                        " for attribute '" + attribute.getLocalName() +
                        "' must be between " + min + " and 65536", reader.getLocation());
            }
        }
        catch (NumberFormatException nfe) {
            throw new XMLStreamException("Illegal value " + value +
                    " for attribute '" + attribute.getLocalName() +
                    "' must be an integer", reader.getLocation(), nfe);
        }
        return legal;
    }

    static class NamedModelUpdates<E extends AbstractModelElement<E>> {
        final String name;
        final List<AbstractModelUpdate<E, ?>> updates;

        NamedModelUpdates(final String name, List<AbstractModelUpdate<E, ?>> updates) {
            this.name = name;
            this.updates = updates;
        }
    }

}
