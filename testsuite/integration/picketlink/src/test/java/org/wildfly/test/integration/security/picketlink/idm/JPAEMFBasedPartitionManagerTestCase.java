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
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.picketlink.idm.PartitionManager;
import org.wildfly.test.integration.security.picketlink.idm.util.AbstractIdentityManagementServerSetupTask;

import javax.annotation.Resource;
import java.io.File;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.wildfly.extension.picketlink.common.model.ModelElement.JPA_STORE;
import static org.wildfly.extension.picketlink.common.model.ModelElement.JPA_STORE_ENTITY_MANAGER_FACTORY;

/**
 * @author Pedro Igor
 */
@RunWith(Arquillian.class)
@ServerSetup(JPAEMFBasedPartitionManagerTestCase.IdentityManagementServerSetupTask.class)
@Ignore
public class JPAEMFBasedPartitionManagerTestCase extends AbstractBasicIdentityManagementTestCase {

    static final String PARTITION_MANAGER_JNDI_NAME = "picketlink/JPAEMFBasedPartitionManager";

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap
            .create(JavaArchive.class, "test.jar")
            .addAsManifestResource(new StringAsset("Dependencies: org.picketlink.idm.api meta-inf, org.picketlink.idm.schema meta-inf,org.jboss.dmr meta-inf,org.jboss.as.controller\n"), "MANIFEST.MF")
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

        IdentityManagementServerSetupTask() {
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
