/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.manual.elytron.seccontext;

import java.security.Principal;
import jakarta.ejb.Remote;

@Remote
public interface WhoAmI {

    /**
     * @return the caller principal obtained from the EJBContext.
     */
    Principal getCallerPrincipal();

    /**
     * Throws IllegalStateException.
     */
    String throwIllegalStateException();

    /**
     * Throws Server2Exception.
     */
    String throwServer2Exception();
}
