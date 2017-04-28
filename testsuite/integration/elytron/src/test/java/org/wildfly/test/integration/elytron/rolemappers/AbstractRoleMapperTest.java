/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat Middleware LLC, and individual contributors
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
package org.wildfly.test.integration.elytron.rolemappers;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.security.auth.login.LoginException;
import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import org.apache.commons.lang.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.integration.security.common.servlets.RolePrintingServlet;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import static org.junit.Assert.fail;

/**
 * Abstract class for Role Mapper related test cases.
 *
 * @author olukas
 */
public abstract class AbstractRoleMapperTest {

    public static final String ROLE1 = "Role1";
    public static final String ROLE2 = "Role2";
    public static final String ROLE3 = "Role3";
    public static final String ROLE4 = "Role4";

    public static WebArchive createDeploymentForPrintingRoles(String securityDomainName) {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, securityDomainName + ".war");
        war.addClasses(RolePrintingServlet.class);
        war.addAsWebInfResource(AbstractRoleMapperTest.class.getPackage(), "role-mapper-web.xml", "web.xml");
        war.addAsWebInfResource(Utils.getJBossWebXmlAsset(securityDomainName), "jboss-web.xml");
        return war;
    }

    public void testAssignedRoles(URL webAppURL, String username, String password, String... assignedRoles)
            throws
            IOException, URISyntaxException, LoginException {
        final URL rolesPrintingURL = prepareRolesPrintingURL(webAppURL);
        final String rolesResponse = Utils.makeCallWithBasicAuthn(rolesPrintingURL, username, password, SC_OK);

        final List<String> assignedRolesList = Arrays.asList(assignedRoles);

        for (String role : allTestedRoles()) {
            if (assignedRolesList.contains(role)) {
                assertInRole(rolesResponse, role);
            } else {
                assertNotInRole(rolesResponse, role);
            }
        }
    }

    public void assertNoRoleAssigned(URL webAppURL, String username, String password) throws Exception {
        final URL rolesPrintingURL = prepareRolesPrintingURL(webAppURL);
        Utils.makeCallWithBasicAuthn(rolesPrintingURL, username, password, SC_FORBIDDEN);
    }

    private URL prepareRolesPrintingURL(URL webAppURL) throws MalformedURLException {
        final List<NameValuePair> qparams = new ArrayList<NameValuePair>();
        for (final String role : allTestedRoles()) {
            qparams.add(new BasicNameValuePair(RolePrintingServlet.PARAM_ROLE_NAME, role));
        }
        String queryRoles = URLEncodedUtils.format(qparams, "UTF-8");
        return new URL(webAppURL.toExternalForm() + RolePrintingServlet.SERVLET_PATH.substring(1) + "?"
                + queryRoles);
    }

    private void assertInRole(final String rolePrintResponse, String role) {
        if (!StringUtils.contains(rolePrintResponse, "," + role + ",")) {
            fail("Missing role '" + role + "' assignment");
        }
    }

    private void assertNotInRole(final String rolePrintResponse, String role) {
        if (StringUtils.contains(rolePrintResponse, "," + role + ",")) {
            fail("Unexpected role '" + role + "' assignment");
        }
    }

    protected abstract String[] allTestedRoles();

}
