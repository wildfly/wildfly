/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.security.common;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.jboss.as.test.integration.security.common.servlets.RolePrintingServlet;
import static org.junit.Assert.fail;

/**
 * Utils class for related methods to printing roles by
 * {@code org.jboss.as.test.integration.security.common.servlets.RolePrintingServlet}.
 *
 * @author olukas
 */
public class RolesPrintingServletUtils {

    /**
     * Prepare URL for printing all possible roles.
     *
     * @param webAppURL application root URL
     * @param allPossibleRoles all possible roles which will be checked by {@link RolePrintingServlets}
     * @return
     */
    public static URL prepareRolePrintingUrl(URL webAppURL, String[] allPossibleRoles) {
        final List<NameValuePair> qparams = new ArrayList<>();
        for (final String role : allPossibleRoles) {
            qparams.add(new BasicNameValuePair(RolePrintingServlet.PARAM_ROLE_NAME, role));
        }
        String queryRoles = URLEncodedUtils.format(qparams, "UTF-8");
        try {
            return new URL(webAppURL.toExternalForm() + RolePrintingServlet.SERVLET_PATH.substring(1) + "?" + queryRoles);
        } catch (MalformedURLException ex) {
            throw new RuntimeException(ex);
        }
    }


    /**
     * Check whether Response includes only expected roles. It checks whether all expected roles are included and whether no
     * other role is included.
     *
     * @param rolesResponse body of HTTP response
     * @param allPossibleRoles all possible roles which can be assigned
     * @param expectedAssignedRoles roles which will be checked whether they were assigned to user
     */
    public static void assertExpectedRoles(String rolesResponse, String[] allPossibleRoles, String[] expectedAssignedRoles) {
        final List<String> assignedRolesList = Arrays.asList(expectedAssignedRoles);

        // for checking whether all roles which should be checked are really checked
        int checkedRoles = 0;
        for (String role : allPossibleRoles) {
            if (assignedRolesList.contains(role)) {
                assertInRole(rolesResponse, role);
                checkedRoles++;
            } else {
                assertNotInRole(rolesResponse, role);
            }
        }
        if (assignedRolesList.size() != checkedRoles) {
            throw new RuntimeException("There are some roles which have not been checked. It is probably test case issue - "
                    + "you try to check some roles which are missing in all possible roles array.");
        }
    }

    /**
     * Asserts, the role list returned from the {@link RolePrintingServlet} contains the given role.
     *
     * @param rolePrintResponse
     * @param role
     */
    private static void assertInRole(final String rolePrintResponse, String role) {
        if (!StringUtils.contains(rolePrintResponse, "," + role + ",")) {
            fail("Missing role '" + role + "' assignment");
        }
    }

    /**
     * Asserts, the role list returned from the {@link RolePrintingServlet} doesn't contain the given role.
     *
     * @param rolePrintResponse
     * @param role
     */
    private static void assertNotInRole(final String rolePrintResponse, String role) {
        if (StringUtils.contains(rolePrintResponse, "," + role + ",")) {
            fail("Unexpected role '" + role + "' assignment");
        }
    }
}
