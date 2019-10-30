/*
Copyright 2017 Red Hat, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package org.wildfly.extension.undertow;

import static org.jboss.as.controller.capability.RuntimeCapability.buildDynamicCapabilityName;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MULTICAST_ADDRESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MULTICAST_PORT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PORT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;
import static org.wildfly.extension.undertow.Capabilities.CAPABILITY_BYTE_BUFFER_POOL;
import static org.wildfly.extension.undertow.Capabilities.REF_IO_WORKER;

import java.security.KeyStore;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLContext;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.capability.registry.RuntimeCapabilityRegistry;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.network.OutboundSocketBinding;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.ControllerInitializer;
import org.jboss.dmr.ModelNode;
import org.wildfly.security.auth.server.HttpAuthenticationFactory;
import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.credential.store.CredentialStore;
import org.xnio.Pool;
import org.xnio.XnioWorker;

import io.undertow.connector.ByteBufferPool;

/**
 * Initialization used in undertow subsystem tests.
 *
 * @author Brian Stansberry
 */
class DefaultInitialization extends AdditionalInitialization.ManagementAdditionalInitialization {

    private static final long serialVersionUID = 1L;

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
        final Map<String, Integer> sockets = new HashMap<>();

        {
            sockets.put("ajp", 8009);
            sockets.put("http", 8080);
            sockets.put("http-2", 8081);
            sockets.put("http-3", 8082);
            sockets.put("https-non-default", 8433);
            sockets.put("https-2", 8434);
            sockets.put("https-3", 8435);
            sockets.put("https-4", 8436);
            sockets.put("ajps", 8010);
            sockets.put("test3", 8012);
        }
        for (Map.Entry<String, Integer> entry : sockets.entrySet()) {
            controllerInitializer.addSocketBinding(entry.getKey(), entry.getValue());
        }

        controllerInitializer.addRemoteOutboundSocketBinding("ajp-remote", "localhost", 7777);
    }

    @Override
    protected void initializeExtraSubystemsAndModel(ExtensionRegistry extensionRegistry, Resource rootResource,
                                                    ManagementResourceRegistration rootRegistration, RuntimeCapabilityRegistry capabilityRegistry) {
        super.initializeExtraSubystemsAndModel(extensionRegistry, rootResource, rootRegistration, capabilityRegistry);
        Map<String, Class> capabilities = new HashMap<>();
        capabilities.put(buildDynamicCapabilityName(REF_IO_WORKER,
                ListenerResourceDefinition.WORKER.getDefaultValue().asString()), XnioWorker.class);
        capabilities.put(buildDynamicCapabilityName(REF_IO_WORKER, "non-default"),
                XnioWorker.class);
        capabilities.put(buildDynamicCapabilityName(CAPABILITY_BYTE_BUFFER_POOL,
                ListenerResourceDefinition.BUFFER_POOL.getDefaultValue().asString()), ByteBufferPool.class);
        capabilities.put(buildDynamicCapabilityName("org.wildfly.io.buffer-pool",
                ListenerResourceDefinition.BUFFER_POOL.getDefaultValue().asString()), Pool.class);
        capabilities.put(buildDynamicCapabilityName(Capabilities.REF_HTTP_AUTHENTICATION_FACTORY, "elytron-factory"), HttpAuthenticationFactory.class);
        capabilities.put(buildDynamicCapabilityName(Capabilities.REF_HTTP_AUTHENTICATION_FACTORY, "factory"), HttpAuthenticationFactory.class);
        capabilities.put(buildDynamicCapabilityName(Capabilities.REF_SECURITY_DOMAIN, "elytron-domain"), SecurityDomain.class);
        capabilities.put(buildDynamicCapabilityName("org.wildfly.security.ssl-context", "TestContext"), SSLContext.class);
        capabilities.put(buildDynamicCapabilityName("org.wildfly.security.ssl-context", "my-ssl-context"), SSLContext.class);
        capabilities.put(buildDynamicCapabilityName("org.wildfly.security.key-store", "my-key-store"), KeyStore.class);
        capabilities.put(buildDynamicCapabilityName("org.wildfly.security.credential-store", "my-credential-store"), CredentialStore.class);

        capabilities.put(buildDynamicCapabilityName("org.wildfly.security.ssl-context", "foo"), SSLContext.class);
        //capabilities.put(buildDynamicCapabilityName("org.wildfly.network.outbound-socket-binding","ajp-remote"), OutboundSocketBinding.class);


        registerServiceCapabilities(capabilityRegistry, capabilities);
        registerCapabilities(capabilityRegistry,
                RuntimeCapability.Builder.of("org.wildfly.network.outbound-socket-binding", true, OutboundSocketBinding.class).build(),
                RuntimeCapability.Builder.of("org.wildfly.security.ssl-context", true, SSLContext.class).build()
        );


    }
}
