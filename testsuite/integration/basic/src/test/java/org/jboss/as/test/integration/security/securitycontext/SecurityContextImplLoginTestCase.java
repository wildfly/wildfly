/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.security.securitycontext;


import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.controller.client.helpers.ClientConstants;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.HttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandContextFactory;
import org.jboss.as.cli.impl.CommandContextConfiguration;
import org.jboss.as.test.shared.SnapshotRestoreSetupTask;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.jboss.as.test.integration.web.security.SecuredServlet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import static org.jboss.as.controller.client.helpers.ClientConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.client.helpers.ClientConstants.RESULT;

/**
 * SecurityContextImplLoginTestCase
 * For more information visit <a href="https://issues.redhat.com/browse/ELYWEB-133">https://issues.redhat.com/browse/ELYWEB-133</a>
 * @author Petr Adamec
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({SnapshotRestoreSetupTask.class, SecurityContextImplLoginTestCase.SecurityContextImplLoginTestCaseServerSetup.class})
public class SecurityContextImplLoginTestCase {
    @ArquillianResource(SecuredServlet.class)
    URL deploymentURL;
    private static CommandContext ctx;
    @ClassRule
    public static final TemporaryFolder temporaryUserHome = new TemporaryFolder();

    private static final ByteArrayOutputStream consoleOutput = new ByteArrayOutputStream();

    private static final String DEPLOYMENT = "simple-webapp";
    private static final String MY_FS_REALM = "MyFsRealm";
    private static final String MY_SECURITY_DOMAIN = "MySecurityDomain";
    private static final String MY_HTTP_AUTH_FACTORY = "MyHttpAuthFactory";
    private static final String MY_APP_SECURITY_DOMAIN = "my_app_security_domain";
    private static final String MY_CACHING_REALM = "MyCachingRealm";
    private static final String MY_REALM_MAPPER = "MyRealmMapper";
    private static final String FROM_ROLES_ATTRIBUTE_DECODER = "FromRolesAttributeDecoder";
    private static final String TEST_USER = "testuser";

    @ContainerResource
    private ManagementClient managementClient;

    @Deployment(name = DEPLOYMENT)
    public static WebArchive createDeployment() throws Exception{
        WebArchive war = ShrinkWrap.create(WebArchive.class, DEPLOYMENT + ".war");
        war.addClass(MyDummyTokenHandler.class);
        war.addClass(MyServletExtension.class);
        war.addClass(SecuredServlet.class);
        war.addAsResource(new StringAsset("org.jboss.as.test.integration.security.securitycontext.MyServletExtension"), "META-INF/services/io.undertow.servlet.ServletExtension");
        war.addAsWebResource(SecurityContextImplLoginTestCase.class.getPackage(), "pub/login.html", "pub/login.html");
        war.addAsWebResource(SecurityContextImplLoginTestCase.class.getPackage(), "pub/login-error.html", "pub/login-error.html");
        war.addAsWebResource(SecurityContextImplLoginTestCase.class.getPackage(), "index.html", "index.html");
        war.addAsWebResource(SecurityContextImplLoginTestCase.class.getPackage(), "main/index.html", "main/index.html");
        war.addAsWebInfResource(SecurityContextImplLoginTestCase.class.getPackage(), "web.xml", "web.xml");
        war.addAsWebInfResource(SecurityContextImplLoginTestCase.class.getPackage(), "jboss-web.xml", "jboss-web.xml");
        return war;
    }


    /**
     * Access to protected page directly.
     * The login page should be back so expected status code is 200
     * @throws Exception
     */
    @Test
    public void accessProtectedPageDirectlyTest() throws Exception{
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpResponse response = client.execute(new HttpGet(getURL()));
            Assert.assertEquals("For more info see ELYWEB-133. " ,200, response.getStatusLine().getStatusCode());
        }
    }

    /**
     * Access the protected page but the custom http handler will login.
     * Protected page content should be returned so expected status code is 200
     * @throws Exception
     */
    @Test
    public void accessProtectedPageWithCustomHttpHandlerLoginTest() throws Exception{
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpResponse response = client.execute(new HttpGet(getURL()+"?login=true"));
            Assert.assertEquals("For more info see ELYWEB-133. " ,200, response.getStatusLine().getStatusCode());
        }
    }

    private static ModelNode createOpNode(String address, String operation) {
        ModelNode op = new ModelNode();

        // set address
        ModelNode list = op.get("address").setEmptyList();
        if (address != null) {
            String[] pathSegments = address.split("/");
            for (String segment : pathSegments) {
                String[] elements = segment.split("=");
                list.add(elements[0], elements[1]);
            }
        }
        op.get("operation").set(operation);
        return op;
    }

    private static boolean domainExists(String domain) throws Exception {
        ModelNode realms = createOpNode("subsystem=undertow", "read-children-names");
        realms.get("child-type").set("application-security-domain");
        List<ModelNode> mn = executeForResult(realms).asList();
        for (ModelNode c : mn) {
            if (c.asString().equals(domain)) {
                return true;
            }
        }
        return false;
    }

    private static ModelNode executeForResult(final ModelNode operation) throws Exception {
        try {
            final ModelNode result = ctx.getModelControllerClient().execute(operation);
            checkSuccessful(result, operation);
            return result.get(RESULT);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void checkSuccessful(final ModelNode result,
                                        final ModelNode operation) throws Exception {
        if (!ClientConstants.SUCCESS.equals(result.get(ClientConstants.OUTCOME).asString())) {
            throw new Exception(result.get(
                    FAILURE_DESCRIPTION).toString());
        }
    }

    private String getURL() {
        return deploymentURL.toString() + "main/";
    }

    static class SecurityContextImplLoginTestCaseServerSetup implements ServerSetupTask {

        @Override
        public void setup(ManagementClient managementClient, String s) throws Exception {
            CommandContextConfiguration.Builder configBuilder = new CommandContextConfiguration.Builder();
            ctx = CommandContextFactory.getInstance().newCommandContext(configBuilder.build());
            ctx.connectController();
            ctx.handle("/subsystem=elytron/filesystem-realm=" + MY_FS_REALM + ":add(path=my-realm-users, relative-to=jboss.server.config.dir)");
            ctx.handle("/subsystem=elytron/filesystem-realm=" + MY_FS_REALM + ":add-identity(identity=" + TEST_USER );
            ctx.handle("/subsystem=elytron/filesystem-realm=" + MY_FS_REALM + ":set-password(identity=" + TEST_USER + ",clear={password=testpassword})");
            ctx.handle("/subsystem=elytron/filesystem-realm="+ MY_FS_REALM + ":add-identity-attribute(identity=" + TEST_USER + ", name=Roles, value=[regular_user])");

            ctx.handle("/subsystem=elytron/caching-realm=" + MY_CACHING_REALM + ":add(realm=" + MY_FS_REALM +", maximum-age=300000");
            ctx.handle("/subsystem=elytron/simple-role-decoder=" + FROM_ROLES_ATTRIBUTE_DECODER + ":add(attribute=Roles)");

            ctx.handle("/subsystem=elytron/security-domain="+ MY_SECURITY_DOMAIN +":add(realms=[{realm=" + MY_CACHING_REALM + ", role-decoder=" + FROM_ROLES_ATTRIBUTE_DECODER + "}], default-realm=" + MY_CACHING_REALM + ", permission-mapper=default-permission-mapper)");
            ctx.handle("/subsystem=elytron/constant-realm-mapper=" + MY_REALM_MAPPER + ":add(realm-name=" + MY_CACHING_REALM + ")");
            ctx.handle("/subsystem=elytron/http-authentication-factory=" + MY_HTTP_AUTH_FACTORY +":add(http-server-mechanism-factory=global, security-domain=" + MY_SECURITY_DOMAIN + ", mechanism-configurations=[{mechanism-name=FORM, realm-mapper=MyRealmMapper, mechanism-realm-configurations=[{realm-name=MyRealm}]}]");
            ctx.handle("/subsystem=undertow/application-security-domain=" + MY_APP_SECURITY_DOMAIN + " :add(http-authentication-factory=" + MY_HTTP_AUTH_FACTORY + ")");
            ctx.handle("reload");
        }

        @Override
        public void tearDown(ManagementClient managementClient, String s) throws Exception {
            Exception e = null;
            ModelNode op = createOpNode("subsystem=undertow/application-security-domain=" + MY_APP_SECURITY_DOMAIN,
                    "remove");
            executeForResult(op);

            op = createOpNode("subsystem=elytron/http-authentication-factory=" + MY_HTTP_AUTH_FACTORY,
                    "remove");
            executeForResult(op);

            ModelNode removeMapper = createOpNode("subsystem=elytron/constant-realm-mapper=" + MY_REALM_MAPPER,
                    "remove");
            executeForResult(removeMapper);

            op = createOpNode("subsystem=elytron/security-domain=" + MY_SECURITY_DOMAIN,
                    "remove");
            executeForResult(op);

            op = createOpNode("subsystem=elytron/caching-realm=" + MY_CACHING_REALM,
                    "remove");
            executeForResult(op);
            if (ctx != null) {
                try {
                    ctx.handle("/subsystem=elytron/simple-role-decoder=" + FROM_ROLES_ATTRIBUTE_DECODER + " :remove");
                    ctx.handle("/subsystem=elytron/filesystem-realm="+ MY_FS_REALM + ":remove-identity(identity=" + TEST_USER + ")");

                    ctx.handle("/subsystem=elytron/filesystem-realm=" + MY_FS_REALM + " :remove");
                } catch (Exception ex) {
                    if (e == null) {
                        e = ex;
                    }
                } finally {
                    try {
                        ctx.handle("reload");
                    } finally {
                        ctx.terminateSession();
                    }
                }
            }
            if (e != null) {
                throw e;
            }
        }
    }

}
