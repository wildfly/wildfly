/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.webservices.dmr;

import static org.jboss.as.webservices.dmr.Constants.ENDPOINT;
import static org.jboss.as.webservices.dmr.Constants.ENDPOINT_CLASS;
import static org.jboss.as.webservices.dmr.Constants.ENDPOINT_CONTEXT;
import static org.jboss.as.webservices.dmr.Constants.ENDPOINT_NAME;
import static org.jboss.as.webservices.dmr.Constants.ENDPOINT_TYPE;
import static org.jboss.as.webservices.dmr.Constants.ENDPOINT_WSDL;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.jboss.as.controller.PathElement;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentResourceSupport;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.webservices.publish.WSEndpointDeploymentUnit;
import org.jboss.dmr.ModelNode;
import org.jboss.ws.common.integration.AbstractDeploymentAspect;
import org.jboss.wsf.spi.deployment.Deployment;
import org.jboss.wsf.spi.deployment.Endpoint;

/**
 * Deployment aspect that modifies DMR WS endpoint model.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class ModelDeploymentAspect extends AbstractDeploymentAspect {

    public ModelDeploymentAspect() {
        super();
    }

    @Override
    public void start(final Deployment dep) {
        final DeploymentUnit unit = dep.getAttachment(DeploymentUnit.class);
        if (unit instanceof WSEndpointDeploymentUnit) return;
        final DeploymentResourceSupport deploymentResourceSupport = unit.getAttachment(Attachments.DEPLOYMENT_RESOURCE_SUPPORT);

        for (final Endpoint endpoint : dep.getService().getEndpoints()) {
            final ModelNode endpointModel;
            try {
                endpointModel = deploymentResourceSupport.getDeploymentSubModel(WSExtension.SUBSYSTEM_NAME,
                        PathElement.pathElement(ENDPOINT, URLEncoder.encode(getId(endpoint), "UTF-8")));
            } catch (final UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }

            endpointModel.get(ENDPOINT_NAME).set(getName(endpoint));
            endpointModel.get(ENDPOINT_CONTEXT).set(getContext(endpoint));
            endpointModel.get(ENDPOINT_CLASS).set(endpoint.getTargetBeanName());
            endpointModel.get(ENDPOINT_TYPE).set(endpoint.getType().toString());
            endpointModel.get(ENDPOINT_WSDL).set(endpoint.getAddress() + "?wsdl");
        }
    }

    @Override
    public void stop(final Deployment dep) {
        //
    }


    private String getName(final Endpoint endpoint) {
        return endpoint.getName().getKeyProperty(Endpoint.SEPID_PROPERTY_ENDPOINT);
    }

    private String getContext(final Endpoint endpoint) {
        return endpoint.getName().getKeyProperty(Endpoint.SEPID_PROPERTY_CONTEXT);
    }

    private String getId(final Endpoint endpoint) {
        final StringBuilder sb = new StringBuilder();
        sb.append(getContext(endpoint));
        sb.append(':');
        sb.append(getName(endpoint));
        return sb.toString();
    }

}
