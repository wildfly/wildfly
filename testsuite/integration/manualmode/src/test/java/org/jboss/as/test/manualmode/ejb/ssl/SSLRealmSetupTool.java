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

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.junit.Assert;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;

/**
 * Setup for ssl ejb remote connection.
 * Keystore created on basis of tutorial at https://community.jboss.org/wiki/SSLSetup.
 *
 * @author Ondrej Chaloupka
 * @author Jan Martiska
 */
public class SSLRealmSetupTool {

    private static final Logger log = Logger.getLogger(SSLRealmSetupTool.class);

    // server config stuff
    public static final String SECURITY_REALM_NAME = "SSLRealm";
    public static final String AUTHENTICATION_PROPERTIES_PATH = "application-users.properties";
    public static final String AUTHENTICATION_PROPERTIES_RELATIVE_TO = "jboss.server.config.dir";

    // server SSL stuff
    public static final String SERVER_KEYSTORE_ALIAS = "jbossalias";
    public static final String SERVER_KEYSTORE_PASSWORD = "JBossPassword";
    public static final String SERVER_KEYSTORE_FILENAME = "jbossServer.keystore";
    public static final String SERVER_KEY_PASSWORD = "123456";

    // client SSL stuff
    public static final String CLIENT_KEYSTORE_FILENAME = "jbossClient.keystore";
    public static final String CLIENT_TRUSTSTORE_FILENAME = "jbossClient.truststore";
    public static final String CLIENT_KEYSTORE_ALIAS = "clientalias";
    public static final String CLIENT_KEY_PASSWORD = "abcdef";
    public static final String CLIENT_KEYSTORE_PASSWORD = "clientPassword";

    // SSL stuff for both
    public static final String KEYSTORES_RELATIVE_PATH = "ejb3" + File.separator + "ssl";
    public static String KEYSTORES_ABSOLUTE_PATH;

    /* ----------------- GETTING ModelNode addresses ----------------- */
    public static ModelNode getSecurityRealmsAddress() {
        ModelNode address = new ModelNode();
        address.add(CORE_SERVICE, MANAGEMENT);
        address.add(SECURITY_REALM, SECURITY_REALM_NAME);
        return address;
    }

    public static ModelNode getSecurityRealmsAddressSSLIdentity() {
        ModelNode address = getSecurityRealmsAddress();
        address.add(SERVER_IDENTITY, SSL);
        address.protect();
        return address;
    }

    public static ModelNode getSecurityRealmsAddressAuthentication() {
        ModelNode address = getSecurityRealmsAddress();
        address.add(AUTHENTICATION, TRUSTSTORE);
        address.protect();
        return address;
    }

    public static ModelNode getRemotingConnectorAddress() {
        ModelNode address = new ModelNode();
        address.add(SUBSYSTEM, "remoting");
        address.add("http-connector", "https-remoting-connector");
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
        // Adding SECURITY REALM
        ModelNode secRealmAddress = getSecurityRealmsAddress();
        secRealmAddress.protect();
        ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).set(secRealmAddress);
        operation.get(OP).set(ADD);
        ModelNode result = managementClient.getControllerClient().execute(operation);
        log.infof("Adding security realm %s with result %s", SECURITY_REALM_NAME, result);
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());

        // Adding SERVER IDENTITY
        // /core-service=management/security-realm=SSLRealm/server-identity=ssl:add(
        // keystore-password=JBossPassword, keystore-path="/path")
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        URL resourcesUrl = tccl.getResource("");
        String resourcePath = resourcesUrl.getPath();
        log.trace("Path to resources is " + resourcePath);
        operation = new ModelNode();
        operation.get(OP_ADDR).set(getSecurityRealmsAddressSSLIdentity());
        operation.get(OP).set(ADD);
        operation.get("keystore-password").set(SERVER_KEYSTORE_PASSWORD);
        KEYSTORES_ABSOLUTE_PATH = resourcePath + KEYSTORES_RELATIVE_PATH;
        operation.get("keystore-path").set(KEYSTORES_ABSOLUTE_PATH + File.separator + SERVER_KEYSTORE_FILENAME);
        operation.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);
        result = managementClient.getControllerClient().execute(operation);
        log.infof("Setting server-identity ssl for realm %s (password %s, keystore path %s) with result %s", SECURITY_REALM_NAME,
                SERVER_KEYSTORE_PASSWORD, KEYSTORES_ABSOLUTE_PATH, result.get(OUTCOME));
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());

        // Adding AUTHENTICATION attribute to SSLRealm
        operation = new ModelNode();
        operation.get(OP_ADDR).set(getSecurityRealmsAddressAuthentication());
        operation.get(OP).set(ADD);
        operation.get("keystore-path").set(resourcePath + "ejb3/ssl/jbossServer.keystore");
        operation.get("keystore-password").set(SERVER_KEYSTORE_PASSWORD);
        operation.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);
        result = managementClient.getControllerClient().execute(operation);
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());

        // make the https connector use our SSL realm
        operation = new ModelNode();
        operation.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        operation.get(OP_ADDR).add(SUBSYSTEM, "undertow");
        operation.get(OP_ADDR).add("server", "default-server");
        operation.get(OP_ADDR).add("https-listener", "https");
        operation.get(NAME).set("security-realm");
        operation.get(VALUE).set(SECURITY_REALM_NAME);
        result = managementClient.getControllerClient().execute(operation);
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());

        //add remoting connector
        operation = new ModelNode();
        operation.get(OP_ADDR).set(SSLRealmSetupTool.getRemotingConnectorAddress());
        operation.get(OP).set(ADD);
        operation.get(SECURITY_REALM).set("ApplicationRealm");
        operation.get(PROTOCOL).set("https-remoting");
        operation.get("connector-ref").set("https");
        operation.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);
        result = managementClient.getControllerClient().execute(operation);
        log.infof("Adding HTTPS connector", result);
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
    }

    public static void readSSLRealmConfig(final ManagementClient managementClient) throws IOException {
        ModelNode operation = new ModelNode();
        operation.get(OP).set(READ_RESOURCE_OPERATION);
        operation.get(OP_ADDR).set(getSecurityRealmsAddress());
        operation.get(RECURSIVE).set("true");
        ModelNode ret = managementClient.getControllerClient().execute(operation);
        log.trace("SSLRealm config looks like this:\n" + ret.get(RESULT).toJSONString(false));
    }

    public static void tearDown(final ManagementClient managementClient, ContainerController controller) throws Exception {

        ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).set(SSLRealmSetupTool.getRemotingConnectorAddress());
        operation.get(OP).set(REMOVE);
        operation.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);
        ModelNode result = managementClient.getControllerClient().execute(operation);
        log.infof("remove HTTPS connector", result);
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
        ServerReload.executeReloadAndWaitForCompletion(managementClient.getControllerClient());

        // restore https connector to previous state
        operation = new ModelNode();
        operation.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        operation.get(OP_ADDR).add(SUBSYSTEM, "undertow");
        operation.get(OP_ADDR).add("server", "default-server");
        operation.get(OP_ADDR).add("https-listener", "https");
        operation.get(NAME).set("security-realm");
        operation.get(VALUE).set("ApplicationRealm");
        operation.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);
        result = managementClient.getControllerClient().execute(operation);
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
        ServerReload.executeReloadAndWaitForCompletion(managementClient.getControllerClient());

        // Removing security realm
        ModelNode secRealmAddress = getSecurityRealmsAddress();
        secRealmAddress.protect();
        operation = new ModelNode();
        operation.get(OP_ADDR).set(secRealmAddress);
        operation.get(OP).set(REMOVE);
        operation.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);
        result = managementClient.getControllerClient().execute(operation);
        log.infof("Removing security realm %s with result %s", SECURITY_REALM_NAME, result);
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
        controller.stop(SSLEJBRemoteClientTestCase.DEFAULT_JBOSSAS);
    }

}
