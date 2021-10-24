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

package org.jboss.as.security;

import static org.jboss.as.security.Constants.CACHE_TYPE;
import static org.jboss.as.security.SecurityDomainResourceDefinition.CACHE_CONTAINER_BASE_CAPABILTIY;
import static org.jboss.as.security.SecurityDomainResourceDefinition.CACHE_CONTAINER_NAME;
import static org.jboss.as.security.SecurityDomainResourceDefinition.LEGACY_SECURITY_DOMAIN;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;

/**
 * Add a security domain configuration.
 *
 * @author Marcus Moyses
 * @author Brian Stansberry
 * @author Jason T. Greene
 */
class SecurityDomainAdd extends AbstractAddStepHandler {

    static final SecurityDomainAdd INSTANCE = new SecurityDomainAdd();

    /**
     * Private to ensure a singleton.
     */
    private SecurityDomainAdd() {
        super(SecurityDomainResourceDefinition.CACHE_TYPE);
    }

    @Override
    protected void recordCapabilitiesAndRequirements(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
        super.recordCapabilitiesAndRequirements(context, operation, resource);
        String cacheType = getAuthenticationCacheType(resource.getModel());
        if (SecurityDomainResourceDefinition.INFINISPAN_CACHE_TYPE.equals(cacheType)) {
            context.registerAdditionalCapabilityRequirement(
                    RuntimeCapability.buildDynamicCapabilityName(CACHE_CONTAINER_BASE_CAPABILTIY, CACHE_CONTAINER_NAME),
                    LEGACY_SECURITY_DOMAIN.getDynamicName(context.getCurrentAddressValue()),
                    SecurityDomainResourceDefinition.CACHE_TYPE.getName());
        }
    }

    static String getAuthenticationCacheType(ModelNode node) {
        String type = null;
        if (node.hasDefined(CACHE_TYPE)) {
            type = node.get(CACHE_TYPE).asString();
        }

        return type;
    }
}
