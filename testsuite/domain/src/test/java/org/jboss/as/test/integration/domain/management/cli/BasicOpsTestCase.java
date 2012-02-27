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
package org.jboss.as.test.integration.domain.management.cli;

import java.util.List;

import org.jboss.as.test.integration.management.util.CLIOpResult;
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.junit.Test;

import static org.jboss.as.test.integration.management.base.AbstractCliTestBase.WAIT_LINETIMEOUT;
import static org.jboss.as.test.integration.management.base.AbstractCliTestBase.WAIT_TIMEOUT;
import static org.junit.Assert.assertTrue;
/**
 *
 * @author Dominik Pospisil <dpospisi@redhat.com>
 */
public class BasicOpsTestCase {

    @Test
    public void testConnect() throws Exception {
        CLIWrapper cli = new CLIWrapper(false);

        // wait for cli welcome message
        String line = cli.readLine(WAIT_TIMEOUT);

        while(! line.contains("You are disconnected")) {
            line = cli.readLine(WAIT_TIMEOUT);
        }

        cli.sendLine("connect", false);
        line = cli.readLine(WAIT_TIMEOUT);

        assertTrue("Check we are disconnected:" + line, line.indexOf("disconnected") >= 0);
        cli.sendLine("version", false);
        line = cli.readLine(WAIT_TIMEOUT);
        assertTrue("Connect failed:" + line, line.indexOf("[domain@") >= 0);
        cli.quit();

    }

    @Test
    public void testDomainSetup() throws Exception {
        CLIWrapper cli = new CLIWrapper(false);

        // wait for cli welcome message
        String line = cli.readLine(WAIT_TIMEOUT);

        while(! line.contains("You are disconnected")) {
            line = cli.readLine(WAIT_TIMEOUT);
        }

        cli.sendLine("connect", false);
        line = cli.readLine(WAIT_TIMEOUT);

        assertTrue("Check we are disconnected:" + line, line.indexOf("disconnected") >= 0);
        cli.sendLine("version", false);
        line = cli.readLine(WAIT_TIMEOUT);
        assertTrue("Connect failed:" + line, line.indexOf("[domain@") >= 0);

        // check hosts
        cli.sendLine(":read-children-names(child-type=host)");
        CLIOpResult res = cli.readAllAsOpResult(WAIT_TIMEOUT, WAIT_LINETIMEOUT);
        assertTrue(res.getResult() instanceof List);
        List  hosts = (List) res.getResult();

        assertTrue(hosts.contains("master"));
        assertTrue(hosts.contains("slave"));

        // check servers
        assertTrue(checkHostServers(cli, "master", new String[] {"main-one", "main-two", "other-one"}));
        assertTrue(checkHostServers(cli, "slave", new String[] {"main-three", "main-four", "other-two"}));
        cli.quit();

    }

        private boolean checkHostServers(CLIWrapper cli, String host, String[] serverList) throws Exception {
            cli.sendLine("/host=" + host + ":read-children-names(child-type=server-config)");
            CLIOpResult res = cli.readAllAsOpResult(WAIT_TIMEOUT, WAIT_LINETIMEOUT);
            assertTrue(res.getResult() instanceof List);
            List  servers = (List) res.getResult();

            if (servers.size() != serverList.length) return false;
            for (String server : serverList) if (!servers.contains(server)) return false;

            return true;
        }

}
