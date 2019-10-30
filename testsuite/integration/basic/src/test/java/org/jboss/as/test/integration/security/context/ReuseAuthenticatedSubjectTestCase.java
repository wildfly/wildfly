/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.security.context;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.integration.security.common.AbstractSecurityDomainsServerSetupTask;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.integration.security.common.config.SecurityDomain;
import org.jboss.as.test.integration.security.common.config.SecurityModule;
import org.jboss.as.test.integration.security.loginmodules.common.CounterLoginModule;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test whether if web app and EJB belong to the same security domain then the user is not unnecessarily reauthenticated when
 * the web app invokes an EJB.
 *
 * @author olukas
 */
@RunWith(Arquillian.class)
@ServerSetup({ReuseAuthenticatedSubjectTestCase.SecurityDomainsSetup.class})
@RunAsClient
public class ReuseAuthenticatedSubjectTestCase {

    private static final Logger LOGGER = Logger.getLogger(ReuseAuthenticatedSubjectTestCase.class);

    private static final String USER = "user";
    private static final String PASSWORD = "password";
    private static final String ROLE = "admin";

    private static final String USERS_PROPERTIES_CONTENT = USER + "=" + PASSWORD;
    private static final String ROLES_PROPERTIES_CONTENT = USER + "=" + ROLE;

    private static final String DEPLOYMENT_NAME = "dep";

    /**
     * Test whether if web app and EJB belong to the same security domain then the user is not unnecessarily reauthenticated
     * when the web app invokes an EJB.
     *
     * @param url
     * @throws Exception
     */
    @OperateOnDeployment(DEPLOYMENT_NAME)
    @Test
    public void testEjbInSameSecurityDomain(@ArquillianResource URL url) throws Exception {
        resetCounter(url);

        final URL servletUrl = new URL(url.toExternalForm() + ReuseAuthenticatedSubjectServlet.SERVLET_PATH.substring(1) + "?"
                + ReuseAuthenticatedSubjectServlet.SAME_SECURITY_DOMAIN_PARAM + "=true");
        String servletOutput = Utils.makeCallWithBasicAuthn(servletUrl, USER, PASSWORD, 200);
        Assert.assertEquals("Unexpected servlet output after EJB call", EjbSecurityDomainAsServletImpl.SAY_HELLO, servletOutput);

        Assert.assertEquals("Authenticated subject was not reused for EJB from the same security domain", "1", getCounter(url));
    }

    /**
     * Test whether if web app and EJB belong to the different security domain then the user is authenticated for both web app
     * and EJB invoked from that app.
     *
     * @param url
     * @throws Exception
     */
    @OperateOnDeployment(DEPLOYMENT_NAME)
    @Test
    public void testEjbInDifferentSecurityDomain(@ArquillianResource URL url) throws Exception {
        resetCounter(url);

        final URL servletUrl = new URL(url.toExternalForm() + ReuseAuthenticatedSubjectServlet.SERVLET_PATH.substring(1) + "?"
                + ReuseAuthenticatedSubjectServlet.SAME_SECURITY_DOMAIN_PARAM + "=false");
        String servletOutput = Utils.makeCallWithBasicAuthn(servletUrl, USER, PASSWORD, 200);
        Assert.assertEquals("Unexpected servlet output after EJB call", EjbOwnSecurityDomainImpl.SAY_HELLO, servletOutput);

        Assert.assertEquals("Authenticated subject was reused for EJB from the different security domain", "2", getCounter(url));
    }

    /**
     * Creates {@link WebArchive} (WAR) for given deployment name.
     *
     * @return
     */
    @Deployment(name = DEPLOYMENT_NAME)
    public static WebArchive deployment() {
        LOGGER.trace("Starting deployment " + DEPLOYMENT_NAME);

        final WebArchive war = ShrinkWrap.create(WebArchive.class, DEPLOYMENT_NAME + ".war");
        war.addClasses(ReuseAuthenticatedSubjectServlet.class,
                EjbOwnSecurityDomain.class,
                EjbOwnSecurityDomainImpl.class,
                EjbSecurityDomainAsServlet.class,
                EjbSecurityDomainAsServletImpl.class,
                CounterLoginModule.class,
                CounterServlet.class
        );
        war.addAsWebInfResource(ReuseAuthenticatedSubjectTestCase.class.getPackage(), "web-basic-authn.xml", "web.xml");
        war.addAsResource(new StringAsset(USERS_PROPERTIES_CONTENT), "users.properties");
        war.addAsResource(new StringAsset(ROLES_PROPERTIES_CONTENT), "roles.properties");
        war.addAsWebInfResource(new StringAsset("<jboss-web>" + //
                "<security-domain>" + EjbSecurityDomainAsServletImpl.SECURITY_DOMAIN + "</security-domain>" + //
                "</jboss-web>"), "jboss-web.xml");

        return war;
    }

    private void resetCounter(URL url) throws Exception {
        final URL servletUrl = new URL(url.toExternalForm() + CounterServlet.SERVLET_PATH.substring(1) + "?"
                + CounterServlet.RESET_PARAM + "=true");
        String counter = Utils.makeCall(servletUrl.toURI(), 200);
        Assert.assertEquals("Counter in CounterLoginModule was not successfully reset", "0", counter);
    }

    private String getCounter(URL url) throws Exception {
        final URL counterServletUrl = new URL(url.toExternalForm() + CounterServlet.SERVLET_PATH.substring(1) + "?"
                + CounterServlet.RESET_PARAM);
        String counter = Utils.makeCall(counterServletUrl.toURI(), 200);
        return counter;
    }

    static class SecurityDomainsSetup extends AbstractSecurityDomainsServerSetupTask {

        @Override
        protected SecurityDomain[] getSecurityDomains() throws Exception {

            final Map<String, String> lmOptions = new HashMap<String, String>();
            final SecurityModule.Builder usersRolesLoginModuleBuilder = new SecurityModule.Builder()
                    .name("UsersRoles")
                    .options(lmOptions);

            final SecurityModule.Builder counterLoginModule = new SecurityModule.Builder()
                    .name(CounterLoginModule.class.getName());

            final SecurityDomain sd1 = new SecurityDomain.Builder()
                    .name(EjbSecurityDomainAsServletImpl.SECURITY_DOMAIN)
                    .loginModules(
                            usersRolesLoginModuleBuilder.build(),
                            counterLoginModule.build())
                    .build();

            final SecurityDomain sd2 = new SecurityDomain.Builder()
                    .name(EjbOwnSecurityDomainImpl.SECURITY_DOMAIN)
                    .loginModules(
                            usersRolesLoginModuleBuilder.build(),
                            counterLoginModule.build())
                    .build();

            return new SecurityDomain[]{sd1, sd2};
        }

    }

}
