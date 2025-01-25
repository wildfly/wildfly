/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.security;

import java.util.Arrays;
import java.util.List;

import org.jboss.as.controller.ModelOnlyRemoveStepHandler;
import org.jboss.as.controller.ModelOnlyWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.access.constraint.ApplicationTypeConfig;
import org.jboss.as.controller.access.management.AccessConstraintDefinition;
import org.jboss.as.controller.access.management.ApplicationTypeAccessConstraintDefinition;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.operations.validation.StringAllowedValuesValidator;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelType;

/**
 * @author Jason T. Greene
 */
class SecurityDomainResourceDefinition extends SimpleResourceDefinition {

    static final String CACHE_CONTAINER_BASE_CAPABILTIY = "org.wildfly.clustering.infinispan.cache-container";
    static final String CACHE_CONTAINER_NAME = "security";
    static final String INFINISPAN_CACHE_TYPE = "infinispan";
    static final RuntimeCapability<Void> LEGACY_SECURITY_DOMAIN = RuntimeCapability.Builder.of("org.wildfly.security.legacy-security-domain", true)
            .build();

    public static final SimpleAttributeDefinition CACHE_TYPE = new SimpleAttributeDefinitionBuilder(Constants.CACHE_TYPE, ModelType.STRING, true)
            .setAllowExpression(true)
            .setValidator(new StringAllowedValuesValidator("default", INFINISPAN_CACHE_TYPE))
            .build();

    private final List<AccessConstraintDefinition> accessConstraints;

    SecurityDomainResourceDefinition() {
        super(new Parameters(SecurityExtension.SECURITY_DOMAIN_PATH,
                SecurityExtension.getResourceDescriptionResolver(Constants.SECURITY_DOMAIN))
                .setAddHandler(SecurityDomainAdd.INSTANCE)
                .setRemoveHandler(ModelOnlyRemoveStepHandler.INSTANCE)
                .setCapabilities(LEGACY_SECURITY_DOMAIN));
        ApplicationTypeConfig atc = new ApplicationTypeConfig(SecurityExtension.SUBSYSTEM_NAME, Constants.SECURITY_DOMAIN);
        AccessConstraintDefinition acd = new ApplicationTypeAccessConstraintDefinition(atc);
        this.accessConstraints = Arrays.asList(SensitiveTargetAccessConstraintDefinition.SECURITY_DOMAIN, acd);
        setDeprecated(SecurityExtension.DEPRECATED_SINCE);
    }

    @Override
    public void registerAttributes(final ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerReadWriteAttribute(CACHE_TYPE, null, new ModelOnlyWriteAttributeHandler(CACHE_TYPE));
    }

    @Override
    public List<AccessConstraintDefinition> getAccessConstraints() {
        return accessConstraints;
    }

}
