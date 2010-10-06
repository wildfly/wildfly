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

import java.util.HashSet;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.deployment.chain.JarDeploymentActivator;
import org.jboss.as.deployment.module.ClassifyingModuleLoaderInjector;
import org.jboss.as.deployment.module.ClassifyingModuleLoaderService;
import org.jboss.as.deployment.module.DeploymentModuleLoaderImpl;
import org.jboss.as.deployment.module.DeploymentModuleLoaderService;
import org.jboss.as.model.socket.InterfaceElement;
import org.jboss.as.model.socket.SocketBindingGroupElement;
import org.jboss.as.services.net.SocketBindingManager;
import org.jboss.as.services.net.SocketBindingManagerService;
import org.jboss.logging.Logger;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * A standalone server descriptor.  In a standalone server environment, this object model is read from XML.  In
 * a domain situation, this object model is assembled from the combination of the domain and host configuration.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author Brian Stansberry
 *
 */
public final class ServerModel extends AbstractModel<ServerModel> {

    /**
     * ServiceName under which a {@link org.jboss.msc.service.Service}<ServerModel>}
     * will be registered on a running server.
     *
     * TODO see if exposing the ServerModel this way can be avoided.
     */
    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("server", "model");

    private static final long serialVersionUID = -7764186426598416630L;
    private static final Logger log = Logger.getLogger("org.jboss.as.server");

    private static final QName ELEMENT_NAME = new QName(Namespace.CURRENT.getUriString(), Element.SERVER.getLocalName());

    private final String serverName;
    private final NavigableMap<String, DeploymentRepositoryElement> repositories = new TreeMap<String, DeploymentRepositoryElement>();
    private final NavigableMap<String, ServerGroupDeploymentElement> deployments = new TreeMap<String, ServerGroupDeploymentElement>();
    private final Set<String> extensions = new HashSet<String>();
    private final NavigableMap<String, InterfaceElement> interfaces = new TreeMap<String, InterfaceElement>();
    private final ProfileElement profile;
    private final SocketBindingGroupElement socketBindings;
    private final int portOffset;
    private PropertiesElement systemProperties;

    /**
     * Construct a new instance.
     *
     * @param serverName the server name
     * @param portOffset the port offset
     */
    public ServerModel(final String serverName, final int portOffset) {
        super(ELEMENT_NAME);
        this.serverName = serverName;
        profile = new ProfileElement((String) null);
        socketBindings = new SocketBindingGroupElement((String) null);
        this.portOffset = portOffset;
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

    public ServerGroupDeploymentElement getDeployment(String deploymentName) {
        return deployments.get(deploymentName);
    }

    /** {@inheritDoc} */
    @Override
    protected Class<ServerModel> getElementClass() {
        return ServerModel.class;
    }

    /** {@inheritDoc} */
    @Override
    public void writeContent(final XMLExtendedStreamWriter streamWriter) throws XMLStreamException {

        writeNamespaces(streamWriter);

        // TODO re-evaluate the element order in the xsd; make sure this is correct
        streamWriter.writeStartElement(Element.NAME.getLocalName());
        streamWriter.writeCharacters(serverName);
        streamWriter.writeEndElement();

        streamWriter.writeStartElement(Element.PROFILE.getLocalName());
        profile.writeContent(streamWriter);

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

        if (socketBindings != null) {
            streamWriter.writeStartElement(Element.SOCKET_BINDING_GROUP.getLocalName());
            socketBindings.writeContent(streamWriter);
        }

        // FIXME ssls

        if (systemProperties != null && systemProperties.size() > 0) {
            streamWriter.writeStartElement(Element.SYSTEM_PROPERTIES.getLocalName());
            systemProperties.writeContent(streamWriter);
        }

        if (! repositories.isEmpty()) {
            for (DeploymentRepositoryElement element : repositories.values()) {
                streamWriter.writeStartElement(Element.DEPLOYMENT_REPOSITORY.getLocalName());
                element.writeContent(streamWriter);
            }
        }

        if (! deployments.isEmpty()) {
            streamWriter.writeStartElement(Element.DEPLOYMENTS.getLocalName());
            for (ServerGroupDeploymentElement element : deployments.values()) {
                streamWriter.writeStartElement(Element.DEPLOYMENT.getLocalName());
                element.writeContent(streamWriter);
            }
            streamWriter.writeEndElement();
        }

        streamWriter.writeEndElement();
    }

    /**
     * Activate the standalone server.  Starts up all the services and deployments in this server.
     *
     * @param context the service activator context
     */
    public void activateSubsystems(final ServiceActivatorContext context) {
        final BatchBuilder batchBuilder = context.getBatchBuilder();

        // Activate extensions

        // Activate profile
//        profile.activate(context);

        // Activate Interfaces
//        final Map<String, ServerInterfaceElement> interfaces;
//        synchronized (this.interfaces) {
//            interfaces = new TreeMap<String, ServerInterfaceElement>(this.interfaces);
//        }
//        for(ServerInterfaceElement interfaceElement : interfaces.values()) {
//            interfaceElement.activate(context);
//        }

        // TODO move service binding manager to somewhere else?
        batchBuilder.addService(SocketBindingManager.SOCKET_BINDING_MANAGER,
                new SocketBindingManagerService(portOffset)).setInitialMode(Mode.ON_DEMAND);

        // Activate socket bindings
        // socketBindings.activate(context);

        // Activate deployment module loader
        batchBuilder.addService(ClassifyingModuleLoaderService.SERVICE_NAME, new ClassifyingModuleLoaderService());

        final DeploymentModuleLoaderService deploymentModuleLoaderService = new DeploymentModuleLoaderService(new DeploymentModuleLoaderImpl());
        batchBuilder.addService(DeploymentModuleLoaderService.SERVICE_NAME, deploymentModuleLoaderService)
            .addDependency(ClassifyingModuleLoaderService.SERVICE_NAME, ClassifyingModuleLoaderService.class, new ClassifyingModuleLoaderInjector("deployment", deploymentModuleLoaderService));

        new JarDeploymentActivator().activate(context); // TODO:  This doesn't belong here.
    }

    public void activateDeployments(final ServiceActivatorContext context, final ServiceContainer serviceContainer) {
        final Map<String, ServerGroupDeploymentElement> deployments;
        synchronized (this.deployments) {
            deployments = new TreeMap<String, ServerGroupDeploymentElement>(this.deployments);
        }
        for(ServerGroupDeploymentElement deploymentElement : deployments.values()) {
            try {
                deploymentElement.activate(context, serviceContainer);
            } catch(Throwable t) {
                // TODO: Rollback deployments services added before failure?
                log.error("Failed to activate deployment " + deploymentElement.getUniqueName(), t);
            }
        }
        final Map<String, DeploymentRepositoryElement> repos;
        synchronized (repositories) {
            repos = new TreeMap<String, DeploymentRepositoryElement>(repositories);
        }
        for (DeploymentRepositoryElement repo : repos.values()) {
            try {
                repo.activate(context);
            }
            catch(Throwable t) {
                // TODO: Rollback deployments services added before failure?
                log.error("Failed to activate deployment repository " + repo.getPath(), t);
            }
        }
    }

    boolean addExtension(final String name) {
        return extensions.add(name);
    }

    AbstractSubsystemElement<?> getSubsystem(final String namespaceUri) {
        return profile.getSubsystem(namespaceUri);
    }

    void addDeployment(final ServerGroupDeploymentElement deploymentElement) {
        synchronized (deployments) {
            if(deployments.put(deploymentElement.getUniqueName(), deploymentElement) != null) {
                throw new IllegalArgumentException("Deployment " + deploymentElement.getUniqueName() +
                                        " with sha1 hash " + bytesToHexString(deploymentElement.getSha1Hash()) +
                                        " already declared");
            }
        }
    }

    ServerGroupDeploymentElement removeDeployment(final String deploymentName) {
        synchronized (deployments) {
            return deployments.remove(deploymentName);
        }
    }

    public Set<ServerGroupDeploymentElement> getDeployments() {
        synchronized (deployments) {
            return new HashSet<ServerGroupDeploymentElement>(deployments.values());
        }
    }

    public InterfaceElement getInterface(final String name) {
        synchronized(interfaces) {
            return interfaces.get(name);
        }
    }

    boolean addSubsystem(final String namespaceUri, final AbstractSubsystemElement<?> element) {
        return profile.addSubsystem(namespaceUri, element);
    }

    ProfileElement getProfile() {
        return profile;
    }

    SocketBindingGroupElement getSocketBindings() {
        return socketBindings;
    }

    InterfaceElement addInterface(final String name) {
        if(interfaces.containsKey(name)) {
            return null;
        }
        final InterfaceElement element = new InterfaceElement(name);
        this.interfaces.put(name, element);
        return element;
    }

    boolean removeInterface(final String name) {
        return interfaces.remove(name) != null;
    }
}
