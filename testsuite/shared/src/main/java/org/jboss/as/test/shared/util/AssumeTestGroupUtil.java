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

    public static final Supplier<Boolean> CONDITION_SKIP_ELYTRON_PROFILE = () -> (System.getProperty("elytron") == null
            || Boolean.getBoolean("wildfly.tmp.enable.elytron.profile.tests"));

    /**
     * Assume for invocation-related test failures. It skips test in case the system property
     * {@codewildfly.tmp.enable.invocation.tests} hasn't value {@code 'true'}.
     */
    public static void assumeInvocationTestsEnabled() {
        assumeCondition("Failing Invocation tests are disabled",
                () -> Boolean.getBoolean("wildfly.tmp.enable.invocation.tests"));
    }

    /**
     * Assume for test failures when running with Elytron profile enabled. It skips test in case the {@code '-Delytron'} Maven
     * argument is used (for Elytron profile activation) and system property {@code wildfly.tmp.enable.elytron.profile.tests}
     * hasn't value {@code 'true'}.
     */
    public static void assumeElytronProfileTestsEnabled() {
        assumeCondition("Tests failing in Elytron profile are disabled", CONDITION_SKIP_ELYTRON_PROFILE);
    }

    /**
     * Assume for test failures when running with Elytron profile enabled. It skips test in case the {@code '-Delytron'} Maven
     * argument is used (for Elytron profile activation). For legacy-security only tests.
     */
    public static void assumeElytronProfileEnabled() {
        assumeCondition("Tests failing in Elytron profile are disabled", () -> System.getProperty("elytron") == null);
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
