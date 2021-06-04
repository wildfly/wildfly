/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat Middleware LLC, and individual contributors
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

package org.wildfly.test.integration.elytron.realm;

import static javax.servlet.http.HttpServletResponse.SC_OK;

import java.net.MalformedURLException;
import java.net.URL;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.integration.security.common.servlets.SimpleSecuredServlet;
import org.jboss.as.test.integration.security.common.servlets.SimpleServlet;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Full authentication tests for Elytron Property realm using hash encoding and
 * hash charsets.
 *
 * @author Sonia Zaldana Calles <szaldana@redhat.com>
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({PropertiesRealmTestCase.SetUpTask.class})
public class PropertiesRealmTestCase {

    private static final String DEPLOYMENT_ENCODED = "propRealmEncoded";
    private static final String DEPLOYMENT_WITH_CHARSET_ENCODED = "propRealmEncodedCharset";
    private static final String USER_ENCODED = "elytron";
    private static final String USER_WITH_CHARSET_ENCODED = "elytron4";
    private static final String CHARSET_PASSWORD = "password密码";
    private static final String ENCODED_PASSWORD = "passwd12#$";

    @Deployment(name = DEPLOYMENT_ENCODED)
    public static WebArchive deploymentWithCharset() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, DEPLOYMENT_ENCODED + ".war");
        war.addClasses(SimpleServlet.class);
        war.addClasses(SimpleSecuredServlet.class);
        war.addAsWebInfResource(PropertiesRealmTestCase.class.getPackage(), "property-realm-web.xml", "web.xml");
        war.addAsWebInfResource(Utils.getJBossWebXmlAsset(DEPLOYMENT_ENCODED), "jboss-web.xml");
        return war;
    }

    @Deployment(name = DEPLOYMENT_WITH_CHARSET_ENCODED)
    public static WebArchive deploymentWithCharsetAndEncoded() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, DEPLOYMENT_WITH_CHARSET_ENCODED + ".war");
        war.addClasses(SimpleServlet.class);
        war.addClasses(SimpleSecuredServlet.class);
        war.addAsWebInfResource(PropertiesRealmTestCase.class.getPackage(), "property-realm-web.xml", "web.xml");
        war.addAsWebInfResource(Utils.getJBossWebXmlAsset(DEPLOYMENT_WITH_CHARSET_ENCODED), "jboss-web.xml");
        return war;
    }

    /**
     *
     * Test Properties realm correctly handles a password using a different character set than
     * UTF-8 when converting the password string to a byte array.
     */
    @Test
    @OperateOnDeployment(DEPLOYMENT_ENCODED)
    public void testCorrectUserCorrectPasswordWithAlternativeCharset(@ArquillianResource URL webAppURL) throws Exception {
        URL url = prepareURL(webAppURL);
        Utils.makeCallWithBasicAuthn(url, USER_ENCODED, ENCODED_PASSWORD, SC_OK);
    }

    /**
     *
     * Test Properties realm correctly handles a password using a different character set to
     * and base64 encoding as the string format for the password if they are not stored in plain text.
     */
    @Test
    @OperateOnDeployment(DEPLOYMENT_WITH_CHARSET_ENCODED)
    public void testCorrectUserCorrectPasswordBase64Encoded(@ArquillianResource URL webAppURL) throws Exception {
        URL url = prepareURL(webAppURL);
        Utils.makeCallWithBasicAuthn(url, USER_WITH_CHARSET_ENCODED, CHARSET_PASSWORD, SC_OK);
    }

    private URL prepareURL(URL url) throws MalformedURLException {
        return new URL(url.toExternalForm() + SimpleSecuredServlet.SERVLET_PATH.substring(1));
    }

    static class SetUpTask implements ServerSetupTask {

        private static final String REALM_NAME_ENCODED = "propRealmEncoded";
        private static final String REALM_NAME_ENCODING_CHARSET = "propRealmEncodingCharset";
        private static final String DOMAIN_NAME_ENCODED = "propDomainEncoded";
        private static final String DOMAIN_NAME_ENCODING_CHARSET = "propDomainEncodingCharset";
        private static final String PREDEFINED_HTTP_SERVER_MECHANISM_FACTORY = "global";


        @Override
        public void setup(ManagementClient managementClient, java.lang.String s) throws Exception {
            setUpTestDomain(DOMAIN_NAME_ENCODED, REALM_NAME_ENCODED, DEPLOYMENT_ENCODED, "users-hashedbase64.properties", "base64", "UTF-8");
            setUpTestDomain(DOMAIN_NAME_ENCODING_CHARSET, REALM_NAME_ENCODING_CHARSET, DEPLOYMENT_WITH_CHARSET_ENCODED, "users-hashedbase64charset", "base64", "GB2312");
            ServerReload.reloadIfRequired(managementClient);

        }

        private void setUpTestDomain(String domainName, String realmName, String deployment, String fileName, String hashEncoding, String hashCharset) throws Exception {
            try (CLIWrapper cli =  new CLIWrapper(true)) {
                cli.sendLine(String.format("/subsystem=elytron/properties-realm=%s:add(groups-attribute=groups, user-properties={path=%s, relative-to=jboss.server.config.dir}, " +
                                "hash-encoding=%s, hash-charset=%s)",
                        realmName, fileName, hashEncoding, hashCharset));

                cli.sendLine(String.format("/subsystem=elytron/security-domain=%1$s:add(realms=[{realm=%2$s,role-decoder=groups-to-roles}],default-realm=%2$s,permission-mapper=default-permission-mapper)",
                        domainName, realmName));
                cli.sendLine(String.format(
                        "/subsystem=elytron/http-authentication-factory=%1$s:add(http-server-mechanism-factory=%2$s,security-domain=%1$s,"
                                + "mechanism-configurations=[{mechanism-name=BASIC,mechanism-realm-configurations=[{realm-name=\"%1$s\"}]}])",
                        domainName, PREDEFINED_HTTP_SERVER_MECHANISM_FACTORY));
                cli.sendLine(String.format(
                        "/subsystem=undertow/application-security-domain=%s:add(http-authentication-factory=%s)",
                        deployment, domainName));
            }
        }

        @Override
        public void tearDown(ManagementClient managementClient, java.lang.String s) throws Exception {
            tearDownDomain(DEPLOYMENT_ENCODED, DOMAIN_NAME_ENCODED, REALM_NAME_ENCODED);
            tearDownDomain(DEPLOYMENT_WITH_CHARSET_ENCODED, DOMAIN_NAME_ENCODING_CHARSET, REALM_NAME_ENCODING_CHARSET);
            ServerReload.reloadIfRequired(managementClient);

        }

        private void tearDownDomain(String deployment, String domainName, String realmName) throws Exception {
            try (CLIWrapper cli = new CLIWrapper(true)) {
                cli.sendLine(String.format("/subsystem=undertow/application-security-domain=%s:remove()", deployment));
                cli.sendLine(String.format("/subsystem=elytron/http-authentication-factory=%s:remove()",
                        domainName));
                cli.sendLine(String.format("/subsystem=elytron/security-domain=%s:remove()",
                        domainName));
                cli.sendLine(String.format("/subsystem=elytron/properties-realm=%s:remove()", realmName));
            }
        }
    }
}
