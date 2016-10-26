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
package org.jboss.as.test.integration.security.jacc;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.net.URL;

import org.dom4j.Document;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.security.Constants;
import org.jboss.as.test.categories.CommonCriteria;
import org.jboss.as.test.integration.security.common.AbstractSecurityDomainsServerSetupTask;
import org.jboss.as.test.integration.security.common.config.SecurityDomain;
import org.jboss.as.test.integration.security.common.config.SecurityModule;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

/**
 * Test based on a section 3.1.3 "Translating Servlet Deployment Descriptors" of the JACC 1.1 specification. This tests works
 * with deployment descriptor (web.xml) content which is a part of the JACC specification as an Example section 3.1.3.4.
 *
 * @author Josef Cacek
 */
@RunWith(Arquillian.class)
@ServerSetup({JACCTranslateServletDDTestCase.SecurityDomainsSetup.class})
@RunAsClient
@Category(CommonCriteria.class)
@Ignore("WFLY-4991")
public class JACCTranslateServletDDTestCase {
    private static final String SECURITY_DOMAIN_NAME = "jacc-test";
    private static final String WEBAPP_NAME = "jacc-test.war";
    private static Logger LOGGER = Logger.getLogger(JACCTranslateServletDDTestCase.class);

    // Public methods --------------------------------------------------------

    /**
     * Creates {@link WebArchive}.
     *
     * @return
     */
    @Deployment
    public static WebArchive warDeployment() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, WEBAPP_NAME);
        war.addClass(ListJACCPoliciesServlet.class);
        war.addAsWebInfResource(JACCTranslateServletDDTestCase.class.getPackage(), "web-JACC11-example.xml", "web.xml");
        war.addAsWebInfResource(new StringAsset("<jboss-web>" + //
                "<security-domain>" + SECURITY_DOMAIN_NAME + "</security-domain>" + //
                "</jboss-web>"), "jboss-web.xml");
        return war;
    }

    /**
     * Test canonical form of HTTP Method list.
     *
     * @see #testHTTPMethodExceptionList(URL) for some other tests
     */
    @Test
    public void testHTTPMethodCanonical(@ArquillianResource URL webAppURL) throws Exception {
        final Node node = getContextPolicyNode(webAppURL, WEBAPP_NAME);
        assertTrue("HTTP Method names should be sorted alphabetically", node.selectNodes("*/Permission[@actions='PUT,DELETE']")
                .isEmpty());
        assertFalse("HTTP Method names should be sorted alphabetically", node
                .selectNodes("*/Permission[@actions='DELETE,PUT']").isEmpty());

        assertFalse("HTTP Method names should be sorted alphabetically",
                node.selectNodes("RolePermissions/Role/Permission[@actions='GET,POST']").isEmpty());
        assertTrue("HTTP Method names should be sorted alphabetically",
                node.selectNodes("RolePermissions/Role/Permission[@actions='POST,GET']").isEmpty());
        assertFalse("HTTP Method names should be sorted alphabetically, followed by colon-separated transport guarantee", node
                .selectNodes("UncheckedPermissions/Permission[@actions='GET,POST:CONFIDENTIAL']").isEmpty());
    }

    /**
     * Test usage of transport guarantee constraints.
     */
    @Test
    public void testConnectionType(@ArquillianResource URL webAppURL) throws Exception {
        final Node node = getContextPolicyNode(webAppURL, WEBAPP_NAME);
        assertFalse(
                "WebUserDataPermission with the connection type :CONFIDENTIAL should be present in unchecked permissions ",
                node.selectNodes(
                        "UncheckedPermissions/Permission[ends-with(@type,'WebUserDataPermission') and @actions='GET:CONFIDENTIAL']")
                        .isEmpty());
        assertFalse(
                "WebUserDataPermission with the connection type :CONFIDENTIAL should be present in unchecked permissions ",
                node.selectNodes(
                        "UncheckedPermissions/Permission[ends-with(@type,'WebUserDataPermission') and @actions='GET,POST:CONFIDENTIAL']")
                        .isEmpty());
    }

    /**
     * Test canonical form of HTTP Method list.
     */
    @Test
    @Ignore("JBPAPP-9405 JACC 1.1 implementation must use exception list instead of missing method list for HTTP methods in the unchecked permissions")
    public void testHTTPMethodSubtraction(@ArquillianResource URL webAppURL) throws Exception {
        final Node node = getContextPolicyNode(webAppURL, WEBAPP_NAME);
        assertTrue("HTTP Method exception list must be used instead of method subtraction from 'big 7'.",
                node.selectNodes("UncheckedPermissions/Permission[@actions='GET,HEAD,OPTIONS,POST,TRACE']").isEmpty());
    }

    /**
     * Test handling of HTTP Method Exception list.
     */
    @Test
    @Ignore("JBPAPP-9400 - JACC permissions with HTTP method exception list are not correctly implemented")
    public void testHTTPMethodExceptionList(@ArquillianResource URL webAppURL) throws Exception {
        final Node node = getContextPolicyNode(webAppURL, WEBAPP_NAME);
        assertFalse("Can't find permissions with a HTTP Method exception list",
                node.selectNodes("UncheckedPermissions/Permission[@actions='!DELETE,GET,PUT']").isEmpty());
        assertFalse("Can't find permissions with a HTTP Method exception list",
                node.selectNodes("UncheckedPermissions/Permission[@actions='!DELETE,GET,POST,PUT']").isEmpty());
        assertFalse("Can't find permissions with a HTTP Method exception list",
                node.selectNodes("UncheckedPermissions/Permission[@actions='!DELETE,PUT']").isEmpty());

        assertTrue("HTTP Method exception list should be constructed by using exclamation mark",
                node.selectNodes("UncheckedPermissions/Permission[@actions='GET,HEAD,OPTIONS,POST,TRACE']").isEmpty());
    }

    /**
     * Test usage of qualified patterns.
     */
    @Test
    public void testQualifiedPatterns(@ArquillianResource URL webAppURL) throws Exception {
        final Node node = getContextPolicyNode(webAppURL, WEBAPP_NAME);
        assertTrue("Default pattern '/' must be qualified.", node.selectNodes("*/Permission[@name='/']").isEmpty());
        assertTrue("Qualified default pattern should not be present in ExcludedPermissions.",
                node.selectNodes("ExcludedPermissions/Permission[starts-with(@name,'/:')]").isEmpty());
        assertFalse("Qualified default pattern should be present in UncheckedPermissions.",
                node.selectNodes("UncheckedPermissions/Permission[starts-with(@name,'/:')]").isEmpty());
        assertTrue("Path prefix pattern must be qualified.", node.selectNodes("*/Permission[@name='/b/*']").isEmpty());
        assertTrue("Path prefix pattern must be qualified.", node.selectNodes("*/Permission[@name='/a/*']").isEmpty());
        assertTrue("Extension pattern must be qualified.", node.selectNodes("*/Permission[@name='*.asp']").isEmpty());
    }

    // Private methods -------------------------------------------------------

    /**
     * Retruns Node representing ContextPolicy with given contextId.
     *
     * @param webAppURL
     */
    private Node getContextPolicyNode(final URL webAppURL, String contextId) throws Exception {
        final URL servletURL = new URL(webAppURL.toExternalForm() + ListJACCPoliciesServlet.SERVLET_PATH.substring(1));
        LOGGER.trace("Testing JACC permissions: " + servletURL);

        final InputStream is = servletURL.openStream();
        try {
            final Document document = new SAXReader().read(is);
            final String xpathBase = "/" + ListJACCPoliciesServlet.ROOT_ELEMENT
                    + "/ActiveContextPolicies/ContextPolicy[@contextID='" + contextId + "']";
            final Node contextPolicyNode = document.selectSingleNode(xpathBase);
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(contextPolicyNode.asXML());
            }
            return contextPolicyNode;
        } finally {
            is.close();
        }
    }

    // Embedded classes ------------------------------------------------------

    /**
     * A {@link ServerSetupTask} instance which creates security domains for this test case.
     *
     * @author Josef Cacek
     */
    static class SecurityDomainsSetup extends AbstractSecurityDomainsServerSetupTask {

        /**
         * @see org.jboss.as.test.integration.security.common.AbstractSecurityDomainsServerSetupTask#getSecurityDomains()
         */
        @Override
        protected SecurityDomain[] getSecurityDomains() {
            return new SecurityDomain[]{new SecurityDomain.Builder().name(SECURITY_DOMAIN_NAME)
                    .authorizationModules(new SecurityModule.Builder().name("JACC").flag(Constants.REQUIRED).build()).build()};
        }
    }
}
