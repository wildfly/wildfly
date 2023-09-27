/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.security;

import org.junit.Test;

/**
 * Base class for web security tests that are based on passwords
 *
 * @author Anil Saldhana
 */
public abstract class WebSecurityPasswordBasedBase {

    /**
     * Test with user "anil" who has the right password and the right role to access the servlet
     *
     * @throws Exception
     */
    @Test
    public void testPasswordBasedSuccessfulAuth() throws Exception {
        makeCall("anil", "anil", 200);
    }

    /**
     * <p>
     * Test with user "marcus" who has the right password but does not have the right role
     * </p>
     * <p>
     * Should be a HTTP/403
     * </p>
     *
     * @throws Exception
     */
    @Test
    public void testPasswordBasedUnsuccessfulAuth() throws Exception {
        makeCall("marcus", "marcus", 403);
    }

    /**
     * Method that needs to be overridden with the HTTPClient code
     *
     * @param user username
     * @param pass password
     * @param expectedCode http status code
     * @throws Exception
     */
    protected abstract void makeCall(String user, String pass, int expectedCode) throws Exception;

}
