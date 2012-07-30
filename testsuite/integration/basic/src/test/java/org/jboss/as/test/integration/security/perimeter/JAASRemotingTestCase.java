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
package org.jboss.as.test.integration.security.perimeter;

import java.io.File;
import java.io.IOException;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.runner.RunWith;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.security.auth.callback.*;
import javax.security.sasl.RealmCallback;
import org.codehaus.plexus.util.StringUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.dmr.ModelNode;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;
import static org.jboss.as.security.Constants.SECURITY_DOMAIN;
import static org.jboss.as.security.Constants.CLASSIC;
import static org.jboss.as.security.Constants.LOGIN_MODULES;
import static org.jboss.as.security.Constants.FLAG;
import org.jboss.as.test.integration.security.common.AbstractSecurityDomainSetup;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Ignore;

/**
 * A JAASRemotingTestCase for testing log in to CLI
 *  
 * @author Ondrej Lukas
 */
@RunWith(Arquillian.class)
@RunAsClient
public class JAASRemotingTestCase {

    @ArquillianResource
    ManagementClient managementClient;

    /**
     * It can't be used as classic Arquillian ServerSetup because it has to restart server and for this reason new 
     * instance of ManagementClient has to be created.
     */
    public static class SecurityDomainSetup extends AbstractSecurityDomainSetup {

        private String nativeInterfaceName;

        protected String getSecurityDomainName() {
            return "JBossTestDomain";
        }

        /**
         * Set up configuration of server  
         * 
         * @param managementClient
         * @param containerId
         * @throws Exception 
         */
        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            
            List<ModelNode> updates = new ArrayList<ModelNode>();
            ModelNode op = new ModelNode();

            // gain security-realm in native-interface tag
            op.get(OP).set(READ_ATTRIBUTE_OPERATION);
            op.get(OP_ADDR).add(CORE_SERVICE, MANAGEMENT);
            op.get(OP_ADDR).add(MANAGEMENT_INTERFACE, NATIVE_INTERFACE);
            op.get(NAME).set(SECURITY_REALM);
            ModelNode temp = managementClient.getControllerClient().execute(new OperationBuilder(op).build());
            nativeInterfaceName = temp.get("result").asString();

            // create new security domain
            op = new ModelNode();
            op.get(OP).set(ADD);
            op.get(OP_ADDR).add(SUBSYSTEM, "security");
            op.get(OP_ADDR).add(SECURITY_DOMAIN, getSecurityDomainName());
            op.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);
            updates.add(op);

            // set up module-options in security domain
            op = new ModelNode();
            op.get(OP).set(ADD);
            op.get(OP_ADDR).add(SUBSYSTEM, "security");
            op.get(OP_ADDR).add(SECURITY_DOMAIN, getSecurityDomainName());
            op.get(OP_ADDR).add(AUTHENTICATION, CLASSIC);
            ModelNode loginModule = op.get(LOGIN_MODULES).add();
            loginModule.get(CODE).set("UsersRoles");
            loginModule.get(FLAG).set(REQUIRED);
            ModelNode moduleOptions = loginModule.get("module-options");
            moduleOptions.get("usersProperties").set(new File(JAASRemotingTestCase.class.getResource("users.properties").toURI()).getAbsolutePath());
            moduleOptions.get("rolesProperties").set(new File(JAASRemotingTestCase.class.getResource("roles.properties").toURI()).getAbsolutePath());
            op.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);
            updates.add(op);

            // create new security realm
            op = new ModelNode();
            op.get(OP).set(ADD);
            op.get(OP_ADDR).add(CORE_SERVICE, MANAGEMENT);
            op.get(OP_ADDR).add(SECURITY_REALM, "JBossTest");
            op.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);
            updates.add(op);

            // set up new security realm
            op = new ModelNode();
            op.get(OP).set(ADD);
            op.get(OP_ADDR).add(CORE_SERVICE, MANAGEMENT);
            op.get(OP_ADDR).add(SECURITY_REALM, "JBossTest");
            op.get(OP_ADDR).add(AUTHENTICATION, JAAS);
            op.get(NAME).set(getSecurityDomainName());            
            updates.add(op);

            Utils.applyUpdates(updates, managementClient.getControllerClient());
            
            //Thread.sleep(3000);
            updates = new ArrayList<ModelNode>();

            // set up native-interface
            op = new ModelNode();
            op.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
            op.get(OP_ADDR).add(CORE_SERVICE, MANAGEMENT);
            op.get(OP_ADDR).add(MANAGEMENT_INTERFACE, NATIVE_INTERFACE);
            op.get(NAME).set(SECURITY_REALM);
            op.get(VALUE).set("JBossTest");
            op.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);
            updates.add(op);

            Utils.applyUpdates(updates, managementClient.getControllerClient());

            // restart server
            updates = new ArrayList<ModelNode>();
            op = new ModelNode();
            op.get(OP).set("reload");
            op.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);
            updates.add(op);

            Utils.applyUpdates(updates, managementClient.getControllerClient());

        }

        @Override
        public void tearDown(final ManagementClient managementClient, final String containerId) {

            super.tearDown(managementClient, containerId);

            List<ModelNode> updates = new ArrayList<ModelNode>();
            ModelNode op = new ModelNode();

            // set up native-interface            
            op.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
            op.get(OP_ADDR).add(CORE_SERVICE, MANAGEMENT);
            op.get(OP_ADDR).add(MANAGEMENT_INTERFACE, NATIVE_INTERFACE);
            op.get(NAME).set(SECURITY_REALM);
            op.get(VALUE).set(nativeInterfaceName);
            op.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);
            updates.add(op);

            applyUpdates(managementClient.getControllerClient(), updates);

            // restart server
            updates = new ArrayList<ModelNode>();
            op = new ModelNode();
            op.get(OP).set("reload");
            op.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);
            updates.add(op);
            try {
                Utils.applyUpdates(updates, managementClient.getControllerClient());
            } catch (Exception ex) {
                Logger.getLogger(JAASRemotingTestCase.class.getName()).log(Level.SEVERE, null, ex);
            }

            updates = new ArrayList<ModelNode>();
            try {
                Thread.sleep(3000);
            } catch (InterruptedException ex) {
                Logger.getLogger(JAASRemotingTestCase.class.getName()).log(Level.SEVERE, null, ex);
            }        
            
            // remove security realm
            op = new ModelNode();
            op.get(OP).set(REMOVE);
            op.get(OP_ADDR).add(CORE_SERVICE, MANAGEMENT);
            op.get(OP_ADDR).add(SECURITY_REALM, "JBossTest");
            op.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);
            updates.add(op);

            applyUpdates(managementClient.getControllerClient(), updates);
            try {
                managementClient.getControllerClient().close();
                managementClient.close();
            } catch (IOException ex) {
                Logger.getLogger(JAASRemotingTestCase.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @Deployment
    public static WebArchive deployment() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "test.war");
        return war;
    }

    /**
     * Create a new ModelControllerClient
     * 
     * @param host
     * @param port
     * @param username
     * @param password
     * @return new ModelControllerClient
     */
    static ModelControllerClient createClient(final InetAddress host, final int port,
            final String username, final char[] password) {

        final CallbackHandler callbackHandler = new CallbackHandler() {

            public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
                for (Callback current : callbacks) {
                    if (current instanceof NameCallback) {
                        NameCallback ncb = (NameCallback) current;
                        ncb.setName(username);
                    } else if (current instanceof PasswordCallback) {
                        PasswordCallback pcb = (PasswordCallback) current;
                        pcb.setPassword(password);
                    } else if (current instanceof RealmCallback) {
                        RealmCallback rcb = (RealmCallback) current;
                        rcb.setText(rcb.getDefaultText());
                    } else {
                        throw new UnsupportedCallbackException(current);
                    }
                }
            }
        };

        return ModelControllerClient.Factory.create(host, port, callbackHandler);
    }

    /**
     * Test that user is logged in to CLI
     * 
     * @throws UnknownHostException
     * @throws Exception 
     */
    @Test
    public void testConnectToCLI() throws UnknownHostException, Exception {
        SecurityDomainSetup sds = new SecurityDomainSetup();
        sds.setup(managementClient, CLASSIC);
        ModelControllerClient mcc = createClient(InetAddress.getByName(getHost()),
                managementClient.getMgmtPort(), "test", "test".toCharArray());
        ModelNode op = new ModelNode();
        op.get(OP).set(READ_CHILDREN_NAMES_OPERATION);
        op.get(OP_ADDR).add(CORE_SERVICE, MANAGEMENT);
        op.get(CHILD_TYPE).set(SECURITY_REALM);
        Thread.sleep(5000);
        
        ModelNode result = mcc.execute(new OperationBuilder(op).build());
        assertEquals("CLI operation wasn't success", SUCCESS, result.get("outcome").asString());
        
        ManagementClient mc = new ManagementClient(mcc, getHost(), managementClient.getMgmtPort());
        sds.tearDown(mc, CLASSIC);
    }

    public String getHost() {
        return StringUtils.strip(managementClient.getMgmtAddress(), "[]");
    }
}
