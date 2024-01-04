/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.elytron.securityapi;

import java.security.Principal;

import jakarta.ejb.Local;

/**
 * The local interface to the simple WhoAmI bean.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
@Local
public interface WhoAmI {

    /**
     * @return the caller principal obtained from the SessionContext.
     */
    Principal getCallerPrincipalSessionContext();

    /**
     * @return the caller principal obtained from the SecurityDomain.
     */
    Principal getCallerPrincipalSecurityDomain();

    /**
     * @return the caller principal obtained from the SecurityContext.
     */
    Principal getCallerPrincipalSecurityContext();

}
