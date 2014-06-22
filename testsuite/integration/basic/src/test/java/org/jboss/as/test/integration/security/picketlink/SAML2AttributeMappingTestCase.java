/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.security.picketlink;

import static org.junit.Assert.assertEquals;

import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import javax.naming.Context;

import org.apache.http.impl.client.DefaultHttpClient;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.security.Constants;
import org.jboss.as.test.integration.security.common.AbstractSecurityDomainsServerSetupTask;
import org.jboss.as.test.integration.security.common.Krb5LoginConfiguration;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.integration.security.common.config.SecurityDomain;
import org.jboss.as.test.integration.security.common.config.SecurityModule;
import org.jboss.as.test.integration.security.common.servlets.PrintAttributeServlet;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * This class tests passUserPrincipalToAttributeManager attribute of Picketlink's IDPWebBrowserSSOValve. It's a regression test
 * for attribute mapping in Identity Provider (IdP) when SPNEGO (Kerberos) authentication is used.
 * <p>
 * PLINK-405
 * 
 * @author Josef Cacek
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({ KerberosServerSetupTask.SystemPropertiesSetup.class, KerberosServerSetupTask.class, KerberosKeyTabSetup.class,
        SAML2AttributeMappingTestCase.SecurityDomainsSetup.class })
@Ignore("AS7-6796 - Undertow SPNEGO")
public class SAML2AttributeMappingTestCase {

    private static final String SP_RESPONSE_BODY = "Welcome to SP";

    private static Logger LOGGER = Logger.getLogger(SAML2AttributeMappingTestCase.class);

    protected static final String IDP = "idp";
    protected static final String SP = "sp";

    private static final String IDP_CONTEXT_PATH = "idp";

    @ArquillianResource
    @OperateOnDeployment(IDP)
    private URL idpUrl;

    @ArquillianResource
    @OperateOnDeployment(SP)
    private URL spUrl;

    @Deployment(name = IDP)
    public static WebArchive deploymentIdP() {
        LOGGER.info("Start deployment " + IDP);
        final WebArchive war = ShrinkWrap.create(WebArchive.class, IDP + ".war");
        war.addAsWebInfResource(SAML2AttributeMappingTestCase.class.getPackage(), "attrib-web.xml", "web.xml");
        war.addAsManifestResource(Utils.getJBossDeploymentStructure("org.jboss.security.negotiation", "org.picketlink"),
                "jboss-deployment-structure.xml");
        war.addAsWebInfResource(new StringAsset(PicketLinkTestBase.propertiesReplacer("picketlink-idp.xml", IDP, "", IDP)),
                "picketlink.xml");
        war.add(new StringAsset("Welcome to IdP"), "index.jsp");
        war.add(new StringAsset("Welcome to IdP hosted"), "hosted/index.jsp");
        war.addAsWebInfResource(SAML2AttributeMappingTestCase.class.getPackage(), "attrib-jboss-web.xml", "jboss-web.xml");
        return war;
    }

    @Deployment(name = SP)
    public static WebArchive deploymentSP() {
        LOGGER.info("Start deployment " + SP);
        final WebArchive war = ShrinkWrap.create(WebArchive.class, SP + ".war");
        war.addAsWebInfResource(SAML2AttributeMappingTestCase.class.getPackage(), "attrib-web.xml", "web.xml");
        war.addAsWebInfResource(Utils.getJBossWebXmlAsset("sp",
                "org.picketlink.identity.federation.bindings.tomcat.sp.ServiceProviderAuthenticator"), "jboss-web.xml");
        war.addAsManifestResource(Utils.getJBossDeploymentStructure("org.picketlink"), "jboss-deployment-structure.xml");
        war.addAsWebInfResource(
                new StringAsset(PicketLinkTestBase.propertiesReplacer("picketlink-sp.xml", SP, "REDIRECT", IDP_CONTEXT_PATH)),
                "picketlink.xml");
        war.add(new StringAsset(SP_RESPONSE_BODY), "index.jsp");
        war.addClass(PrintAttributeServlet.class);
        return war;
    }

    /**
     * Tests IDP attribute mapping when passUserPrincipalToAttributeManager is set to "true". Automatic handling of redirections
     * is enabled for HTTP client used.
     * 
     * @throws Exception
     */
    @Test
    public void testPassUserPrincipalToAttributeManager() throws Exception {

        final DefaultHttpClient httpClient = new DefaultHttpClient();
        httpClient.setRedirectStrategy(Utils.REDIRECT_STRATEGY);

        try {
            String response = PicketLinkTestBase.makeCallWithKerberosAuthn(spUrl.toURI(), httpClient, "jduke", "theduke", 200);
            assertEquals("SP index page was not reached", SP_RESPONSE_BODY, response);
            response = PicketLinkTestBase.makeCall(new URL(spUrl.toString() + PrintAttributeServlet.SERVLET_PATH.substring(1)),
                    httpClient, 200);
            assertEquals("cn attribute not stored", "Java Duke", response);
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    /**
     * A {@link ServerSetupTask} instance which creates security domains for this test case.
     */
    static class SecurityDomainsSetup extends AbstractSecurityDomainsServerSetupTask {

        private static final String SERVER_SECURITY_DOMAIN = "host";

        /**
         * Returns SecurityDomains configuration for this testcase.
         * 
         * @see org.jboss.as.test.integration.security.common.AbstractSecurityDomainsServerSetupTask#getSecurityDomains()
         */
        @Override
        protected SecurityDomain[] getSecurityDomains() {
            List<SecurityDomain> res = new LinkedList<SecurityDomain>();

            // Add host security domain
            res.add(new SecurityDomain.Builder()
                    .name(SERVER_SECURITY_DOMAIN)
                    .cacheType("default")
                    .loginModules(
                            new SecurityModule.Builder()
                                    .name(Krb5LoginConfiguration.getLoginModule())
                                    .flag(Constants.REQUIRED)
                                    .options(
                                            Krb5LoginConfiguration.getOptions(
                                                    KerberosServerSetupTask.getHttpServicePrincipal(managementClient),
                                                    KerberosKeyTabSetup.getKeyTab(), true)).build()).build());

            // Add IdP security domain
            res.add(new SecurityDomain.Builder()
                    .name(IDP)
                    .loginModules(
                            new SecurityModule.Builder()
                                    // Login module used for password negotiation
                                    .name("SPNEGO").flag(Constants.REQUISITE).putOption("password-stacking", "useFirstPass")
                                    .putOption("serverSecurityDomain", SERVER_SECURITY_DOMAIN)
                                    .putOption("removeRealmFromPrincipal", "true").build(),

                            new SecurityModule.Builder()
                                    // Login module used for role retrieval
                                    .name("org.jboss.security.auth.spi.LdapExtLoginModule")
                                    .flag(Constants.REQUIRED)
                                    .putOption("password-stacking", "useFirstPass")
                                    .putOption(
                                            Context.PROVIDER_URL,
                                            "ldap://" + KerberosServerSetupTask.getCannonicalHost(managementClient) + ":"
                                                    + KerberosServerSetupTask.LDAP_PORT)
                                    .putOption("baseCtxDN", "ou=People,dc=jboss,dc=org").putOption("baseFilter", "(uid={0})")
                                    .putOption("rolesCtxDN", "ou=Roles,dc=jboss,dc=org")
                                    .putOption("roleFilter", "(|(objectClass=referral)(member={1}))")
                                    .putOption("roleAttributeID", "cn").putOption("referralUserAttributeIDToCheck", "member")
                                    .putOption("bindDN", KerberosServerSetupTask.SECURITY_PRINCIPAL)
                                    .putOption("bindCredential", KerberosServerSetupTask.SECURITY_CREDENTIALS)
                                    .putOption(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory")
                                    .putOption(Context.SECURITY_AUTHENTICATION, "simple").putOption(Context.REFERRAL, "follow")
                                    .putOption("throwValidateError", "true").putOption("roleRecursion", "5").build())
                    .mappingModules(
                            new SecurityModule.Builder()
                                    .name("org.jboss.security.mapping.providers.attribute.LdapAttributeMappingProvider")
                                    .flag("attribute")
                                    .putOption(
                                            Context.PROVIDER_URL,
                                            "ldap://" + KerberosServerSetupTask.getCannonicalHost(managementClient) + ":"
                                                    + KerberosServerSetupTask.LDAP_PORT)
                                    .putOption("bindDN", KerberosServerSetupTask.SECURITY_PRINCIPAL)
                                    .putOption("bindCredential", KerberosServerSetupTask.SECURITY_CREDENTIALS)
                                    .putOption("baseCtxDN", "ou=People,dc=jboss,dc=org").putOption("baseFilter", "(uid={0})")
                                    .putOption("attributeList", "cn,sn").putOption("searchTimeLimit", "10000")//
                                    .build()) //
                    .build());

            // Add SP security domain
            res.add(new SecurityDomain.Builder()
                    .name(SP)
                    .loginModules(
                            new SecurityModule.Builder()
                                    .name("org.picketlink.identity.federation.bindings.jboss.auth.SAML2LoginModule")
                                    .flag(Constants.REQUIRED).build()).build());

            return res.toArray(new SecurityDomain[0]);
        }
    }
}