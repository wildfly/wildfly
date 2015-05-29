/*
 * JBoss, Home of Professional Open Source
 * Copyright 2015, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.test.integration.management.cli;

import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.management.base.AbstractCliTestBase;
import org.jboss.logging.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * CLI should show the real value of expression properties in addition to the expression
 *
 * https://issues.jboss.org/browse/WFLY-4702
 *
 * @author Marek Kopecky <mkopecky@redhat.com>
 */
@RunWith(Arquillian.class)
public class RealValueExpressionTestCase extends AbstractCliTestBase {

    private static Logger log = Logger.getLogger(RealValueExpressionTestCase.class);

    private static String newSystemPropertyName = "real.value.expression.test.case";

    private static String defaultValue = "01234default56789";

    @BeforeClass
    public static void before() throws Exception {
        AbstractCliTestBase.initCLI();
    }

    @AfterClass
    public static void after() throws Exception {
        AbstractCliTestBase.closeCLI();
    }

    /**
     * Sends command line to CLI, validate and return output.
     *
     * @param line command line
     * @return CLI output
     */
    private String cliRequest(String line) {
        log.info(line);
        cli.sendLine(line);
        String output = cli.readOutput();
        assertTrue("CLI command \"" + line + " doesn't contain \"success\"", output.contains("success"));
        return output;
    }

    /**
     * Prepare CLI command: /system-property=newSystemPropertyName:add(value=${oldSystemProperty:defaultValue})
     *
     * @param oldSystemProperty old system property
     * @return new CLI command
     */
    private String prepareCliAddSystemPropertyCommand(String oldSystemProperty) {
        StringBuilder cliCommand = new StringBuilder();
        cliCommand.append("/system-property=").append(newSystemPropertyName);
        cliCommand.append(":add(value=\\${").append(oldSystemProperty).append(":").append(defaultValue).append("})");
        return cliCommand.toString();
    }

    /**
     * Read value of "test.bind.address"
     */
    @Test
    public void testRealValue() {
        String oldSystemProperty = "test.bind.address";

        // get test.bind.address system property
        String testBindAddress = System.getProperty("node0");
        testBindAddress = testBindAddress == null ? "" : testBindAddress;
        log.info("testBindAddress = " + testBindAddress);

        // cli
        try {
            cliRequest(prepareCliAddSystemPropertyCommand(oldSystemProperty));

            // read-resource test
            String command = "/system-property=" + newSystemPropertyName + ":read-resource(resolve-expressions=true)";
            String output = cliRequest(command);
            String errorMessage = "CLI command \"" + command + "\" returns unexpected output";
            assertTrue(errorMessage, output.contains(testBindAddress));
            assertTrue(errorMessage, output.contains("value"));
            assertFalse(errorMessage, output.contains(oldSystemProperty));
            assertFalse(errorMessage, output.contains(defaultValue));

            // read-attribute test
            command = "/system-property=" + newSystemPropertyName + ":read-attribute(name=value, resolve-expressions=true)";
            output = cliRequest(command);
            errorMessage = "CLI command \"" + command + "\" returns unexpected output";
            assertTrue(errorMessage, output.contains(testBindAddress));
            assertFalse(errorMessage, output.contains(oldSystemProperty));
            assertFalse(errorMessage, output.contains(defaultValue));

        } finally {
            cliRequest("/system-property=" + newSystemPropertyName + ":remove");
        }
    }

    /**
     * Read default value
     */
    @Test
    public void testDefaultValue() {
        String oldSystemProperty = "nonexistent.attribute";

        // cli
        try {
            cliRequest(prepareCliAddSystemPropertyCommand(oldSystemProperty));

            // read-resource test
            String command = "/system-property=" + newSystemPropertyName + ":read-resource(resolve-expressions=true)";
            String output = cliRequest(command);
            String errorMessage = "CLI command \"" + command + "\" returns unexpected output";
            assertTrue(errorMessage, output.contains(defaultValue));
            assertTrue(errorMessage, output.contains("value"));
            assertFalse(errorMessage, output.contains(oldSystemProperty));

            // read-attribute test
            command = "/system-property=" + newSystemPropertyName + ":read-attribute(name=value, resolve-expressions=true)";
            output = cliRequest(command);
            errorMessage = "CLI command \"" + command + "\" returns unexpected output";
            assertTrue(errorMessage, output.contains(defaultValue));
            assertFalse(errorMessage, output.contains(oldSystemProperty));

        } finally {
            cliRequest("/system-property=" + newSystemPropertyName + ":remove");
        }
    }

}
