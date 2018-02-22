/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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
package org.wildfly.iiop.openjdk;

import static org.jboss.as.controller.capability.RuntimeCapability.buildDynamicCapabilityName;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MULTICAST_ADDRESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MULTICAST_PORT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PORT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.net.ssl.SSLContext;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.capability.registry.RuntimeCapabilityRegistry;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.model.test.FailedOperationTransformationConfig;
import org.jboss.as.model.test.ModelTestControllerVersion;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.as.network.OutboundSocketBinding;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.ControllerInitializer;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.security.SecurityDomain;
import org.junit.Assert;
import org.junit.Test;
import org.wildfly.security.credential.store.CredentialStore;

/**
 * <ṕ>
 * IIOP subsystem tests.
 * </ṕ>
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author <a href="sguilhen@jboss.com">Stefan Guilhen</a>
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 */
public class IIOPSubsystemTransformersTestCase extends AbstractSubsystemBaseTest {

    private static ModelVersion EAP7_0_0 = ModelVersion.create(1, 0, 0);

    public IIOPSubsystemTransformersTestCase() {
        super(IIOPExtension.SUBSYSTEM_NAME, new IIOPExtension());
    }


    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("iiop-transformers.xml");
    }

    @Test
    public void testTransformersEAP_7_0_0() throws Exception {
        testTransformers(ModelTestControllerVersion.EAP_7_0_0, EAP7_0_0);
    }

    @Test
    public void testRejectTransformersEAP_7_0_0() throws Exception {
        doRejectTest(ModelTestControllerVersion.EAP_7_0_0, EAP7_0_0);
    }


    private void doRejectTest(ModelTestControllerVersion controllerVersion, ModelVersion targetVersion) throws Exception {
        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization());
        builder.createLegacyKernelServicesBuilder(null, controllerVersion, targetVersion)
                .configureReverseControllerCheck(createAdditionalInitialization(), null)
                //.skipReverseControllerCheck()
                .addMavenResourceURL(String.format("%s:wildfly-iiop-openjdk:%s", controllerVersion.getMavenGroupId(), controllerVersion.getMavenGavVersion()))
                .dontPersistXml();

        KernelServices mainServices = builder.build();
        Assert.assertTrue(mainServices.isSuccessfulBoot());
        KernelServices legacyServices = mainServices.getLegacyServices(targetVersion);
        Assert.assertTrue(legacyServices.isSuccessfulBoot());
        Assert.assertNotNull(legacyServices);

        List<ModelNode> ops = builder.parseXmlResource("iiop-transformers-reject.xml");
        PathAddress subsystemAddress = PathAddress.pathAddress(IIOPExtension.PATH_SUBSYSTEM);

        ModelTestUtils.checkFailedTransformedBootOperations(mainServices, targetVersion, ops, new FailedOperationTransformationConfig()
                .addFailedAttribute(subsystemAddress,
                        new FailedOperationTransformationConfig.NewAttributesConfig(
                                IIOPRootDefinition.CLIENT_REQUIRES_SSL,
                                IIOPRootDefinition.SERVER_REQUIRES_SSL,
                                IIOPRootDefinition.SECURITY,
                                IIOPRootDefinition.AUTHENTICATION_CONTEXT
                        )
                )
        );
    }

    private void testTransformers(ModelTestControllerVersion controllerVersion, ModelVersion undertowVersion) throws Exception {
        //Boot up empty controllers with the resources needed for the ops coming from the xml to work
        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization())
                .setSubsystemXmlResource("iiop-transformers.xml");
        builder.createLegacyKernelServicesBuilder(null, controllerVersion, undertowVersion)
                .addMavenResourceURL(String.format("%s:wildfly-iiop-openjdk:%s", controllerVersion.getMavenGroupId(), controllerVersion.getMavenGavVersion()))
                .configureReverseControllerCheck(createAdditionalInitialization(), null)
                .skipReverseControllerCheck()
                .dontPersistXml();

        KernelServices mainServices = builder.build();
        assertTrue(mainServices.isSuccessfulBoot());
        assertTrue(mainServices.getLegacyServices(undertowVersion).isSuccessfulBoot());

        checkSubsystemModelTransformation(mainServices, undertowVersion, null);
    }

    @Override
    protected AdditionalInitialization createAdditionalInitialization() {
        return new DefaultInitialization();
    }

    @Override
    public void testSchema() throws Exception {
    }

    private static class DefaultInitialization extends AdditionalInitialization {
        protected final Map<String, Integer> sockets = new HashMap<>();

        {
            sockets.put("ajp", 8009);
            sockets.put("http", 8080);
        }

        @Override
        protected ControllerInitializer createControllerInitializer() {
            return new ControllerInitializer() {
                @Override
                protected void initializeSocketBindingsOperations(List<ModelNode> ops) {
                    super.initializeSocketBindingsOperations(ops);
                    ModelNode op = new ModelNode();
                    op.get(OP).set(ADD);
                    op.get(OP_ADDR).set(PathAddress.pathAddress(PathElement.pathElement(SOCKET_BINDING_GROUP, SOCKET_BINDING_GROUP_NAME),
                            PathElement.pathElement(SOCKET_BINDING, "advertise-socket-binding")).toModelNode());
                    op.get(PORT).set(8011);
                    op.get(MULTICAST_ADDRESS).set("224.0.1.105");
                    op.get(MULTICAST_PORT).set("23364");
                    ops.add(op);

                }
            };
        }

        @Override
        protected RunningMode getRunningMode() {
            return RunningMode.ADMIN_ONLY;
        }

        @Override
        protected void setupController(ControllerInitializer controllerInitializer) {
            super.setupController(controllerInitializer);

            for (Map.Entry<String, Integer> entry : sockets.entrySet()) {
                controllerInitializer.addSocketBinding(entry.getKey(), entry.getValue());
            }

            controllerInitializer.addRemoteOutboundSocketBinding("iiop", "localhost", 7777);
            controllerInitializer.addRemoteOutboundSocketBinding("iiop-ssl", "localhost", 7778);
        }

        @Override
        protected void initializeExtraSubystemsAndModel(ExtensionRegistry extensionRegistry, Resource rootResource,
                                                        ManagementResourceRegistration rootRegistration, RuntimeCapabilityRegistry capabilityRegistry) {
            super.initializeExtraSubystemsAndModel(extensionRegistry, rootResource, rootRegistration, capabilityRegistry);
            Map<String, Class> capabilities = new HashMap<>();
            capabilities.put(buildDynamicCapabilityName("org.wildfly.security.ssl-context", "TestContext"), SSLContext.class);
            capabilities.put(buildDynamicCapabilityName("org.wildfly.security.ssl-context", "my-ssl-context"), SSLContext.class);
            capabilities.put(buildDynamicCapabilityName("org.wildfly.security.key-store", "my-key-store"), KeyStore.class);
            capabilities.put(buildDynamicCapabilityName("org.wildfly.security.credential-store", "my-credential-store"), CredentialStore.class);

            capabilities.put(buildDynamicCapabilityName("org.wildfly.security.ssl-context", "foo"), SSLContext.class);
            capabilities.put(buildDynamicCapabilityName("org.wildfly.security.authentication-context", "iiop"), SSLContext.class);

            capabilities.put(buildDynamicCapabilityName("org.wildfly.security.legacy-security-domain", "domain"), SecurityDomain.class);

            registerServiceCapabilities(capabilityRegistry, capabilities);
            registerCapabilities(capabilityRegistry,
                    RuntimeCapability.Builder.of("org.wildfly.network.outbound-socket-binding", true, OutboundSocketBinding.class).build(),
                    RuntimeCapability.Builder.of("org.wildfly.security.ssl-context", true, SSLContext.class).build()
            );


        }
    }

}
