/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.mod_cluster;

import java.util.HashSet;
import java.util.Set;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.model.test.FailedOperationTransformationConfig;
import org.jboss.as.model.test.ModelTestControllerVersion;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.as.subsystem.test.AbstractSubsystemTest;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Radoslav Husar
 */
public class ModClusterTransformersTestCase extends AbstractSubsystemTest {

    public ModClusterTransformersTestCase() {
        super(ModClusterExtension.SUBSYSTEM_NAME, new ModClusterExtension());
    }

    private static String formatEAP6SubsystemArtifact(ModelTestControllerVersion version) {
        return formatArtifact("org.jboss.as:jboss-as-modcluster:%s", version);
    }

    private static String formatEAP7SubsystemArtifact(ModelTestControllerVersion version) {
        return formatArtifact("org.jboss.eap:wildfly-mod_cluster-extension:%s", version);
    }

    private static String formatArtifact(String pattern, ModelTestControllerVersion version) {
        return String.format(pattern, version.getMavenGavVersion());
    }

    private static ModClusterModel getModelVersion(ModelTestControllerVersion controllerVersion) {
        switch (controllerVersion) {
            case EAP_6_4_0:
            case EAP_6_4_7:
                return ModClusterModel.VERSION_1_5_0;
            case EAP_7_0_0:
                return ModClusterModel.VERSION_4_0_0;
            case EAP_7_1_0:
                return ModClusterModel.VERSION_5_0_0;
            case EAP_7_2_0:
                return ModClusterModel.VERSION_6_0_0;
            case EAP_7_3_0:
            case EAP_7_4_0:
                return ModClusterModel.VERSION_7_0_0;
        }
        throw new IllegalArgumentException();
    }

    private static String[] getDependencies(ModelTestControllerVersion version) {
        switch (version) {
            case EAP_6_4_0:
            case EAP_6_4_7:
                return new String[] {formatEAP6SubsystemArtifact(version), "org.jboss.mod_cluster:mod_cluster-core:1.2.11.Final-redhat-1"};
            case EAP_7_0_0:
                return new String[] {formatEAP7SubsystemArtifact(version), "org.jboss.mod_cluster:mod_cluster-core:1.3.2.Final-redhat-1"};
            case EAP_7_1_0:
                return new String[] {
                        formatEAP7SubsystemArtifact(version),
                        "org.jboss.mod_cluster:mod_cluster-core:1.3.7.Final-redhat-1",
                        formatArtifact("org.jboss.eap:wildfly-clustering-common:%s", version),
                };
            case EAP_7_2_0:
                return new String[] {
                        formatArtifact("org.jboss.eap:wildfly-mod_cluster-extension:%s", version),
                        "org.jboss.mod_cluster:mod_cluster-core:1.4.0.Final-redhat-1",
                        formatArtifact("org.jboss.eap:wildfly-clustering-common:%s", version),
                };
            case EAP_7_3_0:
                return new String[] {
                        formatArtifact("org.jboss.eap:wildfly-mod_cluster-extension:%s", version),
                        "org.jboss.mod_cluster:mod_cluster-core:1.4.1.Final-redhat-00001",
                        formatArtifact("org.jboss.eap:wildfly-clustering-common:%s", version),
                };
            case EAP_7_4_0:
                return new String[] {
                        formatArtifact("org.jboss.eap:wildfly-mod_cluster-extension:%s", version),
                        "org.jboss.mod_cluster:mod_cluster-core:1.4.3.Final-redhat-00002",
                        formatArtifact("org.jboss.eap:wildfly-clustering-common:%s", version),
                };
        }
        throw new IllegalArgumentException();
    }

    @Test
    public void testTransformerEAP_6_4_0() throws Exception {
        this.testTransformation(ModelTestControllerVersion.EAP_6_4_0);
    }

    @Test
    public void testTransformerEAP_7_0_0() throws Exception {
        this.testTransformation(ModelTestControllerVersion.EAP_7_0_0);
    }

    @Test
    public void testTransformerEAP_7_1_0() throws Exception {
        this.testTransformation(ModelTestControllerVersion.EAP_7_1_0);
    }

    @Test
    public void testTransformerEAP_7_2_0() throws Exception {
        this.testTransformation(ModelTestControllerVersion.EAP_7_2_0);
    }

    @Test
    public void testTransformerEAP_7_3_0() throws Exception {
        this.testTransformation(ModelTestControllerVersion.EAP_7_3_0);
    }

    @Test
    public void testTransformerEAP_7_4_0() throws Exception {
        this.testTransformation(ModelTestControllerVersion.EAP_7_4_0);
    }

    private void testTransformation(ModelTestControllerVersion controllerVersion) throws Exception {
        ModClusterModel model = getModelVersion(controllerVersion);
        ModelVersion modelVersion = model.getVersion();
        String[] dependencies = getDependencies(controllerVersion);


        Set<String> resources = new HashSet<>();
        resources.add(String.format("subsystem-transform-%d_%d_%d.xml", modelVersion.getMajor(), modelVersion.getMinor(), modelVersion.getMicro()));
        if (modelVersion.getMajor() < 6) {
            // Also test simple-load-provider for legacy slaves which only allow for one mod_cluster proxy configuration
            // which we can now test within scope of multiple proxy configurations
            resources.add("subsystem-transform-simple.xml");
        }

        for (String resource : resources) {
            String subsystemXml = readResource(resource);
            String extensionClassName = (model.getVersion().getMajor() == 1) ? "org.jboss.as.modcluster.ModClusterExtension" : "org.wildfly.extension.mod_cluster.ModClusterExtension";

            KernelServicesBuilder builder = createKernelServicesBuilder(new ModClusterAdditionalInitialization())
                    .setSubsystemXml(subsystemXml);
            builder.createLegacyKernelServicesBuilder(null, controllerVersion, modelVersion)
                    .addMavenResourceURL(dependencies)
                    .setExtensionClassName(extensionClassName)
                    .skipReverseControllerCheck()
                    .dontPersistXml();

            KernelServices mainServices = builder.build();
            KernelServices legacyServices = mainServices.getLegacyServices(modelVersion);

            Assert.assertNotNull(legacyServices);
            Assert.assertTrue(mainServices.isSuccessfulBoot());
            Assert.assertTrue(legacyServices.isSuccessfulBoot());

            checkSubsystemModelTransformation(mainServices, modelVersion, null, false);
        }
    }

    @Test
    public void testRejectionsEAP_6_4_0() throws Exception {
        this.testRejections(ModelTestControllerVersion.EAP_6_4_0);
    }

    @Test
    public void testRejectionsEAP_7_0_0() throws Exception {
        this.testRejections(ModelTestControllerVersion.EAP_7_0_0);
    }

    @Test
    public void testRejectionsEAP_7_1_0() throws Exception {
        this.testRejections(ModelTestControllerVersion.EAP_7_1_0);
    }

    @Test
    public void testRejectionsEAP_7_2_0() throws Exception {
        this.testRejections(ModelTestControllerVersion.EAP_7_2_0);
    }

    @Test
    public void testRejectionsEAP_7_3_0() throws Exception {
        this.testRejections(ModelTestControllerVersion.EAP_7_3_0);
    }

    @Test
    public void testRejectionsEAP_7_4_0() throws Exception {
        this.testRejections(ModelTestControllerVersion.EAP_7_4_0);
    }

    private void testRejections(ModelTestControllerVersion controllerVersion) throws Exception {
        String[] dependencies = getDependencies(controllerVersion);
        String subsystemXml = readResource("subsystem-reject.xml");
        ModClusterModel model = getModelVersion(controllerVersion);
        ModelVersion modelVersion = model.getVersion();
        String extensionClassName = (model.getVersion().getMajor() == 1) ? "org.jboss.as.modcluster.ModClusterExtension" : "org.wildfly.extension.mod_cluster.ModClusterExtension";

        KernelServicesBuilder builder = createKernelServicesBuilder(new ModClusterAdditionalInitialization());
        builder.createLegacyKernelServicesBuilder(model.getVersion().getMajor() >= 4 ? new ModClusterAdditionalInitialization() : null, controllerVersion, modelVersion)
                .addSingleChildFirstClass(ModClusterAdditionalInitialization.class)
                .addMavenResourceURL(dependencies)
                .setExtensionClassName(extensionClassName)
                .skipReverseControllerCheck();

        KernelServices mainServices = builder.build();
        KernelServices legacyServices = mainServices.getLegacyServices(modelVersion);

        Assert.assertNotNull(legacyServices);
        Assert.assertTrue(mainServices.isSuccessfulBoot());
        Assert.assertTrue(legacyServices.isSuccessfulBoot());

        ModelTestUtils.checkFailedTransformedBootOperations(mainServices, modelVersion, parse(subsystemXml), createFailedOperationConfig(modelVersion));
    }

    private static FailedOperationTransformationConfig createFailedOperationConfig(ModelVersion version) {
        FailedOperationTransformationConfig config = new FailedOperationTransformationConfig();

        PathAddress subsystemAddress = PathAddress.pathAddress(ModClusterSubsystemResourceDefinition.PATH);
        PathAddress configurationAddress = subsystemAddress.append(ProxyConfigurationResourceDefinition.pathElement("default"));
        PathAddress dynamicLoadProviderAddress = configurationAddress.append(DynamicLoadProviderResourceDefinition.PATH);

        if (ModClusterModel.VERSION_7_0_0.requiresTransformation(version)) {
            config.addFailedAttribute(dynamicLoadProviderAddress, FailedOperationTransformationConfig.ChainedConfig.createBuilder(DynamicLoadProviderResourceDefinition.Attribute.INITIAL_LOAD.getName())
                    .addConfig(new InitialLoadFailedAttributeConfig())
                    .build());
        }

        if (ModClusterModel.VERSION_6_0_0.requiresTransformation(version)) {
//            config.addFailedAttribute(subsystemAddress.append(ProxyConfigurationResourceDefinition.pathElement("other")), FailedOperationTransformationConfig.REJECTED_RESOURCE);

            config.addFailedAttribute(dynamicLoadProviderAddress.append(CustomLoadMetricResourceDefinition.pathElement("SomeFakeLoadMetricClass1")),
                    FailedOperationTransformationConfig.ChainedConfig.createBuilder(CustomLoadMetricResourceDefinition.Attribute.MODULE.getName())
                            .addConfig(new ModuleAttributeTransformationConfig())
                            .build());
        }

        if (ModClusterModel.VERSION_3_0_0.requiresTransformation(version)) {
            config.addFailedAttribute(configurationAddress, FailedOperationTransformationConfig.ChainedConfig.createBuilder(ProxyConfigurationResourceDefinition.Attribute.STATUS_INTERVAL.getName(), ProxyConfigurationResourceDefinition.Attribute.PROXIES.getName())
                    .addConfig(new StatusIntervalConfig())
                    .addConfig(new ProxiesConfig())
                    .build());
        }

        return config;
    }

    static class ProxiesConfig extends FailedOperationTransformationConfig.AttributesPathAddressConfig<ProxiesConfig> {
        ProxiesConfig() {
            super(ProxyConfigurationResourceDefinition.Attribute.PROXIES.getName());
        }

        @Override
        protected boolean isAttributeWritable(String attributeName) {
            return true;
        }

        @Override
        protected boolean checkValue(String attrName, ModelNode attribute, boolean isWriteAttribute) {
            return !attribute.equals(new ModelNode());
        }

        @Override
        protected ModelNode correctValue(ModelNode toResolve, boolean isWriteAttribute) {
            return new ModelNode();
        }
    }

    static class StatusIntervalConfig extends FailedOperationTransformationConfig.AttributesPathAddressConfig<StatusIntervalConfig> {
        StatusIntervalConfig() {
            super(ProxyConfigurationResourceDefinition.Attribute.STATUS_INTERVAL.getName());
        }

        @Override
        protected boolean isAttributeWritable(String attributeName) {
            return true;
        }

        @Override
        protected boolean checkValue(String attrName, ModelNode attribute, boolean isWriteAttribute) {
            return !attribute.equals(new ModelNode(10));
        }

        @Override
        protected ModelNode correctValue(ModelNode toResolve, boolean isWriteAttribute) {
            return new ModelNode(10);
        }
    }

    static class ModuleAttributeTransformationConfig extends FailedOperationTransformationConfig.AttributesPathAddressConfig<ModuleAttributeTransformationConfig> {
        ModuleAttributeTransformationConfig() {
            super(CustomLoadMetricResourceDefinition.Attribute.MODULE.getName());
        }

        @Override
        protected boolean isAttributeWritable(String attributeName) {
            return true;
        }

        @Override
        protected boolean checkValue(String attrName, ModelNode attribute, boolean isWriteAttribute) {
            return !attribute.equals(CustomLoadMetricResourceDefinition.Attribute.MODULE.getDefinition().getDefaultValue());
        }

        @Override
        protected ModelNode correctValue(ModelNode toResolve, boolean isWriteAttribute) {
            return CustomLoadMetricResourceDefinition.Attribute.MODULE.getDefinition().getDefaultValue();
        }
    }

    static class InitialLoadFailedAttributeConfig extends FailedOperationTransformationConfig.AttributesPathAddressConfig<InitialLoadFailedAttributeConfig> {
        InitialLoadFailedAttributeConfig() {
            super(DynamicLoadProviderResourceDefinition.Attribute.INITIAL_LOAD.getName());
        }

        @Override
        protected boolean isAttributeWritable(String attributeName) {
            return true;
        }

        protected boolean checkValue(String attrName, ModelNode attribute, boolean isGeneratedWriteAttribute) {
            return !attribute.equals(new ModelNode(-1));
        }

        @Override
        protected ModelNode correctValue(ModelNode toResolve, boolean isGeneratedWriteAttribute) {
            return new ModelNode(-1);
        }
    }
}
