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

import java.io.File;
import junit.framework.Assert;
import org.apache.commons.io.FileUtils;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.management.base.AbstractCliTestBase;
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 *
 * @author Dominik Pospisil <dpospisi@redhat.com>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class CliArgumentsTestCase extends AbstractCliTestBase {
    
    private static final String tempDir = System.getProperty("java.io.tmpdir");
    private static final int MGMT_PORT = 9999;
    
    @Test
    public void testVersionArgument() throws Exception {
        CLIWrapper cli = new CLIWrapper(false, new String[] {"--version"});
        String output = cli.readAllUnformated(WAIT_TIMEOUT, WAIT_LINETIMEOUT);
        Assert.assertTrue(output.contains("JBOSS_HOME"));
        Assert.assertTrue(cli.hasQuit());
    }

    @Test
    public void testCommandArgument() throws Exception {
        CLIWrapper cli = new CLIWrapper(false, new String[] {"--command=version"});
        String output = cli.readAllUnformated(WAIT_TIMEOUT, WAIT_LINETIMEOUT);
        Assert.assertTrue(output.contains("JBOSS_HOME"));
        Assert.assertTrue(cli.hasQuit());
    }

    @Test
    public void testCommandsArgument() throws Exception {
        CLIWrapper cli = new CLIWrapper(false, new String[] {"--commands=version,connect,ls"});
        String output = cli.readAllUnformated(WAIT_TIMEOUT, WAIT_LINETIMEOUT);
        Assert.assertTrue("CLI Output: " + output, output.contains("JBOSS_HOME"));
        Assert.assertTrue("CLI Output: " + output, output.contains("subsystem"));
        Assert.assertTrue("CLI Output: " + output, output.contains("extension"));
        
        Assert.assertTrue(cli.hasQuit());
    }
    
    @Test
    public void testFileArgument() throws Exception {
        
        // prepare file
        File cliScriptFile = new File(tempDir, "testScript.cli");
        if (cliScriptFile.exists()) Assert.assertTrue(cliScriptFile.delete());
        FileUtils.writeStringToFile(cliScriptFile, "version" + System.getProperty("line.separator"));
        
        // pass it to CLI
        CLIWrapper cli = new CLIWrapper(false, new String[] {"--file=" + cliScriptFile.getAbsolutePath()});
        String output = cli.readAllUnformated(WAIT_TIMEOUT, WAIT_LINETIMEOUT);
        Assert.assertTrue(output.contains("JBOSS_HOME"));
        Assert.assertTrue(cli.hasQuit());      
        
        cliScriptFile.delete();
    }
    
    @Test
    public void testConnectArgument() throws Exception {
        CLIWrapper cli = new CLIWrapper(false, new String[] {"--commands=version,connect,ls"});
        String output = cli.readAllUnformated(WAIT_TIMEOUT, WAIT_LINETIMEOUT);
        Assert.assertTrue(output.contains("JBOSS_HOME"));
        Assert.assertTrue(output.contains("subsystem"));
        Assert.assertTrue(output.contains("extension"));        
        Assert.assertTrue(cli.hasQuit());
    }    
    
    @Test
    public void testControlerArgument() throws Exception {
        CLIWrapper cli = new CLIWrapper(false, new String[] {"--controller=localhost:" + String.valueOf(MGMT_PORT)});
        cli.sendLine("connect");
        cli.sendLine("ls");
        String output = cli.readAllUnformated(WAIT_TIMEOUT, WAIT_LINETIMEOUT);
        Assert.assertTrue(output.contains("subsystem"));
        Assert.assertTrue(output.contains("extension"));        
        cli.quit();
        
        cli = new CLIWrapper(false, new String[] {"--controller=localhost:" + String.valueOf(MGMT_PORT - 1)});
        cli.sendLine("connect");
        output = cli.readAllUnformated(WAIT_TIMEOUT, WAIT_LINETIMEOUT * 10);
        Assert.assertTrue(output.contains("The controller is not available"));
        cli.quit();        
    }    
    
}
