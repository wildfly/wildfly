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

package org.jboss.as.test.integration.domain.elytron;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADMIN_ONLY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SASL_AUTHENTICATION_FACTORY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;

import javax.security.auth.callback.CallbackHandler;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.test.integration.domain.AbstractSlaveHCAuthenticationTestCase;
import org.jboss.as.test.integration.domain.management.util.Authentication;
import org.jboss.as.test.integration.domain.management.util.DomainLifecycleUtil;
import org.jboss.as.test.integration.domain.management.util.DomainTestSupport;
import org.jboss.as.test.integration.domain.management.util.WildFlyManagedConfiguration;
import org.jboss.dmr.ModelNode;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test a slave HC connecting to the domain Elytron authentication context.
 */
public class SlaveHostControllerElytronAuthenticationTestCase extends AbstractSlaveHCAuthenticationTestCase {

    protected static final String RIGHT_PASSWORD = DomainLifecycleUtil.SLAVE_HOST_PASSWORD;

    private static ModelControllerClient domainMasterClient;
    private static ModelControllerClient domainSlaveClient;
    private static DomainTestSupport testSupport;

    private final String BAD_PASSWORD = "bad_password";
    private final int FAILED_RELOAD_TIMEOUT_MILLIS = 10_000;

    @BeforeClass
    public static void setupDomain() throws Exception {
        // Set up a domain with a master that doesn't support local auth so slaves have to use configured credentials
        testSupport = DomainTestSupport.create(
                DomainTestSupport.Configuration.create(SlaveHostControllerElytronAuthenticationTestCase.class.getSimpleName(),
                        "domain-configs/domain-minimal.xml",
                        "host-configs/host-master-elytron.xml", "host-configs/host-slave-elytron.xml"));

        // Tweak the callback handler so the master test driver client can authenticate
        // To keep setup simple it uses the same credentials as the slave host
        WildFlyManagedConfiguration masterConfig = testSupport.getDomainMasterConfiguration();
        CallbackHandler callbackHandler = Authentication.getCallbackHandler("slave", RIGHT_PASSWORD, "ManagementRealm");
        masterConfig.setCallbackHandler(callbackHandler);

        testSupport.start();

        domainMasterClient = testSupport.getDomainMasterLifecycleUtil().getDomainClient();
        domainSlaveClient = testSupport.getDomainSlaveLifecycleUtil().getDomainClient();
    }

    @AfterClass
    public static void tearDownDomain() throws Exception {
        testSupport.stop();
        testSupport = null;
        domainMasterClient = null;
        domainSlaveClient = null;
    }

    @Override
    protected ModelControllerClient getDomainMasterClient() {
        return domainMasterClient;
    }

    @Override
    protected ModelControllerClient getDomainSlaveClient() {
        return domainSlaveClient;
    }

    @Test
    public void testSlaveRegistration() throws Exception {
        slaveWithDigestM5Mechanism();
        slaveWithPlainMechanism();
        slaveWithInvalidPassword();
    }

    private void slaveWithDigestM5Mechanism() throws Exception {
        // Simply check that the initial startup produced a registered slave
        readHostControllerStatus(getDomainMasterClient(), 0);
    }

    private void slaveWithPlainMechanism() throws Exception {
        // Set the allowed mechanism to PLAIN
        getDomainMasterClient().execute(changePresentedMechanisms("master", new HashSet<>(Arrays.asList("PLAIN"))));
        getDomainSlaveClient().execute(changeSaslMechanism("slave", "PLAIN"));

        // Reload the slave and check that it produces a registered slave
        reloadSlave();
        readHostControllerStatus(getDomainMasterClient(), 0);

        // Set the allowed mechanism back to Digest-MD5
        getDomainMasterClient().execute(changePresentedMechanisms("master", new HashSet<>(Arrays.asList("DIGEST-MD5"))));
        getDomainSlaveClient().execute(changeSaslMechanism("slave", "DIGEST-MD5"));
        reloadSlave();
    }

    /**
     * Since this test results in an invalid configuration of a host, it needs to be run last in the test sequence.
     *
     * @throws Exception
     */
    private void slaveWithInvalidPassword() throws Exception {
        // Set up a bad password
        getDomainSlaveClient().execute(changePassword(BAD_PASSWORD));

        // Reload the slave, after being reloaded, the slave should fail because it won't be able to connect to master
        reloadWithoutChecks();

        // Verify that the slave host is not running
        Assert.assertFalse("Host \"slave\" has connected to master even though it has bad password",
                hostRunning(FAILED_RELOAD_TIMEOUT_MILLIS));

        // Note that the slave is now lost to us - we can't configure it via master
    }

    private ModelNode changeSaslMechanism(String slaveName, String mechanism) {
        ModelNode setAllowedMechanism = new ModelNode();
        setAllowedMechanism.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        setAllowedMechanism.get(OP_ADDR).set(
                new ModelNode().add(HOST, slaveName).add(SUBSYSTEM, "elytron")
                        .add("authentication-configuration", "slaveHostAConfiguration"));
        setAllowedMechanism.get(NAME).set("allow-sasl-mechanisms");
        setAllowedMechanism.get(VALUE).set(new ModelNode().add(mechanism));

        return setAllowedMechanism;
    }

    private ModelNode changePresentedMechanisms(String slaveName, Set<String> mechanisms) {
        ModelNode setPresentedMechanisms = new ModelNode();
        setPresentedMechanisms.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        setPresentedMechanisms.get(OP_ADDR).set(
                new ModelNode().add(HOST, slaveName).add(SUBSYSTEM, "elytron")
                        .add(SASL_AUTHENTICATION_FACTORY, "management-sasl-authentication"));
        setPresentedMechanisms.get(NAME).set("mechanism-configurations");

        ModelNode allMechanismsNode = new ModelNode();

        for (String mechanism : mechanisms) {
            ModelNode mechanismNode = new ModelNode();
            mechanismNode.get("mechanism-name").set(mechanism);
            if (mechanism.equals("LOCAL-JBOSS-USER")) {
                mechanismNode.get("realm-mapper").set("local");
            } else {
                mechanismNode.get("mechanism-realm-configurations").set(new ModelNode().get("realm-name").set("ManagementRealm"));
            }

            allMechanismsNode.add(mechanismNode);
        }

        setPresentedMechanisms.get(VALUE).set(allMechanismsNode);

        return setPresentedMechanisms;
    }

    private ModelNode changePassword(String password) {
        ModelNode setPassword = new ModelNode();
        setPassword.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        setPassword.get(OP_ADDR).set(
                new ModelNode().add(HOST, "slave").add(SUBSYSTEM, "elytron")
                        .add("authentication-configuration", "slaveHostAConfiguration"));
        setPassword.get(NAME).set("credential-reference.clear-text");
        setPassword.get(VALUE).set(password);

        return setPassword;
    }

    private void reloadWithoutChecks() throws IOException {
        ModelNode reloadSlave = new ModelNode();
        reloadSlave.get(OP).set("reload");
        reloadSlave.get(OP_ADDR).add(HOST, "slave");
        reloadSlave.get(ADMIN_ONLY).set(false);
        try {
            getDomainSlaveClient().execute(reloadSlave);
        } catch(IOException e) {
            final Throwable cause = e.getCause();
            if (!(cause instanceof ExecutionException) && !(cause instanceof CancellationException)) {
                throw e;
            } // else ignore, this might happen if the channel gets closed before we got the response
        }
    }

    private boolean hostRunning(long timeout) throws Exception {
        final long time = System.currentTimeMillis() + timeout;
        do {
            Thread.sleep(250);
            if (lookupHostInModel(getDomainMasterClient())) {
                return true;
            }
        } while (System.currentTimeMillis() < time);

        return false;
    }
}
