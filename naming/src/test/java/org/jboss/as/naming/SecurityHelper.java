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

package org.jboss.as.naming;

import static org.junit.Assert.fail;

import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.CodeSource;
import java.security.Permission;
import java.security.Permissions;
import java.security.Policy;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Callable;
import javax.naming.CompositeName;
import javax.naming.Name;
import javax.naming.NamingException;

import org.wildfly.naming.java.permission.JndiPermission;

/**
 * @author Lukas Krejci
 */
public class SecurityHelper {

    private SecurityHelper() {

    }

    public static Object testActionPermission(final int action, final NamingContext namingContext,
                                              final String name, final Object... params) throws Exception {

        return testActionPermission(action, Collections.<JndiPermission>emptyList(), namingContext, name, params);
    }

    public static Object testActionPermission(final int action,
                                              final Collection<JndiPermission> additionalRequiredPerms, final NamingContext namingContext, final String name,
                                              final Object... params) throws Exception {

        Exception positiveTestCaseException = null;

        try {
            //positive test case
            return testActionWithPermission(action, additionalRequiredPerms, namingContext, name, params);
        } catch (Exception e) {
            positiveTestCaseException = e;
            //this is just to satisfy the compiler... the finally clause should always throw an exception in this case
            return null;
        } finally {
            //negative test case
            try {
                testActionWithoutPermission(action, additionalRequiredPerms, namingContext, name, params);
            } catch (Exception e) {
                if (positiveTestCaseException == null) {
                    throw e;
                } else {
                    throw new Exception("Both positive and negative permission test for JNDI action "
                            + action
                            + " failed. The negative test case (which should have resulted in a security exception)"
                            + " failed with a message: "
                            + "("
                            + e.getClass().getName()
                            + "): "
                            + e.getMessage()
                            + ". The exception of the positive testcase"
                            + " is set up as the cause of this exception.", positiveTestCaseException);
                }
            }

            if (positiveTestCaseException != null) {
                throw positiveTestCaseException;
            }
        }

    }

    public static Object testActionWithPermission(final int action,
                                                  final Collection<JndiPermission> additionalRequiredPerms, final NamingContext namingContext, final String name,
                                                  final Object... params) throws Exception {

        final CompositeName n = name == null ? new CompositeName() : new CompositeName(name);
        final String sn = name == null ? "" : name;

        ArrayList<JndiPermission> allPerms = new ArrayList<JndiPermission>(additionalRequiredPerms);
        allPerms.add(new JndiPermission(sn, action));

        return runWithSecurityManager(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return performAction(action, namingContext, n, params);
            }
        }, getSecurityContextForJNDILookup(allPerms));
    }

    public static void testActionWithoutPermission(final int action,
                                                   final Collection<JndiPermission> additionalRequiredPerms, final NamingContext namingContext, final String name,
                                                   final Object... params) throws Exception {

        final CompositeName n = name == null ? new CompositeName() : new CompositeName(name);
        final String sn = name == null ? "" : name;

        ArrayList<JndiPermission> allPerms = new ArrayList<JndiPermission>(additionalRequiredPerms);
        allPerms.add(new JndiPermission(sn, not(action)));

        try {
            runWithSecurityManager(new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    return performAction(action, namingContext, n, params);
                }
            }, getSecurityContextForJNDILookup(allPerms));

            fail("Naming operation " + action + " should not have been permitted");
        } catch (SecurityException e) {
            //expected
        }
    }

    private static int not(int action) {
        return ~action & JndiPermission.ACTION_ALL;
    }

    private static Object performAction(int action, NamingContext namingContext, Name name,
                                        Object... params) throws NamingException {
        switch (action) {
        case JndiPermission.ACTION_BIND:
            if (params.length == 1) {
                namingContext.bind(name, params[0]);
            } else {
                throw new IllegalArgumentException("Invalid number of arguments passed to bind()");
            }
            return null;
        case JndiPermission.ACTION_CREATE_SUBCONTEXT:
            return namingContext.createSubcontext(name);
        case JndiPermission.ACTION_LIST:
            return namingContext.list(name);
        case JndiPermission.ACTION_LIST_BINDINGS:
            return namingContext.listBindings(name);
        case JndiPermission.ACTION_LOOKUP:
            if (params.length == 0) {
                return namingContext.lookup(name);
            } else if (params.length == 1) {
                return namingContext.lookup(name, (Boolean) params[0]);
            } else {
                throw new IllegalArgumentException("Invalid number of arguments passed to lookup()");
            }
        case JndiPermission.ACTION_REBIND:
            if (params.length == 1) {
                namingContext.rebind(name, params[0]);
            } else {
                throw new IllegalArgumentException("Invalid number of arguments passed to rebind()");
            }
            return null;
        case JndiPermission.ACTION_UNBIND:
            namingContext.unbind(name);
            return null;
        default:
            throw new IllegalArgumentException("Action "
                + action
                + " not supported by the generic testActionPermission test");
        }
    }

    public static <T> T runWithSecurityManager(final Callable<T> action, final AccessControlContext securityContext)
            throws Exception {

        Policy previousPolicy = Policy.getPolicy();
        SecurityManager previousSM = System.getSecurityManager();

        //let's be a bit brutal here and just allow any code do anything by default for the time this method executes.
        Policy.setPolicy(new Policy() {
            @Override
            public boolean implies(ProtectionDomain domain, Permission permission) {
                return true;
            }
        });

        //with our new totally unsecure policy, let's install a new security manager
        System.setSecurityManager(new SecurityManager());

        try {
            //run the code to test with limited privs defined by the securityContext
            return AccessController.doPrivileged(new PrivilegedExceptionAction<T>() {
                @Override
                public T run() throws Exception {
                    return action.call();
                }
            }, securityContext);
        } catch (PrivilegedActionException e) {
            throw e.getException();
        } finally {
            //and reset back the previous security settings
            System.setSecurityManager(previousSM);
            Policy.setPolicy(previousPolicy);
        }
    }

    private static AccessControlContext getSecurityContextForJNDILookup(Collection<JndiPermission> jndiPermissions) {
        CodeSource src = new CodeSource(null, (Certificate[]) null);

        Permissions perms = new Permissions();

        for (JndiPermission p : jndiPermissions) {
            perms.add(p);
        }

        ProtectionDomain domain = new ProtectionDomain(src, perms);

        AccessControlContext ctx = new AccessControlContext(new ProtectionDomain[]{domain});

        return ctx;
    }
}
