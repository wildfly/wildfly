package org.jboss.as.test.integration.security.jaspi;

import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.security.Constants;
import org.jboss.as.test.integration.security.common.AbstractSecurityDomainsServerSetupTask;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.integration.security.common.config.AuthnModule;
import org.jboss.as.test.integration.security.common.config.JaspiAuthn;
import org.jboss.as.test.integration.security.common.config.LoginModuleStack;
import org.jboss.as.test.integration.security.common.config.SecurityDomain;
import org.jboss.as.test.integration.security.common.config.SecurityModule;
import org.jboss.as.test.integration.security.jacc.propagation.Manage;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extension.undertow.security.jaspi.modules.HTTPSchemeServerAuthModule;

import java.io.ByteArrayOutputStream;
import java.net.URL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * <p>Tests the JASPI support by deploying a web application that uses a security domain configured with JASPI authentication.</p>
 *
 * <p>The security domain is configured with the {@link HTTPSchemeServerAuthModule} to provide HTTP BASIC authentication.</p>
 *
 * @author Pedro Igor
 */
@RunWith(Arquillian.class)
@ServerSetup({ JASPIHttpSchemeServerAuthModelTestCase.SecurityDomainsSetup.class })
@RunAsClient
public class JASPIHttpSchemeServerAuthModelTestCase {

    private static final String TEST_NAME = "jaspi-http-scheme-server-auth-module";
    public static final String DEPLOYMENT_REALM_NAME = "JASPI";

    @Deployment(name = "war")
    public static WebArchive warDeployment() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, TEST_NAME + ".war");

        final StringAsset usersRolesAsset = new StringAsset(Utils.createUsersFromRoles(Manage.ROLES_ALL));
        war.addAsResource(usersRolesAsset, "users.properties");
        war.addAsResource(usersRolesAsset, "roles.properties");

        war.addAsWebInfResource(JASPIHttpSchemeServerAuthModelTestCase.class.getPackage(), "web.xml", "/web.xml");
        war.addAsWebInfResource(Utils.getJBossWebXmlAsset(TEST_NAME), "jboss-web.xml");

        // temporary. remove once the security subsystem is updated to proper consider the module option
        war.addAsManifestResource(Utils.getJBossDeploymentStructure("org.wildfly.extension.undertow"), "jboss-deployment-structure.xml");

        war.add(new StringAsset("Welcome"), "index.jsp");

        return war;

    }

    @Test
    public void testRequiresAuthentication(@ArquillianResource URL webAppURL) throws Exception {
        DefaultHttpClient httpClient = new DefaultHttpClient();

        HttpResponse httpResponse = httpClient.execute(new HttpGet(webAppURL.toURI()));

        assertEquals(401, httpResponse.getStatusLine().getStatusCode());
    }

    @Test
    public void testSuccessfulAuthentication(@ArquillianResource URL webAppURL) throws Exception {
        DefaultHttpClient httpClient = createHttpClient(webAppURL, "User", "User");

        HttpResponse httpResponse = httpClient.execute(new HttpGet(webAppURL.toURI()));

        assertEquals(200, httpResponse.getStatusLine().getStatusCode());

        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        httpResponse.getEntity().writeTo(bos);

        assertTrue(new String(bos.toByteArray()).contains("Welcome"));
    }

    @Test
    public void testUnsuccessfulAuthentication(@ArquillianResource URL webAppURL) throws Exception {
        DefaultHttpClient httpClient = createHttpClient(webAppURL, "Invalid User", "User");

        HttpResponse httpResponse = httpClient.execute(new HttpGet(webAppURL.toURI()));

        assertEquals(401, httpResponse.getStatusLine().getStatusCode());
    }

    private DefaultHttpClient createHttpClient(final URL webAppURL, final String userName, final String userPassword) {
        DefaultHttpClient httpClient = new DefaultHttpClient();

        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(userName, userPassword);

        httpClient.getCredentialsProvider().setCredentials(new AuthScope(webAppURL.getHost(), webAppURL.getPort(), DEPLOYMENT_REALM_NAME), credentials);

        return httpClient;
    }

    /**
     * A {@link org.jboss.as.arquillian.api.ServerSetupTask} instance which creates security domains for this test case.
     *
     * @author Pedro Igor
     */
    static class SecurityDomainsSetup extends AbstractSecurityDomainsServerSetupTask {

        public static final String UNDERTOW_MODULE_NAME = "org.wildfly.extension.undertow";

        @Override
        protected SecurityDomain[] getSecurityDomains() {
            String loginModuleStacksName = "lm-stack";

            return new SecurityDomain[] { new SecurityDomain.Builder().name(TEST_NAME)
                    .jaspiAuthn(new JaspiAuthn.Builder()
                            .loginModuleStacks(new LoginModuleStack.Builder()
                                    .name(loginModuleStacksName)
                                    .loginModules(new SecurityModule.Builder().name("UsersRoles").flag(Constants.REQUIRED).build())
                                    .build())
                            .authnModules(new AuthnModule.Builder()
                                    .name(HTTPSchemeServerAuthModule.class.getName())
                                    .loginModuleStackRef(loginModuleStacksName)
                                    .module(UNDERTOW_MODULE_NAME)
                                    .build())
                            .build())
                    .cacheType("default")
                    .build() };
        }
    }

}
