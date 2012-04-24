/*
 * JBoss, a division of Red Hat
 * Copyright 2006, Red Hat Middleware, LLC, and individual contributors as indicated
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
package org.jboss.as.test.integration.web.sso;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.test.integration.management.util.ModelUtil.createOpNode;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeoutException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.test.integration.web.SSOBaseCase;
import org.jboss.as.test.integration.web.sso.interfaces.StatelessSession;
import org.jboss.as.test.shared.RetryTaskExecutor;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests of web app single sign-on
 * 
 * @author Scott.Stark@jboss.org
 * @author lbarreiro@redhat.com
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(SingleSignOnUnitTestCase.SingleSignOnUnitTestCaseSetup.class)
//@Ignore(value="ARQ-791 Arquillian is unable to reconnect to JMX server if the connection is lost")
public class SingleSignOnUnitTestCase {

    private static Logger log = Logger.getLogger(SingleSignOnUnitTestCase.class);

    static class SingleSignOnUnitTestCaseSetup implements ServerSetupTask {

        public void setup(final ManagementClient managementClient, final String containerId) throws Exception {
            SingleSignOnUnitTestCase.addSso(managementClient.getControllerClient());
            restartServer(managementClient.getControllerClient());
        }

        public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {
            SingleSignOnUnitTestCase.removeSso(managementClient.getControllerClient());
        }
    }

    public static void addSso(ModelControllerClient client) throws Exception {
        final List<ModelNode> updates = new ArrayList<ModelNode>();

        // SSO element name must be 'configuration'
        updates.add(createOpNode("subsystem=web/virtual-server=default-host/sso=configuration", ADD));

        applyUpdates(updates, client);
    }

    public static void removeSso(final ModelControllerClient client) throws Exception {
        final List<ModelNode> updates = new ArrayList<ModelNode>();

        updates.add(createOpNode("subsystem=web/virtual-server=default-host/sso=configuration", REMOVE));

        applyUpdates(updates, client);
    }

    public static void applyUpdates(final List<ModelNode> updates, final ModelControllerClient client) throws Exception {
        for (ModelNode update : updates) {
            log.info("+++ Update on " + client + ":\n" + update.toString());
            ModelNode result = client.execute(new OperationBuilder(update).build());
            if (result.hasDefined("outcome") && "success".equals(result.get("outcome").asString())) {
                if (result.hasDefined("result"))
                    log.info(result.get("result"));
            } else if (result.hasDefined("failure-description")) {
                throw new RuntimeException(result.get("failure-description").toString());
            } else {
                throw new RuntimeException("Operation not successful; outcome = " + result.get("outcome"));
            }
        }
    }

    // Reload operation is not handled well by Arquillian
    // See ARQ-791: JMX: Arquillian is unable to reconnect to JMX server if the connection is lost
    private static void restartServer(final ModelControllerClient client) {
        try {
            applyUpdates(Arrays.asList(createOpNode(null, "reload")), client);
        } catch (Exception e) {
            throw new RuntimeException("Restart operation not successful. " + e.getMessage());
        }
        try {
            RetryTaskExecutor<Boolean> rte = new RetryTaskExecutor<Boolean>();
            rte.retryTask(new Callable<Boolean>() {
                public Boolean call() throws Exception {
                    ModelNode result = client.execute(new OperationBuilder(createOpNode(null, READ_RESOURCE_OPERATION)).build());
                    if (result.hasDefined("outcome") && "success".equals(result.get("outcome").asString())) {
                        if (result.hasDefined("result"))
                            log.info(result.get("result"));
                        return true;
                    } else {
                        log.info("Server is down.");
                        throw new Exception("Connector not available.");
                    }
                }
            });
        } catch (TimeoutException e) {
            throw new RuntimeException("Timeout on restart operation. " + e.getMessage());
        }
        log.info("Server is up.");
    }

    /**
     * One time setup for all SingleSignOnUnitTestCase unit tests
     */
    @Deployment(name = "web-sso.ear", testable = false)
    public static EnterpriseArchive deployment() {
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        String resourcesLocation = "org/jboss/as/test/integration/web/sso/resources/";

        WebArchive war1 = SingleSignOnUnitTestCase.createSsoWar("sso-form-auth1.war");
        WebArchive war2 = SingleSignOnUnitTestCase.createSsoWar("sso-form-auth2.war");
        WebArchive war3 = SingleSignOnUnitTestCase.createSsoWar("sso-with-no-auth.war");

        // Remove jboss-web.xml so the war will not have an authenticator
        war3.delete(war3.get("WEB-INF/jboss-web.xml").getPath());

        JavaArchive webEjbs = ShrinkWrap.create(JavaArchive.class, "jbosstest-web-ejbs.jar");
        webEjbs.addAsManifestResource(tccl.getResource(resourcesLocation + "ejb-jar.xml"), "ejb-jar.xml");
        webEjbs.addAsManifestResource(tccl.getResource(resourcesLocation + "jboss.xml"), "jboss.xml");
        webEjbs.addPackage(StatelessSession.class.getPackage());

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "web-sso.ear");
        ear.setApplicationXML(tccl.getResource(resourcesLocation + "application.xml"));

        ear.addAsModule(war1);
        ear.addAsModule(war2);
        ear.addAsModule(war3);
        ear.addAsModule(webEjbs);

        log.info(war1.toString(true));
        log.info(war2.toString(true));
        log.info(war3.toString(true));
        log.info(webEjbs.toString(true));
        log.info(ear.toString(true));
        return ear;
    }

    private static WebArchive createSsoWar(String warName) {
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        String resourcesLocation = "org/jboss/as/test/integration/web/sso/resources/";

        WebArchive war = ShrinkWrap.create(WebArchive.class, warName);
        war.setWebXML(tccl.getResource(resourcesLocation + "web-form-auth.xml"));
        war.addAsWebInfResource(tccl.getResource(resourcesLocation + "jboss-web.xml"), "jboss-web.xml");

        war.addAsWebResource(tccl.getResource(resourcesLocation + "error.html"), "error.html");
        war.addAsWebResource(tccl.getResource(resourcesLocation + "index.html"), "index.html");
        war.addAsWebResource(tccl.getResource(resourcesLocation + "index.jsp"), "index.jsp");
        war.addAsWebResource(tccl.getResource(resourcesLocation + "login.html"), "login.html");

        war.addClass(EJBServlet.class);
        war.addClass(LogoutServlet.class);

        return war;
    }

    /**
     * Test single sign-on across two web apps using form based auth
     */
    @Test
    public void testFormAuthSingleSignOn(@ArquillianResource URL baseURLNoAuth) throws Exception {
        log.info("+++ testFormAuthSingleSignOn");
        SSOBaseCase.executeFormAuthSingleSignOnTest(baseURLNoAuth, baseURLNoAuth, log);
    }

    /**
     * Test single sign-on across two web apps using form based auth
     */
    @Test
    public void testNoAuthSingleSignOn(@ArquillianResource URL baseURLNoAuth) throws Exception {
        log.info("+++ testNoAuthSingleSignOn");
        SSOBaseCase.executeNoAuthSingleSignOnTest(baseURLNoAuth, baseURLNoAuth, log);
    }

}
