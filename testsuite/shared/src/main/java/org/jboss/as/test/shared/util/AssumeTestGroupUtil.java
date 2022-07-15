package org.jboss.as.test.shared.util;

import static org.junit.Assume.assumeTrue;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.function.Supplier;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AssumptionViolatedException;
import org.testcontainers.DockerClientFactory;

/**
 * Helper methods which help to skip tests for functionality which is not yet fully working or where the test is not
 * expected to work in certain execution environments. Put the call of the method directly into
 * the failing test method, or if you want to skip whole test class, then put the method call into method annotated with
 * {@link org.junit.BeforeClass}.
 * <p>
 * Methods whose names begin with 'assume' throw {@link AssumptionViolatedException} if the described assumption is not
 * met; otherwise they return normally.  Other methods and constants in this class are meant for related use cases.
 * </p>
 *
 * @author Josef Cacek
 * @author Brian Stansberry
 */
public class AssumeTestGroupUtil {

    /**
     * An empty deployment archive that can be returned from an @Deployment method if the normal deployment
     * returned from the method cannot be successfully deployed under certain conditions. Arquillian will deploy
     * managed deployments <strong>before executing any @BeforeClass method</strong>, so if a @BeforeClass
     * method conditionally disables running the tests with an AssumptionViolatedException, the deployment will
     * still get deployed and may fail the test. So have it deploy this instead so your @BeforeClass gets called.
     * <p>
     * This is private so it can be lazy initialized when needed, in case this class is used for other reasons
     * in-container where Shrinkwrap is not available.
     * </p>
     */
    private static JavaArchive EMPTY_JAR;
    /** Same as {@link #EMPTY_JAR} but is a {@link WebArchive}. */
    private static WebArchive EMPTY_WAR;
    /** Same as {@link #EMPTY_JAR} but is an {@link EnterpriseArchive}. */
    private static EnterpriseArchive EMPTY_EAR;
    /**
     * Creates an empty (except for a manifest) JavaArchive with the name {@code empty.jar}.
     * @return the archive
     */
    public static JavaArchive emptyJar() {
        if (EMPTY_JAR == null) {
            EMPTY_JAR = emptyJar("empty");
        }
        return EMPTY_JAR;
    }
    /**
     * Creates an empty (except for a manifest) JavaArchive with the given name.
     * @param name the jar name. Can end with the '.jar' extension, but if not it will be added
     * @return the archive
     */
    public static JavaArchive emptyJar(String name) {
        String jarName = name.endsWith(".jar") ? name : name + ".jar";
        return ShrinkWrap.create(JavaArchive.class, jarName)
                .addManifest();
    }
    /**
     * Creates an empty (except for a manifest) WebArchive with the name {@code empty.war}.
     * @return the archive
     */
    public static WebArchive emptyWar() {
        if (EMPTY_WAR == null) {
            EMPTY_WAR = emptyWar("empty");
        }
        return EMPTY_WAR;
    }
    /**
     * Creates an empty (except for a manifest)  WebArchive with the given name.
     * @param name the jar name. Can end with the '.war' extension, but if not it will be added
     * @return the archive
     */
    public static WebArchive emptyWar(String name) {
        String warName = name.endsWith(".war") ? name : name + ".war";
        return ShrinkWrap.create(WebArchive.class, warName)
                .addManifest();
    }
    /**
     * Creates an empty (except for a manifest) EnterpriseArchive with the name {@code empty.ear}.
     * @return the archive
     */
    public static EnterpriseArchive emptyEar() {
        if (EMPTY_EAR == null) {
            EMPTY_EAR = emptyEar("empty");
        }
        return EMPTY_EAR;
    }
    /**
     * Creates an empty (except for a manifest) EnterpriseArchive with the given name.
     * @param name the jar name. Can end with the '.ear' extension, but if not it will be added
     * @return the archive
     */
    public static EnterpriseArchive emptyEar(String name) {
        String earName = name.endsWith(".ear") ? name : name + ".ear";
        return ShrinkWrap.create(EnterpriseArchive.class, earName)
                .addManifest();
    }

    /**
     * Assume for tests that fail when the security manager is enabled. This should be used sparingly and issues should
     * be filed for failing tests so a proper fix can be done.
     * <p>
     * Note that this checks the {@code security.manager} system property and <strong>not</strong> that the
     * {@link System#getSecurityManager()} is {@code null}. The property is checked so that the assumption check can be
     * done in a {@link org.junit.Before @Before} or {@link org.junit.BeforeClass @BeforeClass} method.
     * </p>
     *
     * @throws AssumptionViolatedException if the security manager is enabled
     */
    public static void assumeSecurityManagerDisabled() {
        assumeCondition("Tests failing if the security manager is enabled.", AssumeTestGroupUtil::isSecurityManagerDisabled);
    }

    /**
     * Check if the JDK Security Manager is <strong>enabled/strong>.
     * <p>
     * Note that this checks the {@code security.manager} system property and <strong>not</strong> that the
     * {@link System#getSecurityManager()} is not {@code null}. The property is checked so that the assumption check can be
     * done in a {@link org.junit.Before @Before} or {@link org.junit.BeforeClass @BeforeClass} method.
     * </p>
     *
     * @return {@code true} if the {@code security.manager} system property is null.
     */
    public static boolean isSecurityManagerEnabled() {
        return !isSecurityManagerDisabled();
    }

    /**
     * Check if the JDK Security Manager is <strong>disabled</strong>.
     * <p>
     * Note that this checks the {@code security.manager} system property and <strong>not</strong> that the
     * {@link System#getSecurityManager()} is {@code null}. The property is checked so that the assumption check can be
     * done in a {@link org.junit.Before @Before} or {@link org.junit.BeforeClass @BeforeClass} method.
     * </p>
     *
     * @return {@code true} if the {@code security.manager} system property is null.
     */
    public static boolean isSecurityManagerDisabled() {
        return System.getProperty("security.manager") == null;
    }

    /**
     * Assume for tests that fail when the security manager is enabled or the JDK version is prior to a given version
     * <p>
     * Note that this checks the {@code security.manager} system property and <strong>not</strong> that the
     * {@link System#getSecurityManager()} is {@code null}. The property is checked so that the assumption check can be
     * done in a {@link org.junit.Before @Before} or {@link org.junit.BeforeClass @BeforeClass} method.
     * </p>
     *
     * @param javaSpecificationVersion the JDK specification version
     *
     * @throws AssumptionViolatedException if the security manager is enabled or the JDK version is greater than or equal to {@code javaSpecificationVersion}
     */
    public static void assumeSecurityManagerDisabledOrAssumeJDKVersionBefore(int javaSpecificationVersion) {
        assumeCondition("Tests failing if the security manager is enabled and JDK in use is after " + javaSpecificationVersion + ".",
                () -> (isSecurityManagerDisabled() || isJDKVersionBefore(javaSpecificationVersion)));
    }

    /**
     * Assume for tests that fail when the JVM version is too low. This should be used sparingly.
     *
     * @param javaSpecificationVersion the JDK specification version. Use 8 for JDK 8. Must be 8 or higher.
     *
     * @throws AssumptionViolatedException if the JDK version is less than or equal to {@code javaSpecificationVersion}
     */
    public static void assumeJDKVersionAfter(int javaSpecificationVersion) {
        assert javaSpecificationVersion >= 11; // we only support 11 or later
        assumeCondition("Tests failing if the JDK in use is before " + javaSpecificationVersion + ".",
                () -> isJDKVersionAfter(javaSpecificationVersion));
    }

    /**
     * Assume for tests that fail when the JVM version is too high. This should be used sparingly.
     *
     * @param javaSpecificationVersion the JDK specification version. Must be 9 or higher.
     *
     * @throws AssumptionViolatedException if the JDK version is greater than or equal to {@code javaSpecificationVersion}
     */
    public static void assumeJDKVersionBefore(int javaSpecificationVersion) {
        assert javaSpecificationVersion > 11; // we only support 11 or later so no reason to call this for 11
        assumeCondition("Tests failing if the JDK in use is after " + javaSpecificationVersion + ".",
                () -> isJDKVersionBefore(javaSpecificationVersion));
    }

    /**
     * Check if the current JDK specification version is greater than the given value.
     *
     * @param javaSpecificationVersion the JDK specification version. Use 8 for JDK 8.
     */
    public static boolean isJDKVersionAfter(int javaSpecificationVersion) {
        return getJavaSpecificationVersion() > javaSpecificationVersion;
    }

    /**
     * Check if the current JDK specification version is less than the given value.
     *
     * @param javaSpecificationVersion the JDK specification version. Use 8 for JDK 8.
     */
    public static boolean isJDKVersionBefore(int javaSpecificationVersion) {
        return getJavaSpecificationVersion() < javaSpecificationVersion;
    }

    /**
     * Assume for test failures when running against a full distribution.
     * Full distributions are available from build/dist modules. It skips tests in case
     * {@code '-Dtestsuite.default.build.project.prefix'} Maven argument is used with
     * a non empty value, e.g. testsuite.default.build.project.prefix=ee- which means we
     * are using ee-build/ee-dist modules as the source where to find the server under test.
     *
     * @throws AssumptionViolatedException if property {@code testsuite.default.build.project.prefix} is set to a non-empty value
     */
    public static void assumeFullDistribution() {
        assumeCondition("Tests requiring full distribution are disabled", AssumeTestGroupUtil::isFullDistribution);
    }

    /**
     * Checks whether tests are running against a full distribution.
     * Full distributions are available from build/dist modules. It skips tests in case
     * {@code '-Dtestsuite.default.build.project.prefix'} Maven argument is used with
     * a non empty value, e.g. testsuite.default.build.project.prefix=ee- which means we
     * are using ee-build/ee-dist modules as the source where to find the server under test.
     *
     * @throws {@code true} of system property {@code testsuite.default.build.project.prefix} has a non-empty value
     */
    public static boolean isFullDistribution() {
        return System.getProperty("testsuite.default.build.project.prefix", "").equals("");
    }

    /**
     * Assume for tests that require a docker installation.
     *
     * @throws AssumptionViolatedException if a docker client cannot be initialized
     */
    public static void assumeDockerAvailable() {
        assumeCondition("Docker is not available.", AssumeTestGroupUtil::isDockerAvailable);
    }

    /**
     * Checks whether a docker installation is available.
     *
     * @throws AssumptionViolatedException if a docker client cannot be initialized
     */
    public static boolean isDockerAvailable() {
        try {
            DockerClientFactory.instance().client();
            return true;
        } catch (Throwable ex) {
            return false;
        }
    }

    /**
     * Assume for tests that should not run against a WildFly Preview installation.
     *
     * @throws AssumptionViolatedException if one of the system properties that indicates WildFly Preview is being tested is set
     */
    public static void assumeNotWildFlyPreview() {
        assumeCondition("Some tests are disabled on WildFly Preview",
                () -> !isWildFlyPreview());
    }

    public static void assumeWildFlyPreview() {
        assumeCondition("Some tests require WildFly Preview",
                AssumeTestGroupUtil::isWildFlyPreview);
    }

    public static boolean isWildFlyPreview() {
        return System.getProperty("ts.ee9") != null || System.getProperty("ts.bootable.ee9") != null;
    }

    private static int getJavaSpecificationVersion() {
        final String versionString = System.getProperty("java.specification.version");
        return Integer.parseInt(versionString);
    }

    private static void assumeCondition(final String message, final Supplier<Boolean> assumeTrueCondition) {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                assumeTrue(message, assumeTrueCondition.get());
                return null;
            }
        });
    }

}
