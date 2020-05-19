/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.messaging.activemq;

import static org.jboss.as.model.test.ModelTestControllerVersion.EAP_7_0_0;
import static org.jboss.as.model.test.ModelTestControllerVersion.EAP_7_1_0;
import static org.junit.Assert.assertTrue;
import static org.wildfly.extension.messaging.activemq.MessagingDependencies.getActiveMQDependencies;
import static org.wildfly.extension.messaging.activemq.MessagingDependencies.getJGroupsDependencies;
import static org.wildfly.extension.messaging.activemq.MessagingDependencies.getMessagingActiveMQGAV;
import static org.wildfly.extension.messaging.activemq.MessagingExtension.BRIDGE_PATH;
import static org.wildfly.extension.messaging.activemq.MessagingExtension.CLUSTER_CONNECTION_PATH;
import static org.wildfly.extension.messaging.activemq.MessagingExtension.CONNECTION_FACTORY_PATH;
import static org.wildfly.extension.messaging.activemq.MessagingExtension.POOLED_CONNECTION_FACTORY_PATH;
import static org.wildfly.extension.messaging.activemq.MessagingExtension.REPLICATION_COLOCATED_PATH;
import static org.wildfly.extension.messaging.activemq.MessagingExtension.REPLICATION_MASTER_PATH;
import static org.wildfly.extension.messaging.activemq.MessagingExtension.SERVER_PATH;
import static org.wildfly.extension.messaging.activemq.MessagingExtension.SUBSYSTEM_PATH;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.security.CredentialReference;
import org.jboss.as.model.test.FailedOperationTransformationConfig;
import org.jboss.as.model.test.ModelTestControllerVersion;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.dmr.ModelNode;
import org.junit.Test;
import org.wildfly.clustering.jgroups.spi.JGroupsDefaultRequirement;
import org.wildfly.clustering.spi.ClusteringDefaultRequirement;
import org.wildfly.clustering.spi.ClusteringRequirement;
import org.wildfly.extension.messaging.activemq.ha.HAAttributes;
import org.wildfly.extension.messaging.activemq.jms.ConnectionFactoryAttributes;

/**
 *  * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2012 Red Hat inc
 */
public class MessagingActiveMQSubsystem_3_0_TestCase extends AbstractSubsystemBaseTest {

    public MessagingActiveMQSubsystem_3_0_TestCase() {
        super(MessagingExtension.SUBSYSTEM_NAME, new MessagingExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("subsystem_3_0.xml");
    }

    @Override
    protected String getSubsystemXsdPath() throws IOException {
        return "schema/wildfly-messaging-activemq_3_0.xsd";
    }

    @Override
    protected Properties getResolvedProperties() {
        Properties properties = new Properties();
        properties.put("messaging.cluster.user.name", "myClusterUser");
        properties.put("messaging.cluster.user.password", "myClusterPassword");
        return properties;
    }

    @Override
    protected KernelServices standardSubsystemTest(String configId, boolean compareXml) throws Exception {
        return super.standardSubsystemTest(configId, false);
    }
    /////////////////////////////////////////
    //  Tests for HA Policy Configuration  //
    /////////////////////////////////////////

    @Test
    public void testHAPolicyConfiguration() throws Exception {
        standardSubsystemTest("subsystem_3_0_ha-policy.xml");
    }

    ///////////////////////
    // Transformers test //
    ///////////////////////

    @Test
    public void testTransformersEAP_7_1_0() throws Exception {
        testTransformers(EAP_7_1_0, MessagingExtension.VERSION_2_0_0);
    }

    @Test
    public void testTransformersEAP_7_0_0() throws Exception {
        testTransformers(EAP_7_0_0, MessagingExtension.VERSION_1_0_0);
    }

    @Test
    public void testRejectingTransformersEAP_7_1_0() throws Exception {
        testRejectingTransformers(EAP_7_1_0, MessagingExtension.VERSION_2_0_0);
    }

    @Test
    public void testRejectingTransformersEAP_7_0_0() throws Exception {
        testRejectingTransformers(EAP_7_0_0, MessagingExtension.VERSION_1_0_0);
    }

    private void testTransformers(ModelTestControllerVersion controllerVersion, ModelVersion messagingVersion) throws Exception {
        //Boot up empty controllers with the resources needed for the ops coming from the xml to work
        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization())
                .setSubsystemXmlResource("subsystem_3_0_transform.xml");
        builder.createLegacyKernelServicesBuilder(createAdditionalInitialization(), controllerVersion, messagingVersion)
                .addMavenResourceURL(getMessagingActiveMQGAV(controllerVersion))
                .addMavenResourceURL(getActiveMQDependencies(controllerVersion))
                .addMavenResourceURL(getJGroupsDependencies(controllerVersion))
                .skipReverseControllerCheck()
                .dontPersistXml();

        KernelServices mainServices = builder.build();
        assertTrue(mainServices.isSuccessfulBoot());
        assertTrue(mainServices.getLegacyServices(messagingVersion).isSuccessfulBoot());

        checkSubsystemModelTransformation(mainServices, messagingVersion);
        mainServices.shutdown();
    }

    private void testRejectingTransformers(ModelTestControllerVersion controllerVersion, ModelVersion messagingVersion) throws Exception {
        //Boot up empty controllers with the resources needed for the ops coming from the xml to work
        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization());
        builder.createLegacyKernelServicesBuilder(createAdditionalInitialization(), controllerVersion, messagingVersion)
                .addMavenResourceURL(getMessagingActiveMQGAV(controllerVersion))
                .addMavenResourceURL(getActiveMQDependencies(controllerVersion))
                .addMavenResourceURL(getJGroupsDependencies(controllerVersion))
                .skipReverseControllerCheck()
                .dontPersistXml();

        KernelServices mainServices = builder.build();
        assertTrue(mainServices.isSuccessfulBoot());
        assertTrue(mainServices.getLegacyServices(messagingVersion).isSuccessfulBoot());

        List<ModelNode> ops = builder.parseXmlResource("subsystem_3_0_reject_transform.xml");
        System.out.println("ops = " + ops);
        PathAddress subsystemAddress = PathAddress.pathAddress(SUBSYSTEM_PATH);

        FailedOperationTransformationConfig config = new FailedOperationTransformationConfig();
        if (messagingVersion.equals(MessagingExtension.VERSION_1_0_0)) {
            config.addFailedAttribute(subsystemAddress,
                        new FailedOperationTransformationConfig.NewAttributesConfig(
                                MessagingSubsystemRootResourceDefinition.GLOBAL_CLIENT_THREAD_POOL_MAX_SIZE,
                                MessagingSubsystemRootResourceDefinition.GLOBAL_CLIENT_SCHEDULED_THREAD_POOL_MAX_SIZE))
                .addFailedAttribute(subsystemAddress.append(SERVER_PATH),
                        new FailedOperationTransformationConfig.NewAttributesConfig(
                                ServerDefinition.ELYTRON_DOMAIN,
                                ServerDefinition.JOURNAL_DATASOURCE,
                                ServerDefinition.JOURNAL_MESSAGES_TABLE,
                                ServerDefinition.JOURNAL_BINDINGS_TABLE,
                                ServerDefinition.JOURNAL_JMS_BINDINGS_TABLE,
                                ServerDefinition.JOURNAL_LARGE_MESSAGES_TABLE,
                                ServerDefinition.JOURNAL_PAGE_STORE_TABLE,
                                ServerDefinition.JOURNAL_DATABASE,
                                ServerDefinition.JOURNAL_JDBC_NETWORK_TIMEOUT))
                .addFailedAttribute(subsystemAddress.append(SERVER_PATH, REPLICATION_MASTER_PATH),
                        new ChangeToTrueConfig(HAAttributes.CHECK_FOR_LIVE_SERVER.getName()))
                .addFailedAttribute(subsystemAddress.append(SERVER_PATH, REPLICATION_COLOCATED_PATH, MessagingExtension.CONFIGURATION_MASTER_PATH),
                        new ChangeToTrueConfig(HAAttributes.CHECK_FOR_LIVE_SERVER.getName()))
                .addFailedAttribute(subsystemAddress.append(SERVER_PATH, PathElement.pathElement(CommonAttributes.HTTP_CONNECTOR)),
                        new FailedOperationTransformationConfig.NewAttributesConfig(
                                HTTPConnectorDefinition.SERVER_NAME))
                .addFailedAttribute(subsystemAddress.append(SERVER_PATH, BRIDGE_PATH),
                        new FailedOperationTransformationConfig.NewAttributesConfig(
                                BridgeDefinition.PRODUCER_WINDOW_SIZE))
                .addFailedAttribute(subsystemAddress.append(SERVER_PATH, CLUSTER_CONNECTION_PATH),
                        new FailedOperationTransformationConfig.NewAttributesConfig(
                                ClusterConnectionDefinition.PRODUCER_WINDOW_SIZE))
                .addFailedAttribute(subsystemAddress.append(SERVER_PATH, CONNECTION_FACTORY_PATH),
                        new FailedOperationTransformationConfig.NewAttributesConfig(
                                ConnectionFactoryAttributes.Common.DESERIALIZATION_BLACKLIST,
                                ConnectionFactoryAttributes.Common.DESERIALIZATION_WHITELIST))
                .addFailedAttribute(subsystemAddress.append(SERVER_PATH, POOLED_CONNECTION_FACTORY_PATH),
                        new FailedOperationTransformationConfig.NewAttributesConfig(
                                ConnectionFactoryAttributes.Pooled.ALLOW_LOCAL_TRANSACTIONS,
                                ConnectionFactoryAttributes.Pooled.REBALANCE_CONNECTIONS,
                                ConnectionFactoryAttributes.Pooled.STATISTICS_ENABLED,
                                ConnectionFactoryAttributes.Pooled.CREDENTIAL_REFERENCE,
                                ConnectionFactoryAttributes.Common.DESERIALIZATION_BLACKLIST,
                                ConnectionFactoryAttributes.Common.DESERIALIZATION_WHITELIST))
                ;
        }

        config.addFailedAttribute(subsystemAddress.append(SERVER_PATH, MessagingExtension.BROADCAST_GROUP_PATH), new FailedOperationTransformationConfig.NewAttributesConfig(BroadcastGroupDefinition.JGROUPS_CHANNEL));
        config.addFailedAttribute(subsystemAddress.append(SERVER_PATH, DiscoveryGroupDefinition.PATH), new FailedOperationTransformationConfig.NewAttributesConfig(DiscoveryGroupDefinition.JGROUPS_CHANNEL));
        config.addFailedAttribute(subsystemAddress.append(DiscoveryGroupDefinition.PATH), FailedOperationTransformationConfig.REJECTED_RESOURCE);

        ModelTestUtils.checkFailedTransformedBootOperations(mainServices, messagingVersion, ops, config);
        mainServices.shutdown();
    }

    @Override
    protected AdditionalInitialization createAdditionalInitialization() {
        return AdditionalInitialization.withCapabilities(ClusteringRequirement.COMMAND_DISPATCHER_FACTORY.resolve("ee"),
                ClusteringDefaultRequirement.COMMAND_DISPATCHER_FACTORY.getName(),
                JGroupsDefaultRequirement.CHANNEL_FACTORY.getName(),
                Capabilities.ELYTRON_DOMAIN_CAPABILITY,
                Capabilities.ELYTRON_DOMAIN_CAPABILITY + ".elytronDomain",
                CredentialReference.CREDENTIAL_STORE_CAPABILITY + ".cs1",
                Capabilities.DATA_SOURCE_CAPABILITY + ".fooDS");
    }
}
