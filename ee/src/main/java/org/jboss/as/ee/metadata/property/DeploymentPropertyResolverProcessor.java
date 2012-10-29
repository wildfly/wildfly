/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ee.metadata.property;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.metadata.property.CompositePropertyResolver;
import org.jboss.metadata.property.PropertiesPropertyResolver;
import org.jboss.metadata.property.PropertyResolver;

/**
 * @author John Bailey
 */
public class DeploymentPropertyResolverProcessor implements DeploymentUnitProcessor {


    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        DeploymentUnit current = deploymentUnit;
        final List<PropertyResolver> propertyResolvers = new ArrayList<PropertyResolver>();
        do {
            final Properties deploymentProperties = current.getAttachment(Attachments.DEPLOYMENT_PROPERTIES);
            if (deploymentProperties != null) {
                propertyResolvers.add(new PropertiesPropertyResolver(deploymentProperties));
            }
            current = current.getParent();
        } while (current != null);

        if (!propertyResolvers.isEmpty()) {
            deploymentUnit.addToAttachmentList(Attachments.DEPLOYMENT_PROPERTY_RESOLVERS, new CompositePropertyResolver(propertyResolvers));
        }
    }

    public void undeploy(DeploymentUnit context) {
    }
}
