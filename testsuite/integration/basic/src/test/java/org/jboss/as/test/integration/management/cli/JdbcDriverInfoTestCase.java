/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.management.cli;

import java.util.HashMap;
import java.util.Map;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.management.base.AbstractCliTestBase;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test for high level cli command jdbc-driver-info and jdbc-driver-info [driver-name]
 */
@RunWith(Arquillian.class)
@RunAsClient
public class JdbcDriverInfoTestCase extends AbstractCliTestBase {

    @BeforeClass
    public static void before() throws Exception {
        initCLI();
    }

    @AfterClass
    public static void after() throws Exception {
        closeCLI();
    }

    @Test
    public void testJdbcDriverInfo() throws Exception {
        cli.sendLine("jdbc-driver-info");
        String[] lines = cli.readOutput().split("\\n");
        Assert.assertEquals(2, lines.length);

        String[] header = lines[0].split("\\s+");
        Assert.assertEquals("NAME", header[0]);
        Assert.assertEquals("SOURCE", header[1]);

        String[] driver = lines[1].split("\\s+");
        Assert.assertEquals("h2", driver[0]);
        Assert.assertEquals("com.h2database.h2/main", driver[1]);
    }

    @Test
    public void testJdbcDriverInfoWithDriverParameter() throws Exception {
        cli.sendLine("jdbc-driver-info h2");
        String[] lines = cli.readOutput().split("\\n");
        Map<String, String> driverInformation = new HashMap<>(lines.length);
        for (String line : lines) {
            String[] info = line.split("\\s+");
            driverInformation.put(info[0], info.length == 2 ? info[1] : null);
        }

        Assert.assertTrue(driverInformation.containsKey("driver-name"));
        Assert.assertEquals("h2", driverInformation.get("driver-name"));

        Assert.assertTrue(driverInformation.containsKey("deployment-name"));
        Assert.assertTrue(driverInformation.containsKey("driver-module-name"));
        Assert.assertTrue(driverInformation.containsKey("datasource-class-info"));
        Assert.assertTrue(driverInformation.containsKey("driver-class-name"));
        Assert.assertTrue(driverInformation.containsKey("driver-major-version"));
        Assert.assertTrue(driverInformation.containsKey("driver-minor-version"));
    }

}
