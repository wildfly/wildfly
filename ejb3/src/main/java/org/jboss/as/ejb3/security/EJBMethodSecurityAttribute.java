/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.security;

import org.jboss.as.ejb3.logging.EjbLogger;

import java.util.Collections;
import java.util.Set;

/**
 * Holds security metadata of a method corresponding to a Jakarta Enterprise Beans bean's view.
 * <p/>
 * For security metadata that's applicable at Jakarta Enterprise Beans component level (for ex: security domain) take a look at {@link EJBSecurityMetaData}
 *
 * @author Jaikiran Pai
 */
public class EJBMethodSecurityAttribute {


    public static final EJBMethodSecurityAttribute PERMIT_ALL = new EJBMethodSecurityAttribute(true, false, Collections.<String>emptySet());
    public static final EJBMethodSecurityAttribute DENY_ALL = new EJBMethodSecurityAttribute(false, true, Collections.<String>emptySet());
    public static final EJBMethodSecurityAttribute NONE = new EJBMethodSecurityAttribute(false, false, Collections.<String>emptySet());
    private final boolean permitAll;
    private final boolean denyAll;
    private final Set<String> rolesAllowed;

    private EJBMethodSecurityAttribute(final boolean permitAll, final boolean denyAll, final Set<String> rolesAllowed) {
        if (rolesAllowed == null)
            throw EjbLogger.ROOT_LOGGER.paramCannotBeNull("rolesAllowed");
        this.permitAll = permitAll;
        this.denyAll = denyAll;
        this.rolesAllowed = rolesAllowed;
    }

    public static EJBMethodSecurityAttribute none() {
        return NONE;
    }

    public static EJBMethodSecurityAttribute permitAll() {
        return PERMIT_ALL;
    }

    public static EJBMethodSecurityAttribute denyAll() {
        return DENY_ALL;
    }

    public static EJBMethodSecurityAttribute rolesAllowed(final Set<String> roles) {
        return new EJBMethodSecurityAttribute(false, false, roles);
    }

    public boolean isPermitAll() {
        return permitAll;
    }

    public boolean isDenyAll() {
        return denyAll;
    }

    public Set<String> getRolesAllowed() {
        return rolesAllowed;
    }

}
