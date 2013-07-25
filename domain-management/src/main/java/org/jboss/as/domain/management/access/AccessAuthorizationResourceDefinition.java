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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLE;

import java.util.Locale;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.AttributeMarshaller;
import org.jboss.as.controller.ListAttributeDefinition;
import org.jboss.as.controller.ModelOnlyWriteAttributeHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleListAttributeDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.access.DelegatingConfigurableAuthorizer;
import org.jboss.as.controller.access.rbac.ConfigurableRoleMapper;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.parsing.Attribute;
import org.jboss.as.controller.parsing.Element;
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
        SIMPLE,
        RBAC
    }

    public static final ListAttributeDefinition HOST_SCOPED_ROLES = SimpleListAttributeDefinition.Builder.of(ModelDescriptionConstants.HOST_SCOPED_ROLES,
            new SimpleAttributeDefinitionBuilder(ROLE, ModelType.STRING)
                    .setAttributeMarshaller(new AttributeMarshaller() {
                        @Override
                        public void marshallAsElement(AttributeDefinition attribute, ModelNode resourceModel, boolean marshallDefault, XMLStreamWriter writer) throws XMLStreamException {
                            writer.writeEmptyElement(Element.ROLE.getLocalName());
                            writer.writeAttribute(Attribute.NAME.getLocalName(), resourceModel.asString());
                        }
                    }).build())
            .setMinSize(1)
            .setAllowNull(true)
            .setWrapXmlList(false)
            .build();

    public static final SimpleAttributeDefinition PROVIDER = new SimpleAttributeDefinitionBuilder("provider", ModelType.STRING, true)
            .setDefaultValue(new ModelNode(Provider.SIMPLE.name().toLowerCase(Locale.ENGLISH)))
            .setValidator(new EnumValidator<Provider>(Provider.class, true, false))
            .build();

    public static final SimpleAttributeDefinition USE_REALM_ROLES = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.USE_REALM_ROLES, ModelType.BOOLEAN, false)
            .setDefaultValue(new ModelNode(false))
            .setAllowExpression(true).build();

    private final DelegatingConfigurableAuthorizer configurableAuthorizer;

    /*
     * Currently this needs to exist even if it is not currently being used so that even if the current provider is 'simple'
     * updates to the ConfigurableRoleMapper will be in place should the provider be switched to 'rbac'.
     */
    private final ConfigurableRoleMapper roleMapper = new ConfigurableRoleMapper();

    private final boolean isDomain;
    private final boolean isHostController;

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

    private AccessAuthorizationResourceDefinition(DelegatingConfigurableAuthorizer configurableAuthorizer, boolean domain, boolean hostController) {
        super(PATH_ELEMENT, DomainManagementResolver.getResolver("core.access-control"));
        this.configurableAuthorizer = configurableAuthorizer;
        isDomain = domain;
        isHostController = hostController;
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);
        if (isHostController) {
            resourceRegistration.registerReadWriteAttribute(HOST_SCOPED_ROLES, null, new ReloadRequiredWriteAttributeHandler(HOST_SCOPED_ROLES) {
                @Override
                protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode resolvedValue, ModelNode currentValue, HandbackHolder<Void> voidHandback) throws OperationFailedException {
                    return !context.isBooting();
                }
            });
        } else {
            resourceRegistration.registerReadWriteAttribute(USE_REALM_ROLES, null, new AccessAuthorizationUseRealmRolesWriteAttributeHandler(roleMapper));
        }

        if (!isDomain) {
            resourceRegistration.registerReadWriteAttribute(PROVIDER, null, new AccessAuthorizationProviderWriteAttributeHander(configurableAuthorizer, roleMapper));
        } else {
            // TODO handle managed domain
            resourceRegistration.registerReadWriteAttribute(PROVIDER, null, new ModelOnlyWriteAttributeHandler(PROVIDER));
        }
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        if (!isHostController) {
            // Role Mapping
            resourceRegistration.registerSubModel(RoleMappingResourceDefinition.create(roleMapper));
        }

        // Scoped roles
        if (isDomain) {
            resourceRegistration.registerSubModel(new ServerGroupScopedRoleResourceDefinition(configurableAuthorizer));
            if (!isHostController) {
                resourceRegistration.registerSubModel(new HostScopedRolesResourceDefinition(configurableAuthorizer));
            }
        }

        // Constraints
        if (!isHostController) {
            //  -- Application Type
            resourceRegistration.registerSubModel(ApplicationTypeParentResourceDefinition.INSTANCE);
            //  -- Sensitivity Classification
            resourceRegistration.registerSubModel(SensitivityClassificationParentResourceDefinition.INSTANCE);
            //  -- Vault Expression
            resourceRegistration.registerSubModel(SensitivityResourceDefinition.createVaultExpressionConfiguration());
        }
    }

    private static Resource createResource() {
        Resource accessControlRoot =  Resource.Factory.create();
        accessControlRoot.registerChild(AccessConstraintResources.APPLICATION_PATH_ELEMENT, AccessConstraintResources.APPLICATION_RESOURCE);
        accessControlRoot.registerChild(AccessConstraintResources.SENSITIVITY_PATH_ELEMENT, AccessConstraintResources.SENSITIVITY_RESOURCE);
        accessControlRoot.registerChild(AccessConstraintResources.VAULT_PATH_ELEMENT, AccessConstraintResources.VAULT_RESOURCE);
        return accessControlRoot;
    }

}
