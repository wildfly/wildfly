/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
