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
package org.jboss.as.domain.controller.plan;

import static java.security.AccessController.doPrivileged;
import static java.security.AccessController.getContext;

import java.security.AccessControlContext;
import java.security.PrivilegedAction;

import javax.security.auth.Subject;

import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Security actions for the 'org.jboss.as.domain.controller.plan' package.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
class SecurityActions {

    private SecurityActions() {
    }

    static Subject getCurrentSubject() {
        AccessControlContext acc = getContext();

        return subjectActions().getCurrentSubject(acc);
    }

    static SubjectActions subjectActions() {
        return WildFlySecurityManager.isChecking() ? SubjectActions.PRIVILEGED : SubjectActions.NON_PRIVILEGED;
    }

    private interface SubjectActions {

        Subject getCurrentSubject(AccessControlContext acc);

        SubjectActions NON_PRIVILEGED = new SubjectActions() {

            @Override
            public Subject getCurrentSubject(AccessControlContext acc) {
                return Subject.getSubject(acc);
            }
        };

        SubjectActions PRIVILEGED = new SubjectActions() {

            @Override
            public Subject getCurrentSubject(final AccessControlContext acc) {
                return doPrivileged(new PrivilegedAction<Subject>() {

                    @Override
                    public Subject run() {
                        return NON_PRIVILEGED.getCurrentSubject(acc);
                    }
                });
            }
        };

    }

}
