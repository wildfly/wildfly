/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2015, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.web.security.digest;

import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.jboss.as.test.integration.security.WebSecurityPasswordBasedBase;
import org.jboss.as.test.integration.security.common.AbstractSecurityDomainsServerSetupTask;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.integration.security.common.config.SecurityDomain;
import org.jboss.as.test.integration.security.common.config.SecurityModule;
import org.jboss.as.test.integration.security.common.servlets.SimpleSecuredServlet;
import org.jboss.as.test.integration.security.common.servlets.SimpleServlet;
import org.jboss.as.test.integration.web.security.WebSecurityCommon;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.security.common.elytron.PropertyFileBasedDomain;
import org.wildfly.test.security.common.elytron.UndertowDomainMapper;
import org.wildfly.test.security.common.elytron.UserWithRoles;

/**
 * Simple test case for web DIGEST authentication.
 *
 * @author olukas
 */
@RunWith(Arquillian.class)
@ServerSetup({WebSecurityDIGESTTestCase.WebSecurityDigestSecurityDomainSetup.class})
@RunAsClient
public class WebSecurityDIGESTTestCase extends WebSecurityPasswordBasedBase {

    private static final String SECURITY_DOMAIN_NAME = "digestSecurityDomain";
    private static final String DEPLOYMENT = "digestApp";

    private static final String GOOD_USER_NAME = "anil";
    private static final String GOOD_USER_PASSWORD = "anil";
    private static final String GOOD_USER_ROLE = SimpleSecuredServlet.ALLOWED_ROLE;

    private static final String SUPER_USER_NAME = "marcus";
    private static final String SUPER_USER_PASSWORD = "marcus";
    private static final String SUPER_USER_ROLE = "superuser";

    private static final String WEB_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "\n" +
            "<web-app xmlns=\"http://java.sun.com/xml/ns/javaee\"\n" +
            "      xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            "      xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun" +
            ".com/xml/ns/javaee/web-app_3_0.xsd\"\n" +
            "      version=\"3.0\">\n" +
            "\n" +
            "\t<login-config>\n" +
            "\t\t<auth-method>DIGEST</auth-method>\n" +
            "\t\t<realm-name>" + SECURITY_DOMAIN_NAME + "</realm-name>\n" +
            "\t</login-config>\n" +
            "</web-app>";

    @Deployment(name = DEPLOYMENT)
    public static WebArchive deployment() throws Exception {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, DEPLOYMENT + ".war");
        war.addClasses(SimpleServlet.class, SimpleSecuredServlet.class);
        war.addAsWebInfResource(Utils.getJBossWebXmlAsset(SECURITY_DOMAIN_NAME), "jboss-web.xml");
        war.addAsWebInfResource(new StringAsset(WEB_XML), "web.xml");
        return war;
    }

    @ArquillianResource
    private URL url;

    /**
     * Check whether user with incorrect credentials has not access to secured page.
     *
     * @param url
     * @throws Exception
     */
    @OperateOnDeployment(DEPLOYMENT)
    @Test
    public void testWrongUser(@ArquillianResource URL url) throws Exception {
        makeCall(GOOD_USER_NAME, GOOD_USER_PASSWORD + "makeThisPasswordWrong", HTTP_UNAUTHORIZED);
    }

    @Override
    protected void makeCall(String user, String pass, int expectedCode) throws Exception {
        final URL servletUrl = new URL(url.toExternalForm() + SimpleSecuredServlet.SERVLET_PATH.substring(1));
        Utils.makeCallWithBasicAuthn(servletUrl, user, pass, expectedCode);
    }

    static class WebSecurityDigestSecurityDomainSetup implements ServerSetupTask {

        private LegacySecurityDomainsSetup secDomainSetup;

        private CLIWrapper cli;
        private PropertyFileBasedDomain ps;
        private UndertowDomainMapper domainMapper;

        @Override
        public void setup(ManagementClient managementClient, String s) throws Exception {
            if (WebSecurityCommon.isElytron()) {
                cli = new CLIWrapper(true);
                setupElytronBasedSecurityDomain();
            } else {
                secDomainSetup = new LegacySecurityDomainsSetup();
                secDomainSetup.setup(managementClient, s);
            }
        }

        @Override
        public void tearDown(ManagementClient managementClient, String s) throws Exception {
            if (WebSecurityCommon.isElytron()) {
                domainMapper.remove(cli);
                ps.remove(cli);
                cli.close();
            } else {
                secDomainSetup.tearDown(managementClient, s);
            }
        }

        private void setupElytronBasedSecurityDomain() throws Exception {
            ps = PropertyFileBasedDomain.builder()
                    .withUser(GOOD_USER_NAME, GOOD_USER_PASSWORD, GOOD_USER_ROLE)
                    .withUser(SUPER_USER_NAME, SUPER_USER_PASSWORD, SUPER_USER_ROLE)
                    .withName(SECURITY_DOMAIN_NAME).build();
            ps.create(cli);
            domainMapper = UndertowDomainMapper.builder().withName(SECURITY_DOMAIN_NAME).build();
            domainMapper.create(cli);
        }
    }

    static class LegacySecurityDomainsSetup extends AbstractSecurityDomainsServerSetupTask {

        @Override
        protected SecurityDomain[] getSecurityDomains() throws Exception {
            List<UserWithRoles> userWithRoles = new ArrayList<UserWithRoles>();
            userWithRoles.add(UserWithRoles.builder().withName(GOOD_USER_NAME).withPassword(GOOD_USER_PASSWORD)
                    .withRoles(GOOD_USER_ROLE).build());
            userWithRoles.add(UserWithRoles.builder().withName(SUPER_USER_NAME).withPassword(SUPER_USER_PASSWORD)
                    .withRoles(SUPER_USER_ROLE).build());
            WebSecurityCommon.PropertyFiles propFiles = WebSecurityCommon.createPropertiesFiles(userWithRoles, SECURITY_DOMAIN_NAME);

            final Map<String, String> lmOptions = new HashMap<>();
            lmOptions.put("hashAlgorithm", "MD5");
            lmOptions.put("hashEncoding", "RFC2617");
            lmOptions.put("hashUserPassword", "false");
            lmOptions.put("hashStorePassword", "true");
            lmOptions.put("passwordIsA1Hash", "false");
            lmOptions.put("storeDigestCallback", "org.jboss.security.auth.callback.RFC2617Digest");
            lmOptions.put("usersProperties", propFiles.getUsers().getAbsolutePath());
            lmOptions.put("rolesProperties", propFiles.getRoles().getAbsolutePath());

            final SecurityDomain sd1 = new SecurityDomain.Builder()
                    .name(SECURITY_DOMAIN_NAME)
                    .loginModules(new SecurityModule.Builder()
                            .name("UsersRoles")
                            .options(lmOptions)
                            .build())
                    .build();

            return new SecurityDomain[]{sd1};
        }

    }
}
