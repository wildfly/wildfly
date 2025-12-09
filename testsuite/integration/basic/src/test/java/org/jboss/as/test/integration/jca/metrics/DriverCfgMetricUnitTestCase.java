/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jca.metrics;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.as.test.shared.FileUtils;
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
        String h2Version = null;
        try (BufferedReader reader = new BufferedReader(new FileReader(new File("target", "h2.version")))) {
            h2Version = reader.readLine();
        }
        assertNotNull("h2 version should have been written to h2.version file", h2Version);
        Matcher m = Pattern.compile(".+com\\" + File.separator + "h2database\\" + File.separator + "h2\\" +
                File.separator + "(\\d+)\\.(\\d+).+").matcher(h2Version);
        assertTrue("h2 jar location should match maven repository directory structure", m.matches());
        String xml = String.format(FileUtils.readFile(JCAMetricsTestBase.class, "data-sources/complex-driver.xml"), m.group(1), m.group(2));
        setModelXml(xml);
        assertEquals("name", readAttribute(baseAddress, "driver-name").asString());
        assertEquals("org.h2.jdbcx.JdbcDataSource", readAttribute(baseAddress, "driver-xa-datasource-class-name").asString());
        removeDs();
    }

    @Test
    public void testIncorrectDriverVersion() throws Exception {
        try {
            String xml = String.format(FileUtils.readFile(JCAMetricsTestBase.class, "data-sources/complex-driver.xml"), "2", "9999999");
            setModelXml(xml);
            fail("Driver deployment should have failed");
        } catch (MgmtOperationException e) {
            assertTrue(e.getMessage().contains("WFLYJCA0034"));
        }
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
