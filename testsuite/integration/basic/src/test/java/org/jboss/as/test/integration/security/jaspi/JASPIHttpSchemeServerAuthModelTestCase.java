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
import org.jboss.as.test.integration.security.common.Utils;
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
@ServerSetup({ JaspiSecurityDomainsSetup.class })
@RunAsClient
public class JASPIHttpSchemeServerAuthModelTestCase {

    public static final String DEPLOYMENT_REALM_NAME = "JASPI";

    @Deployment(name = "war")
    public static WebArchive warDeployment() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, JASPIHttpSchemeServerAuthModelTestCase.class.getSimpleName() + ".war");

        final StringAsset usersRolesAsset = new StringAsset(Utils.createUsersFromRoles(Manage.ROLES_ALL));
        war.addAsResource(usersRolesAsset, "users.properties");
        war.addAsResource(usersRolesAsset, "roles.properties");

        war.addAsWebInfResource(JASPIHttpSchemeServerAuthModelTestCase.class.getPackage(), "web.xml", "/web.xml");
        war.addAsWebInfResource(Utils.getJBossWebXmlAsset(JaspiSecurityDomainsSetup.SECURITY_DOMAIN_NAME), "jboss-web.xml");

        // temporary. remove once the security subsystem is updated to proper consider the module option
        war.addAsManifestResource(Utils.getJBossDeploymentStructure("org.wildfly.extension.undertow"), "jboss-deployment-structure.xml");

        war.add(new StringAsset("Welcome"), "index.jsp");

        war.add(new StringAsset("Unsecured"), "unsecured/index.jsp");
        return war;

    }

    @Test
    public void testRequiresAuthentication(@ArquillianResource URL webAppURL) throws Exception {
        DefaultHttpClient httpClient = new DefaultHttpClient();

        HttpResponse httpResponse = httpClient.execute(new HttpGet(webAppURL.toURI()));

        assertEquals(401, httpResponse.getStatusLine().getStatusCode());
    }

    @Test
    public void testAuthNotRequired(@ArquillianResource URL webAppURL) throws Exception {
        DefaultHttpClient httpClient = new DefaultHttpClient();

        HttpResponse httpResponse = httpClient.execute(new HttpGet(webAppURL.toURI() + "unsecured/index.jsp"));

        assertEquals(200, httpResponse.getStatusLine().getStatusCode());

        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        httpResponse.getEntity().writeTo(bos);

        assertTrue(new String(bos.toByteArray()).contains("Unsecured"));
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

}
