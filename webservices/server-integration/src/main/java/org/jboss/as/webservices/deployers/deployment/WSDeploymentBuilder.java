/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.webservices.deployers.deployment;

import java.util.HashMap;
import java.util.Map;

import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.webservices.util.ASHelper;
import org.jboss.as.webservices.util.WSAttachmentKeys;
import org.jboss.wsf.spi.deployment.Deployment.DeploymentType;

/**
 * JBossWS deployment model builder.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class WSDeploymentBuilder {
    /** Builder instance. */
    private static final WSDeploymentBuilder SINGLETON = new WSDeploymentBuilder();

    /** Builders registry. */
    private static final Map<DeploymentType, DeploymentModelBuilder> builders = new HashMap<DeploymentType, DeploymentModelBuilder>();;

    static {
        WSDeploymentBuilder.builders.put(DeploymentType.JAXWS_JSE, new DeploymentModelBuilderJAXWS_JSE());
        WSDeploymentBuilder.builders.put(DeploymentType.JAXRPC_JSE, new DeploymentModelBuilderJAXRPC_JSE());
        WSDeploymentBuilder.builders.put(DeploymentType.JAXWS_EJB3, new DeploymentModelBuilderJAXWS_EJB3());
        WSDeploymentBuilder.builders.put(DeploymentType.JAXRPC_EJB21, new DeploymentModelBuilderJAXRPC_EJB21());
    }

    /**
     * Constructor.
     */
    private WSDeploymentBuilder() {
        super();
    }

    /**
     * Factory method for obtaining builder instance.
     *
     * @return builder instance
     */
    public static WSDeploymentBuilder getInstance() {
        return WSDeploymentBuilder.SINGLETON;
    }

    /**
     * Builds JBossWS deployment model if web service deployment is detected.
     *
     * @param unit deployment unit
     */
    public void build(final DeploymentUnit unit) {
        final DeploymentType deploymentType = ASHelper.getOptionalAttachment(unit, WSAttachmentKeys.DEPLOYMENT_TYPE_KEY);

        if (deploymentType != null) {
            WSDeploymentBuilder.builders.get(deploymentType).newDeploymentModel(unit);
        }
    }
}
