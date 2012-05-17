/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2012, Red Hat, Inc., and individual contributors
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

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.http.Authentication;
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.logging.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * This class contains a check that CLI access is secured.
 *
 * @author <a href="mailto:jlanik@redhat.com">Jan Lanik</a>.
 */
@RunWith(Arquillian.class)
@RunAsClient
public class CLISecurityTestCase {

    Logger logger = Logger.getLogger(CLISecurityTestCase.class);

    private static final String JBOSS_INST = System.getProperty("jboss.inst");

    private static File originalTokenDir = new File(JBOSS_INST, "/standalone/tmp/auth");
    private static File renamedTokenDir = new File(JBOSS_INST, "/standalone/tmp/auth.renamed");

    /**
     * Auxiliary class which extends CLIWrapper for specific purposes of this test case.
     */
    public static class UnauthentizedCLI extends CLIWrapper {

        public UnauthentizedCLI() throws Exception {
            super();
        }

        @Override
        protected String getUsername() {
            return null;
        }

        @Override
        protected InputStream createConsoleInput() {
            return new InputStream() {
                private final byte[] bytes = (Authentication.USERNAME + '\n').getBytes();
                private int i = 0;
                @Override
                public int read() throws IOException {
                    if(i >= bytes.length) {
                        return -1;
                    }
                    return bytes[i++];
                }
                @Override
                public int available() {
                    return bytes.length - i;
                }
            };
        }
        public synchronized void shutdown(){
            this.quit();
        }
    }

    /**
     * Workaround to disable silent login on localhost.
     */
    @BeforeClass
    public static void renameTokenDir() {
       originalTokenDir.renameTo(renamedTokenDir);
    }

    /**
     * Enables silent login after the test is completed.
     */
    @AfterClass
    public static void cleanup() {
        renamedTokenDir.renameTo(originalTokenDir);
    }

    /**
     * This test checks that CLI access is secured.
     * @throws Exception
     */
    @Test
    public void testConnect() throws Exception {

        UnauthentizedCLI cli = new UnauthentizedCLI();

        assertFalse(cli.isConnected());
        cli.sendLine("connect " + TestSuiteEnvironment.getServerAddress() + ":" + TestSuiteEnvironment.getServerPort(), true);
        String line = cli.readOutput();
        logger.info("cli response: " + line);
        assertTrue("CLI is not secured:" + line, line.indexOf("Authenticating against security realm") >= 0);

        cli.shutdown();
    }
}
