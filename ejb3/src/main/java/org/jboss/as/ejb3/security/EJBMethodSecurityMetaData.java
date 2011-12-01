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

import java.util.Collections;
import java.util.Set;

/**
 * Holds security metadata of a method corresponding to a EJB's view.
 * <p/>
 * For security metadata that's applicable at EJB component level (for ex: security domain) take a look at {@link EJBSecurityMetaData}
 * <p/>
 * User: Jaikiran Pai
 */
public class EJBMethodSecurityMetaData {


    public static final EJBMethodSecurityMetaData PERMIT_ALL = new EJBMethodSecurityMetaData(true, false, Collections.<String>emptySet());
    public static final EJBMethodSecurityMetaData DENY_ALL = new EJBMethodSecurityMetaData(false, true, Collections.<String>emptySet());
    public static final EJBMethodSecurityMetaData NONE = new EJBMethodSecurityMetaData(false, false, Collections.<String>emptySet());
    private final boolean permitAll;
    private final boolean denyAll;
    private final Set<String> rolesAllowed;

    private EJBMethodSecurityMetaData(final boolean permitAll, final boolean denyAll, final Set<String> rolesAllowed) {
        this.permitAll = permitAll;
        this.denyAll = denyAll;
        this.rolesAllowed = rolesAllowed;
    }

    public static EJBMethodSecurityMetaData none() {
        return NONE;
    }

    public static EJBMethodSecurityMetaData permitAll() {
        return PERMIT_ALL;
    }

    public static EJBMethodSecurityMetaData denyAll() {
        return DENY_ALL;
    }

    public static EJBMethodSecurityMetaData rolesAllowed(final Set<String> roles) {
        return new EJBMethodSecurityMetaData(false, false, roles);
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
