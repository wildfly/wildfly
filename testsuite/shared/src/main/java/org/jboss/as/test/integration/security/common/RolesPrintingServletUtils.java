/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.security.common;

import static org.junit.Assert.fail;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.jboss.as.test.integration.security.common.servlets.RolePrintingServlet;

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
        String queryRoles = URLEncodedUtils.format(qparams, StandardCharsets.UTF_8);
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
