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

package org.jboss.as.controller.security;

import java.security.Principal;
import java.util.Collection;

import javax.security.auth.Subject;

/**
 * A UserInfo definition that also allows for a Subject to be returned.
 *
 * This interface contains a method from the Remoting UserInfo definition, however
 * domain management is both about Remoting and non-Remoting invocations so we do not
 * tie this directly to the Remoting class.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public interface SubjectUserInfo {

    // TODO - This is currently within this module as it needs to be widely accessible - the domain-management
    //        module currently depends on controller so can't see this class.  We really need to split the domain
    //        management into two pieces - 1 for the management operations and 2 the core library handling security
    //        the latter should have no other dependencies within the AS tree.

    /**
     * Get the name for this user.
     *
     * @return the name
     */
    String getUserName();

    /**
     * Get the principals for this user.
     *
     * @return the principals
     */
    Collection<Principal> getPrincipals();

    Subject getSubject();

}
