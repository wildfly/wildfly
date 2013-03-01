/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.util.security;

import java.security.AccessController;
import java.security.Principal;

import javax.security.auth.Subject;

/**
 * General-purpose utility methods relating to {@link Subject}s.  These methods are not privileged, thus the
 * caller must have the relevant permissions.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class SubjectUtils {
    /**
     * Get the current subject of the executing thread.
     *
     * @return the current subject of the executing thread
     */
    public static Subject getCurrent() {
        return Subject.getSubject(AccessController.getContext());
    }

    /**
     * Get the principal of the given type.  If multiple principals of the given type exist, only
     * the first is returned.
     *
     * @return the principal of the given type, or {@code null} if none was found
     */
    public static <P extends Principal> P getPrincipal(Class<P> type, Subject subject) {
        for (Principal principal : subject.getPrincipals()) {
            if (type.isInstance(principal)) {
                return type.cast(principal);
            }
        }
        return null;
    }

    /**
     * Get the public credential of the given type.  If multiple credentials of the given type exist, only
     * the first is returned.
     *
     * @return the public credential of the given type, or {@code null} if none was found
     */
    public static <C> C getPublicCredential(Class<C> type, Subject subject) {
        for (Object cred : subject.getPublicCredentials()) {
            if (type.isInstance(cred)) {
                return type.cast(cred);
            }
        }
        return null;
    }

    /**
     * Get the private credential of the given type.  If multiple credentials of the given type exist, only
     * the first is returned.
     *
     * @return the private credential of the given type, or {@code null} if none was found
     */
    public static <C> C getPrivateCredential(Class<C> type, Subject subject) {
        for (Object cred : subject.getPrivateCredentials()) {
            if (type.isInstance(cred)) {
                return type.cast(cred);
            }
        }
        return null;
    }
}
