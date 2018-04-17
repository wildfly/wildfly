/*
Copyright 2018 Red Hat, Inc.
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
  http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package org.jboss.as.test.integration.security.jacc.context;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.security.common.AbstractElytronSetupTask;
import org.wildfly.test.security.common.elytron.ConfigurableElement;
import org.wildfly.test.security.common.elytron.PropertyFileBasedDomain;
import org.wildfly.test.security.common.elytron.UndertowDomainMapper;
import org.jboss.as.arquillian.api.ServerSetup;



import java.net.URL;
/**
 *
 * @author <a href="mailto:padamec@redhat.com">Petr Adamec</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({ PolicyContextGetContextTestCase.SecurityDomainsSetup.class })
public class PolicyContextGetContextTestCase {
    private static final String NAME= PolicyContextGetContextServlet.class.getSimpleName();
    /**
     * Creates {@link WebArchive} deployment.
     */

    @Deployment(name = "war")
    public static WebArchive warDeployment() {
        return createWarWithPolicyContextGetContextServlet();

    }

    @Test
    public void testPolicyContextGetContext(@ArquillianResource URL webAppURL) throws Exception {
        final URL servletUrl = new URL(webAppURL.toExternalForm() + PolicyContextGetContextServlet.SERVLET_PATH.substring(1));
        String response = Utils.makeCallWithBasicAuthn(servletUrl, "elytron", "password", 200);
        Assert.assertTrue("Test failed because PolicyContext.getContext(\"javax.security.auth.Subject.container\") returns null ",response.contains(PolicyContextGetContextServlet.SUCCESS_MESSAGE));

    }

    private static WebArchive createWarWithPolicyContextGetContextServlet() {
        return ShrinkWrap.create(WebArchive.class, NAME + ".war")
                .addClasses(PolicyContextGetContextServlet.class)
                .addAsWebInfResource(new StringAsset("<web-app>\n" + //
                        "  <login-config><auth-method>BASIC</auth-method><realm-name>Test realm</realm-name></login-config>\n" + //
                        "</web-app>"), "web.xml")
                .addAsWebInfResource(new StringAsset("<jboss-web>\n" + //
                        "  <security-domain>" + NAME + "</security-domain>\n" + //
                        "</jboss-web>"), "jboss-web.xml");
    }

    /**
     * Create properties-file backed Elytron domain with user and mapping in Undertow.
     */
    static class SecurityDomainsSetup extends AbstractElytronSetupTask {

        @Override
        protected ConfigurableElement[] getConfigurableElements() {
            return new ConfigurableElement[] { PropertyFileBasedDomain.builder().withName(NAME)
                    .withUser("elytron", "password")
                    .build(), UndertowDomainMapper.builder().withName(NAME).build() };
        }
    }

}
