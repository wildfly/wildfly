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
import org.jboss.as.test.integration.management.api.AbstractMgmtTestBase;
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

/**
 *
 * @author Dominik Pospisil <dpospisi@redhat.com>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class SecurityDomainTestCase extends AbstractMgmtTestBase {

    @ArquillianResource
    static URL url;

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
        return war;
    }

    @Before
    public void before() throws IOException {
        super.init(url.getHost(), MGMT_PORT);
    }

    @Test
    public void testAddRemoveSecurityDomain(@ArquillianResource Deployer deployer) throws Exception {

        // specify login module options
        ModelNode loginModule = new ModelNode();
        loginModule.get("code").set("SimpleUsers");
        loginModule.get("flag").set("required");
        ModelNode moduleOptions = new ModelNode();
        moduleOptions.add("testUser", "testPassword");
        loginModule.get("module-options").set(moduleOptions);

        // add security domain
        ModelNode op = createOpNode("subsystem=security/security-domain=test", "add");
        op.get("authentication").add(loginModule);
        executeOperation(op);

        // deploy secured servlet
        deployer.deploy("secured-servlet");

        try {
            String response = HttpRequest.get(url.toString() + "/SecuredServlet", 10, TimeUnit.SECONDS);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // undeploy servlet
        deployer.undeploy("secured-servlet");
    }
}
