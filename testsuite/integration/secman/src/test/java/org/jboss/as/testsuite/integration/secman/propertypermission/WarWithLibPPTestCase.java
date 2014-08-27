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

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.testsuite.integration.secman.servlets.CallPermissionUtilServlet;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.runner.RunWith;

/**
 * Test case, which checks PropertyPermissions assigned to lib of deployed war applications. The applications try to do a
 * protected action and it should either complete successfully if {@link java.util.PropertyPermission} is granted, or fail.
 *
 * @author Ondrej Lukas
 * @author Josef Cacek
 */
@RunWith(Arquillian.class)
@ServerSetup(SystemPropertiesSetup.class)
@RunAsClient
public class WarWithLibPPTestCase extends AbstractPPTestsWithLibrary {

    /**
     * Creates archive with a tested application.
     *
     * @return {@link WebArchive} instance
     */
    @Deployment(name = APP_GRANT, testable = false)
    public static WebArchive createDeployment1() {
        return warDeployment(APP_GRANT, GRANT_PERMISSIONS_XML);
    }

    /**
     * Creates archive with a tested application.
     *
     * @return {@link WebArchive} instance
     */
    @Deployment(name = APP_LIMITED, testable = false)
    public static WebArchive createDeployment2() {
        return warDeployment(APP_LIMITED, LIMITED_PERMISSIONS_XML);
    }

    /**
     * Creates archive with a tested application.
     *
     * @return {@link WebArchive} instance
     */
    @Deployment(name = APP_DENY, testable = false)
    public static WebArchive createDeployment3() {
        return warDeployment(APP_DENY, EMPTY_PERMISSIONS_XML);
    }

    private static WebArchive warDeployment(final String suffix, final Asset permissionsXml) {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, suffix + ".war");
        war.addClasses(CallPermissionUtilServlet.class);
        addJSMCheckServlet(war);
        addPermissionsXml(war, EMPTY_PERMISSIONS_XML, permissionsXml);
        war.addAsLibraries(createLibrary());
        return war;
    }
}
