/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.security.loginmodules;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.categories.CommonCriteria;
import org.jboss.as.test.integration.security.common.AbstractSecurityDomainSetup;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.integration.security.common.servlets.PrincipalPrintingServlet;
import org.jboss.logging.Logger;
import org.jboss.security.auth.spi.IdentityLoginModule;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

/**
 * Tests of login via IdentityLoginModule
 *
 * @author <a href="mailto:jlanik@redhat.com">Jan Lanik</a>.
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({IdentityLoginModuleTestCase.SecurityDomain1Setup.class, IdentityLoginModuleTestCase.SecurityDomain2Setup.class})
@Category(CommonCriteria.class)
public class IdentityLoginModuleTestCase {

    private static Logger log = Logger.getLogger(IdentityLoginModuleTestCase.class);

    private static final String DEP1 = "IdentityLoginModule-defaultPrincipal";

    static class SecurityDomain1Setup extends AbstractSecurityDomainSetup {

        @Override
        protected String getSecurityDomainName() {
            return "TestIdentityLoginDomain";
        }

        @Override
        public void setup(final ManagementClient managementClient, final String containerId) throws Exception {
            log.trace("adding module options");
            Map<String, String> moduleOptionsMap = new HashMap<String, String>();
            moduleOptionsMap.put("roles", "role1,role2");

            log.trace("creating security domain: TestIdentityLoginDomain");
            createSecurityDomain(IdentityLoginModule.class, moduleOptionsMap, managementClient.getControllerClient());
            log.trace("security domain created");
        }
    }

    static class SecurityDomain2Setup extends AbstractSecurityDomainSetup {

        @Override
        protected String getSecurityDomainName() {
            return "TestIdentityLoginDomain2";
        }

        @Override
        public void setup(final ManagementClient managementClient, final String containerId) throws Exception {

            log.trace("adding module options");
            Map<String, String> moduleOptionsMap = new HashMap<String, String>();
            moduleOptionsMap.put("roles", "role1,role2");
            moduleOptionsMap.put("principal", "SomeName");

            log.trace("creating security domain: TestIdentityLoginDomain");
            createSecurityDomain(IdentityLoginModule.class, moduleOptionsMap, managementClient.getControllerClient());
            log.trace("security domain created");

        }
    }

    /**
     * Test deployment with <module-option name="roles" value="role1,role2"/>
     */
    @Deployment(name = DEP1, order = 1)
    public static WebArchive appDeployment1() {
        log.trace("create" + DEP1 + "deployment");

        WebArchive war = ShrinkWrap.create(WebArchive.class, DEP1 + ".war");
        war.addClass(PrincipalPrintingServlet.class);
        war.setWebXML(Utils
                .getResource("org/jboss/as/test/integration/security/loginmodules/deployments/IdentityLoginModule/web.xml"));
        war.addAsWebInfResource(
                Utils.getResource("org/jboss/as/test/integration/security/loginmodules/deployments/IdentityLoginModule/dep1/jboss-web.xml"),
                "jboss-web.xml");
        return war;
    }

    private static final String DEP2 = "IdentityLoginModule-customPrincipal";

    /**
     * Test deployment with <module-option name="principal" value="SomeName"/> <module-option name="roles" value="role1,role2"/>
     */
    @Deployment(name = DEP2, order = 2)
    public static WebArchive appDeployment2() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, DEP2 + ".war");
        war.addClass(PrincipalPrintingServlet.class);
        war.setWebXML(Utils
                .getResource("org/jboss/as/test/integration/security/loginmodules/deployments/IdentityLoginModule/web.xml"));
        war.addAsWebInfResource(
                Utils.getResource("org/jboss/as/test/integration/security/loginmodules/deployments/IdentityLoginModule/dep2/jboss-web.xml"),
                "jboss-web.xml");
        return war;
    }

    /**
     * Tests assignment of default principal name to the caller
     */
    @OperateOnDeployment(DEP1)
    @Test
    public void testDefaultPrincipal(@ArquillianResource URL url) {
        assertPrincipal(url, "guest");
    }

    /**
     * Tests assignment of custom principal name to the caller
     */
    @OperateOnDeployment(DEP2)
    @Test
    public void testCustomPrincipal(@ArquillianResource URL url) {
        assertPrincipal(url, "SomeName");
    }

    /**
     * Calls {@link PrincipalPrintingServlet} and checks if the returned principal name is the expected one.
     *
     * @param url
     * @param expectedPrincipal
     * @return Principal name returned from {@link PrincipalPrintingServlet}
     */
    private String assertPrincipal(URL url, String expectedPrincipal) {
        DefaultHttpClient httpclient = new DefaultHttpClient();
        Credentials creds = new UsernamePasswordCredentials("anyUsername");
        httpclient.getCredentialsProvider().setCredentials(new AuthScope(url.getHost(), url.getPort()), creds);
        HttpGet httpget = new HttpGet(url.toExternalForm());
        String text;

        try {
            HttpResponse response = httpclient.execute(httpget);
            assertEquals("Unexpected status code", HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            text = EntityUtils.toString(response.getEntity());
        } catch (IOException e) {
            throw new RuntimeException("Servlet response IO exception", e);
        }

        assertEquals("Unexpected principal name assigned by IdentityLoinModule", expectedPrincipal, text);
        return text;
    }
}
