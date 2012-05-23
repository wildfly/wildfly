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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.jboss.as.test.integration.domain.DomainTestSupport;
import org.jboss.as.test.integration.management.util.CLIOpResult;
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.junit.Test;
/**
 *
 * @author Dominik Pospisil <dpospisi@redhat.com>
 */
public class BasicOpsTestCase {

    @Test
    public void testConnect() throws Exception {
        CLIWrapper cli = new CLIWrapper(false, DomainTestSupport.masterAddress);
        assertFalse(cli.isConnected());
        assertTrue(cli.sendConnect(DomainTestSupport.masterAddress));
        assertTrue(cli.isConnected());
        cli.quit();
    }

    @Test
    public void testDomainSetup() throws Exception {
        CLIWrapper cli = new CLIWrapper(false, DomainTestSupport.masterAddress);
        assertFalse(cli.isConnected());

        assertTrue(cli.sendConnect(DomainTestSupport.masterAddress));
        assertTrue(cli.isConnected());

        // check hosts
        cli.sendLine(":read-children-names(child-type=host)");
        CLIOpResult res = cli.readAllAsOpResult();
        assertTrue(res.getResult() instanceof List);
        List<?> hosts = (List<?>) res.getResult();

        assertTrue(hosts.contains("master"));
        assertTrue(hosts.contains("slave"));

        // check servers
        assertTrue(checkHostServers(cli, "master", new String[] {"main-one", "main-two", "other-one", "reload-one"}));
        assertTrue(checkHostServers(cli, "slave", new String[] {"main-three", "main-four", "other-two", "reload-two"}));
        cli.quit();

    }

    private boolean checkHostServers(CLIWrapper cli, String host, String[] serverList) throws Exception {
        cli.sendLine("/host=" + host + ":read-children-names(child-type=server-config)");
        CLIOpResult res = cli.readAllAsOpResult();
        assertTrue(res.getResult() instanceof List);
        List<?>  servers = (List<?>) res.getResult();

        if (servers.size() != serverList.length) return false;
        for (String server : serverList) if (!servers.contains(server)) return false;

        return true;
    }

}
