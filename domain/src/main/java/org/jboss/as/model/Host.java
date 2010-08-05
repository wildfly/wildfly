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
import java.util.Collections;
import java.util.HashSet;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.Extension;
import org.jboss.as.model.socket.InterfaceElement;
import org.jboss.as.model.socket.ServerInterfaceElement;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoadException;
import org.jboss.msc.service.Location;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class Host extends AbstractModel<Host> {

    private static final long serialVersionUID = 7667892965813702351L;

    private final NavigableMap<String, NamespaceAttribute> namespaces = new TreeMap<String, NamespaceAttribute>();
    private final String schemaLocation;
    private final NavigableMap<String, ServerInterfaceElement> interfaces = new TreeMap<String, ServerInterfaceElement>();
    private final NavigableMap<String, ExtensionElement> extensions = new TreeMap<String, ExtensionElement>();
    private final NavigableMap<String, ServerElement> servers = new TreeMap<String, ServerElement>();
    private final NavigableMap<String, JvmElement> jvms = new TreeMap<String, JvmElement>();
    private LocalDomainControllerElement localDomainController;
    
    
    private PropertiesElement systemProperties;
    
    /**
     * Construct a new instance.
     *
     * @param location the declaration location of the host element
     * @param elementName the name of this host element
     */
    public Host(final Location location, final QName elementName) {
        super(location, elementName);
        this.schemaLocation = null;
    }

    /**
     * Construct a new instance.
     *
     * @param reader the reader from which to build this element
     * @throws XMLStreamException if an error occurs
     */
    public Host(final XMLExtendedStreamReader reader) throws XMLStreamException {
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
                        case SYSTEM_PROPERTIES: {
                            if (systemProperties != null) {
                                throw new XMLStreamException(element.getLocalName() + " already declared", reader.getLocation());
                            }
                            this.systemProperties = new PropertiesElement(reader);
                            break;
                        }
                        case DOMAIN_CONTROLLER: {
                            parseDomainController(reader);
                            break;
                        }
                        case INTERFACES: {
                            parseInterfaces(reader);
                            break;
                        }
                        case JVMS: {
                            parseJvms(reader);
                            break;
                        }
                        case SERVERS: {
                            parseServers(reader);
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
     * Gets the host level configuration for the jvm with the given <code>name</name>.
     * This configuration can extend or override any configuration for a jvm
     * with the same name at the {@link ServerGroupElement#getJvm() server group level}.
     * In turn, the details of the configuration of this jvm can be overridden at the
     * {@link ServerElement#getJvm() server level}.
     *     
     * @param name the name of the jvm
     * @return the jvm configuration, or <code>null</code> if there is none with
     *         the given <code>name</name>
     */
    public JvmElement getJvm(String name) {
        return jvms.get(name);
    }
    
    /**
     * Gets the host level configuration for the interface with the given <code>name</name>.
     * This configuration can override any configuration for an interface
     * with the same name at the {@link Domain#getInterface(String) domain level}.
     * In turn, the details of the configuration of this interface can be overridden at the
     * {@link ServerElement#getInterfaces() server level}.
     *     
     * @param name the name of the interface
     * @return the interface configuration, or <code>null</code> if there is none with
     *         the given <code>name</name>
     */
    public InterfaceElement getInterface(String name) {
        return interfaces.get(name);
    }
    
    /**
     * Gets the named interfaces configured at the host level.
     * 
     * @return the interfaces. May be empty but will not be <code>null</code>
     */
    public Set<ServerInterfaceElement> getInterfaces() {
        return Collections.unmodifiableSet(new HashSet<ServerInterfaceElement>(interfaces.values()));
    }
    
   /**
    * Gets the server-specific configurations for the servers associated with this host.
    * 
    * @return the servers. May be empty but will not be <code>null</code>
    */
    public Set<ServerElement> getServers() {
        return Collections.unmodifiableSet(new HashSet<ServerElement>(servers.values()));
    }
    
    /**
     * Gets the server configuration for the server with the given 
     * <code>name</code>.
     * 
     * @param name the name of the server
     * @return the server configuration, or <code>null</code> if no server
     *         named <code>name</code> is configured
     */
    public ServerElement getServer(String name) {
        return servers.get(name);
    }
    
    /**
     * Gets any system properties defined at the host level. These properties
     * can extend and override any properties declared at the 
     * {@link Domain#getSystemProperties() domain level} or the 
     * {@link ServerGroupElement server group level} and may in turn be extended 
     * or overridden by any properties declared at the
     * {@link ServerElement#getSystemProperties() server level}.
     * 
     * @return the system properties, or <code>null</code> if there are none
     */
    public PropertiesElement getSystemProperties() {
        return systemProperties;
    }
    
    public LocalDomainControllerElement getLocalDomainControllerElement() {
        return localDomainController;
    }

    /** {@inheritDoc} */
    public long elementHash() {
        long cksum = calculateElementHashOf(interfaces.values(), 17l);
        cksum = calculateElementHashOf(extensions.values(), cksum);
        cksum = calculateElementHashOf(jvms.values(), cksum);
        cksum = calculateElementHashOf(servers.values(), cksum);
        if (systemProperties != null) cksum = Long.rotateLeft(cksum, 1) ^ systemProperties.elementHash();
        if (localDomainController != null) cksum = Long.rotateLeft(cksum, 1) ^ localDomainController.elementHash();
        cksum = Long.rotateLeft(cksum, 1) ^ namespaces.hashCode() &  0xffffffffL;
        if (schemaLocation != null) cksum = Long.rotateLeft(cksum, 1) ^ schemaLocation.hashCode() &  0xffffffffL;
        // else FIXME remote domain controller
        return cksum;
    }

    /** {@inheritDoc} */
    protected void appendDifference(final Collection<AbstractModelUpdate<Host>> target, final Host other) {
        // FIXME implement appendDifference
        throw new UnsupportedOperationException("implement me");
    }

    /** {@inheritDoc} */
    protected Class<Host> getElementClass() {
        return Host.class;
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
        
        if (! extensions.isEmpty()) {
            streamWriter.writeStartElement(Element.EXTENSIONS.getLocalName());
            for (ExtensionElement element : extensions.values()) {        
                streamWriter.writeStartElement(Element.EXTENSIONS.getLocalName());
                element.writeContent(streamWriter);
            }
            streamWriter.writeEndElement();
        }

        if (systemProperties != null) {
            streamWriter.writeStartElement(Element.SYSTEM_PROPERTIES.getLocalName());
            systemProperties.writeContent(streamWriter);
            streamWriter.writeEndElement();
        }

        streamWriter.writeStartElement(Element.DOMAIN_CONTROLLER.getLocalName());
        if (localDomainController != null) {
            streamWriter.writeStartElement(Element.LOCAL.getLocalName());
            localDomainController.writeContent(streamWriter);
        }
        // FIXME remote domain controller
        streamWriter.writeEndElement();
        
        if (! interfaces.isEmpty()) {
            streamWriter.writeStartElement(Element.INTERFACES.getLocalName());
            for (InterfaceElement element : interfaces.values()) {
                streamWriter.writeStartElement(Element.INTERFACE.getLocalName());
                element.writeContent(streamWriter);
            }
            streamWriter.writeEndElement();
        }
        
        if (! jvms.isEmpty()) {
            streamWriter.writeStartElement(Element.JVMS.getLocalName());
            for (JvmElement element : jvms.values()) {
                streamWriter.writeStartElement(Element.JVM.getLocalName());
                element.writeContent(streamWriter);
            }
            streamWriter.writeEndElement();
        }
        
        if (! servers.isEmpty()) {
            streamWriter.writeStartElement(Element.SERVERS.getLocalName());
            for (ServerElement server : servers.values()) {
                streamWriter.writeStartElement(Element.SERVER.getLocalName());
                server.writeContent(streamWriter);
                streamWriter.writeEndElement();
            }
            streamWriter.writeEndElement();
        }
        
        streamWriter.writeEndElement();
    }

    private void parseDomainController(XMLExtendedStreamReader reader) throws XMLStreamException {
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case DOMAIN_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case LOCAL: {
                            if (localDomainController != null) {
                                throw new XMLStreamException("Child " + element.getLocalName() + 
                                        " of element " + Element.DOMAIN_CONTROLLER.getLocalName() + 
                                        " already declared", reader.getLocation());
                            }
                            this.localDomainController = new LocalDomainControllerElement(reader);
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
    
    private void parseJvms(XMLExtendedStreamReader reader) throws XMLStreamException {
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case DOMAIN_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case JVM: {
                            final JvmElement jvm = new JvmElement(reader);
                            if (jvms.containsKey(jvm.getName())) {
                                throw new XMLStreamException("JVM " + jvm.getName() + " already declared", reader.getLocation());
                            }
                            jvms.put(jvm.getName(), jvm);
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

    
    private void parseServers(XMLExtendedStreamReader reader) throws XMLStreamException {
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case DOMAIN_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case SERVER: {
                            final ServerElement server = new ServerElement(reader);
                            if (servers.containsKey(server.getName())) {
                                throw new XMLStreamException("Interface " + server.getName() + " already declared", reader.getLocation());
                            }
                            servers.put(server.getName(), server);
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
