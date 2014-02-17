package org.jboss.as.test.integration.picketlink.idm;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.picketlink.idm.util.AbstractIdentityManagementServerSetupTask;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.runner.RunWith;
import org.picketlink.idm.PartitionManager;

import javax.annotation.Resource;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.picketlink.subsystems.idm.model.ModelElement.JPA_STORE;
import static org.jboss.as.picketlink.subsystems.idm.model.ModelElement.JPA_STORE_DATASOURCE;

/**
 * @author Pedro Igor
 */
@RunWith(Arquillian.class)
@ServerSetup(JPADSBasedPartitionManagerTestCase.IdentityManagementServerSetupTask.class)
public class JPADSBasedPartitionManagerTestCase extends AbstractBasicIdentityManagementTestCase {

    static final String PARTITION_MANAGER_JNDI_NAME = "picketlink/JPADSBasedPartitionManager";

    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap
                   .create(WebArchive.class, "test.war")
                   .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                   .addAsManifestResource(new StringAsset("Dependencies: org.picketlink.idm.api meta-inf\n"), "MANIFEST.MF")
                   .addClass(JPADSBasedPartitionManagerTestCase.class)
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

        public IdentityManagementServerSetupTask() {
            this("jpa.ds.idm", PARTITION_MANAGER_JNDI_NAME);
        }

        public IdentityManagementServerSetupTask(String name, String partitionManagerJndiName) {
            super(name, partitionManagerJndiName);
        }

        @Override
        protected void doCreateIdentityManagement(ModelNode identityManagementAddOperation, ModelNode operationSteps) {
            ModelNode operationAddIdentityConfiguration = Util.createAddOperation(createIdentityConfigurationPathAddress("jpa.ds.store"));

            operationSteps.add(operationAddIdentityConfiguration);

            ModelNode operationAddIdentityStore = createIdentityStoreAddOperation(operationAddIdentityConfiguration);

            operationSteps.add(operationAddIdentityStore);

            ModelNode operationAddSupportedTypes = createSupportedAllTypesAddOperation(operationAddIdentityStore);

            operationSteps.add(operationAddSupportedTypes);
        }

        private ModelNode createIdentityStoreAddOperation(ModelNode identityConfigurationModelNode) {
            PathAddress pathAddress = PathAddress.pathAddress(identityConfigurationModelNode.get(OP_ADDR)).append(JPA_STORE.getName(), JPA_STORE.getName());
            ModelNode operationAddIdentityStore = Util.createAddOperation(pathAddress);

            operationAddIdentityStore.get(JPA_STORE_DATASOURCE.getName()).set("jboss/datasources/ExampleDS");

            return operationAddIdentityStore;
        }
    }
}
