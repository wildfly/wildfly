/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.security.jaas;

import static org.junit.Assert.assertEquals;

import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.security.Constants;
import org.jboss.as.test.integration.security.common.AbstractSecurityDomainsServerSetupTask;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.integration.security.common.config.SecurityDomain;
import org.jboss.as.test.integration.security.common.config.SecurityModule;
import org.jboss.as.test.integration.security.common.ejb3.Hello;
import org.jboss.as.test.integration.security.common.ejb3.HelloBean;
import org.jboss.as.test.integration.security.loginmodules.UsersRolesLoginModuleTestCase;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * A JUnit 4 testcase with regression tests for JAAS identity caching issues.
 *
 * @author Josef Cacek
 */
@RunWith(Arquillian.class)
@ServerSetup({JAASIdentityCachingTestCase.SecurityDomainsSetup.class})
@RunAsClient
public class JAASIdentityCachingTestCase {

    private static final String TEST_NAME = "jaas-test";
    private static final String EAR_BASE_NAME = "ear-" + TEST_NAME;
    private static final String WAR_BASE_NAME = "war-" + TEST_NAME;
    private static final String JAR_BASE_NAME = "jar-" + TEST_NAME;
    private static final String LM_LIB_NAME = "custom-login-module.jar";

    // Public methods --------------------------------------------------------

    /**
     * Creates {@link EnterpriseArchive} deployment.
     */
    @Deployment
    public static EnterpriseArchive earDeployment() {
        final JavaArchive libJar = ShrinkWrap.create(JavaArchive.class, LM_LIB_NAME).addClasses(CustomLoginModule.class,
                CustomPrincipal.class);
        final JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, JAR_BASE_NAME + ".jar")
                .addClasses(Hello.class, HelloBean.class)
                .addAsManifestResource(Utils.getJBossEjb3XmlAsset(TEST_NAME), "jboss-ejb3.xml");
        final WebArchive war = ShrinkWrap.create(WebArchive.class, WAR_BASE_NAME + ".war")
                .addClasses(HelloEJBCallServlet.class, LMCounterServlet.class) //
                .addAsWebInfResource(UsersRolesLoginModuleTestCase.class.getPackage(), "web-basic-authn.xml", "web.xml") //
                .addAsWebInfResource(Utils.getJBossWebXmlAsset(TEST_NAME), "jboss-web.xml");
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, EAR_BASE_NAME + ".ear");
        ear.addAsLibrary(libJar);
        ear.addAsModule(war);
        ear.addAsModule(ejbJar);

        return ear;
    }

    /**
     * Test how many times is called login() method of {@link CustomLoginModule} and if the response from HelloBean is the
     * expected one.
     *
     * @param webAppURL
     * @throws Exception
     */
    @Test
    public void test(@ArquillianResource URL webAppURL) throws Exception {
        final URI greetingUri = new URI(webAppURL.toExternalForm() + HelloEJBCallServlet.SERVLET_PATH.substring(1) + "?"
                + HelloEJBCallServlet.PARAM_JNDI_NAME + "="
                + URLEncoder.encode("java:app/" + JAR_BASE_NAME + "/" + HelloBean.class.getSimpleName(), "UTF-8"));
        final URI counterUri = new URI(webAppURL.toExternalForm() + LMCounterServlet.SERVLET_PATH.substring(1));
        BasicCredentialsProvider credentialsProvider =  new BasicCredentialsProvider();
        final UsernamePasswordCredentials credentials = new UsernamePasswordCredentials("admin", CustomLoginModule.PASSWORD);
        credentialsProvider.setCredentials(new AuthScope(greetingUri.getHost(), greetingUri.getPort()),
                            credentials);
        try (CloseableHttpClient httpClient = HttpClients.createDefault()){
            final HttpGet getCounter = new HttpGet(counterUri);
            final HttpGet getGreeting = new HttpGet(greetingUri);
            HttpResponse response = httpClient.execute(getGreeting);
            assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatusLine().getStatusCode());
            EntityUtils.consume(response.getEntity());

            //check if LoginModule #login() counter is initialized correctly
            HttpClientContext context = HttpClientContext.create();
                    context.setCredentialsProvider(credentialsProvider);
            response = httpClient.execute(getCounter, context);
            assertEquals("0", EntityUtils.toString(response.getEntity()));


            //make 2 calls to the servlet
            response = httpClient.execute(getGreeting, context);
            assertEquals("Hello Caller!", EntityUtils.toString(response.getEntity()));
            response = httpClient.execute(getGreeting, context);
            assertEquals("Hello Caller!", EntityUtils.toString(response.getEntity()));

            //There should be only one call to login() method
            response = httpClient.execute(getCounter, context);
            assertEquals("1", EntityUtils.toString(response.getEntity()));
        }
    }

    // Embedded classes ------------------------------------------------------

    /**
     * A {@link ServerSetupTask} instance which creates security domains for this test case.
     *
     * @author Josef Cacek
     */
    static class SecurityDomainsSetup extends AbstractSecurityDomainsServerSetupTask {

        /**
         * @see org.jboss.as.test.integration.security.common.AbstractSecurityDomainsServerSetupTask#getSecurityDomains()
         */
        @Override
        protected SecurityDomain[] getSecurityDomains() {
            return new SecurityDomain[]{new SecurityDomain.Builder()
                    .name(TEST_NAME)
                    .cacheType("default")
                    .loginModules(
                            new SecurityModule.Builder().name(CustomLoginModule.class.getName()).flag(Constants.REQUIRED)
                                    .putOption(CustomLoginModule.MODULE_OPTION_ROLE, HelloBean.ROLE_ALLOWED).build()).build()};
        }
    }
}
