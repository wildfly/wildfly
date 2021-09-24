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
