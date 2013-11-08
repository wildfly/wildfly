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

package org.jboss.as.controller;

import static java.security.AccessController.doPrivileged;

import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.security.auth.Subject;

import org.jboss.as.controller.access.Caller;

/**
 * Security actions to perform possibly privileged operations.  no methods in
 * this class are to be made public under any circumstances!
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
class SecurityActions {

    static String getSystemProperty(final String key) {
        if (System.getSecurityManager() == null) {
            return System.getProperty(key);
        }

        return AccessController.doPrivileged(new PrivilegedAction<String>() {

            @Override
            public String run() {
                return System.getProperty(key);
            }
        });
    }

    static String getSystemProperty(final String key, final String defaultValue) {
        if (System.getSecurityManager() == null) {
            return System.getProperty(key, defaultValue);
        }

        return AccessController.doPrivileged(new PrivilegedAction<String>() {

            @Override
            public String run() {
                return System.getProperty(key, defaultValue);
            }
        });
    }

    static ClassLoader setThreadContextClassLoader(Class cl) {
        if (System.getSecurityManager() == null) {
            return SetThreadContextClassLoaderAction.NON_PRIVILEGED.setThreadContextClassLoader(cl);
        } else {
            return SetThreadContextClassLoaderAction.PRIVILEGED.setThreadContextClassLoader(cl);
        }
    }

    static void setThreadContextClassLoader(ClassLoader cl) {
        if (System.getSecurityManager() == null) {
            SetThreadContextClassLoaderAction.NON_PRIVILEGED.setThreadContextClassLoader(cl);
        } else {
            SetThreadContextClassLoaderAction.PRIVILEGED.setThreadContextClassLoader(cl);
        }
    }

    private interface SetThreadContextClassLoaderAction {

        ClassLoader setThreadContextClassLoader(Class cl);

        void setThreadContextClassLoader(ClassLoader cl);

        SetThreadContextClassLoaderAction NON_PRIVILEGED = new SetThreadContextClassLoaderAction() {
            @Override
            public ClassLoader setThreadContextClassLoader(Class cl) {
                ClassLoader old = Thread.currentThread().getContextClassLoader();
                Thread.currentThread().setContextClassLoader(cl.getClassLoader());
                return old;
            }
            @Override
            public void setThreadContextClassLoader(ClassLoader cl) {
                Thread.currentThread().setContextClassLoader(cl);
            }
        };

        SetThreadContextClassLoaderAction PRIVILEGED = new SetThreadContextClassLoaderAction() {

            @Override
            public ClassLoader setThreadContextClassLoader(final Class cl) {
                return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
                    public ClassLoader run() {
                        ClassLoader old = Thread.currentThread().getContextClassLoader();
                        Thread.currentThread().setContextClassLoader(cl.getClassLoader());
                        return old;
                    }
                });
            }

            @Override
            public void setThreadContextClassLoader(final ClassLoader cl) {
                AccessController.doPrivileged(new PrivilegedAction<Void>() {
                    public Void run() {
                        Thread.currentThread().setContextClassLoader(cl);
                        return null;
                    }
                });
            }
        };
    }

    static Caller getCaller(final Caller currentCaller) {
        return createCallerActions().getCaller(currentCaller);
    }

    static AccessAuditContext currentAccessAuditContext() {
        return createAccessAuditContextActions().currentContext();
    }

    private static AccessAuditContextActions createAccessAuditContextActions() {
        return System.getSecurityManager() != null ? AccessAuditContextActions.PRIVILEGED : AccessAuditContextActions.NON_PRIVILEGED;
    }

    private static CreateCallerActions createCallerActions() {
        return System.getSecurityManager() != null ? CreateCallerActions.PRIVILEGED : CreateCallerActions.NON_PRIVILEGED;
    }

    private interface AccessAuditContextActions {

        AccessAuditContext currentContext();

        AccessAuditContextActions NON_PRIVILEGED = new AccessAuditContextActions() {

            @Override
            public AccessAuditContext currentContext() {
                return AccessAuditContext.currentAccessAuditContext();
            }
        };

        AccessAuditContextActions PRIVILEGED = new AccessAuditContextActions() {

            private final PrivilegedAction<AccessAuditContext> PRIVILEGED_ACTION = new PrivilegedAction<AccessAuditContext>() {

                @Override
                public AccessAuditContext run() {
                    return NON_PRIVILEGED.currentContext();
                }

            };

            @Override
            public AccessAuditContext currentContext() {
                return doPrivileged(PRIVILEGED_ACTION);
            }
        };

    }

    private interface CreateCallerActions {

        Caller getCaller(Caller currentCaller);

        CreateCallerActions NON_PRIVILEGED = new CreateCallerActions() {

            @Override
            public Caller getCaller(Caller currentCaller) {
                AccessControlContext acc = AccessController.getContext();
                Subject subject = Subject.getSubject(acc);
                // This is deliberately checking the Subject is the exact same instance.
                if (currentCaller == null || subject != currentCaller.getSubject()) {
                    if (subject != null) {
                        subject.setReadOnly();
                    }
                    return Caller.createCaller(subject);
                }

                return currentCaller;
            }
        };

        CreateCallerActions PRIVILEGED = new CreateCallerActions() {

            @Override
            public Caller getCaller(final Caller currentCaller) {
                return doPrivileged(new PrivilegedAction<Caller>() {

                    @Override
                    public Caller run() {
                        return NON_PRIVILEGED.getCaller(currentCaller);
                    }
                });
            }
        };
    }
}
