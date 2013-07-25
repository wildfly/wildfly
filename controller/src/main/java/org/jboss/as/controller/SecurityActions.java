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

package org.jboss.as.controller;

import static java.security.AccessController.doPrivileged;

import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.security.auth.Subject;

import org.jboss.as.controller.access.Caller;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Security actions for the 'org.jboss.as.controller' package.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
class SecurityActions {

    private SecurityActions() {
    }

    static Caller createCaller() {
        return createCallerActions().createCaller();
    }

    private static CreateCallerActions createCallerActions() {
        return WildFlySecurityManager.isChecking() ? CreateCallerActions.PRIVILEGED : CreateCallerActions.NON_PRIVILEGED;
    }

    private interface CreateCallerActions {

        Caller createCaller();

        CreateCallerActions NON_PRIVILEGED = new CreateCallerActions() {

            @Override
            public Caller createCaller() {
                AccessControlContext acc = AccessController.getContext();
                Subject subject = Subject.getSubject(acc);
                if (subject != null) {
                    subject.setReadOnly();
                }

                return Caller.createCaller(subject);
            }
        };

        CreateCallerActions PRIVILEGED = new CreateCallerActions() {

            PrivilegedAction<Caller> ACTION = new PrivilegedAction<Caller>() {

                @Override
                public Caller run() {
                    return NON_PRIVILEGED.createCaller();
                }
            };

            @Override
            public Caller createCaller() {
                return doPrivileged(ACTION);
            }
        };

    }
}
