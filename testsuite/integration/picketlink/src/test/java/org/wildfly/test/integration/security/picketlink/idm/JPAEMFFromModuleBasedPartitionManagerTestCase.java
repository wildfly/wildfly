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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.wildfly.extension.picketlink.common.model.ModelElement.JPA_STORE;
import static org.wildfly.extension.picketlink.common.model.ModelElement.JPA_STORE_ENTITY_MODULE;
import static org.wildfly.extension.picketlink.common.model.ModelElement.JPA_STORE_ENTITY_MODULE_UNIT_NAME;

import java.io.File;
import java.io.IOException;

import javax.annotation.Resource;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.module.util.TestModule;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.picketlink.idm.PartitionManager;
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

/**
 * @author Pedro Igor
 */
@RunWith(Arquillian.class)
@ServerSetup(JPAEMFFromModuleBasedPartitionManagerTestCase.IdentityManagementServerSetupTask.class)
@Ignore
public class JPAEMFFromModuleBasedPartitionManagerTestCase extends AbstractBasicIdentityManagementTestCase {

    static final String PARTITION_MANAGER_JNDI_NAME = "picketlink/JPAEMFModuleBasedPartitionManager";

    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap
            .create(WebArchive.class, "test.war")
            .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
            .addAsManifestResource(new StringAsset("Dependencies: org.picketlink.idm.api meta-inf,org.jboss.dmr meta-inf,org.jboss.as.controller\n"), "MANIFEST.MF")
            .addClass(JPAEMFFromModuleBasedPartitionManagerTestCase.class)
            .addClass(TestModule.class)
            .addClass(AbstractBasicIdentityManagementTestCase.class)
            .addClass(AbstractIdentityManagementServerSetupTask.class);
    }

    @Resource(mappedName = PARTITION_MANAGER_JNDI_NAME)
    private PartitionManager partitionManager;

    @Override
    protected PartitionManager getPartitionManager() {
        return this.partitionManager;
    }

    static class IdentityManagementServerSetupTask extends AbstractIdentityManagementServerSetupTask {

        private TestModule module;

        IdentityManagementServerSetupTask() {
            super("jpa.emf.idm", PARTITION_MANAGER_JNDI_NAME);
        }

        private TestModule createModule() throws IOException {
            File moduleXml = new File(JPAEMFFromModuleBasedPartitionManagerTestCase.class
                .getResource(JPAEMFFromModuleBasedPartitionManagerTestCase.class
                    .getSimpleName() + "-module.xml").getFile());

            TestModule module = new TestModule("test.picketlink-emf-module-test", moduleXml);

            module.addResource("picketlink-emf-module-test.jar")
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
            ModelNode operationAddIdentityConfiguration = Util
                .createAddOperation(createIdentityConfigurationPathAddress("jpa.emf.store"));

            operationSteps.add(operationAddIdentityConfiguration);

            ModelNode operationAddIdentityStore = createIdentityStoreAddOperation(operationAddIdentityConfiguration);

            operationSteps.add(operationAddIdentityStore);

            ModelNode operationAddSupportedTypes = createSupportedAllTypesAddOperation(operationAddIdentityStore);

            operationSteps.add(operationAddSupportedTypes);
        }

        private ModelNode createIdentityStoreAddOperation(ModelNode identityConfigurationModelNode) {
            PathAddress pathAddress = PathAddress.pathAddress(identityConfigurationModelNode.get(OP_ADDR)).append(JPA_STORE
                .getName(), JPA_STORE.getName());
            ModelNode operationAddIdentityStore = Util.createAddOperation(pathAddress);

            operationAddIdentityStore.get(JPA_STORE_ENTITY_MODULE.getName()).set("test.picketlink-emf-module-test");
            operationAddIdentityStore.get(JPA_STORE_ENTITY_MODULE_UNIT_NAME.getName()).set("user-defined-pu");

            return operationAddIdentityStore;
        }
    }
}
