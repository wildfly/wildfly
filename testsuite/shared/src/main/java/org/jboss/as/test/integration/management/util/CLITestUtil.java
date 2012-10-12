/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.management.util;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

import org.jboss.as.cli.CliInitializationException;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandContextFactory;
import org.jboss.as.test.shared.TestSuiteEnvironment;

/**
 *
 * @author Dominik Pospisil <dpospisi@redhat.com>
 */
public class CLITestUtil {

    private static final String JBOSS_CLI_CONFIG = "jboss.cli.config";
    private static final String JREADLINE_TERMINAL = "jreadline.terminal";
    private static final String JREADLINE_TEST_TERMINAL = "org.jboss.jreadline.terminal.TestTerminal";

    private static final String serverAddr = TestSuiteEnvironment.getServerAddress();
    private static final int serverPort = TestSuiteEnvironment.getServerPort();

    public static CommandContext getCommandContext() throws CliInitializationException {
        setJBossCliConfig();
        return CommandContextFactory.getInstance().newCommandContext(serverAddr, serverPort, null, null);
    }

    public static CommandContext getCommandContext(String address, int port, String user, char[] pwd, InputStream in, OutputStream out)
            throws CliInitializationException {
        setJBossCliConfig();
        return CommandContextFactory.getInstance().newCommandContext(address, port, user, pwd, in, out);
    }

    public static CommandContext getCommandContext(OutputStream out) throws CliInitializationException {
        SecurityActions.setSystemProperty(JREADLINE_TERMINAL, JREADLINE_TEST_TERMINAL);
        setJBossCliConfig();
        return CommandContextFactory.getInstance().newCommandContext(serverAddr, serverPort, null, null, null, out);
    }

    protected static void setJBossCliConfig() {
        final String jbossCliConfig = SecurityActions.getSystemProperty(JBOSS_CLI_CONFIG);
        if(jbossCliConfig == null) {
            final String jbossDist = System.getProperty("jboss.dist");
            if(jbossDist == null) {
                fail("jboss.dist system property is not set");
            }
            SecurityActions.setSystemProperty(JBOSS_CLI_CONFIG, jbossDist + File.separator + "bin" + File.separator + "jboss-cli.xml");
        }
    }
}
