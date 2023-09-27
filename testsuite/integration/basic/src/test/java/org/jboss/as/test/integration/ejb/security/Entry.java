/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.security;

import jakarta.ejb.Local;

/**
 * Interface for the bean used as the entry point to verify EJB3 security behaviour.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
@Local
public interface Entry {

    /**
     * @return The name of the Principal obtained from a call to EJBContext.getCallerPrincipal()
     */
    String whoAmI();

    /**
     * Obtains the name of the Principal obtained from a call to EJBContext.getCallerPrincipal()
     * both for the bean called and also from a call to a second bean.
     *
     * @return An array containing the name from the local call first followed by the name from
     * the second call.
     */
    String[] doubleWhoAmI();

    /**
     * As doubleWhoAmI except the user is switched before the second call.
     *
     * @see this.doubleWhoAmI()
     * @param username - The username to use for the second call.
     * @param password - The password to use for the second call.
     * @return An array containing the name from the local call first followed by the name from
     * the second call.
     * @throws Exception - If there is an unexpected failure establishing the security context for
     * the second call.
     */
    String[] doubleWhoAmI(String username, String password) throws Exception;

    /**
     * @param roleName - The role to check.
     * @return the response from EJBContext.isCallerInRole() with the supplied role name.
     */
    boolean doIHaveRole(String roleName);

    /**
     * Calls EJBContext.isCallerInRole() with the supplied role name and then calls a second bean
     * which makes the same call.
     *
     * @param roleName - the role name to check.
     * @return the values from the isCallerInRole() calls, the EntryBean is first and the second bean
     * second.
     */
    boolean[] doubleDoIHaveRole(String roleName);

    /**
     * As doubleDoIHaveRole except the user is switched before the second call.
     *
     * @see this.doubleDoIHaveRole(String)
     * @param roleName - The role to check.
     * @param username - The username to use for the second call.
     * @param password - The password to use for the second call.
     * @return @return the values from the isCallerInRole() calls, the EntryBean is first and the second bean
     * second.
     * @throws Exception - If their is an unexpected failure.
     */
    boolean[] doubleDoIHaveRole(String roleName, String username, String password) throws Exception;

}
