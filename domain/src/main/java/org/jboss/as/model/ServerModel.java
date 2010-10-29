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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.model.socket.InterfaceElement;
import org.jboss.as.model.socket.SocketBindingGroupElement;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.msc.service.ServiceContainer;
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
     * ServiceName under which a {@link org.jboss.msc.service.Service}<ServerModel>
     * will be registered on a running server.
     *
     * TODO see if exposing the ServerModel this way can be avoided.
     */
    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("server", "model");

    public static final String DEFAULT_STANDALONE_NAME;
    static {
        try {
            DEFAULT_STANDALONE_NAME = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    private static final long serialVersionUID = -7764186426598416630L;
    private static final Logger log = Logger.getLogger("org.jboss.as.server");

    private static final QName ELEMENT_NAME = new QName(Namespace.CURRENT.getUriString(), Element.SERVER.getLocalName());

    /** Name for this server that was actually provided via configuration */
    private String configuredServerName;
    private final Set<String> extensions = new LinkedHashSet<String>();
    private final Map<String, DeploymentRepositoryElement> repositories = new LinkedHashMap<String, DeploymentRepositoryElement>();
    private final Map<String, ServerGroupDeploymentElement> deployments = new LinkedHashMap<String, ServerGroupDeploymentElement>();
    private final Map<String, InterfaceElement> interfaces = new LinkedHashMap<String, InterfaceElement>();
    private final Map<String, PathElement> paths = new LinkedHashMap<String, PathElement>();
    private ProfileElement profile;
    private SocketBindingGroupElement socketBindings;
    private int portOffset;
    private final PropertiesElement systemProperties = new PropertiesElement(Element.PROPERTY, true);
    private ManagementElement managementElement;

    /**
     * Construct a new instance.
     */
    public ServerModel() {
        super(ELEMENT_NAME);
    }

    /**
     * Construct a new instance.
     *
     * @param configuredServerName the server name
     * @param portOffset the port offset
     */
    public ServerModel(final String serverName, final int portOffset) {
        super(ELEMENT_NAME);
        this.configuredServerName = serverName;
        this.portOffset = portOffset;
    }

    /**
     * Gets the name of the server.
     *
     * @return the name. Will not be <code>null</code>
     */
    public String getServerName() {
        return configuredServerName == null ? DEFAULT_STANDALONE_NAME : configuredServerName;
    }

    void setServerName(final String name) {
        this.configuredServerName = name;
    }

    /**
     * Gets the portOffset of the server.
     *
     * @return the portOffset
     */
    public int getPortOffset() {
        return portOffset;
    }

    void setPortOffset(int portOffset) {
        this.portOffset = portOffset;
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

    public Collection<String> getDeploymentRepositories() {
        return new HashSet<String>(repositories.keySet());
    }

    /**
     * Get a deployment repository.
     *
     * @param path the repository path
     * @return the repository, <code>null</code> if it does not exist
     */
    public DeploymentRepositoryElement getDeploymentRepository(final String path) {
        return repositories.get(path);
    }

    /**
     * Get the paths.
     *
     * @return the paths
     */
    public Collection<PathElement> getPaths() {
        return Collections.unmodifiableCollection(new HashSet<PathElement>(paths.values()));
    }

    /**
     * Get a path element.
     *
     * @param name the path name
     * @return the path, <code>null</code> if it does not exist
     */
    public PathElement getPath(final String name) {
        return paths.get(name);
    }

    public ManagementElement getManagementElement() {
        return managementElement;
    }

    /** {@inheritDoc} */
    @Override
    protected Class<ServerModel> getElementClass() {
        return ServerModel.class;
    }

    /** {@inheritDoc} */
    @Override
    public void writeContent(final XMLExtendedStreamWriter streamWriter) throws XMLStreamException {

        if (configuredServerName != null) {
            streamWriter.writeAttribute(Attribute.NAME.getLocalName(), configuredServerName);
        }

        writeNamespaces(streamWriter);

        if (! extensions.isEmpty()) {
            streamWriter.writeStartElement(Element.EXTENSIONS.getLocalName());
            for (String extension : extensions) {
                streamWriter.writeEmptyElement(Element.EXTENSION.getLocalName());
                streamWriter.writeAttribute(Attribute.MODULE.getLocalName(), extension);
            }
            streamWriter.writeEndElement();
        }

        synchronized(paths) {
            if(! paths.isEmpty()) {
                streamWriter.writeStartElement(Element.PATHS.getLocalName());
                for(final PathElement path : paths.values()) {
                    streamWriter.writeStartElement(Element.PATH.getLocalName());
                    path.writeContent(streamWriter);
                }
                streamWriter.writeEndElement();
            }
        }

        synchronized (managementElement) {
            if (managementElement != null) {
                streamWriter.writeStartElement(Element.MANAGEMENT.getLocalName());
                managementElement.writeContent(streamWriter);
            }
        }

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
//        for (DeploymentRepositoryElement repo : repos.values()) {
//            try {
//                repo.activate(context);
//            }
//            catch(Throwable t) {
//                // TODO: Rollback deployments services added before failure?
//                log.error("Failed to activate deployment repository " + repo.getPath(), t);
//            }
//        }
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

    public ProfileElement getProfile() {
        return profile;
    }

    void setProfile(ProfileElement profile) {
        this.profile = profile;
    }

    SocketBindingGroupElement getSocketBindings() {
        return socketBindings;
    }

    void setSocketBindings(SocketBindingGroupElement socketBindings) {
        this.socketBindings = socketBindings;
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

    PathElement addPath(final String name) {
        if(paths.containsKey(name)) {
            return null;
        }
        final PathElement path = new PathElement(name);
        paths.put(name, path);
        return path;
    }

    boolean removePath(final String name) {
        return paths.remove(name) != null;
    }

    boolean addDeploymentRepository(final String path) {
        if(repositories.containsKey(path)) {
            return false;
        }
        final DeploymentRepositoryElement repository = new DeploymentRepositoryElement(path);
        repositories.put(path, repository);
        return true;
    }

    boolean removeDeploymentRepository(final String path) {
        return repositories.remove(path) != null;
    }

    boolean addManagementElement(String interfaceName, int port) {
        if (managementElement != null)
            return false;
        managementElement = new ManagementElement(interfaceName, port);
        return true;
    }

    boolean removeManagementElement() {
        if (managementElement != null) {
            managementElement = null;
            return true;
        }
        return false;
    }


}
