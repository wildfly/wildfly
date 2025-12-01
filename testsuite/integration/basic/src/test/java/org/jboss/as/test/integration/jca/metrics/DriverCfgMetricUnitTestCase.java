/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jca.metrics;


import static org.junit.Assert.assertEquals;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * XA datasource configuration and metrics unit test.
 *
 * @author <a href="mailto:vrastsel@redhat.com">Vladimir Rastseluev</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class DriverCfgMetricUnitTestCase extends JCAMetricsTestBase {

    @BeforeClass
    public static void before() {
        setBaseAddress("jdbc-driver", "name");
    }

    @Test
    public void testDriverAttributes() throws Exception {
        setModel("complex-driver.xml");
        assertEquals("name", readAttribute(baseAddress, "driver-name").asString());
        assertEquals("org.h2.jdbcx.JdbcDataSource", readAttribute(baseAddress, "driver-xa-datasource-class-name").asString());
        removeDs();
    }

    @Test
    public void testEmptyDriver() throws Exception {
        setModel("empty-driver.xml");
        assertEquals("name", readAttribute(baseAddress, "driver-name").asString());
        removeDs();
    }

    @Test(expected = Exception.class)
    public void testDriverWoName() throws Exception {
        setBadModel("wrong-wo-name-driver.xml");
    }

    @Test(expected = Exception.class)
    public void testDriverWithNoName() throws Exception {
        setBadModel("wrong-empty-name-driver.xml");
    }

    @Test(expected = Exception.class)
    public void test2DriverClasses() throws Exception {
        setBadModel("wrong-2-driver-classes.xml");
    }

    @Test(expected = Exception.class)
    public void test2DSClasses() throws Exception {
        setBadModel("wrong-2-ds-classes-driver.xml");
    }

    @Test(expected = Exception.class)
    public void test2XADSClasses() throws Exception {
        setBadModel("wrong-2-xa-ds-classes-driver.xml");
    }
}
