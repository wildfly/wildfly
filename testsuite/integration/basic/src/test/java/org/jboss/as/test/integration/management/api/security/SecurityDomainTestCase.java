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
package org.jboss.as.test.integration.management.api.security;

import org.junit.AfterClass;
import org.junit.Ignore;
import org.jboss.as.test.integration.management.cli.GlobalOpsTestCase;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.TimeUnit;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.integration.management.base.AbstractMgmtTestBase;
import org.jboss.as.test.integration.management.util.ModelUtil;
import org.jboss.as.test.integration.management.util.SecuredServlet;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import static org.jboss.as.test.integration.management.util.ModelUtil.createOpNode;


/**
 *
 * @author Dominik Pospisil <dpospisi@redhat.com>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class SecurityDomainTestCase extends AbstractMgmtTestBase {

    @ArquillianResource
    URL url;

    @Deployment
    public static Archive<?> getDeployment() {
        JavaArchive ja = ShrinkWrap.create(JavaArchive.class, "dummy.jar");
        ja.addClass(GlobalOpsTestCase.class);
        return ja;
    }

    @Deployment(name = "secured-servlet", managed = false)
    public static Archive<?> getDeployment2() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "SecurityDomainTestCase.war");
        war.addClass(SecuredServlet.class);
        war.addAsWebInfResource(new StringAsset("<jboss-web><security-domain>test</security-domain></jboss-web>"), "jboss-web.xml");
        war.addAsWebInfResource(new StringAsset(
                "<web-app version=\"2.5\"><login-config><auth-method>BASIC</auth-method></login-config></web-app>"), "web.xml");
        return war;
    }

    @Before
    public void before() throws IOException {
        initModelControllerClient(url.getHost(), MGMT_PORT);
    }

    @AfterClass
    public static void after() throws IOException {
        closeModelControllerClient();
    }

    @Test
    public void testAddRemoveSecurityDomain(@ArquillianResource Deployer deployer) throws Exception {

        
        // add security domain
        ModelNode addOp = createOpNode("subsystem=security/security-domain=test", "add");

        // setup lospecify login module options
        ModelNode addLoginModuleOp = createOpNode("subsystem=security/security-domain=test/authentication=classic", "add");
        ModelNode loginModule = new ModelNode();
        loginModule.get("code").set("Simple");
        loginModule.get("flag").set("required");
        addLoginModuleOp.get("login-modules").add(loginModule);
        
        executeOperation(ModelUtil.createCompositeNode(new ModelNode[] {addOp, addLoginModuleOp}));
        
        // deploy secured servlet
        deployer.deploy("secured-servlet");

        // check that the servlet is secured
        boolean failed = false;
        try {
            String response = HttpRequest.get(url.toString() + "/SecurityDomainTestCase/SecuredServlet", 10, TimeUnit.SECONDS);
        } catch (Exception e) {
            assertTrue(e.toString().contains("Status 401"));
            failed = true;
        }
        assertTrue(failed);

        // check that the security domain is active
        try {
            String response = HttpRequest.get(url.toString() + "/SecurityDomainTestCase/SecuredServlet", "test", "test", 10, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new Exception("Unable to access secured servlet.", e);
        }

        // undeploy servlet
        deployer.undeploy("secured-servlet");


        // remove security domain
        ModelNode op = createOpNode("subsystem=security/security-domain=test", "remove");
        executeOperation(op);

        // check that the security domain is removed
        failed = false;
        try {
            deployer.deploy("secured-servlet");
        } catch (Exception e) {
            failed = true;
        }
        assertTrue(failed);

    }
}
