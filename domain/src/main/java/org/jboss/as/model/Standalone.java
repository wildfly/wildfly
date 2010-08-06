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
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.Extension;
import org.jboss.as.model.socket.InterfaceElement;
import org.jboss.as.model.socket.ServerInterfaceElement;
import org.jboss.as.model.socket.SocketBindingElement;
import org.jboss.as.model.socket.SocketBindingGroupElement;
import org.jboss.as.model.socket.SocketBindingGroupRefElement;
import org.jboss.as.services.net.SocketBindingManager;
import org.jboss.as.services.net.SocketBindingManagerService;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoadException;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.Location;
import org.jboss.msc.service.ServiceActivator;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * A standalone server descriptor.  In a standalone server environment, this object model is read from XML.  In
 * a domain situation, this object model is assembled from the combination of the domain and host configuration.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class
        Standalone extends AbstractModel<Standalone> implements ServiceActivator {

    private static final long serialVersionUID = -7764186426598416630L;

    private final String serverName;
    private final NavigableMap<String, ExtensionElement> extensions = new TreeMap<String, ExtensionElement>();
    private final NavigableMap<DeploymentUnitKey, ServerGroupDeploymentElement> deployments = new TreeMap<DeploymentUnitKey, ServerGroupDeploymentElement>();
    private final NavigableMap<String, InterfaceElement> interfaces = new TreeMap<String, InterfaceElement>();
    private final ProfileElement profile;
    private final SocketBindingGroupElement socketBindings;
    private final int portOffset;
    private final JvmElement jvm;
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
        // FIXME implement parsing constructor
        throw new UnsupportedOperationException("implement me");
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
        
        JvmElement serverVM = server.getJvm();
        String serverVMName = serverVM != null ? serverVM.getName() : null;
        
        JvmElement groupVM = serverGroup.getJvm();
        String groupVMName = groupVM != null ? groupVM.getName() : null;
        
        String ourVMName = serverVMName != null ? serverVMName : groupVMName;
        if (ourVMName == null) {
            throw new IllegalStateException("Neither " + Element.SERVER_GROUP.getLocalName() + 
                    " nor " + Element.SERVER.getLocalName() + " has declared a JVM configuration; one or the other must");
        }
        
        if (!ourVMName.equals(groupVMName)) {
            // the server setting replaced the group, so ignore group
            groupVM = null;
        }
        JvmElement hostVM = host.getJvm(ourVMName);
        
        this.jvm = new JvmElement(groupVM, hostVM, serverVM);
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
     * Gets the jvm configuration for this server.
     * 
     * @return the jvm configuration. Will not be <code>null</code>
     */
    public JvmElement getJvm() {
        return jvm;
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
        // FIXME implement elementHash
        throw new UnsupportedOperationException("implement me");
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
        final Map<DeploymentUnitKey, ServerGroupDeploymentElement> deployments = this.deployments;
        for(ServerGroupDeploymentElement deploymentElement : deployments.values()) {
            deploymentElement.activate(context);
        }
    }
}
