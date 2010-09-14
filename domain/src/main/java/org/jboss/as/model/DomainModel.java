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

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.Extension;
import org.jboss.as.model.socket.InterfaceElement;
import org.jboss.as.model.socket.SocketBindingGroupElement;
import org.jboss.as.model.socket.SocketBindingGroupIncludeElement;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoadException;
import org.jboss.msc.service.Location;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * The JBoss AS Domain state.  An instance of this class represents the complete running state of the domain.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class DomainModel extends AbstractModel<DomainModel> {

    private static final long serialVersionUID = 5516070442013067881L;

    private final NavigableMap<String, NamespaceAttribute> namespaces = new TreeMap<String, NamespaceAttribute>();
    private final String schemaLocation;
    private final NavigableMap<String, ExtensionElement> extensions = new TreeMap<String, ExtensionElement>();
    private final NavigableMap<String, ServerGroupElement> serverGroups = new TreeMap<String, ServerGroupElement>();
    private final NavigableMap<DeploymentUnitKey, DeploymentUnitElement> deployments = new TreeMap<DeploymentUnitKey, DeploymentUnitElement>();
    private final NavigableMap<String, ProfileElement> profiles = new TreeMap<String, ProfileElement>();
    private final NavigableMap<String, InterfaceElement> interfaces = new TreeMap<String, InterfaceElement>();
    private final NavigableMap<String, SocketBindingGroupElement> bindingGroups = new TreeMap<String, SocketBindingGroupElement>();

    private PropertiesElement systemProperties;

    /**
     * Construct a new instance.
     *
     * @param location the declaration location of the element
     * @param elementName the element name of this domain element
     */
    public DomainModel(final Location location, final QName elementName) {
        super(location, elementName);
        this.schemaLocation = null;
    }

    /**
     * Construct a new instance.
     *
     * @param reader the reader from which to build this element
     * @throws XMLStreamException if an error occurs
     */
    public DomainModel(final XMLExtendedStreamReader reader) throws XMLStreamException {
        super(reader);
        // Handle namespaces
        namespaces.putAll(readNamespaces(reader));
        // Handle attributes
        schemaLocation = readSchemaLocation(reader);
        // Handle elements
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case DOMAIN_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case EXTENSIONS: {
                            parseExtensions(reader);
                            break;
                        }
                        case PROFILES: {
                            parseProfiles(reader);
                            break;
                        }
                        case INTERFACES: {
                            parseInterfaces(reader);
                            break;
                        }
                        case SOCKET_BINDING_GROUPS: {
                            parseSocketBindingGroups(reader);
                            break;
                        }
                        case DEPLOYMENTS: {
                            parseDeployments(reader);
                            break;
                        }
                        case SERVER_GROUPS: {
                            parseServerGroups(reader);
                            break;
                        }
                        case SYSTEM_PROPERTIES: {
                            if (systemProperties != null) {
                                throw new XMLStreamException(element.getLocalName() + " already declared", reader.getLocation());
                            }
                            this.systemProperties = new PropertiesElement(reader);
                            break;
                        }
                        default: throw unexpectedElement(reader);
                    }
                    break;
                }
                default: throw unexpectedElement(reader);
            }
        }
    }

    /**
     * Gets the extension modules available for use in this domain.
     *
     * @return the extensions. May be empty but will not be <code>null</code>
     */
    public Set<ExtensionElement> getExtensions() {
        synchronized (extensions) {
            return new HashSet<ExtensionElement>(extensions.values());
        }
    }

    /**
     * Gets the named interfaces available for use in this domain.
     *
     * @return the interfaces. May be empty but will not be <code>null</code>
     */
    public Set<InterfaceElement> getInterfaces() {
        Set<InterfaceElement> intfs = new LinkedHashSet<InterfaceElement>();
        synchronized (interfaces) {
            for (Map.Entry<String, InterfaceElement> entry : interfaces.entrySet()) {
                intfs.add(entry.getValue());
            }
        }
        return intfs;
    }

    /**
     * Gets the domain-level configuration for a particular interface. Note that
     * this configuration can be overridden at the {@link ServerGroupElement server group}
     * {@link HostModel host} or {@link ServerElement server} levels.
     *
     * @param name the name of the interface
     * @return the interface configuration, or <code>null</code> if no interface
     *         named <code>name</code> is configured
     */
    public InterfaceElement getInterface(String name) {
        synchronized (interfaces) {
            return interfaces.get(name);
        }
    }

    /**
     * Gets the configuration for a given profile.
     *
     * @param name the name of the profile
     * @return the profile configuration, or <code>null</code> if no profile
     *         named <code>name</code> is configured
     */
    public ProfileElement getProfile(String name) {
        synchronized (profiles) {
            return profiles.get(name);
        }
    }

    /**
     * Gets the socket binding group configuration for the group with the given
     * <code>name</code>.
     *
     * @param name the name of the socket binding group
     * @return the socket binding group configuration, or <code>null</code> if
     *         no socket binding named <code>name</code> is configured
     */
    public SocketBindingGroupElement getSocketBindingGroup(String name) {
        synchronized (bindingGroups) {
            return bindingGroups.get(name);
        }
    }

    /**
     * Gets the server group configuration for the group with the given
     * <code>name</code>.
     *
     * @param name the name of the server group
     * @return the server group configuration, or <code>null</code> if no server
     *         group named <code>name</code> is configured
     */
    public ServerGroupElement getServerGroup(String name) {
        synchronized (serverGroups) {
            return serverGroups.get(name);
        }
    }

    /**
     * Gets any system properties defined at the domain level. These properties
     * may be extended or overridden by any properties declared at the
     * {@link ServerGroupElement#getSystemProperties() server group level}, the
     * {@link HostModel#getSystemProperties() host level} or the
     * {@link ServerElement#getSystemProperties() server level}.
     *
     * @return the system properties, or <code>null</code> if there are none
     */
    public PropertiesElement getSystemProperties() {
        return systemProperties;
    }

    /** {@inheritDoc} */
    public long elementHash() {
        long hash = 0L;
        synchronized (extensions) {
            hash = calculateElementHashOf(extensions.values(), hash);
        }
        synchronized (serverGroups) {
            hash = calculateElementHashOf(serverGroups.values(), hash);
        }
        synchronized (deployments) {
            hash = calculateElementHashOf(deployments.values(), hash);
        }
        synchronized (profiles) {
            hash = calculateElementHashOf(profiles.values(), hash);
        }
        synchronized (interfaces) {
            hash = calculateElementHashOf(interfaces.values(), hash);
        }
        synchronized (bindingGroups) {
            hash = calculateElementHashOf(bindingGroups.values(), hash);
        }
        if (systemProperties != null) hash = Long.rotateLeft(hash, 1) ^ systemProperties.elementHash();
        synchronized (namespaces) {
            hash = Long.rotateLeft(hash, 1) ^ namespaces.hashCode() &  0xffffffffL;
        }
        if (schemaLocation != null) hash = Long.rotateLeft(hash, 1) ^ schemaLocation.hashCode() &  0xffffffffL;
        return hash;
    }

    /** {@inheritDoc} */
    protected void appendDifference(final Collection<AbstractModelUpdate<DomainModel>> target, final DomainModel other) {

        calculateDifference(target, safeCopyMap(namespaces), safeCopyMap(other.namespaces), new DifferenceHandler<String, NamespaceAttribute, DomainModel>() {

            @Override
            public void handleAdd(Collection<AbstractModelUpdate<DomainModel>> target, String name,
                    NamespaceAttribute newElement) {
                throw new UnsupportedOperationException("implement me");
            }

            @Override
            public void handleChange(Collection<AbstractModelUpdate<DomainModel>> target, String name,
                    NamespaceAttribute oldElement, NamespaceAttribute newElement) {
                throw new UnsupportedOperationException("implement me");
            }

            @Override
            public void handleRemove(Collection<AbstractModelUpdate<DomainModel>> target, String name,
                    NamespaceAttribute oldElement) {
                throw new UnsupportedOperationException("implement me");
            }

        });

        calculateDifference(target, safeCopyMap(extensions), safeCopyMap(other.extensions), new DifferenceHandler<String, ExtensionElement, DomainModel>() {
            public void handleAdd(final Collection<AbstractModelUpdate<DomainModel>> target, final String name, final ExtensionElement newElement) {
                throw new UnsupportedOperationException("implement me");
            }

            public void handleRemove(final Collection<AbstractModelUpdate<DomainModel>> target, final String name, final ExtensionElement oldElement) {
                throw new UnsupportedOperationException("implement me");
            }

            public void handleChange(final Collection<AbstractModelUpdate<DomainModel>> target, final String name, final ExtensionElement oldElement, final ExtensionElement newElement) {
                // not possible
                throw new IllegalStateException();
            }
        });

        calculateDifference(target, safeCopyMap(profiles), safeCopyMap(other.profiles), new DifferenceHandler<String, ProfileElement, DomainModel>() {
            public void handleAdd(final Collection<AbstractModelUpdate<DomainModel>> target, final String name, final ProfileElement newElement) {
                // todo add-profile
                throw new UnsupportedOperationException("implement me");
            }

            public void handleRemove(final Collection<AbstractModelUpdate<DomainModel>> target, final String name, final ProfileElement oldElement) {
                // todo remove-profile
                throw new UnsupportedOperationException("implement me");
            }

            public void handleChange(final Collection<AbstractModelUpdate<DomainModel>> target, final String name, final ProfileElement oldElement, final ProfileElement newElement) {
                // todo change profile
                throw new UnsupportedOperationException("implement me");
            }
        });

        calculateDifference(target, safeCopyMap(interfaces), safeCopyMap(other.interfaces), new DifferenceHandler<String, InterfaceElement, DomainModel>() {
            public void handleAdd(final Collection<AbstractModelUpdate<DomainModel>> target, final String name, final InterfaceElement newElement) {
                // todo add-interface
                throw new UnsupportedOperationException("implement me");
            }

            public void handleRemove(final Collection<AbstractModelUpdate<DomainModel>> target, final String name, final InterfaceElement oldElement) {
                // todo remove-interface
                throw new UnsupportedOperationException("implement me");
            }

            public void handleChange(final Collection<AbstractModelUpdate<DomainModel>> target, final String name, final InterfaceElement oldElement, final InterfaceElement newElement) {
                // todo change interface
                throw new UnsupportedOperationException("implement me");
            }
        });

        calculateDifference(target, safeCopyMap(bindingGroups), safeCopyMap(other.bindingGroups), new DifferenceHandler<String, SocketBindingGroupElement, DomainModel>() {
            public void handleAdd(final Collection<AbstractModelUpdate<DomainModel>> target, final String name, final SocketBindingGroupElement newElement) {
                // todo add binding group
                throw new UnsupportedOperationException("implement me");
            }

            public void handleRemove(final Collection<AbstractModelUpdate<DomainModel>> target, final String name, final SocketBindingGroupElement oldElement) {
                // todo remove binding group
                throw new UnsupportedOperationException("implement me");
            }

            public void handleChange(final Collection<AbstractModelUpdate<DomainModel>> target, final String name, final SocketBindingGroupElement oldElement, final SocketBindingGroupElement newElement) {
                // todo change binding group
                throw new UnsupportedOperationException("implement me");
            }
        });

        // todo enclosing diff item
        systemProperties.appendDifference(null, other.systemProperties);

        calculateDifference(target, safeCopyMap(deployments), safeCopyMap(other.deployments), new DifferenceHandler<DeploymentUnitKey, DeploymentUnitElement, DomainModel>() {
            public void handleAdd(final Collection<AbstractModelUpdate<DomainModel>> target, final DeploymentUnitKey key, final DeploymentUnitElement newElement) {
                // todo deploy
                throw new UnsupportedOperationException("implement me");
            }

            public void handleRemove(final Collection<AbstractModelUpdate<DomainModel>> target, final DeploymentUnitKey key, final DeploymentUnitElement oldElement) {
                // todo undeploy
                throw new UnsupportedOperationException("implement me");
            }

            public void handleChange(final Collection<AbstractModelUpdate<DomainModel>> target, final DeploymentUnitKey key, final DeploymentUnitElement oldElement, final DeploymentUnitElement newElement) {
                // todo redeploy...? or maybe just modify stuff
                throw new UnsupportedOperationException("implement me");
            }
        });

        calculateDifference(target, safeCopyMap(serverGroups), safeCopyMap(other.serverGroups), new DifferenceHandler<String, ServerGroupElement, DomainModel>() {
            public void handleAdd(final Collection<AbstractModelUpdate<DomainModel>> target, final String name, final ServerGroupElement newElement) {
                // todo add-server-group operation
                throw new UnsupportedOperationException("implement me");
            }

            public void handleRemove(final Collection<AbstractModelUpdate<DomainModel>> target, final String name, final ServerGroupElement oldElement) {
                // todo remove-server-group operation
                throw new UnsupportedOperationException("implement me");
            }

            public void handleChange(final Collection<AbstractModelUpdate<DomainModel>> target, final String name, final ServerGroupElement oldElement, final ServerGroupElement newElement) {
                // todo update-server-group operation
                oldElement.appendDifference(null, newElement);
            }
        });
    }

    /** {@inheritDoc} */
    protected Class<DomainModel> getElementClass() {
        return DomainModel.class;
    }

    /** {@inheritDoc} */
    public void writeContent(final XMLExtendedStreamWriter streamWriter) throws XMLStreamException {

        synchronized (namespaces) {
            for (NamespaceAttribute namespace : namespaces.values()) {
                if (namespace.isDefaultNamespaceDeclaration()) {
                    // for now I assume this is handled externally
                    continue;
                }
                streamWriter.setPrefix(namespace.getPrefix(), namespace.getNamespaceURI());
            }
        }

        if (schemaLocation != null) {
            NamespaceAttribute ns = namespaces.get("http://www.w3.org/2001/XMLSchema-instance");
            streamWriter.writeAttribute(ns.getPrefix(), ns.getNamespaceURI(), "schemaLocation", schemaLocation);
        }

        synchronized (extensions) {
            if (! extensions.isEmpty()) {
                streamWriter.writeStartElement(Element.EXTENSIONS.getLocalName());
                for (ExtensionElement element : extensions.values()) {
                    streamWriter.writeStartElement(Element.EXTENSIONS.getLocalName());
                    element.writeContent(streamWriter);
                }
                streamWriter.writeEndElement();
            }
        }

        synchronized (profiles) {
            if (! profiles.isEmpty()) {
                streamWriter.writeStartElement(Element.PROFILES.getLocalName());
                for (ProfileElement element : profiles.values()) {
                    streamWriter.writeStartElement(Element.PROFILE.getLocalName());
                    element.writeContent(streamWriter);
                }
                streamWriter.writeEndElement();
            }
        }

        synchronized (interfaces) {
            if (! interfaces.isEmpty()) {
                streamWriter.writeStartElement(Element.INTERFACES.getLocalName());
                for (InterfaceElement element : interfaces.values()) {
                    streamWriter.writeStartElement(Element.INTERFACE.getLocalName());
                    element.writeContent(streamWriter);
                }
                streamWriter.writeEndElement();
            }
        }

        synchronized (bindingGroups) {
            if (!bindingGroups.isEmpty()) {
                streamWriter.writeStartElement(Element.SOCKET_BINDING_GROUPS.getLocalName());
                for (SocketBindingGroupElement element : bindingGroups.values()) {
                    streamWriter.writeStartElement(Element.SOCKET_BINDING_GROUP.getLocalName());
                    element.writeContent(streamWriter);
                }
                streamWriter.writeEndElement();
            }
        }

        if (systemProperties != null && systemProperties.size() > 0) {
            streamWriter.writeStartElement(Element.SYSTEM_PROPERTIES.getLocalName());
            systemProperties.writeContent(streamWriter);
        }

        synchronized (deployments) {
            if (! deployments.isEmpty()) {
                streamWriter.writeStartElement(Element.DEPLOYMENTS.getLocalName());
                for (DeploymentUnitElement element : deployments.values()) {
                    streamWriter.writeStartElement(Element.DEPLOYMENT.getLocalName());
                    element.writeContent(streamWriter);
                }
                streamWriter.writeEndElement();
            }
        }

        synchronized (serverGroups) {
            if (! serverGroups.isEmpty()) {
                streamWriter.writeStartElement(Element.SERVER_GROUPS.getLocalName());
                for (ServerGroupElement element : serverGroups.values()) {

                    streamWriter.writeStartElement(Element.SERVER_GROUP.getLocalName());
                    element.writeContent(streamWriter);
                }
                streamWriter.writeEndElement();
            }
        }

        // close domain
        streamWriter.writeEndElement();
    }

    private void registerExtensionHandlers(ExtensionElement extensionElement, final XMLExtendedStreamReader reader) throws XMLStreamException {
        final String module = extensionElement.getModule();
        try {
            for (Extension extension : Module.loadService(module, Extension.class)) {
                // FIXME - as soon as we can get a mapper from a reader...
//                extension.registerElementHandlers(reader.getMapper());
                throw new UnsupportedOperationException("implement registerExtensionHandlers");
            }
        } catch (ModuleLoadException e) {
            throw new XMLStreamException("Failed to load module", e);
        }
    }

    private void parseExtensions(XMLExtendedStreamReader reader) throws XMLStreamException {
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case DOMAIN_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case EXTENSION: {
                            final ExtensionElement extension = new ExtensionElement(reader);
                            if (extensions.containsKey(extension.getModule())) {
                                throw new XMLStreamException("Extension module " + extension.getModule() + " already declared", reader.getLocation());
                            }
                            extensions.put(extension.getModule(), extension);
                            // load the extension so it can register handlers
                            // TODO do this in ExtensionElement itself?
                            registerExtensionHandlers(extension, reader);
                            break;
                        }
                        default: throw unexpectedElement(reader);
                    }
                    break;
                }
                default: throw unexpectedElement(reader);
            }
        }
    }

    private void parseProfiles(XMLExtendedStreamReader reader) throws XMLStreamException {

        RefResolver<String, ProfileElement> resolver = new SimpleRefResolver<String, ProfileElement>(profiles) ;

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case DOMAIN_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case PROFILE: {
                            final ProfileElement profile = new ProfileElement(reader, resolver);
                            if (profiles.containsKey(profile.getName())) {
                                throw new XMLStreamException("Profile " + profile.getName() + " already declared", reader.getLocation());
                            }
                            profiles.put(profile.getName(), profile);
                            break;
                        }
                        default: throw unexpectedElement(reader);
                    }
                    break;
                }
                default: throw unexpectedElement(reader);
            }
        }
        // Validate included profiles
        // We do this after creating the profiles instead of in the ProfileElement
        // constructor itself because even if we required the user to declare the included
        // profile before the includee, if we ended up resorting the content and then
        // marshalling it, we might break things
        for (ProfileElement profile : profiles.values()) {
            for (ProfileIncludeElement include : profile.getIncludedProfiles()) {
                ProfileElement included = profiles.get(include.getProfile());
                if (included == null) {
                    Location loc = include.getLocation();
                    throw new XMLStreamException("ParseError at [row,col]:[" +
                            loc.getLineNumber() + "," + loc.getColumnNumber() +
                            " Message: Included profile " + include.getProfile() + " not found");
                }
            }
        }
    }

    private void parseInterfaces(XMLExtendedStreamReader reader) throws XMLStreamException {
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case DOMAIN_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case INTERFACE: {
                            final InterfaceElement interfaceEl = new InterfaceElement(reader);
                            if (interfaces.containsKey(interfaceEl.getName())) {
                                throw new XMLStreamException("Interface " + interfaceEl.getName() + " already declared", reader.getLocation());
                            }
                            interfaces.put(interfaceEl.getName(), interfaceEl);
                            break;
                        }
                        default: throw unexpectedElement(reader);
                    }
                    break;
                }
                default: throw unexpectedElement(reader);
            }
        }
    }

    private void parseSocketBindingGroups(XMLExtendedStreamReader reader) throws XMLStreamException {
        RefResolver<String, SocketBindingGroupElement> groupResolver = new SimpleRefResolver<String, SocketBindingGroupElement>(bindingGroups);
        RefResolver<String, InterfaceElement> intfResolver = new SimpleRefResolver<String, InterfaceElement>(interfaces);

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case DOMAIN_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case SOCKET_BINDING_GROUP: {
                            SocketBindingGroupElement group = new SocketBindingGroupElement(reader, intfResolver, groupResolver);
                            if (bindingGroups.containsKey(group.getName())) {
                                throw new XMLStreamException(element.getLocalName() + " with name " +
                                        group.getName() + " already declared", reader.getLocation());
                            }
                            bindingGroups.put(group.getName(), group);
                            break;
                        }
                        default: throw unexpectedElement(reader);
                    }
                    break;
                }
                default: throw unexpectedElement(reader);
            }
        }
        // Validate included groups
        // We do this after creating the groups instead of in the SocketBindingGroupElement
        // constructor itself because even if we required the user to declare the included
        // group before the includee, if we ended up resorting the content and then
        // marshalling it, we might break things
        for (SocketBindingGroupElement group : bindingGroups.values()) {
            for (SocketBindingGroupIncludeElement include : group.getIncludedSocketBindingGroups()) {
                SocketBindingGroupElement included = bindingGroups.get(include.getGroupName());
                if (included == null) {
                    Location loc = include.getLocation();
                    // TODO
                    // 1) this isn't the exact correct location (probably c'est la vie
                    // 2) better to create a javax.xml.stream.Location and let
                    throw new XMLStreamException("ParseError at [row,col]:[" +
                            loc.getLineNumber() + "," + loc.getColumnNumber() +
                            " Message: Included " + Element.SOCKET_BINDING_GROUP.getLocalName() +
                            " " + include.getGroupName() + " not found");
                }
            }
        }
    }

    private void parseDeployments(XMLExtendedStreamReader reader) throws XMLStreamException {
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case DOMAIN_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case DEPLOYMENT: {
                            final DeploymentUnitElement deployment = new DeploymentUnitElement(reader);
                            if (deployments.containsKey(deployment.getKey())) {
                                throw new XMLStreamException("Deployment " + deployment.getName() +
                                        " with sha1 hash " + bytesToHexString(deployment.getSha1Hash()) +
                                        " already declared", reader.getLocation());
                            }
                            deployments.put(deployment.getKey(), deployment);
                            break;
                        }
                        default: throw unexpectedElement(reader);
                    }
                    break;
                }
                default: throw unexpectedElement(reader);
            }
        }
    }

    private void parseServerGroups(XMLExtendedStreamReader reader) throws XMLStreamException {
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case DOMAIN_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case SERVER_GROUP: {
                            final ServerGroupElement serverGroup = new ServerGroupElement(reader);
                            if (serverGroups.containsKey(serverGroup.getName())) {
                                throw new XMLStreamException("Server group " + serverGroup.getName() + " already declared", reader.getLocation());
                            }
                            serverGroups.put(serverGroup.getName(), serverGroup);
                            break;
                        }
                        default: throw unexpectedElement(reader);
                    }
                    break;
                }
                default: throw unexpectedElement(reader);
            }
        }
    }
}
