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

package org.jboss.as.testsuite.integration.secman;

import java.net.URL;

import javax.servlet.http.HttpServletResponse;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.testsuite.integration.secman.servlets.UseStaticMethodServlet;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test case, which checks PropertyPermissions assigned to lib of deployed war applications. The applications try to do a
 * protected action and it should either complete successfully if {@link java.util.PropertyPermission} is granted, or fail.
 *
 * @author olukas
 */
@RunWith(Arquillian.class)
@RunAsClient
public class WarLibPropertyPermissionReadTestCase extends AbstractLibPropertyPermissionReadTestCase {

    private static final String WEBAPP_BASE_NAME = "war-lib-read-props";

    /**
     * Creates archive with a tested application.
     *
     * @return {@link WebArchive} instance
     */
    @Deployment(name = WEBAPP_BASE_NAME + SFX_GRANT)
    public static WebArchive createDeployment1() {
        return warDeployment(SFX_GRANT);
    }

    /**
     * Creates archive with a tested application.
     *
     * @return {@link WebArchive} instance
     */
    @Deployment(name = WEBAPP_BASE_NAME + SFX_LIMITED)
    public static WebArchive createDeployment2() {
        return warDeployment(SFX_LIMITED);
    }

    /**
     * Creates archive with a tested application.
     *
     * @return {@link WebArchive} instance
     */
    @Deployment(name = WEBAPP_BASE_NAME + SFX_DENY)
    public static WebArchive createDeployment3() {
        return warDeployment(SFX_DENY);
    }

    /**
     * Check standard java property access in servlet using static method from lib in war, where PropertyPermission for all
     * properties is granted.
     *
     * @param webAppURL
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(WEBAPP_BASE_NAME + SFX_GRANT)
    public void testJavaHomePropertyFromLibGrant(@ArquillianResource URL webAppURL) throws Exception {
        checkJavaHomeProperty(webAppURL, HttpServletResponse.SC_OK);
    }

    /**
     * Check standard java property access in servlet using static method from lib in war, where not all PropertyPermissions are
     * granted.
     *
     * @param webAppURL
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(WEBAPP_BASE_NAME + SFX_LIMITED)
    public void testJavaHomePropertyFromLibLimited(@ArquillianResource URL webAppURL) throws Exception {
        checkJavaHomeProperty(webAppURL, HttpServletResponse.SC_OK);
    }

    /**
     * Check standard java property access in servlet using static method from lib in war, where no PropertyPermission is
     * granted.
     *
     * @param webAppURL
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(WEBAPP_BASE_NAME + SFX_DENY)
    public void testJavaHomePropertyFromLibDeny(@ArquillianResource URL webAppURL) throws Exception {
        checkJavaHomeProperty(webAppURL, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    /**
     * Check standard java property access in servlet using static method from lib in war, where PropertyPermission for all
     * properties is granted.
     *
     * @param webAppURL
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(WEBAPP_BASE_NAME + SFX_GRANT)
    public void testASLevelPropertyFromLibGrant(@ArquillianResource URL webAppURL) throws Exception {
        checkOsNameProperty(webAppURL, HttpServletResponse.SC_OK);
    }

    /**
     * Check standard java property access in servlet using static method from lib in war, where not all PropertyPermissions are
     * granted.
     *
     * @param webAppURL
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(WEBAPP_BASE_NAME + SFX_LIMITED)
    public void testASLevelPropertyFromLibLimited(@ArquillianResource URL webAppURL) throws Exception {
        checkOsNameProperty(webAppURL, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    /**
     * Check standard java property access in servlet using static method from lib in war, where no PropertyPermission is
     * granted.
     *
     * @param webAppURL
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(WEBAPP_BASE_NAME + SFX_DENY)
    public void testASLevelPropertyFromLibDeny(@ArquillianResource URL webAppURL) throws Exception {
        checkOsNameProperty(webAppURL, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    private static WebArchive warDeployment(final String suffix) {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, WEBAPP_BASE_NAME + suffix + ".war");
        war.addClasses(UseStaticMethodServlet.class);

        war.addAsLibraries(createLibrary());

        return war;
    }

}
