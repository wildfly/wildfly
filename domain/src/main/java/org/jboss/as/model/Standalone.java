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

import org.jboss.as.Extension;
import org.jboss.as.deployment.service.ServiceDeploymentActivator;
import org.jboss.as.model.socket.InterfaceElement;
import org.jboss.as.model.socket.ServerInterfaceElement;
import org.jboss.as.model.socket.SocketBindingElement;
import org.jboss.as.model.socket.SocketBindingGroupElement;
import org.jboss.as.model.socket.SocketBindingGroupRefElement;
import org.jboss.as.services.net.SocketBindingManager;
import org.jboss.as.services.net.SocketBindingManagerService;
import org.jboss.logging.Logger;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoadException;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.Location;
import org.jboss.msc.service.ServiceActivator;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

/**
 * A standalone server descriptor.  In a standalone server environment, this object model is read from XML.  In
 * a domain situation, this object model is assembled from the combination of the domain and host configuration.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author Brian Stansberry
 * 
 */
public final class Standalone extends AbstractModel<Standalone> implements ServiceActivator {

    private static final long serialVersionUID = -7764186426598416630L;
    private static final Logger log = Logger.getLogger("org.jboss.as.server");
    
    private final NavigableMap<String, NamespaceAttribute> namespaces = new TreeMap<String, NamespaceAttribute>();
    private final String schemaLocation;
    private final String serverName;
    private final NavigableMap<String, ExtensionElement> extensions = new TreeMap<String, ExtensionElement>();
    private final NavigableMap<DeploymentUnitKey, ServerGroupDeploymentElement> deployments = new TreeMap<DeploymentUnitKey, ServerGroupDeploymentElement>();
    private final NavigableMap<String, InterfaceElement> interfaces = new TreeMap<String, InterfaceElement>();
    private final ProfileElement profile;
    private final SocketBindingGroupElement socketBindings;
    private final int portOffset;
    private PropertiesElement systemProperties;

    
    /**
     * Construct a new instance.
     *
     * @param location the declaration location of this standalone element
     * @param elementName the element name of this standalone element
     */
    protected Standalone(final Location location, final QName elementName) {
        super(location, elementName);
        // FIXME implement or remove Location-based constructor
        throw new UnsupportedOperationException("implement me");
    }

    /**
     * Construct a new instance.
     *
     * @param reader the reader from which to build this element
     * @throws XMLStreamException if an error occurs
     */
    public Standalone(final XMLExtendedStreamReader reader) throws XMLStreamException {
        super(reader);
        
        this.portOffset = 0;
        
        // Handle namespaces
        namespaces.putAll(readNamespaces(reader));
        // Handle attributes
        schemaLocation = readSchemaLocation(reader);
        // Handle elements
        String name = null;
        ProfileElement profileElement = null;
        SocketBindingGroupElement bindingGroup = null;
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case DOMAIN_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case NAME: {
                            if (name != null) {
                                throw new XMLStreamException(element.getLocalName() + " already declared", reader.getLocation());
                            }
                            name = reader.getElementText();
                            break;
                        }
                        case EXTENSIONS: {
                            parseExtensions(reader);
                            break;
                        }
                        case SYSTEM_PROPERTIES: {
                            if (systemProperties != null) {
                                throw new XMLStreamException(element.getLocalName() + " already declared", reader.getLocation());
                            }
                            this.systemProperties = new PropertiesElement(reader);
                            break;
                        }
                        case INTERFACES: {
                            parseInterfaces(reader);
                            break;
                        }
                        case PROFILE : {
                            if (profileElement != null) {
                                throw new XMLStreamException(element.getLocalName() + " already declared", reader.getLocation());
                            }
                            profileElement = new ProfileElement(reader, null);
                            break;
                        }
                        case DEPLOYMENTS: {
                            parseDeployments(reader);
                            break;
                        }
                        case SOCKET_BINDING_GROUP: {
                            if (bindingGroup != null) {
                                throw new XMLStreamException(element.getLocalName() + " already declared", reader.getLocation());
                            }
                            RefResolver<String, InterfaceElement> intfResolver = new RefResolver<String, InterfaceElement>() {

                                private static final long serialVersionUID = 8976121114197265586L;

                                 @Override
                                 public InterfaceElement resolveRef(String ref) {
                                     if (ref == null)
                                         throw new IllegalArgumentException("ref is null");
                                     return interfaces.get(ref);
                                 }
                                 
                            };
                            bindingGroup = new SocketBindingGroupElement(reader, intfResolver, null);
                            break;
                        }
                        case SSLS: {
                            throw new UnsupportedOperationException("implement parsing of " + element.getLocalName());
                            //break;
                        }
                        default: throw unexpectedElement(reader);
                    }
                    break;
                }
                default: throw unexpectedElement(reader);
            }
        }
        if (name == null) {
            throw missingRequired(reader, Collections.singleton(Element.NAME));
        }
        this.serverName = name;
        if (profileElement == null) {
            throw missingRequired(reader, Collections.singleton(Element.PROFILE));
        }
        this.profile = profileElement;
        this.socketBindings = bindingGroup;
    }

    /**
     * Assemble a standalone server configuration from the domain/host model.
     *
     * @param domain the domain
     * @param host the host
     * @param serverName the name of the server to initialize
     * @return the standalone server model
     */
    public Standalone(final Domain domain, final Host host, final String serverName) {
        super(null, null);
        if (domain == null) {
            throw new IllegalArgumentException("domain is null");
        }
        if (host == null) {
            throw new IllegalArgumentException("host is null");
        }
        if (serverName == null) {
            throw new IllegalArgumentException("serverName is null");
        }
        
        this.schemaLocation = null;
        
        ServerElement server = host.getServer(serverName);
        if (server == null)
            throw new IllegalStateException("Server " + serverName + " is not listed in Host");
        
        this.serverName = serverName;
        
        String serverGroupName = server.getServerGroup();
        ServerGroupElement serverGroup = domain.getServerGroup(serverGroupName);
        if (serverGroup == null)
            throw new IllegalStateException("Server group" + serverGroupName + " is not listed in Domain");
        
        String profileName = serverGroup.getProfileName();
        this.profile = domain.getProfile(profileName);
        if (profile == null)
            throw new IllegalStateException("Profile" + profileName + " is not listed in Domain");
        
        Set<ServerGroupDeploymentElement> groupDeployments = serverGroup.getDeployments();
        for (ServerGroupDeploymentElement dep : groupDeployments) {
            deployments.put(dep.getKey(), dep);
        }
        
        SocketBindingGroupRefElement bindingRef = server.getSocketBindingGroup();
        if (bindingRef == null) {
            bindingRef = serverGroup.getSocketBindingGroup();
        }
        this.socketBindings = domain.getSocketBindingGroup(bindingRef.getRef());
        this.portOffset = bindingRef.getPortOffset();
        
        this.systemProperties = new PropertiesElement(Element.SYSTEM_PROPERTIES, true, 
                domain.getSystemProperties(), serverGroup.getSystemProperties(),
                host.getSystemProperties(), server.getSystemProperties());
        
        Set<String> unspecifiedInterfaces = new HashSet<String>();
        for (InterfaceElement ie : domain.getInterfaces()) {
            if (ie.isFullySpecified())
                interfaces.put(ie.getName(), ie);
            else
                unspecifiedInterfaces.add(ie.getName());
        }
        for (ServerInterfaceElement ie : host.getInterfaces()) {
            interfaces.put(ie.getName(), ie);
            unspecifiedInterfaces.remove(ie.getName());
        }
        for (ServerInterfaceElement ie : server.getInterfaces()) {
            interfaces.put(ie.getName(), ie);
            unspecifiedInterfaces.remove(ie.getName());
        }
        if (unspecifiedInterfaces.size() > 0) {
            // Config didn't fully specify bindings declared in domain; WARN
            // or fail
            if (unspecifiedInterfaces.contains(this.socketBindings.getDefaultInterface())) {
                throw new IllegalStateException("The default interface for socket binding group " + this.socketBindings.getName() +
                        " references interface " + this.socketBindings.getDefaultInterface() + 
                        " but the Server and Host configurations do not specify how to assign an IP address to that interface");
            }
            for (SocketBindingElement binding : this.socketBindings.getAllSocketBindings()) {
                if (unspecifiedInterfaces.contains(binding.getInterfaceName())) {
                    throw new IllegalStateException("Socket binding " + binding.getName() + 
                            " references interface " + binding.getInterfaceName() + 
                            " but the Server and Host configurations do not specify how to assign an IP address to that interface");
                }
            }
            // TODO log a WARN about interfaces that aren't referenced via socket bindings
        }
    }

    /**
     * Gets the name of the server.
     * 
     * @return the name. Will not be <code>null</code>
     */
    public String getServerName() {
        return serverName;
    }
    
    /**
     * Gets any system properties defined for this server.
     * 
     * @return the system properties, or <code>null</code> if there are none
     */
    public PropertiesElement getSystemProperties() {
        return systemProperties;
    }
    
    /** {@inheritDoc} */
    public long elementHash() {
        long cksum = serverName.hashCode() &  0xffffffffL;
        cksum = Long.rotateLeft(cksum, 1) ^ namespaces.hashCode() &  0xffffffffL;
        if (schemaLocation != null) cksum = Long.rotateLeft(cksum, 1) ^ schemaLocation.hashCode() &  0xffffffffL;
        cksum = Long.rotateLeft(cksum, 1) ^ portOffset & 0xffffffffL;
        cksum = Long.rotateLeft(cksum, 1) ^ profile.hashCode() & 0xffffffffL;
        cksum = calculateElementHashOf(deployments.values(), cksum);
        cksum = calculateElementHashOf(extensions.values(), cksum);
        cksum = calculateElementHashOf(interfaces.values(), cksum);
        if (socketBindings != null) cksum = Long.rotateLeft(cksum, 1) ^ socketBindings.elementHash();
        if (systemProperties != null) cksum = Long.rotateLeft(cksum, 1) ^ systemProperties.elementHash();
        return cksum;
    }

    /** {@inheritDoc} */
    protected void appendDifference(final Collection<AbstractModelUpdate<Standalone>> target, final Standalone other) {
        // FIXME implement appendDifference
        throw new UnsupportedOperationException("implement me");
    }

    /** {@inheritDoc} */
    protected Class<Standalone> getElementClass() {
        return Standalone.class;
    }

    /** {@inheritDoc} */
    public void writeContent(final XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        
        for (NamespaceAttribute namespace : namespaces.values()) {
            if (namespace.isDefaultNamespaceDeclaration()) {
                // for now I assume this is handled externally
                continue;
            }
            streamWriter.setPrefix(namespace.getPrefix(), namespace.getNamespaceURI());
        }
        
        if (schemaLocation != null) {
            NamespaceAttribute ns = namespaces.get("http://www.w3.org/2001/XMLSchema-instance");
            streamWriter.writeAttribute(ns.getPrefix(), ns.getNamespaceURI(), "schemaLocation", schemaLocation);
        }
        
        // TODO re-evaluate the element order in the xsd; make sure this is correct
        streamWriter.writeStartElement(Element.NAME.getLocalName());
        streamWriter.writeCharacters(serverName);
        streamWriter.writeEndElement();
        
        if (! extensions.isEmpty()) {
            streamWriter.writeStartElement(Element.EXTENSIONS.getLocalName());
            for (ExtensionElement element : extensions.values()) {        
                streamWriter.writeStartElement(Element.EXTENSIONS.getLocalName());
                element.writeContent(streamWriter);
            }
            streamWriter.writeEndElement();
        }
        
        streamWriter.writeStartElement(Element.PROFILE.getLocalName());
        profile.writeContent(streamWriter);
        
        if (! interfaces.isEmpty()) {
            streamWriter.writeStartElement(Element.INTERFACES.getLocalName());
            for (InterfaceElement element : interfaces.values()) {
                streamWriter.writeStartElement(Element.INTERFACE.getLocalName());
                element.writeContent(streamWriter);
            }
            streamWriter.writeEndElement();
        }
        
        if (socketBindings != null) {
            streamWriter.writeStartElement(Element.SOCKET_BINDING_GROUP.getLocalName());
            socketBindings.writeContent(streamWriter);
        }
        
        // FIXME ssls
        
        if (systemProperties != null && systemProperties.size() > 0) {
            streamWriter.writeStartElement(Element.SYSTEM_PROPERTIES.getLocalName());
            systemProperties.writeContent(streamWriter);
        }
        
        if (! deployments.isEmpty()) {
            streamWriter.writeStartElement(Element.DEPLOYMENTS.getLocalName());
            for (ServerGroupDeploymentElement element : deployments.values()) {
                streamWriter.writeStartElement(Element.DEPLOYMENT.getLocalName());
                element.writeContent(streamWriter);
            }
            streamWriter.writeEndElement();
        } 
    }

    /**
     * Activate the standalone server.  Starts up all the services and deployments in this server.
     *
     * @param context the service activator context
     */
    public void activate(final ServiceActivatorContext context) {
        final BatchBuilder batchBuilder = context.getBatchBuilder();
        // Activate extensions
        final Map<String, ExtensionElement> extensions = this.extensions;
        for(Map.Entry<String, ExtensionElement> extensionEntry : extensions.entrySet()) {
            final ExtensionElement extensionElement = extensionEntry.getValue();
            final String moduleSpec = extensionElement.getModule();
            try {
                for (Extension extension : Module.loadService(moduleSpec, Extension.class)) {
                    extension.activate(context);
                }
            } catch(ModuleLoadException e) {
                throw new RuntimeException("Failed activate subsystem: " + extensionEntry.getKey(), e);
            }
        }

        // Activate profile
        profile.activate(context);

        // Activate Interfaces
        final Map<String, InterfaceElement> interfaces = this.interfaces;
        for(InterfaceElement interfaceElement : interfaces.values()) {
            interfaceElement.activate(context);
        }

        // TODO move service binding manager to somewhere else?
        batchBuilder.addService(SocketBindingManager.SOCKET_BINDING_MANAGER,
        		new SocketBindingManagerService(portOffset)).setInitialMode(Mode.ON_DEMAND);
        
        // Activate socket bindings
        socketBindings.activate(context);

        // Activate deployments
        new ServiceDeploymentActivator().activate(context); // TODO:  This doesn't belong here.
        final Map<DeploymentUnitKey, ServerGroupDeploymentElement> deployments = this.deployments;
        for(ServerGroupDeploymentElement deploymentElement : deployments.values()) {
            try {
                deploymentElement.activate(context);
            } catch(Throwable t) {
                // TODO: Rollback deployments services added before failure?
                log.error("Failed to activate deployment " + deploymentElement.getName(), t);
            }
        }
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
    
    private void parseInterfaces(XMLExtendedStreamReader reader) throws XMLStreamException {
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case DOMAIN_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case INTERFACE: {
                            final ServerInterfaceElement interfaceEl = new ServerInterfaceElement(reader);
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
    
    private void parseDeployments(XMLExtendedStreamReader reader) throws XMLStreamException {
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case DOMAIN_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case DEPLOYMENT: {
                            final ServerGroupDeploymentElement deployment = new ServerGroupDeploymentElement(reader);
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
}
