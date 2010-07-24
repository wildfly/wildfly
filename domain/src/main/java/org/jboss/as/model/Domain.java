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
import java.util.NavigableMap;
import java.util.TreeMap;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.model.socket.InterfaceElement;
import org.jboss.as.Extension;
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
public final class Domain extends AbstractModel<Domain> {

    private static final long serialVersionUID = 5516070442013067881L;

    private final NavigableMap<String, ExtensionElement> extensions = new TreeMap<String, ExtensionElement>();
    private final NavigableMap<String, ServerGroupElement> serverGroups = new TreeMap<String, ServerGroupElement>();
    private final NavigableMap<DeploymentUnitKey, DeploymentUnitElement> deployments = new TreeMap<DeploymentUnitKey, DeploymentUnitElement>();
    private final NavigableMap<String, ProfileElement> profiles = new TreeMap<String, ProfileElement>();
    private final NavigableMap<String, InterfaceElement> interfaces = new TreeMap<String, InterfaceElement>();

    private PropertiesElement systemProperties;

    /**
     * Construct a new instance.
     *
     * @param location the declaration location of the domain element
     * @param elementName the element name of this domain element
     */
    public Domain(final Location location, final QName elementName) {
        super(location, elementName);
    }

    /**
     * Construct a new instance.
     *
     * @param reader the reader from which to build this element
     * @throws XMLStreamException if an error occurs
     */
    public Domain(final XMLExtendedStreamReader reader) throws XMLStreamException {
        super(reader);
        // Handle attributes
        requireNoAttributes(reader);
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
                            final PropertiesElement properties = new PropertiesElement(reader);
                            if (this.systemProperties == null) {
                                this.systemProperties = properties;
                            }
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

    /** {@inheritDoc} */
    public long elementHash() {
        long hash = 0L;
        hash = calculateElementHashOf(extensions.values(), hash);
        hash = calculateElementHashOf(serverGroups.values(), hash);
        hash = calculateElementHashOf(deployments.values(), hash);
        hash = calculateElementHashOf(profiles.values(), hash);
        if (systemProperties != null) hash = Long.rotateLeft(hash, 1) ^ systemProperties.elementHash();
        return hash;
    }

    /** {@inheritDoc} */
    protected void appendDifference(final Collection<AbstractModelUpdate<Domain>> target, final Domain other) {
        calculateDifference(target, extensions, other.extensions, new DifferenceHandler<String, ExtensionElement, Domain>() {
            public void handleAdd(final Collection<AbstractModelUpdate<Domain>> target, final String name, final ExtensionElement newElement) {
                throw new UnsupportedOperationException("implement me");
            }

            public void handleRemove(final Collection<AbstractModelUpdate<Domain>> target, final String name, final ExtensionElement oldElement) {
                throw new UnsupportedOperationException("implement me");
            }

            public void handleChange(final Collection<AbstractModelUpdate<Domain>> target, final String name, final ExtensionElement oldElement, final ExtensionElement newElement) {
                // not possible
                throw new IllegalStateException();
            }
        });
        
        calculateDifference(target, profiles, other.profiles, new DifferenceHandler<String, ProfileElement, Domain>() {
            public void handleAdd(final Collection<AbstractModelUpdate<Domain>> target, final String name, final ProfileElement newElement) {
                // todo add-profile
                throw new UnsupportedOperationException("implement me");
            }

            public void handleRemove(final Collection<AbstractModelUpdate<Domain>> target, final String name, final ProfileElement oldElement) {
                // todo remove-profile
                throw new UnsupportedOperationException("implement me");
            }

            public void handleChange(final Collection<AbstractModelUpdate<Domain>> target, final String name, final ProfileElement oldElement, final ProfileElement newElement) {
                // todo change profile
                throw new UnsupportedOperationException("implement me");
            }
        });
        
        calculateDifference(target, interfaces, other.interfaces, new DifferenceHandler<String, InterfaceElement, Domain>() {
            public void handleAdd(final Collection<AbstractModelUpdate<Domain>> target, final String name, final InterfaceElement newElement) {
                // todo add-interface
                throw new UnsupportedOperationException("implement me");
            }

            public void handleRemove(final Collection<AbstractModelUpdate<Domain>> target, final String name, final InterfaceElement oldElement) {
                // todo remove-interface
                throw new UnsupportedOperationException("implement me");
            }

            public void handleChange(final Collection<AbstractModelUpdate<Domain>> target, final String name, final InterfaceElement oldElement, final InterfaceElement newElement) {
                // todo change interface
                throw new UnsupportedOperationException("implement me");
            }
        });
        
        // todo enclosing diff item
        systemProperties.appendDifference(null, other.systemProperties);
        
        calculateDifference(target, deployments, other.deployments, new DifferenceHandler<DeploymentUnitKey, DeploymentUnitElement, Domain>() {
            public void handleAdd(final Collection<AbstractModelUpdate<Domain>> target, final DeploymentUnitKey key, final DeploymentUnitElement newElement) {
                // todo deploy
                throw new UnsupportedOperationException("implement me");
            }

            public void handleRemove(final Collection<AbstractModelUpdate<Domain>> target, final DeploymentUnitKey key, final DeploymentUnitElement oldElement) {
                // todo undeploy
                throw new UnsupportedOperationException("implement me");
            }

            public void handleChange(final Collection<AbstractModelUpdate<Domain>> target, final DeploymentUnitKey key, final DeploymentUnitElement oldElement, final DeploymentUnitElement newElement) {
                // todo redeploy...? or maybe just modify stuff
                throw new UnsupportedOperationException("implement me");
            }
        });
        
        calculateDifference(target, serverGroups, other.serverGroups, new DifferenceHandler<String, ServerGroupElement, Domain>() {
            public void handleAdd(final Collection<AbstractModelUpdate<Domain>> target, final String name, final ServerGroupElement newElement) {
                // todo add-server-group operation
                throw new UnsupportedOperationException("implement me");
            }

            public void handleRemove(final Collection<AbstractModelUpdate<Domain>> target, final String name, final ServerGroupElement oldElement) {
                // todo remove-server-group operation
                throw new UnsupportedOperationException("implement me");
            }

            public void handleChange(final Collection<AbstractModelUpdate<Domain>> target, final String name, final ServerGroupElement oldElement, final ServerGroupElement newElement) {
                // todo update-server-group operation
                oldElement.appendDifference(null, newElement);
            }
        });
    }

    /** {@inheritDoc} */
    protected Class<Domain> getElementClass() {
        return Domain.class;
    }

    /** {@inheritDoc} */
    public void writeContent(final XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        if (! extensions.isEmpty()) {
            streamWriter.writeStartElement(Element.EXTENSIONS.getLocalName());
            for (ExtensionElement element : extensions.values()) {        
                streamWriter.writeStartElement(Element.EXTENSIONS.getLocalName());
                element.writeContent(streamWriter);
            }
            streamWriter.writeEndElement();
        }
        
        if (! profiles.isEmpty()) {
            streamWriter.writeStartElement(Element.PROFILES.getLocalName());
            for (ProfileElement element : profiles.values()) {
                streamWriter.writeStartElement(Element.PROFILE.getLocalName());
                element.writeContent(streamWriter);
            }
            streamWriter.writeEndElement();
        }
        
        if (! interfaces.isEmpty()) {
            streamWriter.writeStartElement(Element.INTERFACES.getLocalName());
            for (InterfaceElement element : interfaces.values()) {
                streamWriter.writeStartElement(Element.INTERFACE.getLocalName());
                element.writeContent(streamWriter);
            }
            streamWriter.writeEndElement();
        }
        
        // TODO socket-binding-groups
        
        if (systemProperties.size() > 0) {
            streamWriter.writeStartElement("system-properties");
            systemProperties.writeContent(streamWriter);
        }
        
        if (! deployments.isEmpty()) {
            streamWriter.writeStartElement(Element.DEPLOYMENTS.getLocalName());
            for (DeploymentUnitElement element : deployments.values()) {        
                streamWriter.writeStartElement(Element.DEPLOYMENT.getLocalName());
                element.writeContent(streamWriter);
            }
            streamWriter.writeEndElement();
        }
        
        if (! serverGroups.isEmpty()) {
            streamWriter.writeStartElement(Element.SERVER_GROUPS.getLocalName());
            for (ServerGroupElement element : serverGroups.values()) {
        
                streamWriter.writeStartElement(Element.SERVER_GROUP.getLocalName());
                element.writeContent(streamWriter);
            }
            streamWriter.writeEndElement();
        }
        
        // close domain
        streamWriter.writeEndElement();
    }
    
    private void registerExtensionHandlers(ExtensionElement extensionElement, final XMLExtendedStreamReader reader) throws XMLStreamException {
        final String module = extensionElement.getModule();
        try {
            for (Extension extension : Module.loadService(module, Extension.class)) {
                // todo - as soon as we can get a mapper from a reader...
//                extension.registerElementHandlers(reader.getMapper());
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
                }
                default: throw unexpectedElement(reader);
            }
        }    
    }
    
    private void parseProfiles(XMLExtendedStreamReader reader) throws XMLStreamException {
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case DOMAIN_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case PROFILE: {
                            final ProfileElement profile = new ProfileElement(reader);
                            if (profiles.containsKey(profile.getName())) {
                                throw new XMLStreamException("Profile " + profile.getName() + " already declared", reader.getLocation());
                            }
                            profiles.put(profile.getName(), profile);
                            break;
                        }
                        default: throw unexpectedElement(reader);
                    }
                }
                default: throw unexpectedElement(reader);
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
                }
                default: throw unexpectedElement(reader);
            }
        }    
    }
    
    private void parseSocketBindingGroups(XMLExtendedStreamReader reader) throws XMLStreamException {
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case DOMAIN_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case SOCKET_BINDING_GROUP: {
                            throw new UnsupportedOperationException("implement me");
                            //break;
                        }
                        default: throw unexpectedElement(reader);
                    }
                }
                default: throw unexpectedElement(reader);
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
                }
                default: throw unexpectedElement(reader);
            }
        }        
    }
}
