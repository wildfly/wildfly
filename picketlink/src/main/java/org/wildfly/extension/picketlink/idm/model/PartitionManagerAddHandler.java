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

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.services.path.PathManager;
import org.jboss.as.controller.services.path.PathManagerService;
import org.jboss.as.naming.ValueManagedReferenceFactory;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.txn.service.TxnServices;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.value.InjectedValue;
import org.picketlink.idm.PartitionManager;
import org.picketlink.idm.config.FileStoreConfigurationBuilder;
import org.picketlink.idm.config.IdentityConfigurationBuilder;
import org.picketlink.idm.config.IdentityStoreConfigurationBuilder;
import org.picketlink.idm.config.LDAPMappingConfigurationBuilder;
import org.picketlink.idm.config.LDAPStoreConfigurationBuilder;
import org.picketlink.idm.config.NamedIdentityConfigurationBuilder;
import org.picketlink.idm.credential.handler.CredentialHandler;
import org.picketlink.idm.model.AttributedType;
import org.picketlink.idm.model.Relationship;
import org.wildfly.extension.picketlink.common.model.ModelElement;
import org.wildfly.extension.picketlink.idm.config.JPAStoreSubsystemConfiguration;
import org.wildfly.extension.picketlink.idm.config.JPAStoreSubsystemConfigurationBuilder;
import org.wildfly.extension.picketlink.idm.service.FileIdentityStoreService;
import org.wildfly.extension.picketlink.idm.service.JPAIdentityStoreService;
import org.wildfly.extension.picketlink.idm.service.PartitionManagerService;

import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;
import java.util.List;

import static org.jboss.as.controller.PathAddress.EMPTY_ADDRESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.wildfly.extension.picketlink.common.model.ModelElement.FILE_STORE;
import static org.wildfly.extension.picketlink.common.model.ModelElement.IDENTITY_CONFIGURATION;
import static org.wildfly.extension.picketlink.common.model.ModelElement.IDENTITY_STORE_CREDENTIAL_HANDLER;
import static org.wildfly.extension.picketlink.common.model.ModelElement.JPA_STORE;
import static org.wildfly.extension.picketlink.common.model.ModelElement.LDAP_STORE;
import static org.wildfly.extension.picketlink.common.model.ModelElement.LDAP_STORE_ATTRIBUTE;
import static org.wildfly.extension.picketlink.common.model.ModelElement.LDAP_STORE_MAPPING;
import static org.wildfly.extension.picketlink.common.model.ModelElement.SUPPORTED_TYPE;
import static org.wildfly.extension.picketlink.common.model.ModelElement.SUPPORTED_TYPES;
import static org.wildfly.extension.picketlink.logging.PicketLinkLogger.ROOT_LOGGER;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 */
public class PartitionManagerAddHandler extends AbstractAddStepHandler {

    static final PartitionManagerAddHandler INSTANCE = new PartitionManagerAddHandler();

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        for (SimpleAttributeDefinition attribute : PartitionManagerResourceDefinition.INSTANCE.getAttributes()) {
            attribute.validateAndSet(operation, model);
        }
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        final String federationName = address.getLastElement().getValue();
        ModelNode partitionManager = Resource.Tools.readModel(context.readResource(EMPTY_ADDRESS));
        createPartitionManagerService(context, federationName, partitionManager, false);
    }

    public void validateModel(final OperationContext context, String partitionManagerName, final ModelNode partitionManager) throws OperationFailedException {
        createPartitionManagerService(context, partitionManagerName, partitionManager, true);
    }

    public void createPartitionManagerService(final OperationContext context, String partitionManagerName, final ModelNode partitionManager, boolean onlyValidate) throws OperationFailedException {
        String jndiName = PartitionManagerResourceDefinition.IDENTITY_MANAGEMENT_JNDI_URL
            .resolveModelAttribute(context, partitionManager).asString();
        IdentityConfigurationBuilder builder = new IdentityConfigurationBuilder();
        PartitionManagerService partitionManagerService = new PartitionManagerService(partitionManagerName, jndiName, builder);
        ServiceBuilder<PartitionManager> serviceBuilder = null;

        if (!onlyValidate) {
            serviceBuilder = context.getServiceTarget()
                .addService(PartitionManagerService.createServiceName(partitionManagerName), partitionManagerService);
        }

        ModelNode identityConfigurationNode = partitionManager.get(IDENTITY_CONFIGURATION.getName());

        if (!identityConfigurationNode.isDefined()) {
            throw ROOT_LOGGER.idmNoIdentityConfigurationProvided();
        }

        for (Property identityConfiguration : identityConfigurationNode.asPropertyList()) {
            String configurationName = identityConfiguration.getName();
            NamedIdentityConfigurationBuilder namedIdentityConfigurationBuilder = builder.named(configurationName);

            if (!identityConfiguration.getValue().isDefined()) {
                throw ROOT_LOGGER.idmNoIdentityStoreProvided(configurationName);
            }

            List<ModelNode> identityStores = identityConfiguration.getValue().asList();

            for (ModelNode store : identityStores) {
                configureIdentityStore(context, serviceBuilder, partitionManagerService, configurationName, namedIdentityConfigurationBuilder, store);
            }
        }

        if (!onlyValidate) {
            ServiceController<PartitionManager> controller = serviceBuilder
                .setInitialMode(Mode.PASSIVE)
                .install();
        }
    }

    private void configureIdentityStore(OperationContext context, ServiceBuilder<PartitionManager> serviceBuilder, PartitionManagerService partitionManagerService, String configurationName, NamedIdentityConfigurationBuilder namedIdentityConfigurationBuilder, ModelNode modelNode) throws OperationFailedException {
        Property prop = modelNode.asProperty();
        String storeType = prop.getName();
        ModelNode identityStore = prop.getValue().asProperty().getValue();
        IdentityStoreConfigurationBuilder<?, ?> storeConfig = null;

        if (storeType.equals(JPA_STORE.getName())) {
            storeConfig = configureJPAIdentityStore(context, serviceBuilder, partitionManagerService, identityStore, configurationName, namedIdentityConfigurationBuilder);
        } else if (storeType.equals(FILE_STORE.getName())) {
            storeConfig = configureFileIdentityStore(context, serviceBuilder, partitionManagerService, identityStore, configurationName, namedIdentityConfigurationBuilder);
        } else if (storeType.equals(LDAP_STORE.getName())) {
            storeConfig = configureLDAPIdentityStore(context, identityStore, namedIdentityConfigurationBuilder);
        }

        ModelNode supportAttributeNode = JPAStoreResourceDefinition.SUPPORT_ATTRIBUTE.resolveModelAttribute(context, identityStore);

        storeConfig.supportAttributes(supportAttributeNode.asBoolean());

        ModelNode supportCredentialNode = JPAStoreResourceDefinition.SUPPORT_CREDENTIAL
            .resolveModelAttribute(context, identityStore);

        storeConfig.supportCredentials(supportCredentialNode.asBoolean());

        configureSupportedTypes(context, identityStore, storeConfig);
        configureCredentialHandlers(context, identityStore, storeConfig);
    }

    private LDAPStoreConfigurationBuilder configureLDAPIdentityStore(OperationContext context, ModelNode ldapIdentityStore, NamedIdentityConfigurationBuilder builder) throws OperationFailedException {
        LDAPStoreConfigurationBuilder storeConfig = builder.stores().ldap();
        ModelNode url = LDAPStoreResourceDefinition.URL.resolveModelAttribute(context, ldapIdentityStore);
        ModelNode bindDn = LDAPStoreResourceDefinition.BIND_DN.resolveModelAttribute(context, ldapIdentityStore);
        ModelNode bindCredential = LDAPStoreResourceDefinition.BIND_CREDENTIAL.resolveModelAttribute(context, ldapIdentityStore);
        ModelNode baseDn = LDAPStoreResourceDefinition.BASE_DN_SUFFIX.resolveModelAttribute(context, ldapIdentityStore);
        ModelNode uniqueIdAttributeName = LDAPStoreResourceDefinition.UNIQUE_ID_ATTRIBUTE_NAME.resolveModelAttribute(context, ldapIdentityStore);
        ModelNode activeDirectory = LDAPStoreResourceDefinition.ACTIVE_DIRECTORY.resolveModelAttribute(context, ldapIdentityStore);

        if (url.isDefined()) {
            storeConfig.url(url.asString());
        }

        if (bindDn.isDefined()) {
            storeConfig.bindDN(bindDn.asString());
        }

        if (bindCredential.isDefined()) {
            storeConfig.bindCredential(bindCredential.asString());
        }

        if (baseDn.isDefined()) {
            storeConfig.baseDN(baseDn.asString());
        }

        if (uniqueIdAttributeName.isDefined()) {
            storeConfig.uniqueIdentifierAttributeName(uniqueIdAttributeName.asString());
        }

        storeConfig.activeDirectory(activeDirectory.asBoolean());

        if (ldapIdentityStore.hasDefined(LDAP_STORE_MAPPING.getName())) {
            for (Property mappingNode : ldapIdentityStore.get(LDAP_STORE_MAPPING.getName()).asPropertyList()) {
                ModelNode ldapMapping = mappingNode.getValue();
                ModelNode classNameNode = LDAPStoreMappingResourceDefinition.CLASS_NAME.resolveModelAttribute(context, ldapMapping);
                ModelNode codeNode = LDAPStoreMappingResourceDefinition.CODE.resolveModelAttribute(context, ldapMapping);
                ModelNode moduleNode = LDAPStoreMappingResourceDefinition.MODULE.resolveModelAttribute(context, ldapMapping);

                String typeName;

                if (classNameNode.isDefined()) {
                    typeName = classNameNode.asString();
                } else if (codeNode.isDefined()) {
                    typeName = AttributedTypeEnum.forType(codeNode.asString());
                } else {
                    throw ROOT_LOGGER.typeNotProvided(LDAP_STORE_MAPPING.getName());
                }

                LDAPMappingConfigurationBuilder storeMapping = storeConfig
                    .mapping(this.<AttributedType>loadClass(moduleNode, typeName));
                ModelNode relatesToNode = LDAPStoreMappingResourceDefinition.RELATES_TO.resolveModelAttribute(context, ldapMapping);

                if (relatesToNode.isDefined()) {
                    String relatesTo = AttributedTypeEnum.forType(relatesToNode.asString());

                    if (relatesTo == null) {
                        relatesTo = relatesToNode.asString();
                    }

                    storeMapping.forMapping(this.<AttributedType>loadClass(moduleNode, relatesTo));
                } else {
                    String baseDN = LDAPStoreMappingResourceDefinition.BASE_DN.resolveModelAttribute(context, ldapMapping)
                        .asString();

                    storeMapping.baseDN(baseDN);

                    String objectClasses = LDAPStoreMappingResourceDefinition.OBJECT_CLASSES
                        .resolveModelAttribute(context, ldapMapping).asString();

                    for (String objClass : objectClasses.split(",")) {
                        if (!objClass.trim().isEmpty()) {
                            storeMapping.objectClasses(objClass);
                        }
                    }

                    ModelNode parentAttributeName = LDAPStoreMappingResourceDefinition.PARENT_ATTRIBUTE
                        .resolveModelAttribute(context, ldapMapping);

                    if (parentAttributeName.isDefined()) {
                        storeMapping.parentMembershipAttributeName(parentAttributeName.asString());
                    }
                }

                if (ldapMapping.hasDefined(LDAP_STORE_ATTRIBUTE.getName())) {
                    for (Property attributeNode : ldapMapping.get(LDAP_STORE_ATTRIBUTE.getName()).asPropertyList()) {
                        ModelNode attribute = attributeNode.getValue();
                        String name = LDAPStoreAttributeResourceDefinition.NAME.resolveModelAttribute(context, attribute)
                            .asString();
                        String ldapName = LDAPStoreAttributeResourceDefinition.LDAP_NAME.resolveModelAttribute(context, attribute)
                            .asString();
                        boolean readOnly = LDAPStoreAttributeResourceDefinition.READ_ONLY.resolveModelAttribute(context, attribute)
                            .asBoolean();

                        if (readOnly) {
                            storeMapping.readOnlyAttribute(name, ldapName);
                        } else {
                            boolean isIdentifier = LDAPStoreAttributeResourceDefinition.IS_IDENTIFIER
                                .resolveModelAttribute(context, attribute).asBoolean();
                            storeMapping.attribute(name, ldapName, isIdentifier);
                        }
                    }
                }
            }
        } else {
            throw ROOT_LOGGER.idmLdapNoMappingDefined();
        }

        return storeConfig;
    }

    private IdentityStoreConfigurationBuilder<?, ?> configureFileIdentityStore(OperationContext context, ServiceBuilder<PartitionManager> serviceBuilder, PartitionManagerService partitionManagerService, ModelNode resource, String configurationName, final NamedIdentityConfigurationBuilder builder) throws OperationFailedException {
        FileStoreConfigurationBuilder fileStoreBuilder = builder.stores().file();
        String workingDir = FileStoreResourceDefinition.WORKING_DIR.resolveModelAttribute(context, resource).asString();
        String relativeTo = FileStoreResourceDefinition.RELATIVE_TO.resolveModelAttribute(context, resource).asString();
        ModelNode alwaysCreateFiles = FileStoreResourceDefinition.ALWAYS_CREATE_FILE.resolveModelAttribute(context, resource);
        ModelNode asyncWrite = FileStoreResourceDefinition.ASYNC_WRITE.resolveModelAttribute(context, resource);
        ModelNode asyncWriteThreadPool = FileStoreResourceDefinition.ASYNC_WRITE_THREAD_POOL
            .resolveModelAttribute(context, resource);

        fileStoreBuilder.preserveState(!alwaysCreateFiles.asBoolean());
        fileStoreBuilder.asyncWrite(asyncWrite.asBoolean());
        fileStoreBuilder.asyncWriteThreadPool(asyncWriteThreadPool.asInt());

        if (serviceBuilder != null) {
            FileIdentityStoreService storeService = new FileIdentityStoreService(fileStoreBuilder, workingDir, relativeTo);
            ServiceName storeServiceName = PartitionManagerService
                .createIdentityStoreServiceName(partitionManagerService.getName(), configurationName, ModelElement.FILE_STORE
                    .getName());
            ServiceBuilder<FileIdentityStoreService> storeServiceBuilder = context.getServiceTarget()
                .addService(storeServiceName, storeService);

            storeServiceBuilder.addDependency(PathManagerService.SERVICE_NAME, PathManager.class, storeService.getPathManager());

            serviceBuilder.addDependency(storeServiceName);

            ServiceController<FileIdentityStoreService> controller = storeServiceBuilder
                .setInitialMode(Mode.PASSIVE)
                .install();
        }

        return fileStoreBuilder;
    }

    private JPAStoreSubsystemConfigurationBuilder configureJPAIdentityStore(OperationContext context, ServiceBuilder<PartitionManager> serviceBuilder, PartitionManagerService partitionManagerService, final ModelNode identityStore, String configurationName, final NamedIdentityConfigurationBuilder builder) throws OperationFailedException {
        JPAStoreSubsystemConfigurationBuilder storeConfig = builder.stores()
            .add(JPAStoreSubsystemConfiguration.class, JPAStoreSubsystemConfigurationBuilder.class);

        ModelNode jpaDataSourceNode = JPAStoreResourceDefinition.DATA_SOURCE.resolveModelAttribute(context, identityStore);
        ModelNode jpaEntityModule = JPAStoreResourceDefinition.ENTITY_MODULE.resolveModelAttribute(context, identityStore);
        ModelNode jpaEntityModuleUnitName = JPAStoreResourceDefinition.ENTITY_MODULE_UNIT_NAME
            .resolveModelAttribute(context, identityStore);
        ModelNode jpaEntityManagerFactoryNode = JPAStoreResourceDefinition.ENTITY_MANAGER_FACTORY
            .resolveModelAttribute(context, identityStore);

        if (jpaEntityModule.isDefined()) {
            storeConfig.entityModule(jpaEntityModule.asString());
        }

        storeConfig.entityModuleUnitName(jpaEntityModuleUnitName.asString());

        if (serviceBuilder != null) {
            JPAIdentityStoreService storeService = new JPAIdentityStoreService(storeConfig);
            ServiceName storeServiceName = PartitionManagerService
                .createIdentityStoreServiceName(partitionManagerService.getName(), configurationName, ModelElement.JPA_STORE
                    .getName());
            ServiceBuilder<JPAIdentityStoreService> storeServiceBuilder = context.getServiceTarget()
                .addService(storeServiceName, storeService);

            storeServiceBuilder.addDependency(TxnServices.JBOSS_TXN_TRANSACTION_MANAGER, TransactionManager.class, storeService
                .getTransactionManager());

            storeServiceBuilder
                .addDependency(TxnServices.JBOSS_TXN_SYNCHRONIZATION_REGISTRY, TransactionSynchronizationRegistry.class, storeService
                    .getTransactionSynchronizationRegistry());

            if (jpaDataSourceNode.isDefined()) {
                storeConfig.dataSourceJndiUrl(toJndiName(jpaDataSourceNode.asString()));
                storeServiceBuilder
                    .addDependency(ContextNames.JAVA_CONTEXT_SERVICE_NAME
                        .append(toJndiName(jpaDataSourceNode.asString()).split("/")));
            }

            if (jpaEntityManagerFactoryNode.isDefined()) {
                storeConfig.entityManagerFactoryJndiName(jpaEntityManagerFactoryNode.asString());
                storeServiceBuilder
                    .addDependency(ContextNames.JAVA_CONTEXT_SERVICE_NAME.append(jpaEntityManagerFactoryNode.asString().split("/")),
                        ValueManagedReferenceFactory.class, new InjectedValue<ValueManagedReferenceFactory>());
            }

            serviceBuilder.addDependency(storeServiceName);

            ServiceController<JPAIdentityStoreService> controller = storeServiceBuilder
                .setInitialMode(Mode.PASSIVE)
                .install();
        }

        return storeConfig;
    }

    private void configureSupportedTypes(OperationContext context, ModelNode identityStore, IdentityStoreConfigurationBuilder storeConfig) throws OperationFailedException {
        boolean hasSupportedType = identityStore.hasDefined(SUPPORTED_TYPES.getName());

        if (hasSupportedType) {
            ModelNode featuresSetNode = identityStore.get(SUPPORTED_TYPES.getName()).asProperty().getValue();
            ModelNode supportsAllNode = SupportedTypesResourceDefinition.SUPPORTS_ALL
                .resolveModelAttribute(context, featuresSetNode);

            if (supportsAllNode.asBoolean()) {
                storeConfig.supportAllFeatures();
            }

            hasSupportedType = supportsAllNode.asBoolean();

            if (featuresSetNode.hasDefined(SUPPORTED_TYPE.getName())) {
                for (Property supportedTypeNode : featuresSetNode.get(SUPPORTED_TYPE.getName()).asPropertyList()) {
                    ModelNode supportedType = supportedTypeNode.getValue();
                    ModelNode classNameNode = SupportedTypeResourceDefinition.CLASS_NAME
                        .resolveModelAttribute(context, supportedType);
                    ModelNode codeNode = SupportedTypeResourceDefinition.CODE.resolveModelAttribute(context, supportedType);
                    String typeName;

                    if (classNameNode.isDefined()) {
                        typeName = classNameNode.asString();
                    } else if (codeNode.isDefined()) {
                        typeName = AttributedTypeEnum.forType(codeNode.asString());
                    } else {
                        throw ROOT_LOGGER.typeNotProvided(SUPPORTED_TYPE.getName());
                    }

                    ModelNode moduleNode = SupportedTypeResourceDefinition.MODULE.resolveModelAttribute(context, supportedType);
                    Class<? extends AttributedType> attributedTypeClass = loadClass(moduleNode, typeName);

                    if (Relationship.class.isAssignableFrom(attributedTypeClass)) {
                        storeConfig.supportGlobalRelationship((Class<? extends Relationship>) attributedTypeClass);
                    } else {
                        storeConfig.supportType(attributedTypeClass);
                    }

                    hasSupportedType = true;
                }
            }
        }

        if (!hasSupportedType) {
            throw ROOT_LOGGER.idmNoSupportedTypesDefined();
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void configureCredentialHandlers(OperationContext context, ModelNode identityStore, IdentityStoreConfigurationBuilder<?, ?> storeConfig) throws OperationFailedException {
        if (identityStore.hasDefined(IDENTITY_STORE_CREDENTIAL_HANDLER.getName())) {
            for (Property credentialHandler : identityStore.get(IDENTITY_STORE_CREDENTIAL_HANDLER.getName()).asPropertyList()) {
                ModelNode classNameNode = CredentialHandlerResourceDefinition.CLASS_NAME
                    .resolveModelAttribute(context, credentialHandler.getValue());
                ModelNode codeNode = CredentialHandlerResourceDefinition.CODE
                    .resolveModelAttribute(context, credentialHandler.getValue());
                ModelNode moduleNode = CredentialHandlerResourceDefinition.MODULE
                    .resolveModelAttribute(context, credentialHandler.getValue());
                String typeName;

                if (classNameNode.isDefined()) {
                    typeName = classNameNode.asString();
                } else if (codeNode.isDefined()) {
                    typeName = CredentialTypeEnum.forType(codeNode.asString());
                } else {
                    throw ROOT_LOGGER.typeNotProvided(IDENTITY_STORE_CREDENTIAL_HANDLER.getName());
                }

                storeConfig.addCredentialHandler(this.<CredentialHandler>loadClass(moduleNode, typeName));
            }
        }
    }

    private String toJndiName(String jndiName) {
        if (jndiName != null) {
            if (jndiName.startsWith("java:")) {
                return jndiName.substring(jndiName.indexOf(":") + 1);
            }
        }

        return jndiName;
    }

    private Module getModule(ModelNode moduleNode) {
        Module module;

        if (moduleNode.isDefined()) {
            ModuleLoader moduleLoader = Module.getBootModuleLoader();
            try {
                module = moduleLoader.loadModule(ModuleIdentifier.fromString(moduleNode.asString()));
            } catch (ModuleLoadException e) {
                throw ROOT_LOGGER.moduleCouldNotLoad(moduleNode.asString(), e);
            }
        } else {
            // fallback to caller module.
            module = Module.getCallerModule();
        }

        return module;
    }

    @SuppressWarnings("unchecked")
    private <T> Class<T> loadClass(ModelNode moduleNode, String typeName) {
        try {
            Module module = getModule(moduleNode);

            if (module != null) {
                return (Class<T>) module.getClassLoader().loadClass(typeName);
            } else {
                return (Class<T>) getClass().getClassLoader().loadClass(typeName);
            }
        } catch (ClassNotFoundException cnfe) {
            throw ROOT_LOGGER.couldNotLoadClass(typeName, cnfe);
        }
    }
}
