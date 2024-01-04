/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.elytron.ejb.propagation.remote;

import java.security.Principal;
import jakarta.ejb.Remote;

/**
 * The local interface to the simple WhoAmI bean.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
@Remote
public interface WhoAmIRemote {

    /**
     * @return the caller principal obtained from the EJBContext.
     */
    Principal getCallerPrincipal();

    /**
     * @param roleName - The role to check.
     * @return The result of calling EJBContext.isCallerInRole() with the supplied role name.
     */
    boolean doIHaveRole(String roleName);

}
