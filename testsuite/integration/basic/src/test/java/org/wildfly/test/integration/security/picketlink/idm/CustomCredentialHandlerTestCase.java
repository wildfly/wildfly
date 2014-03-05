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

package org.wildfly.test.integration.security.picketlink.idm;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.picketlink.idm.IdentityManager;
import org.picketlink.idm.PartitionManager;
import org.picketlink.idm.credential.Credentials;
import org.wildfly.test.integration.security.picketlink.idm.credentials.CustomCredential;
import org.wildfly.test.integration.security.picketlink.idm.credentials.CustomCredentialHandler;
import org.wildfly.test.integration.security.picketlink.idm.entities.AbstractCredentialTypeEntity;
import org.wildfly.test.integration.security.picketlink.idm.entities.AccountEntity;
import org.wildfly.test.integration.security.picketlink.idm.entities.AttributeTypeEntity;
import org.wildfly.test.integration.security.picketlink.idm.entities.AttributedTypeEntity;
import org.wildfly.test.integration.security.picketlink.idm.entities.GroupTypeEntity;
import org.wildfly.test.integration.security.picketlink.idm.entities.IdentityTypeEntity;
import org.wildfly.test.integration.security.picketlink.idm.entities.PartitionTypeEntity;
import org.wildfly.test.integration.security.picketlink.idm.entities.PasswordCredentialTypeEntity;
import org.wildfly.test.integration.security.picketlink.idm.entities.RelationshipIdentityTypeEntity;
import org.wildfly.test.integration.security.picketlink.idm.entities.RelationshipIdentityTypeReferenceEntity;
import org.wildfly.test.integration.security.picketlink.idm.entities.RelationshipTypeEntity;
import org.wildfly.test.integration.security.picketlink.idm.entities.RoleTypeEntity;
import org.wildfly.test.integration.security.picketlink.idm.util.AbstractIdentityManagementServerSetupTask;
import org.wildfly.test.integration.security.picketlink.util.TestModule;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.junit.Assert.assertEquals;
import static org.wildfly.extension.picketlink.idm.model.ModelElement.COMMON_CLASS_NAME;
import static org.wildfly.extension.picketlink.idm.model.ModelElement.COMMON_MODULE;
import static org.wildfly.extension.picketlink.idm.model.ModelElement.IDENTITY_STORE_CREDENTIAL_HANDLER;
import static org.wildfly.extension.picketlink.idm.model.ModelElement.JPA_STORE;
import static org.wildfly.extension.picketlink.idm.model.ModelElement.JPA_STORE_DATASOURCE;

/**
 * @author Pedro Igor
 */
@RunWith(Arquillian.class)
@ServerSetup(CustomCredentialHandlerTestCase.IdentityManagementServerSetupTask.class)
public class CustomCredentialHandlerTestCase extends AbstractBasicIdentityManagementTestCase {

    static final String PARTITION_MANAGER_JNDI_NAME = "picketlink/CustomCredentialHandlerPartitionManager";

    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap
                   .create(WebArchive.class, "test.war")
                   .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                   .addAsManifestResource(new StringAsset("Dependencies: org.picketlink.idm.api meta-inf, test.picketlink-emf-module-test meta-inf\n"), "MANIFEST.MF")
                   .addClass(CustomCredentialHandlerTestCase.class)
                   .addClass(AbstractIdentityManagementServerSetupTask.class)
                   .addClass(AbstractBasicIdentityManagementTestCase.class);
    }

    @Resource(mappedName = PARTITION_MANAGER_JNDI_NAME)
    private PartitionManager partitionManager;

    @Override
    protected PartitionManager getPartitionManager() {
        return this.partitionManager;
    }

    @Test
    public void testCustomCredential() {
        PartitionManager partitionManager = getPartitionManager();

        IdentityManager identityManager = partitionManager.createIdentityManager();
        CustomCredential credentials = new CustomCredential("valid_token");

        identityManager.validateCredentials(credentials);

        assertEquals(Credentials.Status.VALID, credentials.getStatus());

        credentials = new CustomCredential("invalid_token");

        identityManager.validateCredentials(credentials);

        assertEquals(Credentials.Status.INVALID, credentials.getStatus());
    }

    static class IdentityManagementServerSetupTask extends AbstractIdentityManagementServerSetupTask {

        private TestModule module;

        public IdentityManagementServerSetupTask() {
            super("jpa.ds.idm", PARTITION_MANAGER_JNDI_NAME);
        }

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            this.module = createModule();
            super.setup(managementClient, containerId);
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            super.tearDown(managementClient, containerId);
            this.module.remove();
        }

        @Override
        protected void doCreateIdentityManagement(ModelNode identityManagementAddOperation, ModelNode operationSteps) {
            ModelNode operationAddIdentityConfiguration = Util.createAddOperation(createIdentityConfigurationPathAddress("jpa.ds.store"));

            operationSteps.add(operationAddIdentityConfiguration);

            ModelNode operationAddIdentityStore = createIdentityStoreAddOperation(operationAddIdentityConfiguration);

            operationSteps.add(operationAddIdentityStore);

            ModelNode operationAddSupportedTypes = createSupportedAllTypesAddOperation(operationAddIdentityStore);

            operationSteps.add(operationAddSupportedTypes);

            ModelNode credentialHandlers = createCredentialHandlersAddOperation(operationAddIdentityStore);

            operationSteps.add(credentialHandlers);
        }

        private ModelNode createIdentityStoreAddOperation(ModelNode identityConfigurationModelNode) {
            PathAddress pathAddress = PathAddress.pathAddress(identityConfigurationModelNode.get(OP_ADDR)).append(JPA_STORE.getName(), JPA_STORE.getName());
            ModelNode operationAddIdentityStore = Util.createAddOperation(pathAddress);

            operationAddIdentityStore.get(JPA_STORE_DATASOURCE.getName()).set("jboss/datasources/ExampleDS");

            return operationAddIdentityStore;
        }

        protected ModelNode createCredentialHandlersAddOperation(ModelNode identityStoreModelNode) {
            ModelNode operationAddSupportedTypes = Util.createAddOperation(PathAddress.pathAddress(identityStoreModelNode.get(OP_ADDR)).append(IDENTITY_STORE_CREDENTIAL_HANDLER.getName(), CustomCredentialHandler.class.getName()));

            operationAddSupportedTypes.get(COMMON_CLASS_NAME.getName()).set(CustomCredentialHandler.class.getName());
            operationAddSupportedTypes.get(COMMON_MODULE.getName()).set(this.module.getName());

            return operationAddSupportedTypes;
        }


        private TestModule createModule() throws IOException {
            File moduleXml = new File(JPAEMFFromModuleBasedPartitionManagerTestCase.class
                .getResource(JPAEMFFromModuleBasedPartitionManagerTestCase.class
                    .getSimpleName() + "-module.xml").getFile());

            TestModule module = new TestModule("test.picketlink-emf-module-test", moduleXml);

            module.addResource("picketlink-emf-module-test.jar")
                .addClass(CustomCredential.class)
                .addClass(CustomCredentialHandler.class)
                .addClass(AbstractCredentialTypeEntity.class)
                .addClass(AttributedTypeEntity.class)
                .addClass(AttributeTypeEntity.class)
                .addClass(GroupTypeEntity.class)
                .addClass(IdentityTypeEntity.class)
                .addClass(PartitionTypeEntity.class)
                .addClass(PasswordCredentialTypeEntity.class)
                .addClass(RelationshipIdentityTypeEntity.class)
                .addClass(RelationshipIdentityTypeReferenceEntity.class)
                .addClass(RelationshipTypeEntity.class)
                .addClass(RoleTypeEntity.class)
                .addClass(AccountEntity.class)
                .addAsManifestResource(new File(JPAEMFFromModuleBasedPartitionManagerTestCase.class
                    .getResource(JPAEMFFromModuleBasedPartitionManagerTestCase.class.getSimpleName() + "-persistence.xml")
                    .getFile()), "persistence.xml");

            module.create();

            return module;
        }
    }
}
