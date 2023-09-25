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
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.runner.RunWith;

/**
 * Test case, which checks PropertyPermissions assigned to lib in war of deployed ear applications. The applications try to do a
 * protected action and it should either complete successfully if {@link java.util.PropertyPermission} is granted, or fail.
 *
 * @author Ondrej Lukas
 * @author Josef Cacek
 */
@RunWith(Arquillian.class)
@ServerSetup(SystemPropertiesSetup.class)
@RunAsClient
public class EarLibInWarPPTestCase extends AbstractPPTestsWithLibrary {

    /**
     * Creates archive with a tested application.
     *
     * @return {@link EnterpriseArchive} instance
     */
    @Deployment(name = APP_GRANT, testable = false)
    public static EnterpriseArchive createDeployment1() {
        return earDeployment(APP_GRANT, GRANT_PERMISSIONS_XML);
    }

    /**
     * Creates archive with a tested application.
     *
     * @return {@link EnterpriseArchive} instance
     */
    @Deployment(name = APP_LIMITED, testable = false)
    public static EnterpriseArchive createDeployment2() {
        return earDeployment(APP_LIMITED, LIMITED_PERMISSIONS_XML);
    }

    /**
     * Creates archive with a tested application.
     *
     * @return {@link EnterpriseArchive} instance
     */
    @Deployment(name = APP_DENY, testable = false)
    public static EnterpriseArchive createDeployment3() {
        return earDeployment(APP_DENY, EMPTY_PERMISSIONS_XML);
    }

    private static EnterpriseArchive earDeployment(final String app, Asset permissionsXml) {

        final WebArchive war = ShrinkWrap.create(WebArchive.class, app + ".war");
        addJSMCheckServlet(war);
        war.addClasses(CallPermissionUtilServlet.class);
        war.addAsLibraries(createLibrary());

        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, app + ".ear");
        // override grant-all in permissions.xml by customized jboss-permissions.xm
        addPermissionsXml(ear, ALL_PERMISSIONS_XML, permissionsXml);
        ear.addAsModule(war);

        return ear;
    }
}
