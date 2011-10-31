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
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Dominik Pospisil <dpospisi@redhat.com>
 */
@RunWith(Arquillian.class)
public class HelpTestCase extends AbstractCliTestBase {
    
    private static final String[] COMMANDS = {
        "connect", "deploy", "undeploy", "add-jms-queue", "remove-jms-queue",
        "add-jms-topic", "remove-jms-topic", "add-jms-cf", "remove-jms-cf",
        "data-source", "xa-data-source", "cn"
    };
    
    @Test
    public void testHelpCommand() throws Exception {
        cli.sendLine("help");
        String help = cli.readAllUnformated(WAIT_TIMEOUT);
        for (String cmd : COMMANDS) assertTrue("Command '" + cmd + "' missing in help.", help.contains(cmd));
    }
    
    @Test
    public void testConnectHelp() throws Exception {
        testCmdHelp("connect");
    }

    @Test
    public void testDeployHelp() throws Exception {
        testCmdHelp("deploy");
    }

    @Test
    public void testUndeployHelp() throws Exception {
        testCmdHelp("deploy");
    }

    @Test
    public void testAddJmsQueueHelp() throws Exception {
        testCmdHelp("add-jms-queue");
    }

    @Test
    public void testRemoveJmsQueueHelp() throws Exception {
        testCmdHelp("remove-jms-queue");
    }

    @Test
    public void testAddJmsTopicHelp() throws Exception {
        testCmdHelp("add-jms-topic");
    }

    @Test
    public void testRemoveJmsTopicHelp() throws Exception {
        testCmdHelp("remove-jms-topic");
    }

    @Test
    public void testAddJmsCfHelp() throws Exception {
        testCmdHelp("add-jms-cf");
    }

    @Test
    public void testRemoveJmsCfHelp() throws Exception {
        testCmdHelp("remove-jms-cf");
    }

    @Ignore
    @Test
    public void testDataSourceHelp() throws Exception {
        testCmdHelp("data-source");
    }

    @Ignore
    @Test
    public void testXaDataSourceHelp() throws Exception {
        testCmdHelp("xa-data-source");
    }

    @Test
    public void testCnHelp() throws Exception {
        testCmdHelp("cn");
    }
    
    private void testCmdHelp(String cmd) throws Exception {
        cli.sendLine(cmd + " --help");
        String help = cli.readAllUnformated(WAIT_TIMEOUT);
        assertTrue("Command " + cmd + " help does not have synopsis section.", help.contains("Synopsis:"));
        assertTrue("Command " + cmd + " help does not have description section.", help.contains("Description:"));
        assertTrue("Command " + cmd + " help does not have arguments section.", help.contains("Arguments:"));
        
    }
    
}
