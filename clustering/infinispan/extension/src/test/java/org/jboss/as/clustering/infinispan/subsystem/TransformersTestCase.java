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
package org.jboss.as.clustering.infinispan.subsystem;

import java.io.IOException;
import java.util.List;

import org.jboss.as.clustering.jgroups.subsystem.JGroupsSubsystemInitialization;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.model.test.FailedOperationTransformationConfig;
import org.jboss.as.model.test.ModelFixer;
import org.jboss.as.model.test.ModelTestControllerVersion;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Test cases for transformers used in the Infinispan subsystem
 *
 * Here we perform two types of tests:
 * - testing transformation between the current model and legacy models, in the case
 * where no rejections are expected, but certain discards/conversions/renames are expected
 * - testing transformation between the current model and legacy models, in the case
 * where specific rejections are expected
 *
 * @author <a href="tomaz.cerar@redhat.com">Tomaz Cerar</a>
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */

public class TransformersTestCase extends OperationTestCaseBase {

    private static String formatSubsystemArtifact(ModelTestControllerVersion version) {
        return formatArtifact("org.wildfly:wildfly-clustering-infinispan:%s", version);
    }

    private static String formatLegacySubsystemArtifact(ModelTestControllerVersion version) {
        return formatArtifact("org.jboss.as:jboss-as-clustering-infinispan:%s", version);
    }

    private static String formatArtifact(String pattern, ModelTestControllerVersion version) {
        return String.format(pattern, version.getMavenGavVersion());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("infinispan-transformer.xml");
    }

    @Override
    AdditionalInitialization createAdditionalInitialization() {
        return new JGroupsSubsystemInitialization();
    }

    @Test
    public void testTransformer800() throws Exception {
        ModelTestControllerVersion version = ModelTestControllerVersion.WILDFLY_8_0_0_FINAL;
        this.testTransformation(InfinispanModel.VERSION_2_0_0, version, formatSubsystemArtifact(version),
                formatArtifact("org.wildfly:wildfly-clustering-common:%s", version),
                "org.infinispan:infinispan-core:6.0.1.Final",
                "org.infinispan:infinispan-commons:6.0.1.Final",
                "org.infinispan:infinispan-cachestore-jdbc:6.0.1.Final",
                formatArtifact("org.wildfly:wildfly-clustering-jgroups:%s", version),
                "org.jgroups:jgroups:3.4.2.Final"
        );
    }

    @Test
    public void testTransformer810() throws Exception {
        ModelTestControllerVersion version = ModelTestControllerVersion.WILDFLY_8_1_0_FINAL;
        this.testTransformation(InfinispanModel.VERSION_2_0_0, version, formatSubsystemArtifact(version),
                formatArtifact("org.wildfly:wildfly-clustering-common:%s", version),
                "org.infinispan:infinispan-core:6.0.2.Final",
                "org.infinispan:infinispan-commons:6.0.2.Final",
                "org.infinispan:infinispan-cachestore-jdbc:6.0.2.Final",
                formatArtifact("org.wildfly:wildfly-clustering-jgroups:%s", version),
                "org.jgroups:jgroups:3.4.3.Final"
        );
    }

    @Test
    @Ignore(value = "WFLY-4515")
    public void testTransformer620() throws Exception {
        ModelTestControllerVersion version = ModelTestControllerVersion.EAP_6_2_0;
        this.testTransformation(InfinispanModel.VERSION_1_4_1, version, formatLegacySubsystemArtifact(version),
                "org.infinispan:infinispan-core:5.2.7.Final-redhat-2",
                "org.infinispan:infinispan-cachestore-jdbc:5.2.7.Final-redhat-2"
        );
    }

    @Test
    @Ignore(value = "WFLY-4515")
    public void testTransformer630() throws Exception {
        ModelTestControllerVersion version = ModelTestControllerVersion.EAP_6_3_0;
        this.testTransformation(InfinispanModel.VERSION_1_5_0, version, formatLegacySubsystemArtifact(version),
                "org.infinispan:infinispan-core:5.2.10.Final-redhat-1",
                "org.infinispan:infinispan-cachestore-jdbc:5.2.10.Final-redhat-1"
        );
    }

    private KernelServices buildKernelServices(ModelTestControllerVersion controllerVersion, ModelVersion version, String... mavenResourceURLs) throws Exception {
        return this.buildKernelServices(this.getSubsystemXml(), controllerVersion, version, mavenResourceURLs);
    }

    private KernelServices buildKernelServices(String xml, ModelTestControllerVersion controllerVersion, ModelVersion version, String... mavenResourceURLs) throws Exception {
        KernelServicesBuilder builder = this.createKernelServicesBuilder().setSubsystemXml(xml);

        builder.createLegacyKernelServicesBuilder(AdditionalInitialization.MANAGEMENT, controllerVersion, version)
                .addMavenResourceURL(mavenResourceURLs)
                .skipReverseControllerCheck()
                .dontPersistXml();

        KernelServices services = builder.build();
        Assert.assertTrue(ModelTestControllerVersion.MASTER + " boot failed", services.isSuccessfulBoot());
        Assert.assertTrue(controllerVersion.getMavenGavVersion() + " boot failed", services.getLegacyServices(version).isSuccessfulBoot());
        return services;
    }

    private void testTransformation(InfinispanModel model, ModelTestControllerVersion controller, String... dependencies) throws Exception {
        ModelVersion version = model.getVersion();
        KernelServices services = this.buildKernelServices(controller, version, dependencies);

        // check that both versions of the legacy model are the same and valid
        checkSubsystemModelTransformation(services, version, createModelFixer(model), false);

        ModelNode transformed = services.readTransformedModel(version);

        if (InfinispanModel.VERSION_3_0_0.requiresTransformation(version)) {
            // Verify that mode=BATCH is translated to mode=NONE, batching=true
            ModelNode cache = transformed.get(InfinispanSubsystemResourceDefinition.PATH.getKeyValuePair()).get(CacheContainerResourceDefinition.pathElement("maximal").getKeyValuePair()).get(CacheType.LOCAL.pathElement("local").getKeyValuePair());
            Assert.assertTrue(cache.hasDefined(CacheResourceDefinition.BATCHING.getName()));
            Assert.assertTrue(cache.get(CacheResourceDefinition.BATCHING.getName()).asBoolean());
            ModelNode transaction = cache.get(TransactionResourceDefinition.PATH.getKeyValuePair());
            if (transaction.hasDefined(TransactionResourceDefinition.MODE.getName())) {
                Assert.assertEquals(TransactionMode.NONE.name(), transaction.get(TransactionResourceDefinition.MODE.getName()).asString());
            }
        }
    }

    /*
     * Returns a copy of the model generated by booting legacy controller with legacy operations, but
     * with the following changes:
     * - virtual nodes attribute is removed
     *
     * This is required to address a problem with resource transformers: WFLY-2589
     */
    private static ModelFixer createModelFixer(InfinispanModel version) {
        switch (version) {
            default: {
                return null;
            }
        }
    }

    @Test
    public void testRejections800() throws Exception {
        ModelTestControllerVersion version = ModelTestControllerVersion.WILDFLY_8_0_0_FINAL;
        this.testRejections(InfinispanModel.VERSION_2_0_0, version, formatSubsystemArtifact(version),
                "org.infinispan:infinispan-core:6.0.1.Final",
                "org.infinispan:infinispan-cachestore-jdbc:6.0.1.Final"
        );
    }

    @Test
    public void testRejections810() throws Exception {
        ModelTestControllerVersion version = ModelTestControllerVersion.WILDFLY_8_1_0_FINAL;
        this.testRejections(InfinispanModel.VERSION_2_0_0, version, formatSubsystemArtifact(version),
                "org.infinispan:infinispan-core:6.0.2.Final",
                "org.infinispan:infinispan-cachestore-jdbc:6.0.2.Final"
        );
    }

    @Test
    @Ignore(value = "WFLY-4515")
    public void testRejections620() throws Exception {
        ModelTestControllerVersion version = ModelTestControllerVersion.EAP_6_2_0;
        this.testRejections(InfinispanModel.VERSION_1_4_1, version, formatLegacySubsystemArtifact(version),
                "org.infinispan:infinispan-core:5.2.7.Final-redhat-2",
                "org.infinispan:infinispan-cachestore-jdbc:5.2.7.Final-redhat-2"
        );
    }

    private void testRejections(InfinispanModel model, ModelTestControllerVersion controller, String ... dependencies) throws Exception {
        ModelVersion version = model.getVersion();

        // create builder for current subsystem version
        KernelServicesBuilder builder = this.createKernelServicesBuilder();

        // initialize the legacy services
        builder.createLegacyKernelServicesBuilder(AdditionalInitialization.MANAGEMENT, controller, version)
                .addMavenResourceURL(dependencies)
                //TODO storing the model triggers the weirdness mentioned in SubsystemTestDelegate.LegacyKernelServiceInitializerImpl.install()
                //which is strange since it should be loading it all from the current jboss modules
                //Also this works in several other tests
                .dontPersistXml();

        KernelServices services = builder.build();
        KernelServices legacyServices = services.getLegacyServices(version);
        Assert.assertNotNull(legacyServices);
        Assert.assertTrue("main services did not boot", services.isSuccessfulBoot());
        Assert.assertTrue(legacyServices.isSuccessfulBoot());

        // test failed operations involving backups
        List<ModelNode> xmlOps = builder.parseXmlResource("infinispan-transformer-backup.xml");
        ModelTestUtils.checkFailedTransformedBootOperations(services, version, xmlOps, createFailedOperationConfig(version));
    }

    private static FailedOperationTransformationConfig createFailedOperationConfig(ModelVersion version) {

        FailedOperationTransformationConfig config = new FailedOperationTransformationConfig();
        PathAddress subsystemAddress = PathAddress.pathAddress(InfinispanSubsystemResourceDefinition.PATH);
        PathAddress containerAddress = subsystemAddress.append(CacheContainerResourceDefinition.WILDCARD_PATH);

        if (InfinispanModel.VERSION_2_0_0.requiresTransformation(version)) {
            for (CacheType type: CacheType.values()) {
                if (type.hasSharedState()) {
                    PathAddress cacheAddress = containerAddress.append(type.pathElement());
                    config.addFailedAttribute(cacheAddress.append(BackupSiteResourceDefinition.WILDCARD_PATH), FailedOperationTransformationConfig.REJECTED_RESOURCE);
                    config.addFailedAttribute(cacheAddress.append(BackupForResourceDefinition.PATH), FailedOperationTransformationConfig.REJECTED_RESOURCE);
                }
            }
        }

        return config;
    }
}
