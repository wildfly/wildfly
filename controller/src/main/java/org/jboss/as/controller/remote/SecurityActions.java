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

package org.jboss.as.controller.remote;

import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.security.auth.Subject;

import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Security actions for the 'org.jboss.as.controller.remote' package.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
class SecurityActions {

    static Subject getSubject() {
        AccessControlContext acc = AccessController.getContext();
        return getSubjectAction().getSubject(acc);
    }

    private static GetSubjectAction getSubjectAction() {
        return WildFlySecurityManager.isChecking() ? GetSubjectAction.PRIVILEGED : GetSubjectAction.NON_PRIVILEGED;
    }

    private interface GetSubjectAction {
        Subject getSubject(AccessControlContext acc);

        GetSubjectAction NON_PRIVILEGED = new GetSubjectAction() {

            @Override
            public Subject getSubject(AccessControlContext acc) {
                return Subject.getSubject(acc);
            }
        };

        GetSubjectAction PRIVILEGED = new GetSubjectAction() {

            @Override
            public Subject getSubject(final AccessControlContext acc) {
                return AccessController.doPrivileged(new PrivilegedAction<Subject>() {

                    @Override
                    public Subject run() {
                        return NON_PRIVILEGED.getSubject(acc);
                    }
                });
            }
        };

    }

}
