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

package org.jboss.as.test.integration.domain;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_GROUPS;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

import org.jboss.as.test.integration.domain.management.util.DomainLifecycleUtil;
import org.jboss.as.test.integration.domain.management.util.DomainTestSupport;
import org.jboss.as.test.integration.domain.management.util.JBossAsManagedConfiguration;
import org.jboss.as.test.integration.management.util.CLIOpResult;
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;


public class InconsistentSystemPropertyJBQA11437TestCase extends BuildConfigurationTestBase{

    static final String masterAddress = DomainTestSupport.masterAddress;

    static final File CONFIG_DIR = new File("target/jbossas/domain/configuration/");

    private static final String JBPM_DESIGNER_PERSPECTIVE = "full";
    
    
    @Test
    public void testStandardHost() throws Exception {
        final JBossAsManagedConfiguration config = createConfiguration("domain.xml", "host.xml", getClass().getSimpleName(), "master", masterAddress, 9999);
        final DomainLifecycleUtil utils = new DomainLifecycleUtil(config);
        try {
            utils.start();
            // Double-check server status by confirming server-one can accept a web request to the root
            URLConnection connection = new URL("http://" + TestSuiteEnvironment.formatPossibleIpv6Address(masterAddress) + ":8080").openConnection();
            connection.connect();

            
            CLIWrapper cli = new CLIWrapper(false, masterAddress);
            assertFalse(cli.isConnected());

            assertTrue(cli.sendConnect(masterAddress));
            assertTrue(cli.isConnected());

            cli.sendLine(":resolve-expression-on-domain(expression=\"${org.jbpm.designer.perspective}\")");
            
            CLIOpResult opResult = cli.readAllAsOpResult();
            ModelNode node = opResult.getResponseNode();
            Assert.assertEquals("Result doesn't contain expected value.", JBPM_DESIGNER_PERSPECTIVE, node.get(SERVER_GROUPS, "main-server-group", HOST, "master", "server-one", "response", RESULT).asString());
            Assert.assertEquals("Result doesn't contain expected value.", JBPM_DESIGNER_PERSPECTIVE, node.get(SERVER_GROUPS, "main-server-group", HOST, "master", "server-two", "response", RESULT).asString());
       
            cli.sendLine(":resolve-expression-on-domain(expression=\"${org.jbpm.designer.helper}\")");
            
            opResult = cli.readAllAsOpResult();
            node = opResult.getResponseNode();
            Assert.assertEquals("Result doesn't contain expected value.", File.separator, node.get(SERVER_GROUPS, "main-server-group", HOST, "master", "server-one", "response", RESULT).asString());
            Assert.assertEquals("Result doesn't contain expected value.", File.separator, node.get(SERVER_GROUPS, "main-server-group", HOST, "master", "server-two", "response", RESULT).asString());
//            {
//                "outcome" => "success",
//                "result" => undefined,
//                "server-groups" => {"main-server-group" => {"host" => {"master" => {
//                    "server-one" => {"response" => {
//                        "outcome" => "success",
//                        "result" => "/"
//                    }},
//                    "server-two" => {"response" => {
//                        "outcome" => "success",
//                        "result" => "/"
//                    }}
//                }}}}
//            }
            
        } finally {
            utils.stop(); // Stop
        }
    }
    
    static JBossAsManagedConfiguration createConfiguration(final String domainXmlName, final String hostXmlName,
                                                           final String testConfiguration, final String hostName,
                                                           final String hostAddress, final int hostPort) {
        final JBossAsManagedConfiguration configuration = new JBossAsManagedConfiguration();

        configuration.setHostControllerManagementAddress(hostAddress);
        configuration.setHostControllerManagementPort(hostPort);
        configuration.setHostCommandLineProperties("-Djboss.domain.master.address=" + masterAddress +
                " -Djboss.management.native.port=" + hostPort);
        configuration.setDomainConfigFile(hackFixDomainConfig(new File(CONFIG_DIR, domainXmlName)).getAbsolutePath());
        configuration.setHostConfigFile(hackFixHostConfig(new File(CONFIG_DIR, hostXmlName), hostName, hostAddress).getAbsolutePath());

        configuration.setHostName(hostName); // TODO this shouldn't be needed
        
        String mgmtUserProperties = JBossAsManagedConfiguration.loadConfigFileFromContextClassLoader("mgmt-users/mgmt-users.properties");
        configuration.setMgmtUsersFile(mgmtUserProperties);
        
        final File output = new File("target" + File.separator + "domains" + File.separator + testConfiguration + File.separator + hostName);
        new File(output, "configuration").mkdirs(); // TODO this should not be necessary
        configuration.setDomainDirectory(output.getAbsolutePath());

        return configuration;

    }
    
    static File hackFixDomainConfig(File hostConfigFile) {
        final File file;
        final BufferedWriter writer;
        try {
            file = File.createTempFile("domain", ".xml", hostConfigFile.getAbsoluteFile().getParentFile());
            file.deleteOnExit();
            writer = new BufferedWriter(new FileWriter(file));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            BufferedReader reader = new BufferedReader(new FileReader(hostConfigFile));
            try {
                String line = reader.readLine();
                while (line != null) {
                    int start = line.indexOf("java.net.preferIPv4Stack");
                    if (start < 0) {
                        writer.write(line);
                    }
                    writer.write("\n");
                    
                    if(line.trim().startsWith("<system-properties")){
                        writer.write("<property name=\"org.jbpm.designer.perspective\" value=\"${org.jbpm.designer.perspective:"+JBPM_DESIGNER_PERSPECTIVE+"}\"/>");
                        writer.write("\n");
                        writer.write("<property name=\"org.jbpm.designer.helper\" value=\"${file.separator:"+JBPM_DESIGNER_PERSPECTIVE+"}\"/>");
                        writer.write("\n");
                    }
                    
                    line = reader.readLine();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                safeClose(reader);
                safeClose(writer);
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        return file;
    }
}
