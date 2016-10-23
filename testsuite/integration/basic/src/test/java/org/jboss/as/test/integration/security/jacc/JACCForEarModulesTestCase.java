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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.security.Constants;
import org.jboss.as.test.categories.CommonCriteria;
import org.jboss.as.test.integration.security.common.AbstractSecurityDomainsServerSetupTask;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.integration.security.common.config.SecurityDomain;
import org.jboss.as.test.integration.security.common.config.SecurityModule;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

/**
 * Tests, which checks JACC permissions generated for enterprise applications.
 *
 * @author Josef Cacek
 */
@RunWith(Arquillian.class)
@ServerSetup({JACCForEarModulesTestCase.SecurityDomainsSetup.class})
@RunAsClient
@Category(CommonCriteria.class)
@Ignore("See WFLY-4990")
public class JACCForEarModulesTestCase {

    private static final String SECURITY_DOMAIN_NAME = "jacc-test";
    private static Logger LOGGER = Logger.getLogger(JACCForEarModulesTestCase.class);

    // Public methods --------------------------------------------------------

    /**
     * Creates {@link WebArchive} deployment.
     */
    @Deployment(name = "war")
    public static WebArchive warDeployment() {
        LOGGER.trace("Create WAR deployment");
        return createWar(SECURITY_DOMAIN_NAME);
    }

    /**
     * Creates {@link EnterpriseArchive} deployment.
     */
    @Deployment(name = "ear")
    public static EnterpriseArchive earDeployment() {
        LOGGER.trace("Create EAR deployment");
        final String earName = "ear-" + SECURITY_DOMAIN_NAME;

        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, earName + ".ear");
        final JavaArchive jar = createJar(earName);
        final WebArchive war = createWar(earName);
        ear.addAsModule(war);
        ear.addAsModule(jar);

        return ear;
    }

    /**
     * Creates {@link JavaArchive} deployment.
     */
    @Deployment(name = "jar", testable = false)
    public static JavaArchive jarDeployment() {
        LOGGER.trace("Start JAR deployment");
        return createJar("jar-" + SECURITY_DOMAIN_NAME);
    }

    /**
     * Tests web permissions (war directly and war in ear).
     *
     * @param webAppURL
     * @throws Exception
     */
    @Test
    @OperateOnDeployment("war")
    public void testWebPermissions(@ArquillianResource URL webAppURL) throws Exception {
        final Document doc = getPermissionDocument(webAppURL);
        testJACCWebPermissions(doc.selectSingleNode("/" + ListJACCPoliciesServlet.ROOT_ELEMENT
                + "/ActiveContextPolicies/ContextPolicy[@contextID='jacc-test.war']"));
        testJACCWebPermissions(doc.selectSingleNode("/" + ListJACCPoliciesServlet.ROOT_ELEMENT
                + "/ActiveContextPolicies/ContextPolicy[@contextID='ear-jacc-test.ear!ear-jacc-test.war']"));
    }

    /**
     * Tests EJB permissions (jar directly and jar in ear).
     *
     * @param webAppURL
     * @throws Exception
     */
    @Test
    @OperateOnDeployment("war")
    public void testEJBPermissions(@ArquillianResource URL webAppURL) throws Exception {
        final Document doc = getPermissionDocument(webAppURL);
        testJACCEjbPermissions(doc.selectSingleNode("/" + ListJACCPoliciesServlet.ROOT_ELEMENT
                + "/ActiveContextPolicies/ContextPolicy[@contextID='jar-jacc-test.jar']"));
        testJACCEjbPermissions(doc.selectSingleNode("/" + ListJACCPoliciesServlet.ROOT_ELEMENT
                + "/ActiveContextPolicies/ContextPolicy[@contextID='ear-jacc-test.ear!ear-jacc-test.jar']"));
    }

    // Private methods -------------------------------------------------------

    /**
     * Creates EJB JAR module with the given name.
     *
     * @param jarName
     * @return
     */
    private static JavaArchive createJar(final String jarName) {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, jarName + ".jar");
        jar.addClasses(HelloBeanDD.class);
        jar.addAsManifestResource(EmptyAsset.INSTANCE, ArchivePaths.create("beans.xml"));
        jar.addAsManifestResource(Utils.getJBossEjb3XmlAsset(SECURITY_DOMAIN_NAME), "jboss-ejb3.xml");
        jar.addAsManifestResource(JACCForEarModulesTestCase.class.getPackage(), "ejb-jar.xml", "ejb-jar.xml");
        return jar;
    }

    /**
     * Creates WAR module with the given name.
     *
     * @param warName
     * @return
     */
    private static WebArchive createWar(final String warName) {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, warName + ".war");
        war.addClass(ListJACCPoliciesServlet.class);
        war.addAsWebInfResource(JACCForEarModulesTestCase.class.getPackage(), "web.xml", "web.xml");
        war.addAsWebInfResource(Utils.getJBossWebXmlAsset(SECURITY_DOMAIN_NAME), "jboss-web.xml");
        return war;
    }

    /**
     * Tests web-app permissions in given ContextPolicy Node.
     *
     * @param contextPolicyNode
     * @throws Exception
     */
    private void testJACCWebPermissions(final Node contextPolicyNode) throws Exception {
        assertNotNull("Context policy for the web application should exist.", contextPolicyNode);
        List<?> permNodes = contextPolicyNode.selectNodes("ExcludedPermissions/Permission");
        assertEquals("ExcludedPermissions should exist.", 2, permNodes.size());
        permNodes = contextPolicyNode.selectNodes("UncheckedPermissions/Permission");
        assertFalse("UncheckedPermissions should exist.", permNodes.isEmpty());

    }

    /**
     * Tests EJB permissions in the given ContextPolicy Node.
     *
     * @param contextPolicyNode
     * @throws Exception
     */
    private void testJACCEjbPermissions(final Node contextPolicyNode) throws Exception {
        assertNotNull("Context policy for the EJB module should exist.", contextPolicyNode);
        List<?> permNodes = contextPolicyNode.selectNodes("ExcludedPermissions/Permission");
        assertFalse("ExcludedPermissions should exist.", permNodes.isEmpty());
    }

    /**
     * Returns Dom4j XML Document representation of the JACC policies retrieved from the {@link ListJACCPoliciesServlet}.
     *
     * @param webAppURL
     * @return
     * @throws MalformedURLException
     * @throws IOException
     * @throws DocumentException
     */
    private Document getPermissionDocument(final URL webAppURL) throws IOException, DocumentException {
        final URL servletURL = new URL(webAppURL.toExternalForm() + ListJACCPoliciesServlet.SERVLET_PATH.substring(1));
        LOGGER.trace("Testing JACC permissions: " + servletURL);
        final InputStream is = servletURL.openStream();
        try {
            final Document document = new SAXReader().read(is);
            return document;
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
