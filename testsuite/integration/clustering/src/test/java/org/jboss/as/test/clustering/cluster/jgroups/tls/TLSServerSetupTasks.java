/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.cluster.jgroups.tls;

import static org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase.NODE_1;
import static org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase.NODE_1_2;
import static org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase.NODE_1_2_3;

import java.util.Set;

import org.jboss.as.test.shared.ManagementServerSetupTask;
import org.jgroups.protocols.TCP;
import org.jgroups.protocols.TCP_NIO2;

/**
 * Utility interface containing {@link org.jboss.as.arquillian.api.ServerSetupTask}s for setting up TLS/SSL for JGroups channels.
 *
 * @author Radoslav Husar
 */
public interface TLSServerSetupTasks {

    /**
     * Server setup task that uses Elytron to create physical key and trust store files.
     */
    class PhysicalKeyStoresServerSetupTask extends ManagementServerSetupTask {
        public PhysicalKeyStoresServerSetupTask(Set<String> containers) {
            super(createContainerSetConfigurationBuilder()
                    .addContainers(containers, createContainerConfigurationBuilder()
                            .setupScript(createScriptBuilder()
                                    // n.b. we cannot use a batch here since we need to run the 'generate-key-pair' operation on already running store
                                    // WFLYELY00007: The required service 'service org.wildfly.security.key-store.jgroupsKS' is not UP, it is currently 'STARTING'."}}

                                    // Setup and populate shared KS
                                    .add("/subsystem=elytron/key-store=jgroupsKS-shared:add(path=%sserver.keystore.pkcs12, relative-to=jboss.server.config.dir, credential-reference={clear-text=secret}, type=PKCS12)", containers.size() == 1 ? "../../../" : "")
                                    .add("/subsystem=elytron/key-store=jgroupsKS-shared:generate-key-pair(alias=localhost, algorithm=RSA, key-size=2048, validity=365, credential-reference={clear-text=secret}, distinguished-name=\"CN=localhost\")")
                                    .add("/subsystem=elytron/key-store=jgroupsKS-shared:store")

                                    // Export pem certificate
                                    .add("/subsystem=elytron/key-store=jgroupsKS-shared:export-certificate(alias=localhost, path=server.keystore.pem, relative-to=jboss.server.config.dir, pem=true)")

                                    // Setup and populate shared TS
                                    .add("/subsystem=elytron/key-store=jgroupsTS-shared:add(path=%sserver.truststore.pkcs12, relative-to=jboss.server.config.dir, credential-reference={clear-text=secret}, type=PKCS12)", containers.size() == 1 ? "../../../" : "")
                                    .add("/subsystem=elytron/key-store=jgroupsTS-shared:import-certificate(alias=client, path=server.keystore.pem, relative-to=jboss.server.config.dir, credential-reference={clear-text=secret}, trust-cacerts=true, validate=false)")
                                    .add("/subsystem=elytron/key-store=jgroupsTS-shared:store")
                                    .build())
                            .tearDownScript(createScriptBuilder()
                                    // n.b. clearing these stores to avoid the following issue when reusing these setup tasks or rerunning the test sans clean
                                    // WFLYELY01036: Alias 'localhost' already exists in KeyStore [ \"WFLYELY01036: Alias 'localhost' already exists in KeyStore\" ]"

                                    // Remove certificates from the temporary physical file store
                                    .add("/subsystem=elytron/key-store=jgroupsKS-shared:remove-alias(alias=localhost)")
                                    .add("/subsystem=elytron/key-store=jgroupsKS-shared:store")
                                    .add("/subsystem=elytron/key-store=jgroupsTS-shared:remove-alias(alias=client)")
                                    .add("/subsystem=elytron/key-store=jgroupsTS-shared:store")

                                    // Cleanup temporary store model resources
                                    .add("/subsystem=elytron/key-store=jgroupsTS-shared:remove")
                                    .add("/subsystem=elytron/key-store=jgroupsKS-shared:remove")
                                    .build())
                            .build())
                    .build());
        }
    }

    /**
     * Server setup task that uses Elytron to create a shared physical key and trust store files containing a generated pre-shared key.
     */
    class SharedPhysicalKeyStoresServerSetupTask extends PhysicalKeyStoresServerSetupTask {
        public SharedPhysicalKeyStoresServerSetupTask() {
            super(Set.of(NODE_1));
        }
    }

    /**
     * Server setup task that uses Elytron to create a physical key and trust store for each server.
     */
    class PhysicalKeyStoresServerSetupTask_NODE_1_2 extends PhysicalKeyStoresServerSetupTask {
        public PhysicalKeyStoresServerSetupTask_NODE_1_2() {
            super(NODE_1_2);
        }
    }

    class SecureJGroupsTransportServerSetupTask extends ManagementServerSetupTask {
        public SecureJGroupsTransportServerSetupTask(Set<String> nodes, String tp, boolean sharedKS) {
            super(createContainerSetConfigurationBuilder()
                    .addContainers(nodes, createContainerConfigurationBuilder()
                            .setupScript(createScriptBuilder()
                                    .startBatch()
                                    .add("/subsystem=elytron/key-store=jgroupsKS:add(path=%sserver.keystore.pkcs12, relative-to=jboss.server.config.dir, credential-reference={clear-text=secret}, type=PKCS12)", sharedKS ? "../../../" : "")
                                    .add("/subsystem=elytron/key-manager=jgroupsKM:add(key-store=jgroupsKS, credential-reference={clear-text=secret})")
                                    .add("/subsystem=elytron/key-store=jgroupsTS:add(path=%sserver.truststore.pkcs12, relative-to=jboss.server.config.dir, credential-reference={clear-text=secret}, type=PKCS12)", sharedKS ? "../../../" : "")
                                    .add("/subsystem=elytron/trust-manager=jgroupsTM:add(key-store=jgroupsTS)")
                                    .add("/subsystem=elytron/client-ssl-context=jgroupsCSC:add(key-manager=jgroupsKM, trust-manager=jgroupsTM, protocols=[\"TLSv1.2\"])")
                                    .add("/subsystem=elytron/server-ssl-context=jgroupsSSC:add(key-manager=jgroupsKM, trust-manager=jgroupsTM, protocols=[\"TLSv1.2\"], authentication-optional=true, want-client-auth=true, need-client-auth=true)")
                                    .add("/subsystem=jgroups/stack=tcp/transport=%s:write-attribute(name=client-context, value=jgroupsCSC)", tp)
                                    .add("/subsystem=jgroups/stack=tcp/transport=%s:write-attribute(name=server-context, value=jgroupsSSC)", tp)
                                    .endBatch()
                                    .build())
                            .tearDownScript(createScriptBuilder()
                                    .startBatch()
                                    .add("/subsystem=jgroups/stack=tcp/transport=%s:undefine-attribute(name=server-context)", tp)
                                    .add("/subsystem=jgroups/stack=tcp/transport=%s:undefine-attribute(name=client-context)", tp)
                                    .add("/subsystem=elytron/server-ssl-context=jgroupsSSC:remove")
                                    .add("/subsystem=elytron/client-ssl-context=jgroupsCSC:remove")
                                    .add("/subsystem=elytron/trust-manager=jgroupsTM:remove")
                                    .add("/subsystem=elytron/key-store=jgroupsTS:remove")
                                    .add("/subsystem=elytron/key-manager=jgroupsKM:remove")
                                    .add("/subsystem=elytron/key-store=jgroupsKS:remove")
                                    .endBatch()
                                    .build())
                            .build())
                    .build());
        }
    }

    class UnsharedSecureJGroupsTransportServerSetupTask_NODE_1_2 extends SecureJGroupsTransportServerSetupTask {
        public UnsharedSecureJGroupsTransportServerSetupTask_NODE_1_2() {
            super(NODE_1_2, TCP.class.getSimpleName(), false);
        }
    }

    class SharedStoreSecureJGroupsTransportServerSetupTask_NODE_1_2_3 extends SecureJGroupsTransportServerSetupTask {
        public SharedStoreSecureJGroupsTransportServerSetupTask_NODE_1_2_3() {
            super(NODE_1_2_3, TCP.class.getSimpleName(), true);
        }
    }

    class SharedStoreSecureJGroupsTCP_NIO2TransportServerSetupTask_NODE_1_2 extends SecureJGroupsTransportServerSetupTask {
        public SharedStoreSecureJGroupsTCP_NIO2TransportServerSetupTask_NODE_1_2() {
            super(NODE_1_2, TCP_NIO2.class.getSimpleName(), true);
        }
    }


}
