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

import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.metadata.property.CompositePropertyResolver;
import org.jboss.metadata.property.PropertyReplacers;

/**
 * @author John Bailey
 */
public class PropertyResolverProcessor implements DeploymentUnitProcessor {
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        CompositePropertyResolver propertyResolver = new CompositePropertyResolver(deploymentUnit.getAttachment(Attachments.DEPLOYMENT_PROPERTY_RESOLVERS));
        deploymentUnit.putAttachment(Attachments.FINAL_PROPERTY_RESOLVER, propertyResolver);
        deploymentUnit.putAttachment(Attachments.FINAL_PROPERTY_REPLACER, PropertyReplacers.resolvingReplacer(propertyResolver));
    }

    public void undeploy(DeploymentUnit deploymentUnit) {
        deploymentUnit.removeAttachment(Attachments.FINAL_PROPERTY_REPLACER);
        deploymentUnit.removeAttachment(Attachments.FINAL_PROPERTY_RESOLVER);
        deploymentUnit.removeAttachment(Attachments.DEPLOYMENT_PROPERTY_RESOLVERS);
    }
}
