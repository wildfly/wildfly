/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.cmp.jdbc.bridge;

import java.lang.reflect.UndeclaredThrowableException;
import java.security.AccessController;
import java.security.Principal;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import javax.security.auth.Subject;
import javax.security.jacc.PolicyContext;
import javax.security.jacc.PolicyContextException;
import static org.jboss.as.cmp.CmpMessages.MESSAGES;
import org.jboss.security.RunAs;
import org.jboss.security.SecurityContext;
import org.jboss.security.SecurityContextAssociation;
import org.jboss.security.SecurityContextFactory;


/**
 * A collection of privileged actions for this package
 *
 * @author Scott.Stark@jboss.org
 * @author <a href="mailto:alex@jboss.org">Alexey Loubyansky</a>
 * @author Anil.Saldhana@redhat.com
 * @version $Revison: $
 */
class SecurityActions {

    interface RunAsIdentityActions {
        RunAsIdentityActions PRIVILEGED = new RunAsIdentityActions() {
            private final PrivilegedAction peekAction = new PrivilegedAction() {
                public Object run() {
                    //return SecurityAssociation.peekRunAsIdentity();
                    SecurityContext sc = SecurityContextAssociation.getSecurityContext();
                    if (sc == null)
                        throw MESSAGES.securityContextIsNull();
                    return sc.getOutgoingRunAs();
                }
            };

            private final PrivilegedAction popAction = new PrivilegedAction() {
                public Object run() {
                    //return SecurityAssociation.popRunAsIdentity();
                    SecurityContext sc = SecurityContextAssociation.getSecurityContext();
                    if (sc == null)
                        throw MESSAGES.securityContextIsNull();
                    RunAs ra = sc.getOutgoingRunAs();
                    sc.setOutgoingRunAs(null);
                    return ra;
                }
            };

            public RunAs peek() {
                return (RunAs) AccessController.doPrivileged(peekAction);
            }

            public void push(final RunAs id) {
                AccessController.doPrivileged(
                        new PrivilegedAction() {
                            public Object run() {
                                //SecurityAssociation.pushRunAsIdentity((RunAsIdentity)id);
                                SecurityContext sa = SecurityContextAssociation.getSecurityContext();
                                if (sa == null)
                                    throw MESSAGES.securityContextIsNull();
                                sa.setOutgoingRunAs(id);
                                return null;
                            }
                        }
                );
            }

            public RunAs pop() {
                return (RunAs) AccessController.doPrivileged(popAction);
            }
        };

        RunAsIdentityActions NON_PRIVILEGED = new RunAsIdentityActions() {
            public RunAs peek() {
                //return SecurityAssociation.peekRunAsIdentity();
                SecurityContext sc = SecurityContextAssociation.getSecurityContext();
                if (sc == null)
                    throw MESSAGES.securityContextIsNull();
                return sc.getOutgoingRunAs();
            }

            public void push(RunAs id) {
                //SecurityAssociation.pushRunAsIdentity(id);
                SecurityContext sc = SecurityContextAssociation.getSecurityContext();
                if (sc == null)
                    throw MESSAGES.securityContextIsNull();
                sc.setOutgoingRunAs(id);
            }

            public RunAs pop() {
                //Pop the RAI
                // return SecurityAssociation.popRunAsIdentity();
                SecurityContext sc = SecurityContextAssociation.getSecurityContext();
                if (sc == null)
                    throw MESSAGES.securityContextIsNull();
                RunAs ra = null;
                ra = sc.getOutgoingRunAs();
                sc.setOutgoingRunAs(null);
                return ra;
            }
        };

        RunAs peek();

        void push(RunAs id);

        RunAs pop();
    }


    interface PolicyContextActions {
        /**
         * The JACC PolicyContext key for the current Subject
         */
        String SUBJECT_CONTEXT_KEY = "javax.security.auth.Subject.container";
        PolicyContextActions PRIVILEGED = new PolicyContextActions() {
            private final PrivilegedExceptionAction exAction = new PrivilegedExceptionAction() {
                public Object run() throws Exception {
                    return (Subject) PolicyContext.getContext(SUBJECT_CONTEXT_KEY);
                }
            };

            public Subject getContextSubject()
                    throws PolicyContextException {
                try {
                    return (Subject) AccessController.doPrivileged(exAction);
                } catch (PrivilegedActionException e) {
                    Exception ex = e.getException();
                    if (ex instanceof PolicyContextException)
                        throw (PolicyContextException) ex;
                    else
                        throw new UndeclaredThrowableException(e);
                }
            }
        };

        PolicyContextActions NON_PRIVILEGED = new PolicyContextActions() {
            public Subject getContextSubject()
                    throws PolicyContextException {
                return (Subject) PolicyContext.getContext(SUBJECT_CONTEXT_KEY);
            }
        };

        Subject getContextSubject()
                throws PolicyContextException;
    }

    static ClassLoader getContextClassLoader() {
        return TCLAction.UTIL.getContextClassLoader();
    }

    static void setContextClassLoader(ClassLoader loader) {
        TCLAction.UTIL.setContextClassLoader(loader);
    }


    static RunAs peekRunAsIdentity() {
        if (System.getSecurityManager() == null) {
            return RunAsIdentityActions.NON_PRIVILEGED.peek();
        } else {
            return RunAsIdentityActions.PRIVILEGED.peek();
        }
    }

    static void pushRunAsIdentity(RunAs principal) {
        if (System.getSecurityManager() == null) {
            RunAsIdentityActions.NON_PRIVILEGED.push(principal);
        } else {
            RunAsIdentityActions.PRIVILEGED.push(principal);
        }
    }

    static RunAs popRunAsIdentity() {
        if (System.getSecurityManager() == null) {
            return RunAsIdentityActions.NON_PRIVILEGED.pop();
        } else {
            return RunAsIdentityActions.PRIVILEGED.pop();
        }
    }


    static Subject getContextSubject()
            throws PolicyContextException {
        if (System.getSecurityManager() == null) {
            return PolicyContextActions.NON_PRIVILEGED.getContextSubject();
        } else {
            return PolicyContextActions.PRIVILEGED.getContextSubject();
        }
    }

    interface TCLAction {
        class UTIL {
            static TCLAction getTCLAction() {
                return System.getSecurityManager() == null ? NON_PRIVILEGED : PRIVILEGED;
            }

            static ClassLoader getContextClassLoader() {
                return getTCLAction().getContextClassLoader();
            }

            static ClassLoader getContextClassLoader(Thread thread) {
                return getTCLAction().getContextClassLoader(thread);
            }

            static void setContextClassLoader(ClassLoader cl) {
                getTCLAction().setContextClassLoader(cl);
            }

            static void setContextClassLoader(Thread thread, ClassLoader cl) {
                getTCLAction().setContextClassLoader(thread, cl);
            }
        }

        TCLAction NON_PRIVILEGED = new TCLAction() {
            public ClassLoader getContextClassLoader() {
                return Thread.currentThread().getContextClassLoader();
            }

            public ClassLoader getContextClassLoader(Thread thread) {
                return thread.getContextClassLoader();
            }

            public void setContextClassLoader(ClassLoader cl) {
                Thread.currentThread().setContextClassLoader(cl);
            }

            public void setContextClassLoader(Thread thread, ClassLoader cl) {
                thread.setContextClassLoader(cl);
            }
        };

        TCLAction PRIVILEGED = new TCLAction() {
            private final PrivilegedAction getTCLPrivilegedAction = new PrivilegedAction() {
                public Object run() {
                    return Thread.currentThread().getContextClassLoader();
                }
            };

            public ClassLoader getContextClassLoader() {
                return (ClassLoader) AccessController.doPrivileged(getTCLPrivilegedAction);
            }

            public ClassLoader getContextClassLoader(final Thread thread) {
                return (ClassLoader) AccessController.doPrivileged(new PrivilegedAction() {
                    public Object run() {
                        return thread.getContextClassLoader();
                    }
                });
            }

            public void setContextClassLoader(final ClassLoader cl) {
                AccessController.doPrivileged(
                        new PrivilegedAction() {
                            public Object run() {
                                Thread.currentThread().setContextClassLoader(cl);
                                return null;
                            }
                        }
                );
            }

            public void setContextClassLoader(final Thread thread, final ClassLoader cl) {
                AccessController.doPrivileged(
                        new PrivilegedAction() {
                            public Object run() {
                                thread.setContextClassLoader(cl);
                                return null;
                            }
                        }
                );
            }
        };

        ClassLoader getContextClassLoader();

        ClassLoader getContextClassLoader(Thread thread);

        void setContextClassLoader(ClassLoader cl);

        void setContextClassLoader(Thread thread, ClassLoader cl);
    }


    static void createAndSetSecurityContext(final Principal p, final Object cred, final String domain)
            throws PrivilegedActionException {
        AccessController.doPrivileged(new PrivilegedExceptionAction() {

            public Object run() throws Exception {
                SecurityContext sc = SecurityContextFactory.createSecurityContext(p, cred, null, domain);
                SecurityContextAssociation.setSecurityContext(sc);
                return null;
            }
        });
    }

    static void createAndSetSecurityContext(final Principal p, final Object cred, final String domain,
                                            final Subject subject) throws PrivilegedActionException {
        AccessController.doPrivileged(new PrivilegedExceptionAction() {

            public Object run() throws Exception {
                SecurityContext sc = SecurityContextFactory.createSecurityContext(domain);
                sc.getUtil().createSubjectInfo(p, cred, subject);
                SecurityContextAssociation.setSecurityContext(sc);
                return null;
            }
        });
    }

    static void clearSecurityContext() {
        AccessController.doPrivileged(new PrivilegedAction() {

            public Object run() {
                SecurityContextAssociation.setSecurityContext(null);
                return null;
            }
        });
    }

    static SecurityContext getSecurityContext() {
        return (SecurityContext) AccessController.doPrivileged(new PrivilegedAction() {
            public Object run() {
                return SecurityContextAssociation.getSecurityContext();
            }
        });
    }

    static Exception getContextException() {
        return (Exception) AccessController.doPrivileged(new PrivilegedAction() {
            static final String EX_KEY = "org.jboss.security.exception";

            public Object run() {
                SecurityContext sc = getSecurityContext();
                return sc.getData().get(EX_KEY);
            }
        });
    }

    static void pushSubjectContext(final Principal p, final Object cred, final Subject s) {
        AccessController.doPrivileged(new PrivilegedAction() {

            public Object run() {
                SecurityContext sc = getSecurityContext();
                if (sc == null)
                    throw MESSAGES.securityContextIsNull();
                sc.getUtil().createSubjectInfo(p, cred, s);
                return null;
            }
        }
        );
    }

    static void popSubjectContext() {
        AccessController.doPrivileged(new PrivilegedAction() {

            public Object run() {
                SecurityContext sc = getSecurityContext();
                if (sc == null)
                    throw MESSAGES.securityContextIsNull();
                Principal p = sc.getUtil().getUserPrincipal();
                Object cred = sc.getUtil().getCredential();
                sc.getUtil().createSubjectInfo(p, cred, null);
                return null;
            }
        }
        );
    }

    static void pushCallerRunAsIdentity(final RunAs ra) {
        AccessController.doPrivileged(new PrivilegedAction() {
            public Object run() {
                SecurityContext sc = SecurityContextAssociation.getSecurityContext();
                if (sc == null)
                    throw MESSAGES.securityContextIsNull();
                sc.setIncomingRunAs(ra);
                return null;
            }
        });
    }

    static void popCallerRunAsIdentity() {
        AccessController.doPrivileged(new PrivilegedAction() {
            public Object run() {
                SecurityContext sc = SecurityContextAssociation.getSecurityContext();
                if (sc == null)
                    throw MESSAGES.securityContextIsNull();
                sc.setIncomingRunAs(null);
                return null;
            }
        });
    }
}
