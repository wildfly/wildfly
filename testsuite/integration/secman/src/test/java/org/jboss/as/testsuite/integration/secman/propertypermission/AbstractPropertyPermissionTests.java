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

package org.jboss.as.testsuite.integration.secman.propertypermission;

import static org.jboss.as.testsuite.integration.secman.propertypermission.SystemPropertiesSetup.PROPERTY_NAME;
import static org.junit.Assert.assertEquals;

import java.net.URI;
import java.net.URL;
import java.security.AllPermission;
import java.util.PropertyPermission;

import javax.servlet.http.HttpServletResponse;

import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.shared.integration.ejb.security.PermissionUtils;
import org.jboss.as.testsuite.integration.secman.servlets.JSMCheckServlet;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.container.ClassContainer;
import org.jboss.shrinkwrap.api.container.ManifestContainer;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Abstract parent for testcases aimed on PropertyPermission.
 *
 * @author Josef Cacek
 */
@RunWith(Arquillian.class)
@ServerSetup(SystemPropertiesSetup.class)
@RunAsClient
public abstract class AbstractPropertyPermissionTests {

    public static final Asset ALL_PERMISSIONS_XML = PermissionUtils.createPermissionsXmlAsset(new AllPermission());
    public static final Asset EMPTY_PERMISSIONS_XML = PermissionUtils.createPermissionsXmlAsset();

    public static final Asset GRANT_PERMISSIONS_XML = PermissionUtils.createPermissionsXmlAsset(new PropertyPermission("*",
            "read,write"));
    public static final Asset LIMITED_PERMISSIONS_XML = PermissionUtils.createPermissionsXmlAsset(new PropertyPermission(
            "java.home", "read"));

    protected static final String APP_GRANT = "read-props-grant";
    protected static final String APP_LIMITED = "read-props-limited";
    protected static final String APP_DENY = "read-props-deny";

    private static Logger LOGGER = Logger.getLogger(AbstractPropertyPermissionTests.class);

    /**
     * Checks if the AS runs with security manager enabled.
     *
     * @param webAppURL
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(APP_GRANT)
    public void testJSMEnabled(@ArquillianResource URL webAppURL) throws Exception {
        final URI checkJSMuri = new URI(webAppURL.toExternalForm() + JSMCheckServlet.SERVLET_PATH.substring(1));
        LOGGER.debug("Checking if JSM is enabled: " + checkJSMuri);
        assertEquals("JSM should be enabled.", Boolean.toString(true), Utils.makeCall(checkJSMuri, 200));
    }

    /**
     * Check standard java property access in application, where PropertyPermission for all properties is granted.
     *
     * @param webAppURL
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(APP_GRANT)
    public void testJavaHomePropertyGrant(@ArquillianResource URL webAppURL) throws Exception {
        checkJavaHomeProperty(webAppURL, HttpServletResponse.SC_OK);
    }

    /**
     * Check standard java property access in application, where not all PropertyPermissions are granted.
     *
     * @param webAppURL
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(APP_LIMITED)
    public void testJavaHomePropertyLimited(@ArquillianResource URL webAppURL) throws Exception {
        checkJavaHomeProperty(webAppURL, HttpServletResponse.SC_OK);
    }

    /**
     * Check standard java property access in application, where no PropertyPermission is granted.
     *
     * @param webAppURL
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(APP_DENY)
    public void testJavaHomePropertyDeny(@ArquillianResource URL webAppURL) throws Exception {
        checkJavaHomeProperty(webAppURL, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    /**
     * Check AS defined (standalone.xml) property access in application, where PropertyPermission for all properties is granted.
     *
     * @param webAppURL
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(APP_GRANT)
    public void testASLevelPropertyGrant(@ArquillianResource URL webAppURL) throws Exception {
        checkTestProperty(webAppURL, HttpServletResponse.SC_OK);
    }

    /**
     * Check AS defined (standalone.xml) property access in application, where not all PropertyPermissions are granted.
     *
     * @param webAppURL
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(APP_LIMITED)
    public void testASLevelPropertyLimited(@ArquillianResource URL webAppURL) throws Exception {
        checkTestProperty(webAppURL, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    /**
     * Check AS defined (standalone.xml) property access in application, where no PropertyPermission is granted.
     *
     * @param webAppURL
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(APP_DENY)
    public void testASLevelPropertyDeny(@ArquillianResource URL webAppURL) throws Exception {
        checkTestProperty(webAppURL, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    /**
     * Check access to 'java.home' property.
     */
    protected void checkJavaHomeProperty(URL webAppURL, int expectedStatus) throws Exception {
        checkProperty(webAppURL, "java.home", expectedStatus, null);
    }

    /**
     * Check access to {@value #APP_BASE_NAME} property.
     */
    protected void checkTestProperty(URL webAppURL, final int expectedStatus) throws Exception {
        checkProperty(webAppURL, PROPERTY_NAME, expectedStatus, PROPERTY_NAME);
    }

    /**
     * Adds {@link JSMCheckServlet} to the given archive.
     *
     * @param archive
     */
    protected static void addJSMCheckServlet(final ClassContainer<?> archive) {
        archive.addClass(JSMCheckServlet.class);
    }

    /**
     * Adds {@link JSMCheckServlet} to the given archive.
     *
     * @param archive
     */
    protected static void addPermissionsXml(final ManifestContainer<?> archive, final Asset permissionsAsset,
            final Asset jbossPermissionsAsset) {
        if (permissionsAsset != null) {
            archive.addAsManifestResource(permissionsAsset, "permissions.xml");
        }
        if (jbossPermissionsAsset != null) {
            archive.addAsManifestResource(jbossPermissionsAsset, "jboss-permissions.xml");
        }
    }

    /**
     * Checks access to a system property on the server.
     *
     * @param webAppURL
     * @param propertyName
     * @param expectedCode expected HTTP Status code
     * @param expectedBody expected response value; if null then response body is not checked
     * @throws Exception
     */
    protected abstract void checkProperty(final URL webAppURL, final String propertyName, final int expectedCode,
            final String expectedBody) throws Exception;

}
