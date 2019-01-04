package org.jboss.as.test.shared.util;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.function.Supplier;

import org.junit.Assume;

/**
 * Helper methods which help to skip tests for feature which is not yet fully working. Put the call of the method directly into
 * the failing test method, or if you want to skip whole test class, then put the method call into method annotated with
 * {@link org.junit.BeforeClass}.
 *
 * @author Josef Cacek
 */
public class AssumeTestGroupUtil {

    /**
     * Assume for test failures when running with Elytron profile enabled. It skips test in case the {@code '-Delytron'} Maven
     * argument is used (for Elytron profile activation). For legacy-security only tests.
     */
    public static void assumeElytronProfileEnabled() {
        assumeCondition("Tests failing in Elytron profile are disabled", () -> System.getProperty("elytron") == null);
    }

    /**
     * Assume for tests that fail when the security manager is enabled. This should be used sparingly and issues should
     * be filed for failing tests so a proper fix can be done.
     * <p>
     * Note that this checks the {@code security.manager} system property and <strong>not</strong> that the
     * {@link System#getSecurityManager()} is {@code null}. The property is checked so that the assumption check can be
     * done in a {@link org.junit.Before @Before} or {@link org.junit.BeforeClass @BeforeClass} method.
     * </p>
     */
    public static void assumeSecurityManagerDisabled() {
        assumeCondition("Tests failing if the security manager is enabled.", () -> System.getProperty("security.manager") == null);
    }

    private static void assumeCondition(final String message, final Supplier<Boolean> assumeTrueCondition) {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                Assume.assumeTrue(message, assumeTrueCondition.get());
                return null;
            }
        });
    }

}
