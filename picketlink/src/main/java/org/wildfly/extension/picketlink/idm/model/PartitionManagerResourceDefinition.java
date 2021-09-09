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

package org.wildfly.extension.picketlink.idm.model;

import static org.wildfly.extension.picketlink.common.model.ModelElement.IDENTITY_CONFIGURATION;
import static org.wildfly.extension.picketlink.common.model.ModelElement.LDAP_STORE;
import static org.wildfly.extension.picketlink.common.model.ModelElement.LDAP_STORE_MAPPING;
import static org.wildfly.extension.picketlink.common.model.ModelElement.SUPPORTED_TYPE;
import static org.wildfly.extension.picketlink.common.model.ModelElement.SUPPORTED_TYPES;
import static org.wildfly.extension.picketlink.logging.PicketLinkLogger.ROOT_LOGGER;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.access.constraint.SensitivityClassification;
import org.jboss.as.controller.access.management.AccessConstraintDefinition;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;
import org.wildfly.extension.picketlink.common.model.ModelElement;
import org.wildfly.extension.picketlink.idm.IDMExtension;

import java.util.List;
import java.util.function.Function;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 * @since Mar 16, 2012
 */
public class PartitionManagerResourceDefinition extends AbstractIDMResourceDefinition {

    private static final List<AccessConstraintDefinition> CONSTRAINTS = new SensitiveTargetAccessConstraintDefinition(
        new SensitivityClassification(IDMExtension.SUBSYSTEM_NAME, "partition-manager", false, true, true)
    ).wrapAsList();

    public static final SimpleAttributeDefinition IDENTITY_MANAGEMENT_JNDI_URL = new SimpleAttributeDefinitionBuilder(ModelElement.IDENTITY_MANAGEMENT_JNDI_NAME.getName(), ModelType.STRING, false).setAllowExpression(true).build();
    public static final PartitionManagerResourceDefinition INSTANCE = new PartitionManagerResourceDefinition();

    private PartitionManagerResourceDefinition() {
        super(ModelElement.PARTITION_MANAGER, Function.identity(),
                 IDENTITY_MANAGEMENT_JNDI_URL);
        setDeprecated(IDMExtension.DEPRECATED_SINCE);
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        addChildResourceDefinition(IdentityConfigurationResourceDefinition.INSTANCE, resourceRegistration);
    }

    @Override
    public List<AccessConstraintDefinition> getAccessConstraints() {
        return CONSTRAINTS;
    }

    //  This method encapsulates the aspects of the old PartitionManagerAddHandler's validateModel method that
    //  are relevant to Stage.MODEL execution, dropping the parts that involved Stage.RUNTIME service creation
    public static void validateModel(final OperationContext context, final PathAddress partitionAddress) throws OperationFailedException {

        ModelNode partitionManager = getPartitionManagerModel(context, partitionAddress);

        if (partitionManager == null) {
            // removed; nothing to validate
            return;
        }

        ModelNode identityConfigurationNode = partitionManager.get(IDENTITY_CONFIGURATION.getName());

        if (!identityConfigurationNode.isDefined()) {
            throw ROOT_LOGGER.idmNoIdentityConfigurationProvided();
        }

        for (Property identityConfiguration : identityConfigurationNode.asPropertyList()) {
            String configurationName = identityConfiguration.getName();

            if (!identityConfiguration.getValue().isDefined()) {
                throw ROOT_LOGGER.idmNoIdentityStoreProvided(configurationName);
            }

            List<ModelNode> identityStores = identityConfiguration.getValue().asList();

            for (ModelNode store : identityStores) {
                configureIdentityStore(context, store);
            }
        }
    }

    private static ModelNode getPartitionManagerModel(final OperationContext context, final PathAddress partitionAddress) {
        try {
            Resource resource = context.readResourceFromRoot(partitionAddress);
            return Resource.Tools.readModel(resource);
        } catch (Resource.NoSuchResourceException e) {
            return null;
        }
    }

    private static void configureIdentityStore(OperationContext context, ModelNode modelNode) throws OperationFailedException {
        Property prop = modelNode.asProperty();
        String storeType = prop.getName();
        ModelNode identityStore = prop.getValue().asProperty().getValue();

        if (storeType.equals(LDAP_STORE.getName())) {
            if (!identityStore.hasDefined(LDAP_STORE_MAPPING.getName())) {
                throw ROOT_LOGGER.idmLdapNoMappingDefined();
            }
        }

        validateSupportedTypes(context, identityStore);
    }

    private static void validateSupportedTypes(OperationContext context, ModelNode identityStore) throws OperationFailedException {
        boolean hasSupportedType = identityStore.hasDefined(SUPPORTED_TYPES.getName());

        if (hasSupportedType) {
            ModelNode featuresSetNode = identityStore.get(SUPPORTED_TYPES.getName()).asProperty().getValue();
            try {
                ModelNode supportsAllNode = SupportedTypesResourceDefinition.SUPPORTS_ALL
                        .resolveModelAttribute(context, featuresSetNode);

                hasSupportedType = supportsAllNode.asBoolean();
            } catch (OperationFailedException ofe) {
                if (featuresSetNode.get(SupportedTypesResourceDefinition.SUPPORTS_ALL.getName()).getType() == ModelType.EXPRESSION) {
                    // We just tried to resolve an expression is Stage.MODEL. That's not reliable.
                    // Just assume it would resolve to true and don't fail validation.
                    // If it's invalid it will be caught on a server
                    hasSupportedType = true;
                } else {
                    throw ofe;
                }
            }

            if (featuresSetNode.hasDefined(SUPPORTED_TYPE.getName())) {
                for (Property supportedTypeNode : featuresSetNode.get(SUPPORTED_TYPE.getName()).asPropertyList()) {
                    ModelNode supportedType = supportedTypeNode.getValue();

                    if (!supportedType.hasDefined(SupportedTypeResourceDefinition.CLASS_NAME.getName())
                            && !supportedType.hasDefined(SupportedTypeResourceDefinition.CODE.getName())) {
                        throw ROOT_LOGGER.typeNotProvided(SUPPORTED_TYPE.getName());
                    }

                    hasSupportedType = true;
                }
            }
        }

        if (!hasSupportedType) {
            throw ROOT_LOGGER.idmNoSupportedTypesDefined();
        }
    }
}
