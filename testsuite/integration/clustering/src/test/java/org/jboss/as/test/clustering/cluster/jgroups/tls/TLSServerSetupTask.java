/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.cluster.jgroups.tls;

import static org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase.*;

import java.util.List;
import java.util.Set;

import org.jboss.as.test.shared.ManagementServerSetupTask;
import org.jgroups.protocols.TCP;

/**
 * Utility interface containing {@link org.jboss.as.arquillian.api.ServerSetupTask}s for setting up TLS/SSL for JGroups channels.
 *
 * @author Radoslav Husar
 */
public interface TLSServerSetupTask {

    /**
     * Server setup task that generates a unique key pair on each node and configures TLS transport.
     * Each node uses its own keystore as both key store and trust store, so nodes trust only themselves
     * and will NOT form a cluster. Used by the untrusted/negative TLS test cases.
     */
    class UntrustedCertSecureJGroupsTransport extends ManagementServerSetupTask {
        @SuppressWarnings("deprecation")
        public UntrustedCertSecureJGroupsTransport(Set<String> nodes, String tp) {
            super(createContainerSetConfigurationBuilder()
                    .addContainers(nodes, createContainerConfigurationBuilder()
                            .setupScript(createScriptBuilder()
                                    // n.b. cannot batch: key-store must be STARTED before generate-key-pair
                                    .add("/subsystem=elytron/key-store=jgroupsKS:add(path=server.keystore.pkcs12, relative-to=jboss.server.config.dir, credential-reference={clear-text=secret}, type=PKCS12)")
                                    .add("/subsystem=elytron/key-store=jgroupsKS:generate-key-pair(alias=jgroups, algorithm=RSA, key-size=2048, validity=365, credential-reference={clear-text=secret}, distinguished-name=\"CN=jgroups\")")
                                    .add("/subsystem=elytron/key-store=jgroupsKS:store")
                                    .startBatch()
                                    .add("/subsystem=elytron/key-manager=jgroupsKM:add(key-store=jgroupsKS, credential-reference={clear-text=secret})")
                                    .add("/subsystem=elytron/trust-manager=jgroupsTM:add(key-store=jgroupsKS)")
                                    .add("/subsystem=elytron/client-ssl-context=jgroupsCSC:add(key-manager=jgroupsKM, trust-manager=jgroupsTM, protocols=[\"TLSv1.2\"])")
                                    .add("/subsystem=elytron/server-ssl-context=jgroupsSSC:add(key-manager=jgroupsKM, trust-manager=jgroupsTM, protocols=[\"TLSv1.2\"], need-client-auth=true)")
                                    .add("/subsystem=jgroups/stack=tcp/transport=%s:write-attribute(name=client-ssl-context, value=jgroupsCSC)", tp)
                                    .add("/subsystem=jgroups/stack=tcp/transport=%s:write-attribute(name=server-ssl-context, value=jgroupsSSC)", tp)
                                    .endBatch()
                                    .build())
                            .tearDownScript(createScriptBuilder()
                                    .startBatch()
                                    .add("/subsystem=jgroups/stack=tcp/transport=%s:undefine-attribute(name=server-ssl-context)", tp)
                                    .add("/subsystem=jgroups/stack=tcp/transport=%s:undefine-attribute(name=client-ssl-context)", tp)
                                    .add("/subsystem=elytron/server-ssl-context=jgroupsSSC:remove")
                                    .add("/subsystem=elytron/client-ssl-context=jgroupsCSC:remove")
                                    .add("/subsystem=elytron/trust-manager=jgroupsTM:remove")
                                    .add("/subsystem=elytron/key-manager=jgroupsKM:remove")
                                    .add("/subsystem=elytron/key-store=jgroupsKS:remove-alias(alias=jgroups)")
                                    .endBatch()
                                    .add("/subsystem=elytron/key-store=jgroupsKS:store")
                                    .add("/subsystem=elytron/key-store=jgroupsKS:remove")
                                    .build())
                            .build())
                    .build());
        }
    }

    class UntrustedCertSecureJGroupsTransport_TCP_NODE_1_2 extends UntrustedCertSecureJGroupsTransport {
        public UntrustedCertSecureJGroupsTransport_TCP_NODE_1_2() {
            super(NODE_1_2, TCP.class.getSimpleName());
        }
    }

    /**
     * Server setup task that generates a separate private key for each node and creates a shared truststore
     * containing all nodes' public certificates, enabling proper mutual TLS authentication with per-node identity.
     * All key generation and truststore population is performed on the first node in the list.
     */
    class PerNodeKeyStore extends ManagementServerSetupTask {
        @SuppressWarnings("deprecation")
        public PerNodeKeyStore(List<String> nodes) {
            super(createContainerSetConfigurationBuilder()
                    .addContainer(nodes.get(0), createContainerConfigurationBuilder()
                            .setupScript(perNodeKeySetupScript(nodes))
                            .tearDownScript(perNodeKeyTearDownScript(nodes))
                            .build())
                    .build());
        }

        private static List<List<String>> perNodeKeySetupScript(List<String> nodes) {
            ScriptBuilder builder = createScriptBuilder();
            for (String node : nodes) {
                builder.add("/subsystem=elytron/key-store=jgroupsKS-%s:add(path=../../../server-%s.keystore.pkcs12, relative-to=jboss.server.config.dir, credential-reference={clear-text=secret}, type=PKCS12)", node, node);
                builder.add("/subsystem=elytron/key-store=jgroupsKS-%s:generate-key-pair(alias=%s, algorithm=RSA, key-size=2048, validity=365, credential-reference={clear-text=secret}, distinguished-name=\"CN=%s\")", node, node, node);
                builder.add("/subsystem=elytron/key-store=jgroupsKS-%s:store", node);
                builder.add("/subsystem=elytron/key-store=jgroupsKS-%s:export-certificate(alias=%s, path=server-%s.keystore.pem, relative-to=jboss.server.config.dir, pem=true)", node, node, node);
            }
            builder.add("/subsystem=elytron/key-store=jgroupsTS-shared:add(path=../../../server.truststore.pkcs12, relative-to=jboss.server.config.dir, credential-reference={clear-text=secret}, type=PKCS12)");
            for (String node : nodes) {
                builder.add("/subsystem=elytron/key-store=jgroupsTS-shared:import-certificate(alias=%s, path=server-%s.keystore.pem, relative-to=jboss.server.config.dir, credential-reference={clear-text=secret}, trust-cacerts=true, validate=false)", node, node);
            }
            builder.add("/subsystem=elytron/key-store=jgroupsTS-shared:store");
            return builder.build();
        }

        private static List<List<String>> perNodeKeyTearDownScript(List<String> nodes) {
            ScriptBuilder builder = createScriptBuilder();
            for (String node : nodes) {
                builder.add("/subsystem=elytron/key-store=jgroupsKS-%s:remove-alias(alias=%s)", node, node);
                builder.add("/subsystem=elytron/key-store=jgroupsKS-%s:store", node);
            }
            for (String node : nodes) {
                builder.add("/subsystem=elytron/key-store=jgroupsTS-shared:remove-alias(alias=%s)", node);
            }
            builder.add("/subsystem=elytron/key-store=jgroupsTS-shared:store");
            builder.add("/subsystem=elytron/key-store=jgroupsTS-shared:remove");
            for (String node : nodes) {
                builder.add("/subsystem=elytron/key-store=jgroupsKS-%s:remove", node);
            }
            return builder.build();
        }
    }

    class PerNodeKeyStore_NODE_1_2 extends PerNodeKeyStore {
        public PerNodeKeyStore_NODE_1_2() {
            super(List.of(NODE_1, NODE_2));
        }
    }

    class PerNodeKeyStore_NODE_1_2_3 extends PerNodeKeyStore {
        public PerNodeKeyStore_NODE_1_2_3() {
            super(List.of(NODE_1, NODE_2, NODE_3));
        }
    }

    /**
     * Server setup task that configures TLS on the specified transport protocol with per-node keystores and a shared truststore.
     * Each node references its own keystore ({@code server-<node>.keystore.pkcs12}) containing its own private key
     * and a shared truststore ({@code server.truststore.pkcs12}) containing all nodes' public certificates.
     * Requires {@link PerNodeKeyStore} to be run first (once) to generate the keystores and truststore.
     */
    class PerNodeSecureJGroupsTransport extends ManagementServerSetupTask {
        public PerNodeSecureJGroupsTransport(List<String> nodes, String tp) {
            super(perNodeTransportConfig(nodes, tp));
        }

        @SuppressWarnings("deprecation")
        private static ContainerSetConfiguration perNodeTransportConfig(List<String> nodes, String tp) {
            ContainerSetConfigurationBuilder builder = createContainerSetConfigurationBuilder();
            for (String node : nodes) {
                builder.addContainer(node, createContainerConfigurationBuilder()
                        .setupScript(createScriptBuilder()
                                .startBatch()
                                .add("/subsystem=elytron/key-store=jgroupsKS:add(path=../../../server-%s.keystore.pkcs12, relative-to=jboss.server.config.dir, credential-reference={clear-text=secret}, type=PKCS12)", node)
                                .add("/subsystem=elytron/key-manager=jgroupsKM:add(key-store=jgroupsKS, credential-reference={clear-text=secret})")
                                .add("/subsystem=elytron/key-store=jgroupsTS:add(path=../../../server.truststore.pkcs12, relative-to=jboss.server.config.dir, credential-reference={clear-text=secret}, type=PKCS12)")
                                .add("/subsystem=elytron/trust-manager=jgroupsTM:add(key-store=jgroupsTS)")
                                .add("/subsystem=elytron/client-ssl-context=jgroupsCSC:add(key-manager=jgroupsKM, trust-manager=jgroupsTM, protocols=[\"TLSv1.2\"])")
                                .add("/subsystem=elytron/server-ssl-context=jgroupsSSC:add(key-manager=jgroupsKM, trust-manager=jgroupsTM, protocols=[\"TLSv1.2\"], need-client-auth=true)")
                                .add("/subsystem=jgroups/stack=tcp/transport=%s:write-attribute(name=client-ssl-context, value=jgroupsCSC)", tp)
                                .add("/subsystem=jgroups/stack=tcp/transport=%s:write-attribute(name=server-ssl-context, value=jgroupsSSC)", tp)
                                .endBatch()
                                .build())
                        .tearDownScript(createScriptBuilder()
                                .startBatch()
                                .add("/subsystem=jgroups/stack=tcp/transport=%s:undefine-attribute(name=server-ssl-context)", tp)
                                .add("/subsystem=jgroups/stack=tcp/transport=%s:undefine-attribute(name=client-ssl-context)", tp)
                                .add("/subsystem=elytron/server-ssl-context=jgroupsSSC:remove")
                                .add("/subsystem=elytron/client-ssl-context=jgroupsCSC:remove")
                                .add("/subsystem=elytron/trust-manager=jgroupsTM:remove")
                                .add("/subsystem=elytron/key-store=jgroupsTS:remove")
                                .add("/subsystem=elytron/key-manager=jgroupsKM:remove")
                                .add("/subsystem=elytron/key-store=jgroupsKS:remove")
                                .endBatch()
                                .build())
                        .build());
            }
            return builder.build();
        }
    }

    class PerNodeSecureJGroupsTransport_TCP_NODE_1_2 extends PerNodeSecureJGroupsTransport {
        public PerNodeSecureJGroupsTransport_TCP_NODE_1_2() {
            super(List.of(NODE_1, NODE_2), TCP.class.getSimpleName());
        }
    }

    class PerNodeSecureJGroupsTransport_TCP_NODE_1 extends PerNodeSecureJGroupsTransport {
        public PerNodeSecureJGroupsTransport_TCP_NODE_1() {
            super(List.of(NODE_1), TCP.class.getSimpleName());
        }
    }

    class PerNodeSecureJGroupsTransport_TCP_NODE_1_2_3 extends PerNodeSecureJGroupsTransport {
        public PerNodeSecureJGroupsTransport_TCP_NODE_1_2_3() {
            super(List.of(NODE_1, NODE_2, NODE_3), TCP.class.getSimpleName());
        }
    }

}
