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
package org.jboss.as.test.integration.management.cli;

import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertTrue;

/**
 *
 * @author Dominik Pospisil <dpospisi@redhat.com>
 */
@RunWith(Arquillian.class)
public class BasicOpsTestCase {

    @Test
    public void testConnect() throws Exception {
        CLIWrapper cli = new CLIWrapper(false);

        // wait for cli welcome message
        String line = cli.readLine(10000);

        while(! line.contains("You are disconnected")) {
            line = cli.readLine(10000);
        }

        cli.sendLine("connect " + TestSuiteEnvironment.getServerAddress() + ":" + TestSuiteEnvironment.getServerPort());
        cli.sendLine("version", false);
        line = cli.readLine(5000);
        assertTrue("Connect failed:" + line, line.indexOf("[standalone@") >= 0);

        cli.quit();

    }

    @Test
    public void testLs() throws Exception {
        CLIWrapper cli = new CLIWrapper(true);
        cli.sendLine("ls", true);
        String ls = cli.readAllUnformated(5000, 500);

        assertTrue(ls.contains("subsystem"));
        assertTrue(ls.contains("interface"));
        assertTrue(ls.contains("extension"));
        assertTrue(ls.contains("subsystem"));
        assertTrue(ls.contains("core-service"));
        assertTrue(ls.contains("system-property"));
        assertTrue(ls.contains("socket-binding-group"));
        assertTrue(ls.contains("deployment"));
        assertTrue(ls.contains("path"));

        cli.quit();
    }


}
