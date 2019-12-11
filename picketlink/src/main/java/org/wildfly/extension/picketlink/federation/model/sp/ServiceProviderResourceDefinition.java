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
package org.wildfly.extension.picketlink.federation.model.sp;

import org.jboss.as.controller.ModelOnlyAddStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.extension.picketlink.common.model.ModelElement;
import org.wildfly.extension.picketlink.federation.model.AbstractFederationResourceDefinition;
import org.wildfly.extension.picketlink.federation.model.handlers.HandlerResourceDefinition;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 * @since Mar 16, 2012
 */
public class ServiceProviderResourceDefinition extends AbstractFederationResourceDefinition {

    public static final SimpleAttributeDefinition SECURITY_DOMAIN = new SimpleAttributeDefinitionBuilder(ModelElement.COMMON_SECURITY_DOMAIN.getName(), ModelType.STRING, false)
        .setAccessConstraints(SensitiveTargetAccessConstraintDefinition.SECURITY_DOMAIN_REF)
        .build();
    public static final SimpleAttributeDefinition URL = new SimpleAttributeDefinitionBuilder(ModelElement.COMMON_URL.getName(),ModelType.STRING, false)
        .setAllowExpression(true)
        .build();
    public static final SimpleAttributeDefinition POST_BINDING = new SimpleAttributeDefinitionBuilder(ModelElement.SERVICE_PROVIDER_POST_BINDING.getName(), ModelType.BOOLEAN, true)
        .setDefaultValue(ModelNode.TRUE)
        .setAllowExpression(true)
        .build();
    public static final SimpleAttributeDefinition SUPPORT_SIGNATURES = new SimpleAttributeDefinitionBuilder(ModelElement.COMMON_SUPPORTS_SIGNATURES.getName(), ModelType.BOOLEAN, true)
        .setDefaultValue(ModelNode.FALSE)
        .setAllowExpression(true)
        .build();
    public static final SimpleAttributeDefinition SUPPORT_METADATA = new SimpleAttributeDefinitionBuilder(ModelElement.COMMON_SUPPORT_METADATA.getName(), ModelType.BOOLEAN, true)
        .setDefaultValue(ModelNode.FALSE)
        .setAllowExpression(true)
        .build();
    public static final SimpleAttributeDefinition STRICT_POST_BINDING = new SimpleAttributeDefinitionBuilder(ModelElement.COMMON_STRICT_POST_BINDING.getName(), ModelType.BOOLEAN, true)
        .setDefaultValue(ModelNode.TRUE)
        .setAllowExpression(true)
        .build();
    public static final SimpleAttributeDefinition ERROR_PAGE = new SimpleAttributeDefinitionBuilder(ModelElement.SERVICE_PROVIDER_ERROR_PAGE.getName(), ModelType.STRING, true)
        .setDefaultValue(new ModelNode("/error.jsp"))
        .setAllowExpression(true)
        .build();
    public static final SimpleAttributeDefinition LOGOUT_PAGE = new SimpleAttributeDefinitionBuilder(ModelElement.SERVICE_PROVIDER_LOGOUT_PAGE.getName(), ModelType.STRING, true)
        .setDefaultValue(new ModelNode("/logout.jsp"))
        .setAllowExpression(true)
        .build();
    public static final SimpleAttributeDefinition[] ATTRIBUTE_DEFINITIONS = new SimpleAttributeDefinition[]{SECURITY_DOMAIN, URL,
        POST_BINDING,
        SUPPORT_SIGNATURES,
        SUPPORT_METADATA,
        STRICT_POST_BINDING,
        ERROR_PAGE,
        LOGOUT_PAGE};

    public ServiceProviderResourceDefinition() {
        super(ModelElement.SERVICE_PROVIDER, new ModelOnlyAddStepHandler(ATTRIBUTE_DEFINITIONS), ATTRIBUTE_DEFINITIONS);
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        addChildResourceDefinition(HandlerResourceDefinition.INSTANCE, resourceRegistration);
    }
}
