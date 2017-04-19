/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.wildfly.test.integration.elytron.ejb;

import javax.ejb.Local;

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
