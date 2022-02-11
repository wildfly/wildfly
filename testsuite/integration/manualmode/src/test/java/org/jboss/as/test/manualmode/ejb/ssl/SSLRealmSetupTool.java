/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.manualmode.ejb.ssl;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALLOW_RESOURCE_SERVICE_RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROTOCOL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;

import java.net.URL;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.test.integration.security.common.SecurityTestConstants;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.junit.Assert;
import org.wildfly.test.security.common.AbstractElytronSetupTask;
import org.wildfly.test.security.common.elytron.ConfigurableElement;
import org.wildfly.test.security.common.elytron.CredentialReference;
import org.wildfly.test.security.common.elytron.Path;
import org.wildfly.test.security.common.elytron.SimpleKeyManager;
import org.wildfly.test.security.common.elytron.SimpleKeyStore;
import org.wildfly.test.security.common.elytron.SimpleServerSslContext;
import org.wildfly.test.security.common.elytron.SimpleTrustManager;

/**
 * Setup for ssl Jakarta Enterprise Beans remote connection.
 * Keystore created on basis of tutorial at https://community.jboss.org/wiki/SSLSetup.
 *
 * @author Ondrej Chaloupka
 * @author Jan Martiska
 */
public class SSLRealmSetupTool {

    private static final Logger log = Logger.getLogger(SSLRealmSetupTool.class);

    // server config stuff
    public static final String SSL_CONTEXT_NAME = "SSLEJBContext";

    // server SSL stuff
    public static final String SERVER_KEYSTORE_PASSWORD = "JBossPassword";
    public static final String SERVER_KEYSTORE_FILENAME = "jbossServer.keystore";

    // SSL stuff for both
    public static final String KEYSTORES_RELATIVE_PATH = "ejb3/ssl";

    /* ----------------- GETTING ModelNode addresses ----------------- */

    public static ModelNode getRemotingConnectorAddress() {
        ModelNode address = new ModelNode();
        address.add(SUBSYSTEM, "remoting");
        address.add("http-connector", "https-remoting-connector");
        address.protect();
        return address;
    }

    public static ModelNode getRemoteAddress() {
        ModelNode address = new ModelNode();
        address.add(SUBSYSTEM, "ejb3");
        address.add("service", "remote");
        address.protect();
        return address;
    }

    /* ----------------- SetupTask methods ----------------- */

    /**
     * <security-realm name="SSLRealm">
     * <server-identities>
     * <ssl>
     * <keystore path="$resources/ejb3/ssl/jbossServer.keystore" keystore-password="JBossPassword"/>
     * </ssl>
     * </server-identities>
     * <authentication>
     * <truststore path="$resources/ejb3/ssl/jbossServer.keystore" keystore-password="JBossPassword"/>
     * </authentication>
     * </security-realm>
     */
    public static void setup(final ManagementClient managementClient) throws Exception {

        new SslContextSetup().setup(managementClient.getControllerClient());

        // make the https connector use our SSL realm
        ModelNode operation = new ModelNode();
        operation.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        operation.get(OP_ADDR).add(SUBSYSTEM, "undertow");
        operation.get(OP_ADDR).add("server", "default-server");
        operation.get(OP_ADDR).add("https-listener", "https");
        operation.get(NAME).set("ssl-context");
        operation.get(VALUE).set(SSL_CONTEXT_NAME);
        ModelNode result = managementClient.getControllerClient().execute(operation);
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());

        //add remoting connector
        operation = new ModelNode();
        operation.get(OP_ADDR).set(SSLRealmSetupTool.getRemotingConnectorAddress());
        operation.get(OP).set(ADD);
        operation.get("sasl-authentication-factory").set("application-sasl-authentication");
        operation.get(PROTOCOL).set("https-remoting");
        operation.get("connector-ref").set("https");
        operation.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);
        result = managementClient.getControllerClient().execute(operation);
        log.debugf("Adding HTTPS connector", result);
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());

        // add remoting connector to connectors list <remote connectors="..."/>
        operation = new ModelNode();
        operation.get(OP_ADDR).set(SSLRealmSetupTool.getRemoteAddress());
        operation.get(OP).set("list-add");
        operation.get(NAME).set("connectors");
        operation.get(VALUE).set("https-remoting-connector");
        operation.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);
        result = managementClient.getControllerClient().execute(operation);
        log.debugf("Adding connector to remote connectors list", result);
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());

    }

    public static void tearDown(final ManagementClient managementClient, ContainerController controller) throws Exception {

        // remove remoting connector from connectors list <remote connectors="..."/>
        ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).set(SSLRealmSetupTool.getRemoteAddress());
        operation.get(OP).set("list-remove");
        operation.get(NAME).set("connectors");
        operation.get(VALUE).set("https-remoting-connector");
        operation.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);
        ModelNode result = managementClient.getControllerClient().execute(operation);
        log.debugf("remove connector from remote connectors", result);
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
        ServerReload.executeReloadAndWaitForCompletion(managementClient);

        operation = new ModelNode();
        operation.get(OP_ADDR).set(SSLRealmSetupTool.getRemotingConnectorAddress());
        operation.get(OP).set(REMOVE);
        operation.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);
        result = managementClient.getControllerClient().execute(operation);
        log.debugf("remove HTTPS connector", result);
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
        ServerReload.executeReloadAndWaitForCompletion(managementClient);

        // restore https connector to previous state
        operation = new ModelNode();
        operation.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        operation.get(OP_ADDR).add(SUBSYSTEM, "undertow");
        operation.get(OP_ADDR).add("server", "default-server");
        operation.get(OP_ADDR).add("https-listener", "https");
        operation.get(NAME).set("ssl-context");
        operation.get(VALUE).set("applicationSSC");
        operation.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);
        result = managementClient.getControllerClient().execute(operation);
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
        ServerReload.executeReloadAndWaitForCompletion(managementClient);

        new SslContextSetup().tearDown(managementClient.getControllerClient());

        controller.stop(SSLEJBRemoteClientTestCase.DEFAULT_JBOSSAS);
    }

    private static class SslContextSetup extends AbstractElytronSetupTask {

        @Override
        protected void setup(ModelControllerClient modelControllerClient) throws Exception {
            super.setup(modelControllerClient);
        }

        @Override
        protected void tearDown(ModelControllerClient modelControllerClient) throws Exception {
            super.tearDown(modelControllerClient);
        }

        @Override
        protected ConfigurableElement[] getConfigurableElements() {
            URL keystoreResource = Thread.currentThread().getContextClassLoader().getResource(KEYSTORES_RELATIVE_PATH + "/" + SERVER_KEYSTORE_FILENAME);
            return new ConfigurableElement[] {
                    SimpleKeyStore.builder().withName(SSL_CONTEXT_NAME + SecurityTestConstants.SERVER_KEYSTORE)
                            .withPath(Path.builder().withPath(keystoreResource.getPath()).build())
                            .withCredentialReference(CredentialReference.builder().withClearText(SERVER_KEYSTORE_PASSWORD).build())
                            .build(),
                    SimpleKeyStore.builder().withName(SSL_CONTEXT_NAME + SecurityTestConstants.SERVER_TRUSTSTORE)
                            .withPath(Path.builder().withPath(keystoreResource.getPath()).build())
                            .withCredentialReference(CredentialReference.builder().withClearText(SERVER_KEYSTORE_PASSWORD).build())
                            .build(),
                    SimpleKeyManager.builder().withName(SSL_CONTEXT_NAME)
                            .withKeyStore(SSL_CONTEXT_NAME + SecurityTestConstants.SERVER_KEYSTORE)
                            .withCredentialReference(CredentialReference.builder().withClearText(SERVER_KEYSTORE_PASSWORD).build())
                            .build(),
                    SimpleTrustManager.builder().withName(SSL_CONTEXT_NAME)
                            .withKeyStore(SSL_CONTEXT_NAME + SecurityTestConstants.SERVER_TRUSTSTORE)
                            .build(),
                    SimpleServerSslContext.builder().withName(SSL_CONTEXT_NAME)
                            .withKeyManagers(SSL_CONTEXT_NAME)
                            .withTrustManagers(SSL_CONTEXT_NAME)
                            .withProtocols("TLSv1.2")
                            .withNeedClientAuth(true)
                            .build()
            };
        }
    }

}