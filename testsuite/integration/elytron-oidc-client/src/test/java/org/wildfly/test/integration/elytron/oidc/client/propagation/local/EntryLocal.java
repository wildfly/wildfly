/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.elytron.oidc.client.propagation.local;

import jakarta.ejb.Local;

/**
 * Interface for the bean used as the entry point to verify Enterprise Beans 3 security behaviour.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
@Local
public interface EntryLocal {

    /**
     * @return The name of the Principal obtained from a call to EJBContext.getCallerPrincipal()
     */
    String whoAmI();

    /**
     * @param roleName - The role to check.
     * @return the response from EJBContext.isCallerInRole() with the supplied role name.
     */
    boolean doIHaveRole(String roleName);

}
