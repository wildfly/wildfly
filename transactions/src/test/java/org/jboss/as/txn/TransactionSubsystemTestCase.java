/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.txn;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.txn.subsystem.TransactionSubsystemRootResourceDefinition.BINDING;
import static org.jboss.as.txn.subsystem.TransactionSubsystemRootResourceDefinition.DEFAULT_TIMEOUT;
import static org.jboss.as.txn.subsystem.TransactionSubsystemRootResourceDefinition.ENABLE_STATISTICS;
import static org.jboss.as.txn.subsystem.TransactionSubsystemRootResourceDefinition.ENABLE_TSM_STATUS;
import static org.jboss.as.txn.subsystem.TransactionSubsystemRootResourceDefinition.NODE_IDENTIFIER;
import static org.jboss.as.txn.subsystem.TransactionSubsystemRootResourceDefinition.OBJECT_STORE_PATH;
import static org.jboss.as.txn.subsystem.TransactionSubsystemRootResourceDefinition.OBJECT_STORE_RELATIVE_TO;
import static org.jboss.as.txn.subsystem.TransactionSubsystemRootResourceDefinition.PATH;
import static org.jboss.as.txn.subsystem.TransactionSubsystemRootResourceDefinition.PROCESS_ID_SOCKET_BINDING;
import static org.jboss.as.txn.subsystem.TransactionSubsystemRootResourceDefinition.PROCESS_ID_SOCKET_MAX_PORTS;
import static org.jboss.as.txn.subsystem.TransactionSubsystemRootResourceDefinition.RECOVERY_LISTENER;
import static org.jboss.as.txn.subsystem.TransactionSubsystemRootResourceDefinition.RELATIVE_TO;
import static org.jboss.as.txn.subsystem.TransactionSubsystemRootResourceDefinition.STATUS_BINDING;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.model.test.ChildFirstClassLoaderBuilder;
import org.jboss.as.model.test.FailedOperationTransformationConfig;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.as.txn.subsystem.TransactionExtension;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class TransactionSubsystemTestCase extends AbstractSubsystemBaseTest {

    private static boolean canTest713 = false;

    @BeforeClass
    public static void checkVersionAvailability() {
        // See if we can load the 7.1.3 GAV
        try {
            new ChildFirstClassLoaderBuilder().addMavenResourceURL("org.jboss.as:jboss-as-transactions:7.1.3.Final");
            canTest713 = true;
        } catch (MalformedURLException oops) {
            throw new RuntimeException(oops);
        } catch (RuntimeException e) {
            canTest713 = false;
        }
    }

    public TransactionSubsystemTestCase() {
        super(TransactionExtension.SUBSYSTEM_NAME, new TransactionExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("subsystem.xml");
    }

    @Override
    protected void compareXml(String configId, String original, String marshalled) throws Exception {
        super.compareXml(configId, original, marshalled, true);
    }

    @Test
    public void testExpressions() throws Exception {
        standardSubsystemTest("full-expressions.xml");
    }

    @Test
    public void testMinimalConfig() throws Exception {
        standardSubsystemTest("minimal.xml");
    }

    @Test
    public void testJdbcStore() throws Exception {
        standardSubsystemTest("jdbc-store.xml");
    }

    @Test
    public void testJdbcStoreMinimal() throws Exception {
        standardSubsystemTest("jdbc-store-minimal.xml");
    }

    @Test
    public void testJdbcStoreExpressions() throws Exception {
        standardSubsystemTest("jdbc-store-expressions.xml");
    }

    @Test
    public void testParser_1_2() throws Exception {
        standardSubsystemTest("full-1.2.xml");
    }

    @Test
    public void testTransformers110() throws Exception {
        String subsystemXml = readResource("subsystem.xml");
        ModelVersion modelVersion = ModelVersion.create(1, 1, 0);
        //Use the non-runtime version of the extension which will happen on the HC
        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                .setSubsystemXml(subsystemXml);

        // Add legacy subsystems
        builder.createLegacyKernelServicesBuilder(null, modelVersion)
            .addMavenResourceURL("org.jboss.as:jboss-as-transactions:7.1.2.Final");

        KernelServices mainServices = builder.build();
        KernelServices legacyServices = mainServices.getLegacyServices(modelVersion);
        Assert.assertNotNull(legacyServices);

        checkSubsystemModelTransformation(mainServices, modelVersion);
    }

    @Test
    @Ignore("Need a 7.1.3.dmr")
    public void testTransformers111() throws Exception {
        if (!canTest713)
            return;
        String subsystemXml = readResource("subsystem.xml");
        ModelVersion modelVersion = ModelVersion.create(1, 1, 1);
        //Use the non-runtime version of the extension which will happen on the HC
        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                .setSubsystemXml(subsystemXml);

        // Add legacy subsystems
        builder.createLegacyKernelServicesBuilder(null, modelVersion)
                .addMavenResourceURL("org.jboss.as:jboss-as-transactions:7.1.3.Final");

        KernelServices mainServices = builder.build();
        KernelServices legacyServices = mainServices.getLegacyServices(modelVersion);
        Assert.assertNotNull(legacyServices);

        checkSubsystemModelTransformation(mainServices, modelVersion);
    }


    @Test
    public void testTransformersFull110() throws Exception {
        String subsystemXml = readResource("full.xml");
        ModelVersion modelVersion = ModelVersion.create(1, 1, 0);
        //Use the non-runtime version of the extension which will happen on the HC
        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                .setSubsystemXml(subsystemXml);

        // Add legacy subsystems
        builder.createLegacyKernelServicesBuilder(null, modelVersion)
            .addMavenResourceURL("org.jboss.as:jboss-as-transactions:7.1.2.Final");

        KernelServices mainServices = builder.build();
        KernelServices legacyServices = mainServices.getLegacyServices(modelVersion);
        Assert.assertNotNull(legacyServices);

        checkSubsystemModelTransformation(mainServices, modelVersion);

    }


    @Test
    @Ignore("Need a 7.1.3.dmr")
    public void testTransformersFull111() throws Exception {
        if (!canTest713)
            return;

        String subsystemXml = readResource("full-expressions.xml");
        ModelVersion modelVersion = ModelVersion.create(1, 1, 1);
        //Use the non-runtime version of the extension which will happen on the HC
        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                .setSubsystemXml(subsystemXml);

        // Add legacy subsystems
        builder.createLegacyKernelServicesBuilder(null, modelVersion)
                .addMavenResourceURL("org.jboss.as:jboss-as-transactions:7.1.3.Final");

        KernelServices mainServices = builder.build();
        KernelServices legacyServices = mainServices.getLegacyServices(modelVersion);
        Assert.assertNotNull(legacyServices);

        checkSubsystemModelTransformation(mainServices, modelVersion);

    }

    @Test
    public void testRejectTransformers110() throws Exception {
        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization());

        // Add legacy subsystems
        ModelVersion version_1_1 = ModelVersion.create(1, 1, 0);
        builder.createLegacyKernelServicesBuilder(createAdditionalInitialization(), version_1_1)
            .addMavenResourceURL("org.jboss.as:jboss-as-transactions:7.1.2.Final");

        KernelServices mainServices = builder.build();
        assertTrue(mainServices.isSuccessfulBoot());
        KernelServices legacyServices = mainServices.getLegacyServices(version_1_1);
        assertNotNull(legacyServices);
        assertTrue(legacyServices.isSuccessfulBoot());

        List<ModelNode> ops = builder.parseXmlResource("full-expressions.xml");
        ModelTestUtils.checkFailedTransformedBootOperations(mainServices, version_1_1, ops, new FailedOperationTransformationConfig()
            .addFailedAttribute(PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, TransactionExtension.SUBSYSTEM_NAME)),
                    new FailedOperationTransformationConfig.RejectExpressionsConfig(
                            DEFAULT_TIMEOUT,
                            ENABLE_STATISTICS,
                            ENABLE_TSM_STATUS,
                            BINDING,
                            STATUS_BINDING,
                            RECOVERY_LISTENER,
                            NODE_IDENTIFIER,
                            PATH,
                            RELATIVE_TO,
                            PROCESS_ID_SOCKET_BINDING,
                            PROCESS_ID_SOCKET_MAX_PORTS,
                            OBJECT_STORE_PATH,
                            OBJECT_STORE_RELATIVE_TO
                            )));
    }

    @Test
    public void testRejectTransformers111() throws Exception {
        if (!canTest713)
            return;

        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization());

        // Add legacy subsystems
        ModelVersion version_1_1_1 = ModelVersion.create(1, 1, 1);
        builder.createLegacyKernelServicesBuilder(createAdditionalInitialization(), version_1_1_1)
                .addMavenResourceURL("org.jboss.as:jboss-as-transactions:7.1.3.Final");

        KernelServices mainServices = builder.build();
        assertTrue(mainServices.isSuccessfulBoot());
        KernelServices legacyServices = mainServices.getLegacyServices(version_1_1_1);
        assertNotNull(legacyServices);
        assertTrue(legacyServices.isSuccessfulBoot());

        List<ModelNode> ops = builder.parseXmlResource("full-expressions.xml");
        ModelTestUtils.checkFailedTransformedBootOperations(mainServices, version_1_1_1, ops, new FailedOperationTransformationConfig());
    }

}
