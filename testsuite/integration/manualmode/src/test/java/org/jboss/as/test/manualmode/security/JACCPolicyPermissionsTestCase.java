/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
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

package org.jboss.as.test.manualmode.security;

import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.logging.Logger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.manual.elytron.seccontext.AbstractSecurityContextPropagationTestBase;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

/**
 * Tests if the WARN message was logged during server shutdown when Elytron JACC is set.
 * Test for [ JBEAP-13656 ].
 *
 * @author Daniel Cihak
 */
@RunWith(Arquillian.class)
public class JACCPolicyPermissionsTestCase extends AbstractSecurityContextPropagationTestBase {

    private static Logger LOGGER = Logger.getLogger(JACCPolicyPermissionsTestCase.class);

    private static final String CONTAINER = "default-jbossas";
    private static final int TIMEOUT = TimeoutUtil.adjust(3000);
    private static final ServerHolder server = new ServerHolder(CONTAINER, TestSuiteEnvironment.getServerAddressNode1(), 0);
    private PrintStream oldOut;

    @Before
    public void before() {
        server.startContainer();
        server.initServerConfiguration("add-jacc-policy.cli");
    }

    @After
    public void after() throws IOException {
        server.shutDown();
    }

    protected boolean isEntryStateful() {
        return false;
    }

    protected boolean isWhoAmIStateful() {
        return false;
    }

    @Test
    public void testInvalidTransactionAttributeWarnLogged() throws Exception {
        oldOut = System.out;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(baos));
            server.stopContainer();
            Thread.sleep(TIMEOUT);
            System.setOut(oldOut);
            String output = new String(baos.toByteArray());
            Assert.assertFalse(output, output.contains("ELY08509"));
            Assert.assertFalse(output, output.contains("ELY03018"));
            Assert.assertFalse(output, output.contains("ERROR"));
        } finally {
            System.setOut(oldOut);
        }
    }
}
