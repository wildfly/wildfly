/*
 * Copyright 2020 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.extension.messaging.activemq;

import static org.jboss.as.model.test.ModelTestControllerVersion.EAP_7_4_0;
import static org.junit.Assert.assertTrue;
import static org.wildfly.extension.messaging.activemq.MessagingDependencies.getActiveMQDependencies;
import static org.wildfly.extension.messaging.activemq.MessagingDependencies.getJGroupsDependencies;
import static org.wildfly.extension.messaging.activemq.MessagingDependencies.getMessagingActiveMQGAV;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.security.CredentialReference;
import org.jboss.as.model.test.ModelTestControllerVersion;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.dmr.ModelNode;
import org.junit.Test;
import org.wildfly.clustering.server.service.ClusteringServiceDescriptor;
import org.wildfly.clustering.server.service.LegacyClusteringServiceDescriptor;

public class MessagingActiveMQSubsystem_17_0_TestCase extends AbstractSubsystemBaseTest {

    public MessagingActiveMQSubsystem_17_0_TestCase() {
        super(MessagingExtension.SUBSYSTEM_NAME, new MessagingExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("subsystem_17_0.xml");
    }

    @Override
    protected String getSubsystemXsdPath() throws IOException {
        return "schema/wildfly-messaging-activemq_17_0.xsd";
    }

    ///////////////////////
    // Transformers test //
    ///////////////////////

    @Test
    public void testTransformersWF_36() throws Exception {
        testTransformers(ModelTestControllerVersion.MASTER, MessagingExtension.VERSION_16_0_0);
    }

    @Test
    public void testTransformersEAP_7_4_0() throws Exception {
        testTransformers(EAP_7_4_0, MessagingExtension.VERSION_13_0_0);
    }

    private void testTransformers(ModelTestControllerVersion controllerVersion, ModelVersion messagingVersion) throws Exception {
        //Boot up empty controllers with the resources needed for the ops coming from the xml to work
        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization())
                .setSubsystemXmlResource("subsystem_17_0_transform.xml");
        builder.createLegacyKernelServicesBuilder(createAdditionalInitialization(), controllerVersion, messagingVersion)
                .addMavenResourceURL(getMessagingActiveMQGAV(controllerVersion))
                .addMavenResourceURL(getActiveMQDependencies(controllerVersion))
                .addMavenResourceURL(getJGroupsDependencies(controllerVersion))
                .skipReverseControllerCheck()
                .dontPersistXml();

        KernelServices mainServices = builder.build();
        assertTrue(mainServices.isSuccessfulBoot());
        assertTrue(mainServices.getLegacyServices(messagingVersion).isSuccessfulBoot());

        checkSubsystemModelTransformation(mainServices, messagingVersion, (ModelNode modelNode) -> {
            ModelNode legacyModel = modelNode.clone();
            if (modelNode.hasDefined("server", "default", "address-setting", "test", "page-size-bytes")) {
                int legacyNodeValue = modelNode.get("server", "default", "address-setting", "test", "page-size-bytes").asInt();
                legacyModel.get("server", "default", "address-setting", "test", "page-size-bytes").set(legacyNodeValue);
            }
            return legacyModel;
        });
        mainServices.shutdown();
    }

    @Override
    protected Set<PathAddress> getIgnoredChildResourcesForRemovalTest() {
        Set<PathAddress> ignoredChildResources = new HashSet<>(super.getIgnoredChildResourcesForRemovalTest());
        ignoredChildResources.add(PathAddress.parseCLIStyleAddress("/subsystem=messaging-activemq/server=default/discovery-group=groupS"));
        return ignoredChildResources;
    }

    @Override
    protected AdditionalInitialization createAdditionalInitialization() {
        return AdditionalInitialization.withCapabilities(RuntimeCapability.resolveCapabilityName(ClusteringServiceDescriptor.COMMAND_DISPATCHER_FACTORY, "ee"),
                RuntimeCapability.resolveCapabilityName(LegacyClusteringServiceDescriptor.COMMAND_DISPATCHER_FACTORY, "ee"),
                ClusteringServiceDescriptor.DEFAULT_COMMAND_DISPATCHER_FACTORY.getName(),
                LegacyClusteringServiceDescriptor.DEFAULT_COMMAND_DISPATCHER_FACTORY.getName(),
                Capabilities.ELYTRON_DOMAIN_CAPABILITY,
                Capabilities.ELYTRON_DOMAIN_CAPABILITY + ".elytronDomain",
                CredentialReference.CREDENTIAL_STORE_CAPABILITY + ".cs1",
                Capabilities.DATA_SOURCE_CAPABILITY + ".fooDS",
                Capabilities.LEGACY_SECURITY_DOMAIN_CAPABILITY.getDynamicName("other"),
                Capabilities.ELYTRON_SSL_CONTEXT_CAPABILITY.getDynamicName("messaging"));
    }
}
