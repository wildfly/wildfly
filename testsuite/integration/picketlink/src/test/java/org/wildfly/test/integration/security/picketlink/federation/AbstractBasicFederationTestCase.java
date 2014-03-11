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
import org.junit.Test;
import org.picketlink.identity.federation.bindings.jboss.auth.SAML2LoginModule;

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
public class AbstractBasicFederationTestCase {

    @ArquillianResource
    @OperateOnDeployment("identity-provider")
    private URL idpUrl;

    @ArquillianResource
    @OperateOnDeployment("service-provider-1")
    private URL serviceProvider1;

    @ArquillianResource
    @OperateOnDeployment("service-provider-2")
    private URL serviceProvider2;

    @Test
    public void testFederation() throws Exception {
        WebConversation conversation = new WebConversation();
        HttpUnitOptions.setLoggingHttpHeaders(true);
        WebRequest request = new GetMethodWebRequest(formatUrl(this.serviceProvider1));
        WebResponse response = conversation.getResponse(request);

        assertTrue(response.getURL().getPath().startsWith("/idp"));
        assertEquals(1, response.getForms().length);

        WebForm webForm = response.getForms()[0];

        webForm.setParameter("j_username", "tomcat");
        webForm.setParameter("j_password", "tomcat");

        webForm.getSubmitButtons()[0].click();

        response = conversation.getCurrentPage();

        assertTrue(response.getText().contains("Welcome to " + formatContextPath(this.serviceProvider1)));

        request = new GetMethodWebRequest(formatUrl(this.serviceProvider2));

        response = conversation.getResponse(request);

        assertTrue(response.getText().contains("Welcome to " + formatContextPath(this.serviceProvider2)));
    }

    private String formatUrl(URL url) {
        String stringUrl = url.toString();

        if (stringUrl.contains("127.0.0.1")) {
            return stringUrl.replace("127.0.0.1", "localhost");
        }

        return stringUrl;
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
