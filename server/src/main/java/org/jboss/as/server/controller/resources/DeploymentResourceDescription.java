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
package org.jboss.as.server.controller.resources;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReadResourceNameOperationStepHandler;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.server.deployment.DeploymentStatusHandler;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public abstract class DeploymentResourceDescription extends SimpleResourceDefinition {

    private DeploymentResourceParent parent;

    protected DeploymentResourceDescription(DeploymentResourceParent parent, OperationStepHandler addHandler, OperationStepHandler removeHandler) {
        super(PathElement.pathElement(DEPLOYMENT),
                DeploymentAttributes.DEPLOYMENT_RESOLVER,
                addHandler,
                removeHandler);
        this.parent = parent;
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {

        for (AttributeDefinition attr : parent.getResourceAttributes()) {
            if (attr.getName().equals(DeploymentAttributes.STATUS.getName())) {
                resourceRegistration.registerMetric(attr, DeploymentStatusHandler.INSTANCE);
            } else if (attr.getName().equals(DeploymentAttributes.NAME.getName())) {
                resourceRegistration.registerReadOnlyAttribute(DeploymentAttributes.NAME, ReadResourceNameOperationStepHandler.INSTANCE);
            } else {
                resourceRegistration.registerReadOnlyAttribute(attr, null);
            }
        }
    }

    protected DeploymentResourceParent getParent() {
        return parent;
    }

    public static enum DeploymentResourceParent {
        DOMAIN (DeploymentAttributes.DOMAIN_RESOURCE_ATTRIBUTES, DeploymentAttributes.DOMAIN_ADD_ATTRIBUTES),
        SERVER_GROUP (DeploymentAttributes.SERVER_GROUP_RESOURCE_ATTRIBUTES, DeploymentAttributes.SERVER_GROUP_ADD_ATTRIBUTES),
        SERVER (DeploymentAttributes.SERVER_RESOURCE_ATTRIBUTES, DeploymentAttributes.SERVER_ADD_ATTRIBUTES);

        final AttributeDefinition[] resourceAttributes;
        final AttributeDefinition[] addAttributes;
        private DeploymentResourceParent(AttributeDefinition[] resourceAttributes, AttributeDefinition[] addAttributes) {
            this.resourceAttributes = resourceAttributes;
            this.addAttributes = addAttributes;
        }

        AttributeDefinition[] getResourceAttributes() {
            return resourceAttributes;
        }

        AttributeDefinition[] getAddAttributes() {
            return addAttributes;
        }
    }
}
