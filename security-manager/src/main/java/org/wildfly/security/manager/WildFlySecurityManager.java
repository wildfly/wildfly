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

package org.wildfly.security.manager;

import java.io.FileDescriptor;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.CodeSource;
import java.security.Permission;
import java.security.Principal;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.ProtectionDomain;
import java.util.Arrays;
import org.wildfly.security.manager._private.SecurityMessages;
import sun.reflect.Reflection;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.lang.System.getSecurityManager;
import static java.security.AccessController.doPrivileged;
import static org.wildfly.security.manager._private.SecurityMessages.access;
import static sun.reflect.Reflection.getCallerClass;

/**
 * The security manager.  This security manager implementation can be switched on and off on a per-thread basis,
 * and additionally logs access violations in a way that should be substantially clearer than most JDK implementations.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class WildFlySecurityManager extends SecurityManager {

    private static final Permission SECURITY_MANAGER_PERMISSION = new RuntimePermission("setSecurityManager");
    private static final Permission UNCHECKED_PERMISSION = new RuntimePermission("doUnchecked");

    private static final InheritableThreadLocal<Boolean> CHECKING = new InheritableThreadLocal<>();
    private static final ThreadLocal<Boolean> ENTERED = new ThreadLocal<Boolean>();

    private static final Field PD_STACK;

    static {
        PD_STACK = doPrivileged(new GetAccessibleDeclaredFieldAction(AccessControlContext.class, "context"));
    }

    /**
     * Determine whether the security manager is currently checking permissions.
     *
     * @return {@code true} if the security manager is currently checking permissions
     */
    public static boolean isChecking() {
        final SecurityManager sm = getSecurityManager();
        return sm instanceof WildFlySecurityManager ? CHECKING.get() == TRUE : sm != null;
    }

    /**
     * Perform a permission check.
     *
     * @param perm the permission to check
     * @throws SecurityException if the check fails
     */
    public void checkPermission(final Permission perm) throws SecurityException {
        checkPermission(perm, AccessController.getContext());
    }

    /**
     * Perform a permission check.
     *
     * @param perm the permission to check
     * @param context the security context to use for the check (must be an {@link AccessControlContext} instance)
     * @throws SecurityException if the check fails
     */
    public void checkPermission(final Permission perm, final Object context) throws SecurityException {
        if (context instanceof AccessControlContext) {
            checkPermission(perm, (AccessControlContext) context);
        } else {
            throw access.unknownContext();
        }
    }

    /**
     * Find the protection domain in the given list which denies a permission, or {@code null} if the permission
     * check would pass.
     *
     * @param permission the permission to test
     * @param domains the protection domains to try
     * @return the first denying protection domain, or {@code null} if there is none
     */
    public static ProtectionDomain findAccessDenial(final Permission permission, final ProtectionDomain... domains) {
        if (domains != null) for (ProtectionDomain domain : domains) {
            if (! domain.implies(permission)) {
                return domain;
            }
        }
        return null;
    }

    /**
     * Try a permission check.  Any violations will be logged to the {@code org.wildfly.security.access} category
     * at a {@code DEBUG} level.
     *
     * @param permission the permission to check
     * @param domains the protection domains to try
     * @return {@code true} if the access check succeeded, {@code false} otherwise
     */
    public static boolean tryCheckPermission(final Permission permission, final ProtectionDomain... domains) {
        final ProtectionDomain protectionDomain = findAccessDenial(permission, domains);
        if (protectionDomain != null) {
            if (ENTERED.get() != TRUE) {
                ENTERED.set(TRUE);
                try {
                    final CodeSource codeSource = protectionDomain.getCodeSource();
                    final ClassLoader classLoader = protectionDomain.getClassLoader();
                    final Principal[] principals = protectionDomain.getPrincipals();
                    if (principals == null || principals.length == 0) {
                        access.accessCheckFailed(permission, codeSource, classLoader);
                    } else {
                        access.accessCheckFailed(permission, codeSource, classLoader, Arrays.toString(principals));
                    }
                } finally {
                    ENTERED.set(FALSE);
                }
            }
            return false;
        }
        return true;
    }

    /**
     * Perform a permission check.
     *
     * @param perm the permission to check
     * @param context the security context to use for the check
     * @throws SecurityException if the check fails
     */
    public void checkPermission(final Permission perm, final AccessControlContext context) throws SecurityException {
        if (perm.implies(SECURITY_MANAGER_PERMISSION)) {
            throw access.secMgrChange();
        }
        if (CHECKING.get() == TRUE) {
            if (ENTERED.get() == TRUE) {
                return;
            }
            ENTERED.set(TRUE);
            try {
                final ProtectionDomain[] stack;
                try {
                    stack = (ProtectionDomain[]) PD_STACK.get(context);
                } catch (IllegalAccessException e) {
                    // should be impossible
                    throw new IllegalAccessError(e.getMessage());
                }
                if (stack != null && ! tryCheckPermission(perm, stack)) {
                    throw access.accessControlException(perm, perm);
                }
            } finally {
                ENTERED.set(FALSE);
            }
        }
    }

    public void checkCreateClassLoader() {
        if (CHECKING.get() == TRUE) {
            super.checkCreateClassLoader();
        }
    }

    public void checkAccess(final Thread t) {
        if (CHECKING.get() == TRUE) {
            super.checkAccess(t);
        }
    }

    public void checkAccess(final ThreadGroup g) {
        if (CHECKING.get() == TRUE) {
            super.checkAccess(g);
        }
    }

    public void checkExit(final int status) {
        if (CHECKING.get() == TRUE) {
            super.checkExit(status);
        }
    }

    public void checkExec(final String cmd) {
        if (CHECKING.get() == TRUE) {
            super.checkExec(cmd);
        }
    }

    public void checkLink(final String lib) {
        if (CHECKING.get() == TRUE) {
            super.checkLink(lib);
        }
    }

    public void checkRead(final FileDescriptor fd) {
        if (CHECKING.get() == TRUE) {
            super.checkRead(fd);
        }
    }

    public void checkRead(final String file) {
        if (CHECKING.get() == TRUE) {
            super.checkRead(file);
        }
    }

    public void checkRead(final String file, final Object context) {
        if (CHECKING.get() == TRUE) {
            super.checkRead(file, context);
        }
    }

    public void checkWrite(final FileDescriptor fd) {
        if (CHECKING.get() == TRUE) {
            super.checkWrite(fd);
        }
    }

    public void checkWrite(final String file) {
        if (CHECKING.get() == TRUE) {
            super.checkWrite(file);
        }
    }

    public void checkDelete(final String file) {
        if (CHECKING.get() == TRUE) {
            super.checkDelete(file);
        }
    }

    public void checkConnect(final String host, final int port) {
        if (CHECKING.get() == TRUE) {
            super.checkConnect(host, port);
        }
    }

    public void checkConnect(final String host, final int port, final Object context) {
        if (CHECKING.get() == TRUE) {
            super.checkConnect(host, port, context);
        }
    }

    public void checkListen(final int port) {
        if (CHECKING.get() == TRUE) {
            super.checkListen(port);
        }
    }

    public void checkAccept(final String host, final int port) {
        if (CHECKING.get() == TRUE) {
            super.checkAccept(host, port);
        }
    }

    public void checkMulticast(final InetAddress maddr) {
        if (CHECKING.get() == TRUE) {
            super.checkMulticast(maddr);
        }
    }

    public void checkMulticast(final InetAddress maddr, final byte ttl) {
        if (CHECKING.get() == TRUE) {
            super.checkMulticast(maddr, ttl);
        }
    }

    public void checkPropertiesAccess() {
        if (CHECKING.get() == TRUE) {
            super.checkPropertiesAccess();
        }
    }

    public void checkPropertyAccess(final String key) {
        if (CHECKING.get() == TRUE) {
            super.checkPropertyAccess(key);
        }
    }

    public void checkPrintJobAccess() {
        if (CHECKING.get() == TRUE) {
            super.checkPrintJobAccess();
        }
    }

    public void checkPackageAccess(final String pkg) {
        if (CHECKING.get() == TRUE) {
            super.checkPackageAccess(pkg);
        }
    }

    public void checkPackageDefinition(final String pkg) {
        if (CHECKING.get() == TRUE) {
            super.checkPackageDefinition(pkg);
        }
    }

    public void checkSetFactory() {
        if (CHECKING.get() == TRUE) {
            super.checkSetFactory();
        }
    }

    public void checkMemberAccess(final Class<?> clazz, final int which) {
        if (CHECKING.get() == TRUE) {
            super.checkMemberAccess(clazz, which);
        }
    }

    public void checkSecurityAccess(final String target) {
        if (CHECKING.get() == TRUE) {
            super.checkSecurityAccess(target);
        }
    }

    /**
     * Perform an action with permission checking enabled.  If permission checking is already enabled, the action is
     * simply run.
     *
     * @param action the action to perform
     * @param <T> the action return type
     * @return the return value of the action
     */
    public static <T> T doChecked(PrivilegedAction<T> action) {
        final ThreadLocal<Boolean> checking = WildFlySecurityManager.CHECKING;
        if (checking.get() == TRUE) {
            return action.run();
        }
        checking.set(TRUE);
        try {
            return action.run();
        } finally {
            checking.set(FALSE);
        }
    }

    /**
     * Perform an action with permission checking enabled.  If permission checking is already enabled, the action is
     * simply run.
     *
     * @param action the action to perform
     * @param <T> the action return type
     * @return the return value of the action
     * @throws PrivilegedActionException if the action threw an exception
     */
    public static <T> T doChecked(PrivilegedExceptionAction<T> action) throws PrivilegedActionException {
        final ThreadLocal<Boolean> checking = WildFlySecurityManager.CHECKING;
        if (checking.get() == TRUE) {
            try {
                return action.run();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new PrivilegedActionException(e);
            }
        }
        checking.set(TRUE);
        try {
            return action.run();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new PrivilegedActionException(e);
        } finally {
            checking.set(FALSE);
        }
    }

    /**
     * Perform an action with permission checking disabled.  If permission checking is already disabled, the action is
     * simply run.  The immediate caller must have the {@code doUnchecked} runtime permission.
     *
     * @param action the action to perform
     * @param <T> the action return type
     * @return the return value of the action
     */
    public static <T> T doUnchecked(PrivilegedAction<T> action) {
        final ThreadLocal<Boolean> checking = WildFlySecurityManager.CHECKING;
        if (checking.get() != TRUE) {
            return action.run();
        }
        final SecurityManager sm = getSecurityManager();
        if (sm != null) {
            assert getCallerClass(0) == Reflection.class;
            assert getCallerClass(1) == WildFlySecurityManager.class;
            if (! getCallerClass(2).getProtectionDomain().implies(UNCHECKED_PERMISSION)) {
                throw SecurityMessages.access.accessControlException(UNCHECKED_PERMISSION, UNCHECKED_PERMISSION);
            }
        }
        checking.set(FALSE);
        try {
            return action.run();
        } finally {
            checking.set(TRUE);
        }
    }

    /**
     * Perform an action with permission checking disabled.  If permission checking is already disabled, the action is
     * simply run.  The caller must have the {@code doUnchecked} runtime permission.
     *
     * @param action the action to perform
     * @param <T> the action return type
     * @return the return value of the action
     * @throws PrivilegedActionException if the action threw an exception
     */
    public static <T> T doUnchecked(PrivilegedExceptionAction<T> action) throws PrivilegedActionException {
        final ThreadLocal<Boolean> checking = WildFlySecurityManager.CHECKING;
        if (checking.get() != TRUE) {
            try {
                return action.run();
            } catch (Exception e) {
                throw new PrivilegedActionException(e);
            }
        }
        final SecurityManager sm = getSecurityManager();
        if (sm != null) {
            assert getCallerClass(0) == Reflection.class;
            assert getCallerClass(1) == WildFlySecurityManager.class;
            if (! getCallerClass(2).getProtectionDomain().implies(UNCHECKED_PERMISSION)) {
                throw SecurityMessages.access.accessControlException(UNCHECKED_PERMISSION, UNCHECKED_PERMISSION);
            }
        }
        checking.set(FALSE);
        try {
            return action.run();
        } catch (Exception e) {
            throw new PrivilegedActionException(e);
        } finally {
            checking.set(TRUE);
        }
    }
}
