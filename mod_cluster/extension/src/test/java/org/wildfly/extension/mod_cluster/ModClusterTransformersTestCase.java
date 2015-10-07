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

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.model.test.FailedOperationTransformationConfig;
import org.jboss.as.model.test.ModelTestControllerVersion;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.as.subsystem.test.AbstractSubsystemTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.ControllerInitializer;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Radoslav Husar
 * @version Oct 2015
 */
public class ModClusterTransformersTestCase extends AbstractSubsystemTest {

    public ModClusterTransformersTestCase() {
        super(ModClusterExtension.SUBSYSTEM_NAME, new ModClusterExtension());
    }

    private static String formatSubsystemArtifact(ModelTestControllerVersion version) {
        return formatArtifact("org.wildfly:wildfly-mod_cluster-extension:%s", version);
    }

    private static String formatLegacySubsystemArtifact(ModelTestControllerVersion version) {
        return formatArtifact("org.jboss.as:jboss-as-modcluster:%s", version);
    }

    private static String formatArtifact(String pattern, ModelTestControllerVersion version) {
        return String.format(pattern, version.getMavenGavVersion());
    }

    @Test
    public void testTransformerEAP_6_2_0() throws Exception {
        ModelTestControllerVersion version = ModelTestControllerVersion.EAP_6_2_0;
        this.testTransformation(ModClusterModel.VERSION_1_4_0, version,
                formatLegacySubsystemArtifact(version),
                "org.jboss.mod_cluster:mod_cluster-core:1.2.6.Final-redhat-1"
        );
    }

    @Test
    public void testTransformerEAP_6_3_0() throws Exception {
        ModelTestControllerVersion version = ModelTestControllerVersion.EAP_6_3_0;
        this.testTransformation(ModClusterModel.VERSION_1_5_0, version,
                formatLegacySubsystemArtifact(version),
                "org.jboss.mod_cluster:mod_cluster-core:1.2.9.Final-redhat-1"
        );
    }

    @Test
    public void testTransformerEAP_6_4_0() throws Exception {
        ModelTestControllerVersion version = ModelTestControllerVersion.EAP_6_4_0;
        this.testTransformation(ModClusterModel.VERSION_1_5_0, version,
                formatLegacySubsystemArtifact(version),
                "org.jboss.mod_cluster:mod_cluster-core:1.2.11.Final-redhat-1"
        );
    }

    private void testTransformation(ModClusterModel model, ModelTestControllerVersion controllerVersion, String... dependencies) throws Exception {
        String subsystemXml = readResource("subsystem-transform.xml");
        ModelVersion modelVersion = model.getVersion();
        String extensionClassName = (model.getVersion().getMajor() == 1) ? "org.jboss.as.modcluster.ModClusterExtension" : "org.wildfly.extension.mod_cluster.ModClusterExtension";

        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization())
                .setSubsystemXml(subsystemXml);
        builder.createLegacyKernelServicesBuilder(null, controllerVersion, modelVersion)
                .addMavenResourceURL(dependencies)
                .setExtensionClassName(extensionClassName);

        KernelServices mainServices = builder.build();
        KernelServices legacyServices = mainServices.getLegacyServices(modelVersion);

        Assert.assertNotNull(legacyServices);
        Assert.assertTrue(mainServices.isSuccessfulBoot());
        Assert.assertTrue(legacyServices.isSuccessfulBoot());

        checkSubsystemModelTransformation(mainServices, modelVersion, null, false);
    }

    @Test
    public void testRejectionsEAP_6_2_0() throws Exception {
        ModelTestControllerVersion version = ModelTestControllerVersion.EAP_6_2_0;
        this.testRejections(ModClusterModel.VERSION_1_4_0, version,
                formatLegacySubsystemArtifact(version),
                "org.jboss.mod_cluster:mod_cluster-core:1.2.6.Final-redhat-1"
        );
    }

    @Test
    public void testRejectionsEAP_6_3_0() throws Exception {
        ModelTestControllerVersion version = ModelTestControllerVersion.EAP_6_3_0;
        this.testRejections(ModClusterModel.VERSION_1_5_0, version,
                formatLegacySubsystemArtifact(version),
                "org.jboss.mod_cluster:mod_cluster-core:1.2.9.Final-redhat-1"
        );
    }

    @Test
    public void testRejectionsEAP_6_4_0() throws Exception {
        ModelTestControllerVersion version = ModelTestControllerVersion.EAP_6_4_0;
        this.testRejections(ModClusterModel.VERSION_1_5_0, version,
                formatLegacySubsystemArtifact(version),
                "org.jboss.mod_cluster:mod_cluster-core:1.2.11.Final-redhat-1"
        );
    }

    private void testRejections(ModClusterModel model, ModelTestControllerVersion controllerVersion, String... dependencies) throws Exception {
        String subsystemXml = readResource("subsystem-reject.xml");
        ModelVersion modelVersion = model.getVersion();
        String extensionClassName = (model.getVersion().getMajor() == 1) ? "org.jboss.as.modcluster.ModClusterExtension" : "org.wildfly.extension.mod_cluster.ModClusterExtension";

        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization());
        builder.createLegacyKernelServicesBuilder(null, controllerVersion, modelVersion)
                .addMavenResourceURL(dependencies)
                .setExtensionClassName(extensionClassName);

        KernelServices mainServices = builder.build();
        KernelServices legacyServices = mainServices.getLegacyServices(modelVersion);

        Assert.assertNotNull(legacyServices);
        Assert.assertTrue(mainServices.isSuccessfulBoot());
        Assert.assertTrue(legacyServices.isSuccessfulBoot());

        ModelTestUtils.checkFailedTransformedBootOperations(mainServices, modelVersion, parse(subsystemXml), createFailedOperationConfig(modelVersion));
    }

    /**
     * Changed attributes:
     *
     * - proxies configuration
     * - status-interval is rejected if set to value other than 10
     * - session-draining-strategy configuration
     */
    private static FailedOperationTransformationConfig createFailedOperationConfig(ModelVersion version) {
        FailedOperationTransformationConfig config = new FailedOperationTransformationConfig();

        PathAddress subsystemAddress = PathAddress.pathAddress(ModClusterSubsystemResourceDefinition.PATH);
        PathAddress configurationAddress = subsystemAddress.append(ModClusterConfigResourceDefinition.PATH);

        if (ModClusterModel.VERSION_3_0_0.requiresTransformation(version)) {
            config.addFailedAttribute(configurationAddress, FailedOperationTransformationConfig.ChainedConfig.createBuilder(CommonAttributes.STATUS_INTERVAL, CommonAttributes.PROXIES)
                    .addConfig(new StatusIntervalConfig(CommonAttributes.STATUS_INTERVAL))
                    .addConfig(new ProxiesConfig(CommonAttributes.PROXIES))
                    .build());
        }

        if (ModClusterModel.VERSION_1_5_0.requiresTransformation(version)) {
            config.addFailedAttribute(configurationAddress, FailedOperationTransformationConfig.ChainedConfig.createBuilder(CommonAttributes.STATUS_INTERVAL, CommonAttributes.PROXIES, CommonAttributes.SESSION_DRAINING_STRATEGY)
                    .addConfig(new StatusIntervalConfig(CommonAttributes.STATUS_INTERVAL))
                    .addConfig(new ProxiesConfig(CommonAttributes.PROXIES))
                    .addConfig(new SessionDrainingStrategyConfig(CommonAttributes.SESSION_DRAINING_STRATEGY))
                    .build());
        }

        return config;
    }

    private static class SessionDrainingStrategyConfig extends FailedOperationTransformationConfig.AttributesPathAddressConfig<SessionDrainingStrategyConfig> {
        public SessionDrainingStrategyConfig(String... attributes) {
            super(attributes);
        }

        @Override
        protected boolean isAttributeWritable(String attributeName) {
            return true;
        }

        @Override
        protected boolean checkValue(String attrName, ModelNode attribute, boolean isWriteAttribute) {
            return !(attribute.equals(new ModelNode()) || attribute.equals(new ModelNode("DEFAULT")));
        }

        @Override
        protected ModelNode correctValue(ModelNode toResolve, boolean isWriteAttribute) {
            return new ModelNode("DEFAULT");
        }
    }

    private static class ProxiesConfig extends FailedOperationTransformationConfig.AttributesPathAddressConfig<ProxiesConfig> {
        public ProxiesConfig(String... attributes) {
            super(attributes);
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

    private static class StatusIntervalConfig extends FailedOperationTransformationConfig.AttributesPathAddressConfig<StatusIntervalConfig> {
        public StatusIntervalConfig(String... attributes) {
            super(attributes);
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

    protected AdditionalInitialization createAdditionalInitialization() {
        return new AdditionalInitialization.ManagementAdditionalInitialization() {
            @Override
            protected void setupController(ControllerInitializer controllerInitializer) {
                super.setupController(controllerInitializer);

                controllerInitializer.addSocketBinding("modcluster", 0); // "224.0.1.105", "23364"
                controllerInitializer.addRemoteOutboundSocketBinding("proxy1", "localhost", 6666);
                controllerInitializer.addRemoteOutboundSocketBinding("proxy2", "localhost", 6766);
                controllerInitializer.addRemoteOutboundSocketBinding("proxy3", "localhost", 6866);
            }
        };
    }
}
