package org.jboss.as.test.integration.picketlink.idm;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.picketlink.idm.util.AbstractIdentityManagementServerSetupTask;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.runner.RunWith;
import org.picketlink.idm.PartitionManager;

import javax.annotation.Resource;
import java.io.File;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.picketlink.subsystems.idm.model.ModelElement.JPA_STORE;
import static org.jboss.as.picketlink.subsystems.idm.model.ModelElement.JPA_STORE_ENTITY_MANAGER_FACTORY;

/**
 * @author Pedro Igor
 */
@RunWith(Arquillian.class)
@ServerSetup(JPAEMFBasedPartitionManagerTestCase.IdentityManagementServerSetupTask.class)
public class JPAEMFBasedPartitionManagerTestCase extends AbstractBasicIdentityManagementTestCase {

    static final String PARTITION_MANAGER_JNDI_NAME = "picketlink/JPAEMFBasedPartitionManager";

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap
            .create(JavaArchive.class, "test.jar")
            .addAsManifestResource(new StringAsset("Dependencies: org.picketlink.idm.api meta-inf, org.picketlink.idm.schema meta-inf\n"), "MANIFEST.MF")
            .addClass(JPAEMFBasedPartitionManagerTestCase.class)
            .addClass(AbstractIdentityManagementServerSetupTask.class)
            .addClass(AbstractBasicIdentityManagementTestCase.class)
            .addAsManifestResource(new File(JPAEMFBasedPartitionManagerTestCase.class.getResource("simple-schema-persistence.xml").getFile()), "persistence.xml");
    }

    @Resource(mappedName = PARTITION_MANAGER_JNDI_NAME)
    private PartitionManager partitionManager;

    @Override
    protected PartitionManager getPartitionManager() {
        return this.partitionManager;
    }

    static class IdentityManagementServerSetupTask extends AbstractIdentityManagementServerSetupTask {

        public IdentityManagementServerSetupTask() {
            super("jpa.emf.idm", PARTITION_MANAGER_JNDI_NAME);
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
            PathAddress pathAddress = PathAddress.pathAddress(identityConfigurationModelNode.get(OP_ADDR))
                .append(JPA_STORE.getName(), JPA_STORE.getName());
            ModelNode operationAddIdentityStore = Util.createAddOperation(pathAddress);

            operationAddIdentityStore.get(JPA_STORE_ENTITY_MANAGER_FACTORY.getName()).set("jboss/TestingIDMEMF");

            return operationAddIdentityStore;
        }
    }
}
