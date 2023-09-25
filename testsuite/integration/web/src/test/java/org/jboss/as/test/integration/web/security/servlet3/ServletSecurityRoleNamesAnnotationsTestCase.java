/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.web.security.servlet3;

import java.net.URL;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.categories.CommonCriteria;
import org.jboss.as.test.integration.web.security.WebTestsSecurityDomainSetup;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

/**
 * Unit Test the 'rolesAllowed' annotations defined in servlet work as expected.
 *
 * @author Jan Stourac
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(WebTestsSecurityDomainSetup.class)
@Category(CommonCriteria.class)
public class ServletSecurityRoleNamesAnnotationsTestCase extends ServletSecurityRoleNamesCommon {
    private static final String warName = ServletSecurityRoleNamesAnnotationsTestCase.class.getName();

    @ArquillianResource
    protected URL url;

    @Deployment
    public static WebArchive deployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, warName + WAR_SUFFIX);
        war.addAsWebInfResource(ServletSecurityRoleNamesTestCase.class.getPackage(), "jboss-web.xml", "jboss-web" +
                ".xml");
        war.setWebXML(ServletSecurityRoleNamesTestCase.class.getPackage(), "role-names-annotations-web.xml");
        war.addClass(RoleNamesAnnotationsServlet.RoleNamesAnnotationsSecuredServlet.class);
        war.addClass(RoleNamesAnnotationsServlet.RoleNamesAnnotationsWeaklySecuredServlet.class);
        war.addClass(RoleNamesAnnotationsServlet.RoleNamesAnnotationsHardSecuredServlet.class);
        war.addPackage(CommonCriteria.class.getPackage());

        return war;
    }

    protected void makeCallSecured(String user, String pass, int expectedCode) throws Exception {
        makeCall(user, pass, expectedCode, new URL(url.toExternalForm() + SECURED_INDEX));
    }

    protected void makeCallWeaklySecured(String user, String pass, int expectedCode) throws Exception {
        makeCall(user, pass, expectedCode, new URL(url.toExternalForm() + WEAKLY_SECURED_INDEX));
    }

    protected void makeCallHardSecured(String user, String pass, int expectedCode) throws Exception {
        makeCall(user, pass, expectedCode, new URL(url.toExternalForm() + HARD_SECURED_INDEX));
    }
}
