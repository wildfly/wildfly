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

import static org.junit.Assert.assertTrue;
import static org.wildfly.test.integration.security.picketlink.federation.util.FederationArchiveUtil.identityProvider;
import static org.wildfly.test.integration.security.picketlink.federation.util.FederationArchiveUtil.serviceProvider;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;

import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebForm;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Pedro Igor
 * @author Peter Skopek - porting to WF integration testsuite
 */
@RunWith(Arquillian.class)
// the standalone-picketlink.xml, the example configuration used by this test, contains all required security domains already
//@ServerSetup({ AbstractBasicFederationTestCase.BasicSecurityDomainServerSetupTask.class })
@RunAsClient
public class SAMLIDPInitiatedTestCase {

    private static Logger LOGGER = Logger.getLogger(SAMLIDPInitiatedTestCase.class);
    private static String webURI;

    static {
        String node0 = System.getProperty("node0", "127.0.0.1");
        String port0 = "8080";
        try {
            webURI = URLEncoder.encode("http://" + (node0.indexOf(":") > -1 ? "[" + node0 + "]" : node0) + ":" + port0, "ISO-8859-1");
        } catch (UnsupportedEncodingException e) {
            LOGGER.error(e);
        }
    }

    @Deployment(name = "identity-provider")
    public static WebArchive deploymentIdP() {
        LOGGER.debug("webURI in deployment:" + webURI);
        return identityProvider("idp-post.war", null,
                "Welcome to IdP hosted<br/>"
                        + "<a id=\"service-provider-1-id\" href=\"?SAML_VERSION=2.0&TARGET=" + webURI + "/sp-post1/\">Service Provider 1 Test Link</a>");
    }

    @Deployment(name = "service-provider-1")
    public static WebArchive deploymentSP1() {
        WebArchive serviceProvider = serviceProvider("sp-post1.war");
        serviceProvider.add(new StringAsset("Back to the original requested resource."), "savedRequest/savedRequest.jsp");
        return serviceProvider;
    }

    @ArquillianResource
    @OperateOnDeployment("service-provider-1")
    private URL serviceProviderPostURL;

    @Test
    @OperateOnDeployment("identity-provider")
    public void testPostOriginalRequest(@ArquillianResource URL url) throws Exception {
        WebRequest request = new GetMethodWebRequest(url.toString());
        WebConversation conversation = new WebConversation();
        WebResponse response = conversation.getResponse(request);

        WebForm webForm = response.getForms()[0];

        webForm.setParameter("j_username", "tomcat");
        webForm.setParameter("j_password", "tomcat");

        webForm.getSubmitButtons()[0].click();

        request = new GetMethodWebRequest(url + "?SAML_VERSION=2.0&TARGET=" + this.serviceProviderPostURL + "/savedRequest/savedRequest.jsp");

        response = conversation.getResponse(request);

        assertTrue(response.getText().contains("Back to the original requested resource."));
    }
}

