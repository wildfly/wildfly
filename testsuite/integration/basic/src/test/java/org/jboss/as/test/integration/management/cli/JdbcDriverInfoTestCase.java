/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.management.cli;

import java.util.HashMap;
import java.util.Map;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.as.test.integration.management.base.AbstractCliTestBase;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Test for high level cli command jdbc-driver-info and jdbc-driver-info [driver-name]
 */
@ExtendWith(ArquillianExtension.class)
@RunAsClient
public class JdbcDriverInfoTestCase extends AbstractCliTestBase {

    @BeforeAll
    public static void before() throws Exception {
        initCLI();
    }

    @AfterAll
    public static void after() throws Exception {
        closeCLI();
    }

    @Test
    public void testJdbcDriverInfo() throws Exception {
        cli.sendLine("jdbc-driver-info");
        String[] lines = cli.readOutput().split("\\n");
        Assertions.assertEquals(2, lines.length);

        String[] header = lines[0].split("\\s+");
        Assertions.assertEquals("NAME", header[0]);
        Assertions.assertEquals("SOURCE", header[1]);

        String[] driver = lines[1].split("\\s+");
        Assertions.assertEquals("h2", driver[0]);
        Assertions.assertEquals("com.h2database.h2/main", driver[1]);
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

        Assertions.assertTrue(driverInformation.containsKey("driver-name"));
        Assertions.assertEquals("h2", driverInformation.get("driver-name"));

        Assertions.assertTrue(driverInformation.containsKey("deployment-name"));
        Assertions.assertTrue(driverInformation.containsKey("driver-module-name"));
        Assertions.assertTrue(driverInformation.containsKey("datasource-class-info"));
        Assertions.assertTrue(driverInformation.containsKey("driver-class-name"));
        Assertions.assertTrue(driverInformation.containsKey("driver-major-version"));
        Assertions.assertTrue(driverInformation.containsKey("driver-minor-version"));
    }

}
