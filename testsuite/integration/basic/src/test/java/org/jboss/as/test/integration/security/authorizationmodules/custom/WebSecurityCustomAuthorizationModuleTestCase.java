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
package org.jboss.as.test.integration.security.authorizationmodules.custom;

import java.io.IOException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.as.test.integration.security.common.AbstractSecurityDomainsServerSetupTask;
import org.jboss.as.test.integration.security.common.config.SecurityDomain;
import org.jboss.as.test.integration.security.common.config.SecurityModule;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Unit Test the usage of a custom authorization module.
 *
 * @author <a href="mailto:ehugonne@redhat.com">Emmanuel Hugonnet</a>
 */
@RunWith(Arquillian.class)
@ServerSetup(WebSecurityCustomAuthorizationModuleTestCase.SecurityDomainSetup.class)
@RunAsClient
public class WebSecurityCustomAuthorizationModuleTestCase {

    @ArquillianResource
    private ManagementClient managementClient;

    @Deployment(testable = true)
    public static WebArchive deployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "web-secure-custom-authorization-module.war");
        war.addAsResource(WebSecurityCustomAuthorizationModuleTestCase.class.getPackage(), "users.properties", "users.properties");
        war.addAsResource(WebSecurityCustomAuthorizationModuleTestCase.class.getPackage(), "roles.properties", "roles.properties");
        war.addAsWebInfResource(WebSecurityCustomAuthorizationModuleTestCase.class.getPackage(), "jboss-web.xml", "jboss-web.xml");
        war.addAsWebInfResource(WebSecurityCustomAuthorizationModuleTestCase.class.getPackage(), "web.xml", "web.xml");
        war.addAsWebResource(WebSecurityCustomAuthorizationModuleTestCase.class.getPackage(), "index.html", "index.html");
        war.addAsWebResource(WebSecurityCustomAuthorizationModuleTestCase.class.getPackage(), "index.html", "secure.html");
        return war;
    }

    /**
     * Test with user "anil" who has the right password and the right role to access the servlet
     *
     * @throws Exception
     */
    @Test
    public void testPasswordBasedSuccessfulAuth() throws Exception {
        makeCall("admin", "admin", "index.html", 403);
    }

    @Test
    public void testSSLRedirectOnResource() throws Exception {
        makeCall("admin", "admin", "secure.html", 302);
    }

    /**
     * <p>
     * Test with user "marcus" who has the right password but does not have the right role
     * </p>
     * <p>
     * Should be a HTTP/403
     * </p>
     *
     * @throws Exception
     */
    @Test
    public void testPasswordBasedUnsuccessfulAuth() throws Exception {
        makeCall("marcus", "marcus", "index.html", 403);
    }

    protected void makeCall(String user, String pass, String page, int expectedStatusCode) throws Exception {
        HttpParams httpParams = new BasicHttpParams();
        HttpClientParams.setRedirecting(httpParams, false);
        DefaultHttpClient httpclient = new DefaultHttpClient(httpParams);
        try {
            // test hitting programmatic login servlet
            String uri = managementClient.getWebUri().getScheme() + "://" + user + ':' + pass
                    + '@' + managementClient.getWebUri().getHost() + ':'
                    + managementClient.getWebUri().getPort() + "/web-secure-custom-authorization-module/" + page;
            HttpGet httpget = new HttpGet(uri);

            System.out.println("executing request" + httpget.getRequestLine());
            HttpResponse response = httpclient.execute(httpget);
            HttpEntity entity = response.getEntity();

            System.out.println("----------------------------------------");
            StatusLine statusLine = response.getStatusLine();
            System.out.println(statusLine);
            if (entity != null) {
                System.out.println("Response content length: " + entity.getContentLength());
            }
            assertEquals(expectedStatusCode, statusLine.getStatusCode());
            EntityUtils.consume(entity);
        } finally {
            // When HttpClient instance is no longer needed,
            // shut down the connection manager to ensure
            // immediate deallocation of all system resources
            httpclient.getConnectionManager().shutdown();
        }
    }

    static class SecurityDomainSetup extends AbstractSecurityDomainsServerSetupTask {

        public void takeSnapshot(ManagementClient managementClient) throws IOException, MgmtOperationException {
            final ModelNode snapshotOperation = new ModelNode();
            snapshotOperation.get(OP).set("take-snapshot");
            ModelNode ret = managementClient.getControllerClient().execute(snapshotOperation);
            if (!SUCCESS.equals(ret.get(OUTCOME).asString())) {
                throw new MgmtOperationException("Management operation failed: " + ret.get(FAILURE_DESCRIPTION), snapshotOperation, ret);
            }
            ModelNode result = ret.get(RESULT);
            System.out.println(result.asString());
        }

        @Override
        protected SecurityDomain[] getSecurityDomains() throws Exception {
            SecurityDomain securityDomain = new SecurityDomain.Builder().name("deny-all")
                    .loginModules(new SecurityModule.Builder().flag("required").name("UsersRoles").build())
                    .authorizationModules(new SecurityModule.Builder().flag("required").name(
                                    "org.jboss.security.authorization.modules.AllDenyAuthorizationModule").build()).build();
            return new SecurityDomain[]{securityDomain};
        }
    }

}
