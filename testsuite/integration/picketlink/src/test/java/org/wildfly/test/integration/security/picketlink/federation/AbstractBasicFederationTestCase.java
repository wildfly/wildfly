/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
import com.meterware.httpunit.HttpUnitOptions;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebForm;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;
import org.apache.commons.io.FileUtils;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.security.common.AbstractSecurityDomainsServerSetupTask;
import org.jboss.as.test.integration.security.common.config.SecurityDomain;
import org.jboss.as.test.integration.security.common.config.SecurityModule;
import org.jboss.logging.Logger;
import org.junit.Test;
import org.picketlink.identity.federation.bindings.wildfly.SAML2LoginModule;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Pedro Igor
 */
public abstract class AbstractBasicFederationTestCase {

    @ArquillianResource
    @OperateOnDeployment("identity-provider")
    private URL idpUrl;

    @ArquillianResource
    @OperateOnDeployment("service-provider-1")
    private URL serviceProvider1;

    @ArquillianResource
    @OperateOnDeployment("service-provider-2")
    private URL serviceProvider2;
    
    public static final String GLOBAL_LOGOUT_URL_PARAM = "?GLO=true"; 
    public static final String LOCAL_LOGOUT_URL_PARAM = "?LLO=true"; 
    private static Logger LOGGER = Logger.getLogger(AbstractBasicFederationTestCase.class);
    
    
    @Test
    public void testFederationWithGlobalLogout() throws Exception {
        WebConversation conversation = new WebConversation();
        HttpUnitOptions.setLoggingHttpHeaders(true);
        LOGGER.trace("REQEST: " +formatUrl(this.serviceProvider1));
        WebRequest request = new GetMethodWebRequest(formatUrl(this.serviceProvider1));
        WebResponse response = conversation.getResponse(request);
        LOGGER.trace("RESPONSE: " + response.getText());
        
        assertTrue(response.getURL().getPath().startsWith("/idp"));
        assertEquals(1, response.getForms().length);

        WebForm webForm = response.getForms()[0];

        webForm.setParameter("j_username", "tomcat");
        webForm.setParameter("j_password", "tomcat");

        webForm.getSubmitButtons()[0].click();

        response = conversation.getCurrentPage();

        assertTrue("cannot reach protected content at " + formatUrl(this.serviceProvider1),
          response.getText().contains("Welcome to " + formatContextPath(this.serviceProvider1)));

        LOGGER.trace("REQEST: " +formatUrl(this.serviceProvider2));
        request = new GetMethodWebRequest(formatUrl(this.serviceProvider2));

        response = conversation.getResponse(request);
        LOGGER.trace("RESPONSE: " + response.getText());

        assertTrue("cannot reach protected content at " + formatUrl(this.serviceProvider2),
                response.getText().contains("Welcome to " + formatContextPath(this.serviceProvider2)));
        
        if (performGlobalLogout()) {
            // global logout from serviceProvider2 
            LOGGER.trace("REQEST: " + formatUrl(this.serviceProvider2) + GLOBAL_LOGOUT_URL_PARAM);
            response = conversation.getResponse(formatUrl(this.serviceProvider2) + GLOBAL_LOGOUT_URL_PARAM);
            LOGGER.trace("GLO response(" + this.serviceProvider2 +
                    "):" + response.getText());
            assertTrue("cannot reach logged out page", response.getText().contains("Logout"));
    
            // check if GLO was successful, so serviceProvider1 is requesting IDP login form
            LOGGER.trace("REQEST: " +formatUrl(this.serviceProvider1));
            request = new GetMethodWebRequest(formatUrl(this.serviceProvider1));
            response = conversation.getResponse(request);
            LOGGER.trace("RESPONSE: " + response.getText());
    
            assertTrue("cannot reach IDP", response.getURL().getPath().startsWith("/idp"));
            assertEquals("no form present on supposed IDP login page", 1, response.getForms().length);
        }
    }
    
    @Test
    public void testFederationWithLocalLogout() throws Exception {
        WebConversation conversation = new WebConversation();
        HttpUnitOptions.setLoggingHttpHeaders(true);
        LOGGER.trace("REQEST: " +formatUrl(this.serviceProvider1));
        WebRequest request = new GetMethodWebRequest(formatUrl(this.serviceProvider1));
        WebResponse response = conversation.getResponse(request);
        LOGGER.trace("RESPONSE: " + response.getText());
        
        assertTrue(response.getURL().getPath().startsWith("/idp"));
        assertEquals(1, response.getForms().length);

        WebForm webForm = response.getForms()[0];

        webForm.setParameter("j_username", "tomcat");
        webForm.setParameter("j_password", "tomcat");

        webForm.getSubmitButtons()[0].click();

        response = conversation.getCurrentPage();

        assertTrue("cannot reach protected content at " + formatUrl(this.serviceProvider1),
                response.getText().contains("Welcome to " + formatContextPath(this.serviceProvider1)));

        LOGGER.trace("REQEST: " +formatUrl(this.serviceProvider2));
        request = new GetMethodWebRequest(formatUrl(this.serviceProvider2));

        response = conversation.getResponse(request);
        LOGGER.trace("RESPONSE: " + response.getText());

        assertTrue("cannot reach protected content at " + formatUrl(this.serviceProvider2),
                response.getText().contains("Welcome to " + formatContextPath(this.serviceProvider2)));
        
        // local logout from serviceProvider2 
        LOGGER.trace("REQEST: " + formatUrl(this.serviceProvider2) + LOCAL_LOGOUT_URL_PARAM);
        response = conversation.getResponse(formatUrl(this.serviceProvider2) + LOCAL_LOGOUT_URL_PARAM);
        LOGGER.trace("LLO response(" + this.serviceProvider2 +
                "):" + response.getText());
        assertTrue("cannot reach locally logged out page", response.getText().contains("Logout"));

        // check if it was really LLO
        LOGGER.trace("REQEST: " +formatUrl(this.serviceProvider1));
        request = new GetMethodWebRequest(formatUrl(this.serviceProvider1));
        response = conversation.getResponse(request);
        LOGGER.trace("RESPONSE: " + response.getText());
        assertTrue("cannot reach protected content at " + formatUrl(this.serviceProvider1),
                response.getText().contains("Welcome to " + formatContextPath(this.serviceProvider1)));

        // LLO from serviceProvider1
        LOGGER.trace("REQEST: " + formatUrl(this.serviceProvider1) + LOCAL_LOGOUT_URL_PARAM);
        response = conversation.getResponse(formatUrl(this.serviceProvider1) + LOCAL_LOGOUT_URL_PARAM);
        LOGGER.trace("LLO response(" + this.serviceProvider1 +
                "):" + response.getText());
        assertTrue("cannot reach locally logged out page", response.getText().contains("Logout"));

    }

    public boolean performGlobalLogout() {
        return true;
    }
    
    private String formatUrl(URL url) {
        return url.toString();
    }

    private String formatContextPath(URL url) {
        return url.getPath().replace("/", "");
    }

    static class BasicSecurityDomainServerSetupTask extends AbstractSecurityDomainsServerSetupTask {

        public static final File FILE_USERS = new File("test-users.properties");
        public static final File FILE_ROLES = new File("test-roles.properties");

        @Override
        protected SecurityDomain[] getSecurityDomains() throws Exception {
            return new SecurityDomain[] {createIdPSecurityDomain(), createSPSecurityDomain()};
        }

        private SecurityDomain createSPSecurityDomain() {
            final SecurityModule.Builder loginModuleBuilder2 = new SecurityModule.Builder().name(SAML2LoginModule.class.getName());

            return new SecurityDomain.Builder().name("sp").loginModules(loginModuleBuilder2.build()).build();
        }

        private SecurityDomain createIdPSecurityDomain() throws IOException {
            final Map<String, String> lmOptions = new HashMap<String, String>();
            final SecurityModule.Builder loginModuleBuilder = new SecurityModule.Builder().name("UsersRoles").options(lmOptions);

            lmOptions.put("usersProperties", FILE_USERS.getAbsolutePath());
            lmOptions.put("rolesProperties", FILE_ROLES.getAbsolutePath());

            FileUtils.writeStringToFile(FILE_USERS, "tomcat=tomcat", "ISO-8859-1");
            FileUtils.writeStringToFile(FILE_ROLES, "tomcat=gooduser", "ISO-8859-1");

            return new SecurityDomain.Builder().name("idp").loginModules(loginModuleBuilder.build()).build();
        }
    }
}
