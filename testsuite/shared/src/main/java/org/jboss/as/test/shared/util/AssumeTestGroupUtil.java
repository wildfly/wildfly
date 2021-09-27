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

    /**
     * Assume for tests that fail when the JVM version is too low. This should be used sparingly.
     *
     * @param javaSpecificationVersion the JDK specification version. Use 8 for JDK 8. Must be 8 or higher.
     */
    public static void assumeJDKVersionAfter(int javaSpecificationVersion) {
        assert javaSpecificationVersion >= 8; // we only support 8 or later so no reason to call this for 8
        assumeCondition("Tests failing if the JDK in use is before " + javaSpecificationVersion + ".",
                () -> getJavaSpecificationVersion() > javaSpecificationVersion);
    }

    /**
     * Assume for tests that fail when the JVM version is too high. This should be used sparingly.
     *
     * @param javaSpecificationVersion the JDK specification version. Must be 9 or higher.
     */
    public static void assumeJDKVersionBefore(int javaSpecificationVersion) {
        assert javaSpecificationVersion >= 9; // we only support 8 or later so no reason to call this for 8
        assumeCondition("Tests failing if the JDK in use is after " + javaSpecificationVersion + ".",
                () -> getJavaSpecificationVersion() < javaSpecificationVersion);
    }

    // BES 2020/05/18 I added this along with assumeJDKVersionAfter/assumeJDKVersionBefore but commented it
    // out because using it seems like bad practice. If there's a legit need some day, well, here's the code...
//    /**
//     * Assume for tests that fail when the JVM version is something. This should be used sparingly and issues should
//     * be filed for failing tests so a proper fix can be done, as it's inappropriate to limit a test to a single version.
//     *
//     * @param javaSpecificationVersion the JDK specification version. Use 8 for JDK 8. Must be 8 or higher.
//     */
//    public static void assumeJDKVersionEquals(int javaSpecificationVersion) {
//        assert javaSpecificationVersion >= 8; // we only support 8 or later
//        assumeCondition("Tests failing if the JDK in use is other than " + javaSpecificationVersion + ".",
//                () -> getJavaSpecificationVersion() == javaSpecificationVersion);
//    }

    /**
     * Assume for test failures when running against a full distribution.
     * Full distributions are available from build/dist modules. It skips tests in case
     * {@code '-Dtestsuite.default.build.project.prefix'} Maven argument is used with
     * a non empty value, e.g. testsuite.default.build.project.prefix=ee- which means we
     * are using ee-build/ee-dist modules as the source where to find the server under test.
     */
    public static void assumeFullDistribution() {
        assumeCondition("Tests requiring full distribution are disabled", () -> System.getProperty("testsuite.default.build.project.prefix", "").equals(""));
    }

    private static int getJavaSpecificationVersion() {
        String versionString = System.getProperty("java.specification.version");
        versionString = versionString.startsWith("1.") ? versionString.substring(2) : versionString;
        return Integer.parseInt(versionString);
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
