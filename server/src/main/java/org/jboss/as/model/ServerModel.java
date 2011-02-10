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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import org.jboss.logging.Logger;
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
    private final Map<String, ServerGroupDeploymentElement> deployments = new LinkedHashMap<String, ServerGroupDeploymentElement>();

    /**
     * Construct a new instance.
     */
    public ServerModel() {
        super(ELEMENT_NAME);
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
    }

    public void addDeployment(final ServerGroupDeploymentElement deploymentElement) {
        synchronized (deployments) {
            if(deployments.put(deploymentElement.getUniqueName(), deploymentElement) != null) {
                throw new IllegalArgumentException("Deployment " + deploymentElement.getUniqueName() +
                                        " with sha1 hash " + bytesToHexString(deploymentElement.getSha1Hash()) +
                                        " already declared");
            }
        }
    }

    public ServerGroupDeploymentElement removeDeployment(final String deploymentName) {
        synchronized (deployments) {
            return deployments.remove(deploymentName);
        }
    }

    public Set<ServerGroupDeploymentElement> getDeployments() {
        synchronized (deployments) {
            return new HashSet<ServerGroupDeploymentElement>(deployments.values());
        }
    }
}
