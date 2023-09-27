/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
