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
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.URL;

import static org.junit.Assert.assertTrue;
import static org.wildfly.test.integration.security.picketlink.federation.AbstractBasicFederationTestCase.formatUrl;
import static org.wildfly.test.integration.security.picketlink.federation.util.FederationArchiveUtil.identityProvider;
import static org.wildfly.test.integration.security.picketlink.federation.util.FederationArchiveUtil.serviceProvider;

/**
 * @author Pedro Igor
 * @author Peter Skopek - porting to WF integration testsuite
 */
@RunWith(Arquillian.class)
// the standalone-picketlink.xml, the example configuration used by this test, contains all required security domains already
//@ServerSetup({ AbstractBasicFederationTestCase.BasicSecurityDomainServerSetupTask.class })
@RunAsClient
public class RestoreOriginalRequestTestCase {

    @Deployment(name = "identity-provider-1")
    public static WebArchive deploymentIdP1() {
        return identityProvider("idp-redirect.war");
    }

    @Deployment(name = "identity-provider-2")
    public static WebArchive deploymentIdP2() {
        return identityProvider("idp-post.war");
    }

    @Deployment(name = "service-provider-1")
    public static WebArchive deploymentSP1() {
        WebArchive serviceProvider = serviceProvider("sp-redirect1.war");
        serviceProvider.add(new StringAsset("Back to the original requested resource."), "savedRequest/savedRequest.html");
        return serviceProvider;
    }

    @Deployment(name = "service-provider-2")
    public static WebArchive deploymentSP2() {
        WebArchive serviceProvider = serviceProvider("sp-post2.war");
        serviceProvider.add(new StringAsset("Back to the original requested resource."), "savedRequest/savedRequest.html");
        serviceProvider.add(new StringAsset("<%= request.getParameter(\"SAVED_PARAM\") %>."), "savedRequest/savedRequest.jsp");
        return serviceProvider;
    }

    @Test
    @OperateOnDeployment("service-provider-2")
    public void testPostOriginalRequest(@ArquillianResource URL serviceProvider2) throws Exception {
        WebRequest request = new GetMethodWebRequest(formatUrl(serviceProvider2) + "/savedRequest/savedRequest.html");
        WebConversation conversation = new WebConversation();
        WebResponse response = conversation.getResponse(request);

        WebForm webForm = response.getForms()[0];

        webForm.setParameter("j_username", "tomcat");
        webForm.setParameter("j_password", "tomcat");

        webForm.getSubmitButtons()[0].click();

        response = conversation.getCurrentPage();

        assertTrue(response.getText().contains("Back to the original requested resource."));
    }

    @Test
    @OperateOnDeployment("service-provider-2")
    public void testPostOriginalRequestWithParams(@ArquillianResource URL serviceProvider2) throws Exception {
        WebRequest request = new GetMethodWebRequest(formatUrl(serviceProvider2) + "/savedRequest/savedRequest.jsp");

        request.setParameter("SAVED_PARAM", "Param was saved.");

        WebConversation conversation = new WebConversation();
        WebResponse response = conversation.getResponse(request);

        WebForm webForm = response.getForms()[0];

        webForm.setParameter("j_username", "tomcat");
        webForm.setParameter("j_password", "tomcat");

        webForm.getSubmitButtons()[0].click();

        response = conversation.getCurrentPage();

        assertTrue(response.getText().contains("Param was saved."));
    }

    @Test
    @OperateOnDeployment("service-provider-1")
    public void testRedirectOriginalRequest(@ArquillianResource URL serviceProvider1) throws Exception {
        WebRequest request = new GetMethodWebRequest(formatUrl(serviceProvider1) + "/savedRequest/savedRequest.html");
        WebConversation conversation = new WebConversation();
        WebResponse response = conversation.getResponse(request);

        WebForm webForm = response.getForms()[0];

        webForm.setParameter("j_username", "tomcat");
        webForm.setParameter("j_password", "tomcat");

        webForm.getSubmitButtons()[0].click();

        response = conversation.getCurrentPage();

        assertTrue(response.getText().contains("Back to the original requested resource."));
    }

}
