/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.security.jacc.context;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.security.SecurityPermission;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@RunAsClient
public class PolicyContextTestCase {

    private static Logger LOGGER = Logger.getLogger(PolicyContextTestCase.class);

    @Deployment(name = "ear")
    public static EnterpriseArchive createDeployment() {
        final String earName = "ear-jacc-context";

        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, earName + ".ear");
        final JavaArchive jar = createJar(earName);
        final WebArchive war = createWar(earName);
        ear.addAsModule(war);
        ear.addAsModule(jar);

        ear.addAsManifestResource(createPermissionsXmlAsset(new SecurityPermission("setPolicy")), "permissions.xml");

        return ear;
    }

    @Test
    public void testHttpServletRequestFromPolicyContext(@ArquillianResource URL webAppURL) throws Exception {
        String externalFormURL = webAppURL.toExternalForm();
        String servletURL = externalFormURL.substring(0, externalFormURL.length() - 1) + PolicyContextTestServlet.SERVLET_PATH;
        LOGGER.trace("Testing Jakarta Authorization Context: " + servletURL);

        String response = HttpRequest.get(servletURL, 1000, SECONDS);
        assertTrue(response.contains("HttpServletRequest successfully obtained from both containers."));
    }

    private static JavaArchive createJar(final String jarName) {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, jarName + ".jar");
        jar.addClasses(PolicyContextTestBean.class);
        jar.addAsManifestResource(PolicyContextTestCase.class.getPackage(), "ejb-jar.xml", "ejb-jar.xml");
        return jar;
    }

    private static WebArchive createWar(final String warName) {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, warName + ".war");
        war.addClass(PolicyContextTestServlet.class);
        return war;
    }

}
