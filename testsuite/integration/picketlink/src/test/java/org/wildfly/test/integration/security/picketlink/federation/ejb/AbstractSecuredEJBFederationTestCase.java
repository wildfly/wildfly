/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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
package org.wildfly.test.integration.security.picketlink.federation.ejb;

import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebForm;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;
import org.apache.commons.io.FileUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.security.common.AbstractSecurityDomainsServerSetupTask;
import org.jboss.as.test.integration.security.common.config.SecurityDomain;
import org.jboss.as.test.integration.security.common.config.SecurityModule;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.wildfly.test.integration.security.picketlink.federation.util.FederationArchiveUtil.identityProvider;
import static org.wildfly.test.integration.security.picketlink.federation.util.FederationArchiveUtil.serviceProvider;

/**
 *
 * Test for issue: https://issues.jboss.org/browse/PLINK-793 Exact steps are described in issue.
 */
public abstract class AbstractSecuredEJBFederationTestCase {

    private static final String TOMCAT = "tomcat";
    private static final String TOMCAT_PSWD = "tomcat";
    private static final String USERS_EXT = TOMCAT + "=" + TOMCAT_PSWD;
    private static final String ROLES_EXT = TOMCAT + "=All";

    @Deployment(name = "identity-provider")
    public static WebArchive deploymentIdP() {
        WebArchive identityProvider = identityProvider("idp.war");

        identityProvider.addAsWebResource(AbstractSecuredEJBFederationTestCase.class.getResource("idp/idp-picketlink.xml"), "WEB-INF/picketlink.xml");
        identityProvider.addAsWebInfResource(AbstractSecuredEJBFederationTestCase.class.getResource("idp/web.xml"), "web.xml");
        identityProvider.addAsWebInfResource(AbstractSecuredEJBFederationTestCase.class.getResource("idp/idp-jboss-web.xml"), "jboss-web.xml");

        identityProvider.addAsWebResource(AbstractSecuredEJBFederationTestCase.class.getResource("idp/login.jsp"), "login.jsp");
        identityProvider.addAsWebResource(AbstractSecuredEJBFederationTestCase.class.getResource("idp/error.jsp"), "error.jsp");

        return identityProvider;
    }

    @Deployment(name = "service-provider")
    public static WebArchive deploymentSP() {
        WebArchive serviceProvider = serviceProvider("sp.war");

        serviceProvider.addAsWebInfResource(AbstractSecuredEJBFederationTestCase.class.getResource("sp/web.xml"), "web.xml");
        serviceProvider.addAsWebResource(AbstractSecuredEJBFederationTestCase.class.getResource("sp/sp-jboss-web.xml"), "WEB-INF/jboss-web.xml");

        serviceProvider.addAsWebResource(AbstractSecuredEJBFederationTestCase.class.getResource("sp/sp-picketlink.xml"),
                "WEB-INF/picketlink.xml");
        serviceProvider.addClass(SecuredStatelessBean.class);
        serviceProvider.addClass(RestRoot.class);
        serviceProvider.addClass(SecuredRest.class);

        return serviceProvider;
    }

    @ArquillianResource
    @OperateOnDeployment("identity-provider")
    protected URL serviceProviderPostURL;

    @Test
    @OperateOnDeployment("service-provider")
    public void testEJBNoCacheRequest(@ArquillianResource URL url) throws Exception {

        // send text as a value in post form
        WebRequest request = new GetMethodWebRequest(url.toString() + "rest/test");
        WebConversation conversation = new WebConversation();
        WebResponse response = conversation.getResponse(request);
        response = conversation.getCurrentPage();
        WebForm webForm = response.getForms()[0];

        //it redirects to idp -> fill username and password
        webForm.setParameter("j_username", TOMCAT);
        webForm.setParameter("j_password", TOMCAT_PSWD);
        webForm.getSubmitButtons()[0].click();

        // it runs secured ejb
        response = conversation.getCurrentPage();
        assertEquals("The ejb authentication did not succeed",200, response.getResponseCode());
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
            final SecurityModule.Builder loginModuleBuilder = new SecurityModule.Builder().name("UsersRoles")
                    .options(lmOptions);

            lmOptions.put("usersProperties", PropertyFilesSetup.FILE_USERS.getAbsolutePath());
            lmOptions.put("rolesProperties", PropertyFilesSetup.FILE_ROLES.getAbsolutePath());
            final SecurityDomain idp = new SecurityDomain.Builder().name("ejb-idp").cacheType("default").loginModules(loginModuleBuilder.build())
                    .build();

            lmOptions.remove("usersProperties");
            lmOptions.remove("rolesProperties");

            final SecurityModule.Builder samlLoginModuleBuilder = new SecurityModule.Builder()
                    .name("org.picketlink.identity.federation.bindings.jboss.auth.SAML2LoginModule");
            final SecurityDomain sp = new SecurityDomain.Builder().name("ejb-sp").cacheType("default").loginModules(samlLoginModuleBuilder.build())
                    .build();
            final SecurityModule.Builder ejbModuleBuilder = new SecurityModule.Builder().name("Delegating").flag("required");
            final SecurityDomain ejbsp = new SecurityDomain.Builder().name("jboss-ejb-policy").cacheType("default")
                    .authorizationModules(ejbModuleBuilder.build()).build();

            return new SecurityDomain[] { idp, sp, ejbsp };
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
