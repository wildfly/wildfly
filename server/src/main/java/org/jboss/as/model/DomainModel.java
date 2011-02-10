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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * The JBoss AS Domain state.  An instance of this class represents the complete running state of the domain.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class DomainModel extends AbstractModel<DomainModel> {

    private static final long serialVersionUID = 5516070442013067881L;

    // model fields
    private final Map<String, ServerGroupElement> serverGroups = new LinkedHashMap<String, ServerGroupElement>();
    private final Map<String, DeploymentUnitElement> deployments = new LinkedHashMap<String, DeploymentUnitElement>();

    private static final QName ELEMENT_NAME = new QName(Namespace.CURRENT.getUriString(), Element.DOMAIN.getLocalName());

    /**
     * Construct a new instance.
     */
    public DomainModel() {
        super(ELEMENT_NAME);
    }

    public Set<String> getServerGroupNames() {
        synchronized (serverGroups) {
            return new HashSet<String>(serverGroups.keySet());
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
     * Gets the deployment configuration for a given deployment.
     *
     * @param uniqueName the user-specified unique name for the deployment
     *
     * @return the deployment configuration or <code>null</code> if no matching
     *         deployment exists
     */
    public DeploymentUnitElement getDeployment(String uniqueName) {
        synchronized (deployments) {
            return deployments.get(uniqueName);
        }
    }

    public Set<String> getDeploymentNames() {
        synchronized (deployments) {
            return new LinkedHashSet<String>(deployments.keySet());
        }
    }

    /** {@inheritDoc} */
    @Override
    protected Class<DomainModel> getElementClass() {
        return DomainModel.class;
    }

    /** {@inheritDoc} */
    @Override
    public void writeContent(final XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
    }

    boolean addServerGroup(String name, String profile) {
        if(serverGroups.containsKey(name)) {
            return false;
        }
        final ServerGroupElement group = new ServerGroupElement(name, profile);
        serverGroups.put(name, group);
        return true;
    }

    boolean removeServerGroup(final String name) {
        return serverGroups.remove(name) != null;
    }

    boolean addDeployment(DeploymentUnitElement deployment) {
        if (deployments.containsKey(deployment.getUniqueName()))
            return false;
        deployments.put(deployment.getUniqueName(), deployment);
        return true;
    }

    boolean removeDeployment(String uniqueName) {
        return deployments.remove(uniqueName) != null;
    }

    Set<String> getServerGroupDeploymentsMappings(String deploymentUniqueName) {
        Set<String> mappings = new HashSet<String>();
        for (ServerGroupElement sge : serverGroups.values()) {
            if (sge.getDeployment(deploymentUniqueName) != null) {
                mappings.add(sge.getName());
            }
        }
        return mappings;
    }

}
