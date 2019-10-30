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
package org.wildfly.test.integration.security.picketlink.federation;

import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebForm;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;
import org.apache.commons.io.FileUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.security.common.AbstractSecurityDomainsServerSetupTask;
import org.jboss.as.test.integration.security.common.config.SecurityDomain;
import org.jboss.as.test.integration.security.common.config.SecurityModule;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertTrue;
import static org.wildfly.test.integration.security.picketlink.federation.util.FederationArchiveUtil.identityProvider;
import static org.wildfly.test.integration.security.picketlink.federation.util.FederationArchiveUtil.serviceProvider;

/**
 *
 * Test for issue: https://issues.jboss.org/browse/WFLY-10080
 * Exact steps are described in issue.
 *
 * @author Jiri Ondrusek (jondruse@redhat.com)
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({SAMLPostKeepFormDataTestCase.PropertyFilesSetup.class, SAMLPostKeepFormDataTestCase.SecurityDomainsSetup.class})
public class SAMLPostKeepFormDataTestCase {

    private static final String TOMCAT="tomcat";
    private static final String TOMCAT_PSWD="tomcat";
    private static final String USERS_EXT = TOMCAT + "=" + TOMCAT_PSWD;
    private static final String ROLES_EXT = TOMCAT + "=manager";

    private final String TEXT = "test12345";

    @Deployment(name = "identity-provider")
    public static WebArchive deploymentIdP() {
        WebArchive identityProvider = identityProvider("idp.war");

        identityProvider.addAsWebResource(SAMLPostKeepFormDataTestCase.class.getResource("SAMLPostKeepFormData/idp/idp-metadata.xml"), "WEB-INF/picketlink.xml");

        identityProvider.addAsWebInfResource(SAMLPostKeepFormDataTestCase.class.getResource("SAMLPostKeepFormData/idp/web.xml"), "web.xml");

        identityProvider.addAsWebResource(SAMLPostKeepFormDataTestCase.class.getResource("SAMLPostKeepFormData/idp/login.jsp"), "jsp/login.jsp");

        identityProvider.addAsWebInfResource(new StringAsset("<jboss-web>" + //
                "<security-domain>SAMLPostKeepFormData-idp</security-domain>" + //
                "</jboss-web>"), "jboss-web.xml");

        return identityProvider;
    }

    @Deployment(name = "service-provider")
    public static WebArchive deploymentSP() {
        WebArchive serviceProvider = serviceProvider("sp-post.war");

        serviceProvider.addAsWebResource(SAMLPostKeepFormDataTestCase.class.getResource("SAMLPostKeepFormData/sp/post-test-main.jsp"), "freezone/post-test-main.jsp");
        serviceProvider.addAsWebResource(SAMLPostKeepFormDataTestCase.class.getResource("SAMLPostKeepFormData/sp/index.jsp"), "index.jsp");
        serviceProvider.addAsWebInfResource(SAMLPostKeepFormDataTestCase.class.getResource("SAMLPostKeepFormData/sp/web.xml"), "web.xml");

        serviceProvider.addAsWebInfResource(new StringAsset("<jboss-web>" + //
                "<security-domain>SAMLPostKeepFormData-sp</security-domain>" +
                "<valve><class-name>org.picketlink.identity.federation.bindings.tomcat.sp.ServiceProviderAuthenticator</class-name></valve>" +
                "</jboss-web>"), "jboss-web.xml");
        serviceProvider.addAsWebResource(SAMLPostKeepFormDataTestCase.class.getResource("SAMLPostKeepFormData/sp/sp-metadata.xml"), "WEB-INF/picketlink.xml");

        return serviceProvider;
    }

    @ArquillianResource
    @OperateOnDeployment("identity-provider")
    private URL serviceProviderPostURL;

    @Test
    @OperateOnDeployment("service-provider")
    public void testPostOriginalRequest(@ArquillianResource URL url) throws Exception {

        //send text as a value in post form
        WebRequest request = new GetMethodWebRequest(url.toString()+"/freezone/post-test-main.jsp");
        WebConversation conversation = new WebConversation();
        WebResponse response = conversation.getResponse(request);
        WebForm webForm = response.getForms()[0];
        webForm.setParameter("test-data", TEXT);
        webForm.getSubmitButtons()[0].click();

        //it redirects to idp -> fill userbane and password
        response = conversation.getCurrentPage();
        webForm = response.getForms()[0];
        webForm.setParameter("j_username", TOMCAT);
        webForm.setParameter("j_password", TOMCAT_PSWD);
        webForm.getSubmitButtons()[0].click();

        //it redirects to page, which should contain text sent in first form
        response = conversation.getCurrentPage();
        assertTrue("There should be a text '"+TEXT+"' from first form in response. " + response.getText(), response.getText().contains(TEXT));
    }

    static class SecurityDomainsSetup extends AbstractSecurityDomainsServerSetupTask {

        /**
         * Returns SecurityDomains configuration for this testcase.
         *
         * @see org.jboss.as.test.integration.security.common.AbstractSecurityDomainsServerSetupTask#getSecurityDomains()
         */
        @Override
        protected SecurityDomain[] getSecurityDomains() {

            final Map<String, String> lmOptions = new HashMap<String, String>();
            final SecurityModule.Builder loginModuleBuilder = new SecurityModule.Builder().name("UsersRoles").options(lmOptions);

            lmOptions.put("usersProperties", PropertyFilesSetup.FILE_USERS.getAbsolutePath());
            lmOptions.put("rolesProperties", PropertyFilesSetup.FILE_ROLES.getAbsolutePath());
            final SecurityDomain idp = new SecurityDomain.Builder().name("SAMLPostKeepFormData-idp").loginModules(loginModuleBuilder.build()).build();

            lmOptions.remove("usersProperties");
            lmOptions.remove("rolesProperties");

            final SecurityModule.Builder samlLoginModuleBuilder = new SecurityModule.Builder().name("org.picketlink.identity.federation.bindings.jboss.auth.SAML2LoginModule");
            final SecurityDomain sp = new SecurityDomain.Builder().name("SAMLPostKeepFormData-sp").loginModules(samlLoginModuleBuilder.build())
                    .build();

            return new SecurityDomain[]{idp, sp};
        }
    }

    /**
     * A {@link ServerSetupTask} instance which creates property files with users and roles.
     */
    static class PropertyFilesSetup implements ServerSetupTask {

        public static final File FILE_USERS = new File("test-users.properties");
        public static final File FILE_ROLES = new File("test-roles.properties");

        /**
         * Generates property files.
         */
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            FileUtils.writeStringToFile(FILE_USERS, USERS_EXT, "ISO-8859-1");
            FileUtils.writeStringToFile(FILE_ROLES, ROLES_EXT, "ISO-8859-1");
        }

        /**
         * Removes generated property files.
         */
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            FILE_USERS.delete();
            FILE_ROLES.delete();
        }
    }

}

