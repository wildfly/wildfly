/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.manualmode.model;


import org.jboss.as.arquillian.container.ManagementClient;
import org.junit.Test;

/**
 * Smoke test of expression support for default configurations.
 *
 * @author Ivan Straka (c) 2020 Red Hat Inc.
 */
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
