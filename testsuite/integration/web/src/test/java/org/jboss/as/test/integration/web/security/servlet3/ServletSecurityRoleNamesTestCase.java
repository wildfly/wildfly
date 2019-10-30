/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2017, Red Hat, Inc., and individual contributors
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
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

/**
 * Unit Test the 'role-name' elements defined in the web.xml work as expected.
 *
 * @author Jan Stourac
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(WebTestsSecurityDomainSetup.class)
@Category(CommonCriteria.class)
public class ServletSecurityRoleNamesTestCase extends ServletSecurityRoleNamesCommon {
    private static final String warName = ServletSecurityRoleNamesTestCase.class.getName();

    @ArquillianResource
    protected URL url;

    @Deployment
    public static WebArchive deployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, warName + WAR_SUFFIX);
        war.addAsWebInfResource(ServletSecurityRoleNamesTestCase.class.getPackage(), "jboss-web.xml", "jboss-web" +
                ".xml");
        war.setWebXML(ServletSecurityRoleNamesTestCase.class.getPackage(), "role-names-web.xml");
        war.addAsWebResource(new StringAsset(SECURED_INDEX_CONTENT), "/" + SECURED_INDEX);
        war.addAsWebResource(new StringAsset(WEAKLY_SECURED_INDEX_CONTENT), "/" + WEAKLY_SECURED_INDEX);
        war.addAsWebResource(new StringAsset(HARD_SECURED_INDEX_CONTENT), "/" + HARD_SECURED_INDEX);
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
