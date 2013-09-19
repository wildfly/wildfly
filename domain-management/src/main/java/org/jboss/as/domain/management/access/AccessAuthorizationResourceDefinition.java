/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.domain.management.access;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ACCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.AUTHORIZATION;

import java.util.Arrays;
import java.util.List;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.access.CombinationPolicy;
import org.jboss.as.controller.access.management.AccessConstraintDefinition;
import org.jboss.as.controller.access.management.DelegatingConfigurableAuthorizer;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.access.management.WritableAuthorizerConfiguration;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.domain.management._private.DomainManagementResolver;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * {@link org.jboss.as.controller.ResourceDefinition} for the Access Control model.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class AccessAuthorizationResourceDefinition extends SimpleResourceDefinition {

    public static final PathElement PATH_ELEMENT = PathElement.pathElement(ACCESS, AUTHORIZATION);
    public static final Resource RESOURCE = createResource();

    public enum Provider {
        SIMPLE("simple"),
        RBAC("rbac");

        private final String toString;

        private Provider(String toString) {
            this.toString = toString;
        }

        @Override
        public String toString() {
            return toString;
        }
    }

    public static final SimpleAttributeDefinition PERMISSION_COMBINATION_POLICY =
            new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.PERMISSION_COMBINATION_POLICY, ModelType.STRING, true)
            .setDefaultValue(new ModelNode(CombinationPolicy.PERMISSIVE.toString()))
            .setValidator(new EnumValidator<CombinationPolicy>(CombinationPolicy.class, true, false))
            .build();

    public static final SimpleAttributeDefinition PROVIDER = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.PROVIDER, ModelType.STRING, true)
            .setDefaultValue(new ModelNode(Provider.SIMPLE.toString()))
            .setValidator(new EnumValidator<Provider>(Provider.class, true, false))
            .build();

    public static final List<AttributeDefinition> ATTRIBUTES = Arrays.<AttributeDefinition>asList(PROVIDER, PERMISSION_COMBINATION_POLICY);

    public static AccessAuthorizationResourceDefinition forDomain(DelegatingConfigurableAuthorizer configurableAuthorizer) {
        return new AccessAuthorizationResourceDefinition(configurableAuthorizer, true, false);
    }

    public static AccessAuthorizationResourceDefinition forHost(DelegatingConfigurableAuthorizer configurableAuthorizer) {
        return new AccessAuthorizationResourceDefinition(configurableAuthorizer, true, true);
    }

    public static AccessAuthorizationResourceDefinition forDomainServer(DelegatingConfigurableAuthorizer configurableAuthorizer) {
        return new AccessAuthorizationResourceDefinition(configurableAuthorizer, true, false);
    }

    public static AccessAuthorizationResourceDefinition forStandaloneServer(DelegatingConfigurableAuthorizer configurableAuthorizer) {
        return new AccessAuthorizationResourceDefinition(configurableAuthorizer, false, false);
    }

    private final DelegatingConfigurableAuthorizer configurableAuthorizer;

    private final boolean isDomain;
    private final boolean isHostController;
    private final List<AccessConstraintDefinition> accessConstraints;

    private AccessAuthorizationResourceDefinition(DelegatingConfigurableAuthorizer configurableAuthorizer, boolean domain, boolean hostController) {
        super(PATH_ELEMENT, DomainManagementResolver.getResolver("core.access-control"));
        this.configurableAuthorizer = configurableAuthorizer;
        isDomain = domain;
        isHostController = hostController;
        this.accessConstraints = SensitiveTargetAccessConstraintDefinition.ACCESS_CONTROL.wrapAsList();
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);
        if (!isHostController) {
            WritableAuthorizerConfiguration authorizerConfiguration = configurableAuthorizer.getWritableAuthorizerConfiguration();
            resourceRegistration.registerReadWriteAttribute(PROVIDER, null, new AccessAuthorizationProviderWriteAttributeHander(configurableAuthorizer));
            resourceRegistration.registerReadWriteAttribute(PERMISSION_COMBINATION_POLICY, null,
                    new AccessAuthorizationCombinationPolicyWriteAttributeHandler(authorizerConfiguration));
        }
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        if (!isHostController) {
            // Role Mapping
            resourceRegistration.registerSubModel(RoleMappingResourceDefinition.create(configurableAuthorizer));
        }

        // Scoped roles
        if (isDomain) {
            WritableAuthorizerConfiguration authorizerConfiguration = configurableAuthorizer.getWritableAuthorizerConfiguration();
            resourceRegistration.registerSubModel(new ServerGroupScopedRoleResourceDefinition(authorizerConfiguration));
            if (!isHostController) {
                resourceRegistration.registerSubModel(new HostScopedRolesResourceDefinition(authorizerConfiguration));
            }
        }

        // Constraints
        if (!isHostController) {
            //  -- Application Type
            resourceRegistration.registerSubModel(ApplicationClassificationParentResourceDefinition.INSTANCE);
            //  -- Sensitivity Classification
            resourceRegistration.registerSubModel(SensitivityClassificationParentResourceDefinition.INSTANCE);
            //  -- Vault Expression
            resourceRegistration.registerSubModel(SensitivityResourceDefinition.createVaultExpressionConfiguration());
        }
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
        if (isDomain) {
            // Op to apply config from the master to a slave
            resourceRegistration.registerOperationHandler(AccessAuthorizationDomainSlaveConfigHandler.DEFINITION,
                    new AccessAuthorizationDomainSlaveConfigHandler(configurableAuthorizer));
        }
    }

    @Override
    public List<AccessConstraintDefinition> getAccessConstraints() {
        return accessConstraints;
    }

    private static Resource createResource() {
        Resource accessControlRoot =  Resource.Factory.create();
        accessControlRoot.registerChild(AccessConstraintResources.APPLICATION_PATH_ELEMENT, AccessConstraintResources.APPLICATION_RESOURCE);
        accessControlRoot.registerChild(AccessConstraintResources.SENSITIVITY_PATH_ELEMENT, AccessConstraintResources.SENSITIVITY_RESOURCE);
        accessControlRoot.registerChild(AccessConstraintResources.VAULT_PATH_ELEMENT, AccessConstraintResources.VAULT_RESOURCE);
        return accessControlRoot;
    }

}
