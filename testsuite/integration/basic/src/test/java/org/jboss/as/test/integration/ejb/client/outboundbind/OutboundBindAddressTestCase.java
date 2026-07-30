/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.client.outboundbind;

import static org.junit.Assert.assertTrue;

import javax.naming.Context;
import javax.naming.InitialContext;
import java.util.Properties;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.naming.client.WildFlyInitialContextFactory;

@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(OutboundBindAddressTestCase.Setup.class)
public class OutboundBindAddressTestCase {

    static final String SERVER_MODULE = "outbound-bind-address-server";
    static final String CLIENT_MODULE = "outbound-bind-address-client";

    private static final String BIND_ADDRESS = "127.0.0.5";
    private static final int    BIND_PORT    = 12354;

    private static final String SOCKET_NAME     = "self-ejb-socket";
    private static final String CONNECTION_NAME = "self-remote-ejb-connection";

    // -------------------------------------------------------------------------
    // Server setup — add outbound socket + connection + outbound-bind-address
    // -------------------------------------------------------------------------

    static class Setup implements ServerSetupTask {

        @Override
        public void setup(ManagementClient mc, String containerId) throws Exception {
            ModelControllerClientHelper client = new ModelControllerClientHelper(mc);

            // remote-destination-outbound-socket-binding → 127.0.0.1:8080 (literal IPv4 to ensure outbound-bind-address match applies in both IPv4 and IPv6 test modes)
            ModelNode addSocket = Util.createAddOperation(
                    PathAddress.parseCLIStyleAddress(
                            "/socket-binding-group=standard-sockets" +
                            "/remote-destination-outbound-socket-binding=" + SOCKET_NAME));
            addSocket.get("host").set("127.0.0.1");
            addSocket.get("port").set(8080);
            client.execute(addSocket);

            // remote-outbound-connection
            ModelNode addConn = Util.createAddOperation(
                    PathAddress.parseCLIStyleAddress(
                            "/subsystem=remoting/remote-outbound-connection=" + CONNECTION_NAME));
            addConn.get("outbound-socket-binding-ref").set(SOCKET_NAME);
            addConn.get("protocol").set("http-remoting");
            client.execute(addConn);

            // outbound-bind-address on the default IO worker
            ModelNode addBind = Util.createAddOperation(
                    PathAddress.parseCLIStyleAddress(
                            "/subsystem=io/worker=default/outbound-bind-address=default"));
            addBind.get("bind-address").set(BIND_ADDRESS);
            addBind.get("bind-port").set(BIND_PORT);
            addBind.get("match").set("0.0.0.0/0");
            client.execute(addBind);

            ServerReload.reloadIfRequired(mc);
        }

        @Override
        public void tearDown(ManagementClient mc, String containerId) throws Exception {
            ModelControllerClientHelper client = new ModelControllerClientHelper(mc);

            client.execute(Util.createRemoveOperation(
                    PathAddress.parseCLIStyleAddress(
                            "/subsystem=io/worker=default/outbound-bind-address=default")));
            client.execute(Util.createRemoveOperation(
                    PathAddress.parseCLIStyleAddress(
                            "/subsystem=remoting/remote-outbound-connection=" + CONNECTION_NAME)));
            client.execute(Util.createRemoveOperation(
                    PathAddress.parseCLIStyleAddress(
                            "/socket-binding-group=standard-sockets" +
                            "/remote-destination-outbound-socket-binding=" + SOCKET_NAME)));

            ServerReload.reloadIfRequired(mc);
        }

        private static class ModelControllerClientHelper {
            private final ManagementClient mc;

            ModelControllerClientHelper(ManagementClient mc) {
                this.mc = mc;
            }

            void execute(ModelNode op) throws Exception {
                ModelNode result = mc.getControllerClient().execute(op);
                if (!"success".equals(result.get("outcome").asString())) {
                    throw new RuntimeException("Operation failed: " + result);
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Deployments
    // -------------------------------------------------------------------------

    @Deployment(name = SERVER_MODULE, testable = false, order = 1)
    public static JavaArchive serverDeployment() {
        return ShrinkWrap.create(JavaArchive.class, SERVER_MODULE + ".jar")
                .addClasses(SourceAddressBean.class, SourceAddressRemote.class);
    }

    @Deployment(name = CLIENT_MODULE, testable = false, order = 2)
    public static JavaArchive clientDeployment() {
        return ShrinkWrap.create(JavaArchive.class, CLIENT_MODULE + ".jar")
                .addClasses(CallerBean.class, CallerRemote.class, SourceAddressRemote.class)
                .addAsManifestResource(
                        OutboundBindAddressTestCase.class.getPackage(),
                        "jboss-ejb-client.xml", "jboss-ejb-client.xml");
    }

    // -------------------------------------------------------------------------
    // Test
    // -------------------------------------------------------------------------

    @Test
    @InSequence(10)
    @OperateOnDeployment(CLIENT_MODULE)
    public void testOutboundBindAddressIsHonoured() throws Exception {
        CallerRemote caller = lookupCaller();
        String sourceAddress = caller.callAndGetSourceAddress();

        assertTrue(
                "Expected source address to contain " + BIND_ADDRESS + ":" + BIND_PORT +
                " but server saw: " + sourceAddress,
                sourceAddress.contains(BIND_ADDRESS + ":" + BIND_PORT));
    }

    private CallerRemote lookupCaller() throws Exception {
        Properties props = new Properties();
        props.put(Context.INITIAL_CONTEXT_FACTORY, WildFlyInitialContextFactory.class.getName());
        props.put(Context.PROVIDER_URL, "remote+http://localhost:8080");
        InitialContext ctx = new InitialContext(props);
        return (CallerRemote) ctx.lookup(
                "ejb:/" + CLIENT_MODULE + "/CallerBean!" +
                CallerRemote.class.getName() + "?stateful");
    }
}
