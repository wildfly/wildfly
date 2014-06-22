/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.security;

import org.jboss.as.ejb3.logging.EjbLogger;

import java.util.Collections;
import java.util.Set;

/**
 * Holds security metadata of a method corresponding to an EJB's view.
 * <p/>
 * For security metadata that's applicable at EJB component level (for ex: security domain) take a look at {@link EJBSecurityMetaData}
 * <p/>
 * User: Jaikiran Pai
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
