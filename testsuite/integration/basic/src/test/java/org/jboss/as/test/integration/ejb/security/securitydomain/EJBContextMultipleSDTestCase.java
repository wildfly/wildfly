/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ejb.security.securitydomain;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.test.integration.ejb.security.securitydomain.ejb.Hello;
import org.jboss.as.test.integration.ejb.security.securitydomain.ejb.Info;
import org.jboss.as.test.integration.management.util.CLITestUtil;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.as.test.integration.management.util.ServerReload;
import org.jboss.as.test.shared.ServerSnapshot;
import org.jboss.as.test.shared.util.AssumeTestGroupUtil;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * @author Francesco Marchioni
 * JIRA: https://issues.redhat.com/browse/WFLY-12516
 * EJBContext principal is not popped back after invoking another EJB using a different Security Domain
 */

@RunWith(Arquillian.class)
@ServerSetup(EJBContextMultipleSDTestCase.EJBContextMultipleSDTestCaseServerSetup.class)
public class EJBContextMultipleSDTestCase {

    private static final String ARCHIVE_NAME = "EJBContextMultipleSDTestCase";
    protected Logger log = Logger.getLogger(this.getClass().getSimpleName());

    @Deployment
    public static Archive<?> deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar");
        jar.addAsManifestResource(Hello.class.getPackage(), "jboss-ejb3.xml", "jboss-ejb3.xml");
        jar.addPackage(Hello.class.getPackage());
        return jar;
    }

    @ArquillianResource
    private InitialContext initialContext;

    private static final String HOST_PORT = "localhost:8080";
    private static final String REMOTE_HTTP = "remote+http://" + HOST_PORT;
    private static final String CTX_FACTORY = "org.wildfly.naming.client.WildFlyInitialContextFactory";

    private static final String USER = "ADMIN";
    private static final String PWD = "redhat1!";

    private static Context getInitialContext() throws NamingException {
        Properties ejbClientProperties = new Properties();
        ejbClientProperties.put(Context.INITIAL_CONTEXT_FACTORY, CTX_FACTORY);
        ejbClientProperties.put(Context.PROVIDER_URL, REMOTE_HTTP);
        ejbClientProperties.put(Context.SECURITY_PRINCIPAL, USER);
        ejbClientProperties.put(Context.SECURITY_CREDENTIALS, PWD);

        Context context = new InitialContext(ejbClientProperties);
        return context;
    }

    @BeforeClass
    public static void beforeClass() {
        AssumeTestGroupUtil.assumeElytronProfileEnabled(); // PicketBox specific scenario, Elytron testing not required.
    }

    @Test
    public void testSequentialInvocations() throws Exception {
        Context context = getInitialContext();

        Info info = new Info("Test1: client -> ejb1 (MySecurityDomain/SimplePrincipal) -> ejb2 (MyNonValidatingSecurityDomain/MyPrincipal)");
        Hello hello = (Hello) context.lookup(Hello.HelloOne);
        List<String> list = hello.sayHelloSeveralTimes(info);

        Assert.assertEquals("hello", "hello");
        Assert.assertEquals(list.get(0), "Correct: org.jboss.security.SimplePrincipal == org.jboss.security.SimplePrincipal");
        Assert.assertEquals(list.get(1), "Correct: loginmodule.custom.MyPrincipal == loginmodule.custom.MyPrincipal");
        Assert.assertEquals(list.get(2), "Correct: org.jboss.security.SimplePrincipal == org.jboss.security.SimplePrincipal");

        info = new Info("Test2: client -> ejb2 (MyNonValidatingSecurityDomain/MyPrincipal) -> ejb1 (MySecurityDomain/SimplePrincipal)");
        hello = (Hello) context.lookup(Hello.HelloTwo);
        list = hello.sayHelloSeveralTimes(info);

        Assert.assertEquals(list.get(0), "Correct: loginmodule.custom.MyPrincipal == loginmodule.custom.MyPrincipal");
        Assert.assertEquals(list.get(1), "Correct: org.jboss.security.SimplePrincipal == org.jboss.security.SimplePrincipal");
        Assert.assertEquals(list.get(2), "Correct: loginmodule.custom.MyPrincipal == loginmodule.custom.MyPrincipal");
    }

    static class EJBContextMultipleSDTestCaseServerSetup implements ServerSetupTask {
        private AutoCloseable snapshot;
        protected Logger log = Logger.getLogger(this.getClass().getSimpleName());
        private ModelControllerClient modelControllerClient;

        public static String executeCommand(CommandContext ctx, ModelNode modelNode) {

            ModelControllerClient client = ctx.getModelControllerClient();
            if (client != null) {
                try {
                    ModelNode response = client.execute(modelNode);
                    return (response.toJSONString(true));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                throw new RuntimeException("Connection Error! The ModelControllerClient is not available.");
            }
            return null;
        }

        @Override
        public void setup(final ManagementClient managementClient, final String containerId) throws Exception {
            snapshot = ServerSnapshot.takeSnapshot(managementClient);
            log.info("Installing modules and Security Manager");
            try {

                CommandContext ctx = CLITestUtil.getCommandContext();
                ctx.connectController();
                ModelNode cliCommand = null;
                File fileCLI = null;
                File fileModule = null;

                // Install module and SecurityManager
                URL urlCLI = getClass().getResource("securitydomain/cli/WFLY-12516.deploy.cli");
                fileCLI = new File(urlCLI.toURI());

                String request1 = "run-batch --file=" + fileCLI.getAbsolutePath();
                cliCommand = ctx.buildRequest(request1);
                String output = executeCommand(ctx, cliCommand);

                log.info(output);
                ServerReload.executeReloadAndWaitForCompletion(managementClient.getControllerClient());
                // Gather info in order to write the application-users.properties
                String strServerProperties = "/core-service=platform-mbean/type=runtime:read-attribute(name=system-properties)";
                cliCommand = ctx.buildRequest(strServerProperties);
                output = executeCommand(ctx, cliCommand);

                String configFolder = output.replaceAll("^.*jboss.server.config.dir\" : \"", "").replaceAll("\".*", "");

                if (!new File(configFolder).exists()) {
                    throw new RuntimeException("Unable to find the jboss.server.config.dir :" + configFolder);
                }
                String usersProperties = configFolder + File.separator + "application-users.properties";

                Files.write(Paths.get(usersProperties), "\nADMIN=55372d397fd6f1dc0ee6ff989d0c93ec".getBytes(), StandardOpenOption.APPEND);

            } catch (Exception e) {

                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                String exceptionAsString = sw.toString();
                log.info(exceptionAsString);

            }
        }

        @Override
        public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {
            try {
                log.info("Shutdown in progress");
                File fileCLI = null;
                CommandContext ctx = CLITestUtil.getCommandContext();
                ctx.connectController();
                ModelNode cliCommand = null;

                URL urlCLI = getClass().getResource("securitydomain/cli/WFLY-12516.undeploy.cli");
                fileCLI = new File(urlCLI.toURI());
                String request1 = "run-batch --file=" + fileCLI.getAbsolutePath();
                cliCommand = ctx.buildRequest(request1);
                String output = executeCommand(ctx, cliCommand);
                log.info(output);
            } finally {
                snapshot.close();
            }
        }

        private void removeContentItem(final ManagementClient managementClient, final String overlayName, final String content) throws IOException, MgmtOperationException {
        }

        private void removeDeploymentItem(final ManagementClient managementClient, final String overlayName, final String deploymentRuntimeName) throws IOException, MgmtOperationException {
        }
    }
}
