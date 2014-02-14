package org.jboss.as.test.integration.picketlink.idm;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.picketlink.idm.entities.AbstractCredentialTypeEntity;
import org.jboss.as.test.integration.picketlink.idm.entities.AccountEntity;
import org.jboss.as.test.integration.picketlink.idm.entities.AttributeTypeEntity;
import org.jboss.as.test.integration.picketlink.idm.entities.AttributedTypeEntity;
import org.jboss.as.test.integration.picketlink.idm.entities.GroupTypeEntity;
import org.jboss.as.test.integration.picketlink.idm.entities.IdentityTypeEntity;
import org.jboss.as.test.integration.picketlink.idm.entities.PartitionTypeEntity;
import org.jboss.as.test.integration.picketlink.idm.entities.PasswordCredentialTypeEntity;
import org.jboss.as.test.integration.picketlink.idm.entities.RelationshipIdentityTypeEntity;
import org.jboss.as.test.integration.picketlink.idm.entities.RelationshipIdentityTypeReferenceEntity;
import org.jboss.as.test.integration.picketlink.idm.entities.RelationshipTypeEntity;
import org.jboss.as.test.integration.picketlink.idm.entities.RoleTypeEntity;
import org.jboss.as.test.integration.picketlink.idm.util.AbstractIdentityManagementServerSetupTask;
import org.jboss.as.test.integration.picketlink.util.TestModule;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.runner.RunWith;
import org.picketlink.idm.PartitionManager;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.picketlink.subsystems.idm.model.ModelElement.JPA_STORE;
import static org.jboss.as.picketlink.subsystems.idm.model.ModelElement.JPA_STORE_ENTITY_MODULE;
import static org.jboss.as.picketlink.subsystems.idm.model.ModelElement.JPA_STORE_ENTITY_MODULE_UNIT_NAME;

/**
 * @author Pedro Igor
 */
@RunWith(Arquillian.class)
@ServerSetup(JPAEMFFromModuleBasedPartitionManagerTestCase.IdentityManagementServerSetupTask.class)
public class JPAEMFFromModuleBasedPartitionManagerTestCase extends AbstractBasicIdentityManagementTestCase {

    static final String PARTITION_MANAGER_JNDI_NAME = "picketlink/JPAEMFModuleBasedPartitionManager";

    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap
            .create(WebArchive.class, "test.war")
            .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
            .addAsManifestResource(new StringAsset("Dependencies: org.picketlink.idm.api meta-inf\n"), "MANIFEST.MF")
            .addClass(JPAEMFFromModuleBasedPartitionManagerTestCase.class)
            .addClass(AbstractIdentityManagementServerSetupTask.class)
            .addClass(AbstractBasicIdentityManagementTestCase.class);
    }

    @Resource(mappedName = PARTITION_MANAGER_JNDI_NAME)
    private PartitionManager partitionManager;

    @Override
    protected PartitionManager getPartitionManager() {
        return this.partitionManager;
    }

    static class IdentityManagementServerSetupTask extends AbstractIdentityManagementServerSetupTask {

        private TestModule module;

        public IdentityManagementServerSetupTask() {
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
