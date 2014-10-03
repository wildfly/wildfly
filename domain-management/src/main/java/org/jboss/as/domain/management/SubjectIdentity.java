/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.domain.management;

import javax.security.auth.Subject;

/**
 * Obtained from a {@link SecurityRealm} to provide a {@link Subject} representing the servers identity.
 *
 * This identity is returned instead of just the {@link Subject} as it adds the ability to logout so that the {@link Subject}
 * can be cleaned up.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public interface SubjectIdentity {

    /**
     * Get the {@link Subject} instance wrapped by this {@code SubjectIdentity}.
     *
     * This method will always return the same instance of {@link Subject} and will never return {@code null}.
     *
     * @return The associated {@link Subject}
     * @throws IllegalStateException If called after {@link SubjectIdentity#logout()}
     */
    Subject getSubject();

    /**
     * Clean up this instance and logout any identity associated with the {@link Subject}.
     *
     * After this method is called the instance of this SubjectIdentity should be discarded as it will no longer be useable to
     * obtain a {@link Subject} instance.
     */
    void logout();

}
