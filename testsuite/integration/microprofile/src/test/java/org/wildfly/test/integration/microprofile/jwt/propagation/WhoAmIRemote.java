/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.jwt.propagation;

import jakarta.ejb.Remote;

/**
 * The interface to the simple WhoAmI bean.
 *
 * @author <a href="mailto:fjuma@redhat.com">Farah Juma</a>
 */
@Remote
public interface WhoAmIRemote {

    /**
     * @return the caller principal obtained from the EJBContext.
     */
    String getCallerPrincipal();

    /**
     * @param roleName - The role to check.
     * @return The result of calling EJBContext.isCallerInRole() with the supplied role name.
     */
    boolean isCallerInRole(String roleName);

    public String getSessionContext(); // rls debug
}