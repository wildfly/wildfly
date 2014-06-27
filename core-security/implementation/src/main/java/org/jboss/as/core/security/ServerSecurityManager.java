/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.core.security;

import java.lang.reflect.Method;
import java.security.CodeSource;
import java.security.Principal;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;

/**
 * Interface to the servers security manager implementation.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public interface ServerSecurityManager {

    void push(final String securityDomain);
    void push(final String securityDomain, String userName, char[] password, final Subject subject);

    void authenticate();
    void authenticate(final String runAs, final String runAsPrincipal, final Set<String> extraRoles);

    void pop();

    Principal getCallerPrincipal();

    Subject getSubject();

    //TODO: we have no internal users of this, find out if it is used downstream
    @Deprecated
    boolean isCallerInRole(final String ejbName, final Object mappedRoles, final Map<String, Collection<String>> roleLinks,
            final String... roleNames);

    boolean isCallerInRole(final String ejbName, String policyContextId, final Object mappedRoles, final Map<String, Collection<String>> roleLinks,
            final String... roleNames);

    boolean authorize(String ejbName, CodeSource ejbCodeSource, String ejbMethodIntf, Method ejbMethod, Set<Principal> methodRoles, String contextID);

}
