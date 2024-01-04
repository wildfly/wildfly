/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.manualmode.model;


import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.management.base.AbstractExpressionSupportTestCase;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Smoke test of expression support for default configurations.
 *
 * @author Ivan Straka (c) 2020 Red Hat Inc.
 */
@RunWith(Arquillian.class)
public class ExpressionSupportSmokeTestCase extends AbstractExpressionSupportTestCase {

    private static final String JBOSSAS = "jbossas-custom";
    private static final String JBOSSAS_HA = "jbossas-custom-ha";
    private static final String JBOSSAS_FULL = "jbossas-custom-full";
    private static final String JBOSSAS_FULL_HA = "jbossas-custom-full-ha";
    private static final String JBOSSAS_ACTIVEMQ_COLOCATED = "jbossas-custom-activemq-colocated";
    private static final String JBOSSAS_GENERICJMS = "jbossas-custom-genericjms";
    private static final String JBOSSAS_RTS = "jbossas-custom-rts";
    private static final String JBOSSAS_XTS = "jbossas-custom-xts";
    private ManagementClient managementClient;

    private void setup(String containerName) {
        if (!container.isStarted(containerName)) {
            container.start(containerName);
        }
        managementClient = createManagementClient();
    }

    private void teardown(String containerName) {
        container.stop(containerName);
        managementClient.close();
    }

    private void testContainer(String containerName) throws Exception {
        try {
            setup(containerName);
            test(managementClient);
        } finally {
            teardown(containerName);
        }
    }

    @Test
    public void testDefault() throws Exception {
        testContainer(JBOSSAS);
    }

    @Test
    public void testHA() throws Exception {
        testContainer(JBOSSAS_HA);
    }

    @Test
    public void testFull() throws Exception {
        testContainer(JBOSSAS_FULL);
    }

    @Test
    public void testFullHa() throws Exception {
        testContainer(JBOSSAS_FULL_HA);
    }

    @Test
    public void testActivemqColocated() throws Exception {
        testContainer(JBOSSAS_ACTIVEMQ_COLOCATED);
    }

    @Test
    public void testGenericJMS() throws Exception {
        testContainer(JBOSSAS_GENERICJMS);
    }

    @Test
    public void testRTS() throws Exception {
        testContainer(JBOSSAS_RTS);
    }

    @Test
    public void testXTS() throws Exception {
        testContainer(JBOSSAS_XTS);
    }
}
