/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.cluster.modcluster;

import static org.jboss.as.controller.client.helpers.ClientConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.client.helpers.ClientConstants.INCLUDE_RUNTIME;
import static org.jboss.as.controller.client.helpers.ClientConstants.OUTCOME;
import static org.jboss.as.controller.client.helpers.ClientConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.client.helpers.ClientConstants.STATUS;
import static org.jboss.as.controller.client.helpers.ClientConstants.SUCCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.time.Duration;
import java.util.Set;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.awaitility.Awaitility;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase;
import org.jboss.as.test.shared.ManagementServerSetupTask;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Verifies mutual TLS authentication of the MCMP channel between the mod_cluster subsystem and the Undertow-based mod_cluster load balancer.
 * The load balancer accepts MCMP traffic only over an HTTPS listener which requires a client certificate present in its trust store.
 * Each test configures the worker node with a different client SSL context, leaving all other configuration the same on the worker.
 * <p>
 * Be aware that since a node can fail to register for any reason, not only due to a rejected certificate.
 * Thus, the rejection tests are only useful in conjunction with {@link #clientWithTrustedCertificateIsRegistered()} test,
 * which verifies that the very *SAME* setup otherwise registers the node.
 *
 * @author Radoslav Husar
 */
@RunAsClient
@ExtendWith(ArquillianExtension.class)
// n.b. the LB cannot be configured via ServerSetupTask; it's configured using ModClusterMutualTLSTestCase#createLBClient
@ServerSetup(ModClusterMutualTLSTestCase.MutualTLSServerSetupTask.class)
public class ModClusterMutualTLSTestCase extends AbstractClusteringTestCase {

    private static final String MODULE_NAME = ModClusterMutualTLSTestCase.class.getSimpleName();
    private static final String DEPLOYMENT_NAME = MODULE_NAME + ".war";
    private static final String CONTENT = "Hello World";
    private static final String RESOURCE_NOT_FOUND_CODE = "WFLYCTL0216";

    private static final Duration REGISTRATION_TIMEOUT = Duration.ofMillis(TimeoutUtil.adjust(30_000));
    // Spans multiple status intervals, during each of which a rejected node attempts to connect again
    private static final Duration REJECTION_PERIOD = Duration.ofMillis(TimeoutUtil.adjust(5_000));

    private static final String TRUSTED_CLIENT_SSL_CONTEXT = "mcmpTrustedCSC";
    private static final String UNTRUSTED_CLIENT_SSL_CONTEXT = "mcmpUntrustedCSC";
    private static final String NO_CERTIFICATE_CLIENT_SSL_CONTEXT = "mcmpNoCertificateCSC";

    @Deployment(name = DEPLOYMENT_1, testable = false, managed = false)
    @TargetsContainer(NODE_1)
    public static WebArchive deployment() {
        // n.b. use a static page rather than a servlet creating a session, since the session would need to be drained on undeploy
        return ShrinkWrap.create(WebArchive.class, DEPLOYMENT_NAME)
                .add(new StringAsset(CONTENT), "index.html");
    }

    public ModClusterMutualTLSTestCase() {
        // n.b. the deployment is managed by the tests since deploying it is what triggers the server setup task
        super(Set.of(LOAD_BALANCER_1, NODE_1), Set.of());
    }

    @Test
    void clientWithTrustedCertificateIsRegistered(@ArquillianResource @OperateOnDeployment(DEPLOYMENT_1) ManagementClient client) throws Exception {
        deployer.deploy(DEPLOYMENT_1);
        try {
            useClientSSLContext(client, TRUSTED_CLIENT_SSL_CONTEXT);

            assertContextEnabled();
            assertRequestIsProxied();
        } finally {
            deployer.undeploy(DEPLOYMENT_1);
        }
    }

    @Test
    void clientWithUntrustedCertificateIsRejected(@ArquillianResource @OperateOnDeployment(DEPLOYMENT_1) ManagementClient client) throws Exception {
        deployer.deploy(DEPLOYMENT_1);
        try {
            useClientSSLContext(client, UNTRUSTED_CLIENT_SSL_CONTEXT);

            assertNodeRemainsUnregistered();
        } finally {
            deployer.undeploy(DEPLOYMENT_1);
        }
    }

    @Test
    void clientWithoutCertificateIsRejected(@ArquillianResource @OperateOnDeployment(DEPLOYMENT_1) ManagementClient client) throws Exception {
        deployer.deploy(DEPLOYMENT_1);
        try {
            useClientSSLContext(client, NO_CERTIFICATE_CLIENT_SSL_CONTEXT);

            assertNodeRemainsUnregistered();
        } finally {
            deployer.undeploy(DEPLOYMENT_1);
        }
    }

    private static void useClientSSLContext(ManagementClient client, String sslContext) throws Exception {
        ModelNode op = Util.getWriteAttributeOperation(PathAddress.parseCLIStyleAddress("/subsystem=modcluster/proxy=default"), "ssl-context", sslContext);

        ModelNode result = client.getControllerClient().execute(op);
        assertEquals(SUCCESS, result.get(OUTCOME).asString(), result.toString());
        ServerReload.executeReloadAndWaitForCompletion(client);
    }

    private static void assertContextEnabled() throws Exception {
        ModelNode op = Util.createOperation(READ_RESOURCE_OPERATION, PathAddress.parseCLIStyleAddress("/subsystem=undertow/configuration=filter/mod-cluster=load-balancer/balancer=mycluster/node=" + NODE_1 + "/context=\\/" + MODULE_NAME));
        op.get(INCLUDE_RUNTIME).set(true);

        try (ModelControllerClient client = createLBClient()) {
            Awaitility.await("context to become enabled on load balancer")
                    .atMost(REGISTRATION_TIMEOUT)
                    .pollInterval(Duration.ofMillis(100))
                    .untilAsserted(() -> {
                        ModelNode result = client.execute(op);
                        assertEquals(SUCCESS, result.get(OUTCOME).asString(), result.toString());
                        assertEquals(ModClusterContextStatus.ENABLED.toString(), result.get(RESULT).get(STATUS).asString());
                    });
        }
    }

    private static void assertRequestIsProxied() throws Exception {
        URI uri = new URI("http", null, TestSuiteEnvironment.getHttpAddress(), TestSuiteEnvironment.getHttpPort() + LOAD_BALANCER_1_PORT_OFFSET, "/" + MODULE_NAME + "/", null, null);

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            // UNDERTOW-1585 nodes are added in ERROR state, so requests fail until the load balancer receives the first status of the node
            Awaitility.await("request to be proxied by load balancer")
                    .atMost(REGISTRATION_TIMEOUT)
                    .pollInterval(Duration.ofMillis(100))
                    .untilAsserted(() -> {
                        try (CloseableHttpResponse response = client.execute(new HttpGet(uri))) {
                            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
                            assertEquals(CONTENT, EntityUtils.toString(response.getEntity()));
                        }
                    });
        }
    }

    /**
     * Waits for any registration established by a previous test to be removed, and verifies that the node then remains absent from the load balancer.
     */
    private static void assertNodeRemainsUnregistered() throws Exception {
        ModelNode op = Util.createOperation(READ_RESOURCE_OPERATION, PathAddress.parseCLIStyleAddress("/subsystem=undertow/configuration=filter/mod-cluster=load-balancer/balancer=mycluster/node=" + NODE_1));
        op.get(INCLUDE_RUNTIME).set(true);

        try (ModelControllerClient client = createLBClient()) {
            Awaitility.await("node to remain unregistered on load balancer")
                    .during(REJECTION_PERIOD)
                    .atMost(REGISTRATION_TIMEOUT.plus(REJECTION_PERIOD))
                    .pollInterval(Duration.ofMillis(100))
                    .untilAsserted(() -> {
                        ModelNode result = client.execute(op);
                        // Require the specific failure so that an unrelated management failure cannot pass as a rejected node
                        assertTrue(result.get(FAILURE_DESCRIPTION).asString("").contains(RESOURCE_NOT_FOUND_CODE), result.toString());
                    });
        }
    }

    // n.b. unlike the node's client, the load balancer's client cannot be injected as an Arquillian resource since it has no deployment
    private static ModelControllerClient createLBClient() {
        return TestSuiteEnvironment.getModelControllerClient(null, TestSuiteEnvironment.getServerAddress(), TestSuiteEnvironment.getServerPort() + LOAD_BALANCER_1_PORT_OFFSET);
    }

    /**
     * Generates the key material and configures both servers.
     * The Arquillian server setup observer runs a task only for the container targeted by a deployment,
     * and since the load balancer has no deployment, it is configured here using its own management client.
     */
    static class MutualTLSServerSetupTask implements ServerSetupTask {
        private final ServerSetupTask nodeSetupTask = new NodeServerSetupTask();
        private final ServerSetupTask loadBalancerSetupTask = new LoadBalancerServerSetupTask();
        private ManagementClient lbManagementClient;

        @Override
        public void setup(ManagementClient client, String containerId) throws Exception {
            // n.b. the node generates the key stores that the load balancer loads
            this.nodeSetupTask.setup(client, containerId);
            // n.b. the server snapshot retains this client in order to restore the load balancer during tear down
            this.lbManagementClient = new ManagementClient(createLBClient(), TestSuiteEnvironment.getServerAddress(), TestSuiteEnvironment.getServerPort() + LOAD_BALANCER_1_PORT_OFFSET, "remote+http");
            this.loadBalancerSetupTask.setup(this.lbManagementClient, LOAD_BALANCER_1);
        }

        @Override
        public void tearDown(ManagementClient client, String containerId) throws Exception {
            try (ManagementClient lbManagementClient = this.lbManagementClient) {
                this.loadBalancerSetupTask.tearDown(lbManagementClient, LOAD_BALANCER_1);
            } finally {
                this.nodeSetupTask.tearDown(client, containerId);
            }
        }
    }

    static class NodeServerSetupTask extends ManagementServerSetupTask {
        NodeServerSetupTask() {
            super(NODE_1, createContainerConfigurationBuilder()
                    .setupScript(createScriptBuilder()
                            // n.b. cannot batch: key stores must be started before their key pairs can be generated!
                            // n.b. key store files are not restored by the server snapshot and generating or importing an existing alias fails, so each store is cleared first.
                            // Elytron has no operation to clear a key store, but removing an alias that does not exist succeeds; exporting a certificate overwrites an existing file.
                            // Identity of the load balancer
                            .add("/subsystem=elytron/key-store=mcmpProxyKS:add(path=../../../mcmp-proxy.keystore.pkcs12, relative-to=jboss.server.config.dir, credential-reference={clear-text=secret}, type=PKCS12)")
                            .add("/subsystem=elytron/key-store=mcmpProxyKS:remove-alias(alias=proxy)")
                            .add("/subsystem=elytron/key-store=mcmpProxyKS:generate-key-pair(alias=proxy, algorithm=RSA, key-size=2048, validity=365, credential-reference={clear-text=secret}, distinguished-name=\"CN=mcmp-proxy\")")
                            .add("/subsystem=elytron/key-store=mcmpProxyKS:store")
                            .add("/subsystem=elytron/key-store=mcmpProxyKS:export-certificate(alias=proxy, path=../../../mcmp-proxy.pem, relative-to=jboss.server.config.dir, pem=true)")
                            // Identity of the worker node, which the load balancer trusts
                            .add("/subsystem=elytron/key-store=mcmpClientKS:add(path=../../../mcmp-client.keystore.pkcs12, relative-to=jboss.server.config.dir, credential-reference={clear-text=secret}, type=PKCS12)")
                            .add("/subsystem=elytron/key-store=mcmpClientKS:remove-alias(alias=client)")
                            .add("/subsystem=elytron/key-store=mcmpClientKS:generate-key-pair(alias=client, algorithm=RSA, key-size=2048, validity=365, credential-reference={clear-text=secret}, distinguished-name=\"CN=mcmp-client\")")
                            .add("/subsystem=elytron/key-store=mcmpClientKS:store")
                            .add("/subsystem=elytron/key-store=mcmpClientKS:export-certificate(alias=client, path=../../../mcmp-client.pem, relative-to=jboss.server.config.dir, pem=true)")
                            // Identity whose certificate is not distributed to the trust store of the load balancer
                            .add("/subsystem=elytron/key-store=mcmpUntrustedKS:add(path=../../../mcmp-untrusted.keystore.pkcs12, relative-to=jboss.server.config.dir, credential-reference={clear-text=secret}, type=PKCS12)")
                            .add("/subsystem=elytron/key-store=mcmpUntrustedKS:remove-alias(alias=untrusted)")
                            .add("/subsystem=elytron/key-store=mcmpUntrustedKS:generate-key-pair(alias=untrusted, algorithm=RSA, key-size=2048, validity=365, credential-reference={clear-text=secret}, distinguished-name=\"CN=mcmp-untrusted\")")
                            .add("/subsystem=elytron/key-store=mcmpUntrustedKS:store")
                            // Distribute the public certificates to the respective trust stores
                            .add("/subsystem=elytron/key-store=mcmpProxyTS:add(path=../../../mcmp-proxy.truststore.pkcs12, relative-to=jboss.server.config.dir, credential-reference={clear-text=secret}, type=PKCS12)")
                            .add("/subsystem=elytron/key-store=mcmpProxyTS:remove-alias(alias=client)")
                            .add("/subsystem=elytron/key-store=mcmpProxyTS:import-certificate(alias=client, path=../../../mcmp-client.pem, relative-to=jboss.server.config.dir, credential-reference={clear-text=secret}, trust-cacerts=true, validate=false)")
                            .add("/subsystem=elytron/key-store=mcmpProxyTS:store")
                            .add("/subsystem=elytron/key-store=mcmpClientTS:add(path=../../../mcmp-client.truststore.pkcs12, relative-to=jboss.server.config.dir, credential-reference={clear-text=secret}, type=PKCS12)")
                            .add("/subsystem=elytron/key-store=mcmpClientTS:remove-alias(alias=proxy)")
                            .add("/subsystem=elytron/key-store=mcmpClientTS:import-certificate(alias=proxy, path=../../../mcmp-proxy.pem, relative-to=jboss.server.config.dir, credential-reference={clear-text=secret}, trust-cacerts=true, validate=false)")
                            .add("/subsystem=elytron/key-store=mcmpClientTS:store")
                            .startBatch()
                            .add("/subsystem=elytron/key-manager=mcmpClientKM:add(key-store=mcmpClientKS, credential-reference={clear-text=secret})")
                            .add("/subsystem=elytron/key-manager=mcmpUntrustedKM:add(key-store=mcmpUntrustedKS, credential-reference={clear-text=secret})")
                            .add("/subsystem=elytron/trust-manager=mcmpClientTM:add(key-store=mcmpClientTS)")
                            .add("/subsystem=elytron/client-ssl-context=%s:add(key-manager=mcmpClientKM, trust-manager=mcmpClientTM)", TRUSTED_CLIENT_SSL_CONTEXT)
                            .add("/subsystem=elytron/client-ssl-context=%s:add(key-manager=mcmpUntrustedKM, trust-manager=mcmpClientTM)", UNTRUSTED_CLIENT_SSL_CONTEXT)
                            .add("/subsystem=elytron/client-ssl-context=%s:add(trust-manager=mcmpClientTM)", NO_CERTIFICATE_CLIENT_SSL_CONTEXT)
                            .add("/socket-binding-group=standard-sockets/remote-destination-outbound-socket-binding=proxy1:add(host=localhost, port=%d)", LOAD_BALANCER_1_MCMP_PORT)
                            .add("/subsystem=modcluster/proxy=default:write-attribute(name=advertise, value=false)")
                            .add("/subsystem=modcluster/proxy=default:write-attribute(name=status-interval, value=1)")
                            .add("/subsystem=modcluster/proxy=default:list-add(name=proxies, value=proxy1)")
                            .add("/subsystem=modcluster/proxy=default:write-attribute(name=ssl-context, value=%s)", NO_CERTIFICATE_CLIENT_SSL_CONTEXT)
                            .endBatch()
                            .build())
                    .build());
        }
    }

    static class LoadBalancerServerSetupTask extends ManagementServerSetupTask {
        LoadBalancerServerSetupTask() {
            super(LOAD_BALANCER_1, createContainerConfigurationBuilder()
                    .setupScript(createScriptBuilder()
                            .startBatch()
                            .add("/subsystem=elytron/key-store=mcmpProxyKS:add(path=../../../mcmp-proxy.keystore.pkcs12, relative-to=jboss.server.config.dir, credential-reference={clear-text=secret}, type=PKCS12)")
                            .add("/subsystem=elytron/key-manager=mcmpProxyKM:add(key-store=mcmpProxyKS, credential-reference={clear-text=secret})")
                            .add("/subsystem=elytron/key-store=mcmpProxyTS:add(path=../../../mcmp-proxy.truststore.pkcs12, relative-to=jboss.server.config.dir, credential-reference={clear-text=secret}, type=PKCS12)")
                            .add("/subsystem=elytron/trust-manager=mcmpProxyTM:add(key-store=mcmpProxyTS)")
                            .add("/subsystem=elytron/server-ssl-context=mcmpProxySSC:add(key-manager=mcmpProxyKM, trust-manager=mcmpProxyTM, need-client-auth=true)")
                            // Replace the plain text listener for MCMP traffic
                            .add("/subsystem=undertow/server=default-server/http-listener=management:remove")
                            .add("/subsystem=undertow/server=default-server/https-listener=management:add(socket-binding=mcmp-management, ssl-context=mcmpProxySSC)")
                            .endBatch()
                            .build())
                    .build());
        }
    }
}
