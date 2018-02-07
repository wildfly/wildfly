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
import static org.wildfly.extension.picketlink.common.model.ModelElement.IDENTITY_STORE_CREDENTIAL_HANDLER;
import static org.wildfly.extension.picketlink.common.model.ModelElement.LDAP_STORE;
import static org.wildfly.extension.picketlink.common.model.ModelElement.LDAP_STORE_MAPPING;
import static org.wildfly.extension.picketlink.common.model.ModelElement.SUPPORTED_TYPE;
import static org.wildfly.extension.picketlink.common.model.ModelElement.SUPPORTED_TYPES;
import static org.wildfly.extension.picketlink.logging.PicketLinkLogger.ROOT_LOGGER;

import java.util.List;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 */
class PartitionManagerAddHandler extends AbstractAddStepHandler {

    static final PartitionManagerAddHandler INSTANCE = new PartitionManagerAddHandler();

    private PartitionManagerAddHandler() {
        super(PartitionManagerResourceDefinition.IDENTITY_MANAGEMENT_JNDI_URL);
    }

    public void validatePartitionManager(final OperationContext context, final ModelNode partitionManager) throws OperationFailedException {

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
                validateIdentityStore(context, store);
            }
        }

    }

    private void validateIdentityStore(OperationContext context, ModelNode modelNode) throws OperationFailedException {
        Property prop = modelNode.asProperty();
        String storeType = prop.getName();
        ModelNode identityStore = prop.getValue().asProperty().getValue();


        if (storeType.equals(LDAP_STORE.getName())) {
            validateLDAPIdentityStore(identityStore);
        }

        validateSupportedTypes(context, identityStore);
        validateCredentialHandlers(context, identityStore);
    }

    private void validateLDAPIdentityStore(ModelNode ldapIdentityStore) throws OperationFailedException {

        if (!ldapIdentityStore.hasDefined(LDAP_STORE_MAPPING.getName())) {
            throw ROOT_LOGGER.idmLdapNoMappingDefined();
        }
    }

    private void validateSupportedTypes(OperationContext context, ModelNode identityStore) throws OperationFailedException {
        boolean hasSupportedType = identityStore.hasDefined(SUPPORTED_TYPES.getName());

        if (hasSupportedType) {
            ModelNode featuresSetNode = identityStore.get(SUPPORTED_TYPES.getName()).asProperty().getValue();
            ModelNode supportsAllNode = SupportedTypesResourceDefinition.SUPPORTS_ALL
                    .resolveModelAttribute(context, featuresSetNode);


            hasSupportedType = supportsAllNode.asBoolean();

            if (featuresSetNode.hasDefined(SUPPORTED_TYPE.getName())) {
                for (Property supportedTypeNode : featuresSetNode.get(SUPPORTED_TYPE.getName()).asPropertyList()) {
                    ModelNode supportedType = supportedTypeNode.getValue();
                    ModelNode classNameNode = SupportedTypeResourceDefinition.CLASS_NAME
                            .resolveModelAttribute(context, supportedType);
                    ModelNode codeNode = SupportedTypeResourceDefinition.CODE.resolveModelAttribute(context, supportedType);

                    if (!classNameNode.isDefined() && !codeNode.isDefined()) {
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


    private void validateCredentialHandlers(OperationContext context, ModelNode identityStore) throws OperationFailedException {
        if (identityStore.hasDefined(IDENTITY_STORE_CREDENTIAL_HANDLER.getName())) {
            for (Property credentialHandler : identityStore.get(IDENTITY_STORE_CREDENTIAL_HANDLER.getName()).asPropertyList()) {
                ModelNode classNameNode = CredentialHandlerResourceDefinition.CLASS_NAME.resolveModelAttribute(context, credentialHandler.getValue());
                ModelNode codeNode = CredentialHandlerResourceDefinition.CODE.resolveModelAttribute(context, credentialHandler.getValue());
                if (!classNameNode.isDefined() && !codeNode.isDefined()) {
                    throw ROOT_LOGGER.typeNotProvided(IDENTITY_STORE_CREDENTIAL_HANDLER.getName());
                }
            }
        }
    }


}
