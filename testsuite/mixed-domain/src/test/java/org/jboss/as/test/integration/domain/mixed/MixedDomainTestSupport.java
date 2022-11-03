/*
* JBoss, Home of Professional Open Source.
* Copyright 2012, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.test.integration.domain.mixed;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.TimeUnit;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.client.helpers.domain.DomainClient;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.domain.management.util.DomainLifecycleUtil;
import org.jboss.as.test.integration.domain.management.util.DomainTestSupport;
import org.jboss.as.test.integration.domain.management.util.DomainTestUtils;
import org.jboss.as.test.integration.domain.management.util.WildFlyManagedConfiguration;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Assume;


/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class MixedDomainTestSupport extends DomainTestSupport {

    public static final String STANDARD_DOMAIN_CONFIG = "copied-primary-config/domain.xml";
    private static final String JBOSS_DOMAIN_SERVER_ARGS = "jboss.domain.server.args";
    private static final int TEST_VM_VERSION;

    static {
        String spec = System.getProperty("java.specification.version");
        TEST_VM_VERSION = "1.8".equals(spec) ? 8 : Integer.parseInt(spec);
    }

    private final Version.AsVersion version;
    private final boolean adjustDomain;
    private final boolean legacyConfig;
    private final boolean withPrimaryServers;
    private final String profile;


    private MixedDomainTestSupport(Version.AsVersion version, String testClass, String domainConfig, String primaryConfig, String secondaryConfig,
                                   String jbossHome, String profile, boolean adjustDomain, boolean legacyConfig, boolean withPrimaryServers)
            throws Exception {
        super(testClass, domainConfig, primaryConfig, secondaryConfig, configWithDisabledAsserts(null), configWithDisabledAsserts(jbossHome));
        this.version = version;
        this.adjustDomain = adjustDomain;
        this.legacyConfig = legacyConfig;
        this.withPrimaryServers = withPrimaryServers;
        this.profile = profile;
        configureSecondaryJavaHome();
    }

    private static WildFlyManagedConfiguration configWithDisabledAsserts(String jbossHome){
        WildFlyManagedConfiguration config = new WildFlyManagedConfiguration(jbossHome);
        config.setEnableAssertions(false);
        return config;
    }

    public static MixedDomainTestSupport create(String testClass, Version.AsVersion version) throws Exception {
        return create(testClass, version, STANDARD_DOMAIN_CONFIG, "primary-config/host.xml",
                "secondary-config/host-secondary.xml", "full-ha", true, false, false);
    }

    public static MixedDomainTestSupport create(String testClass, Version.AsVersion version, String domainConfig,
                                                boolean adjustDomain, boolean legacyConfig) throws Exception {
        return create(testClass, version, domainConfig, "primary-config/host.xml",
                "secondary-config/host-secondary.xml", "full-ha", adjustDomain, legacyConfig, false);
    }

    public static MixedDomainTestSupport create(String testClass, Version.AsVersion version, String domainConfig, String profile,
                                                boolean adjustDomain, boolean legacyConfig, boolean withPrimaryServers) throws Exception {
        return create(testClass, version, domainConfig, "primary-config/host.xml",
                "secondary-config/host-secondary.xml", profile, adjustDomain, legacyConfig, withPrimaryServers);
    }

    public static MixedDomainTestSupport create(String testClass, Version.AsVersion version, String domainConfig, String primaryConfig, String secondaryConfig,
                                                 String profile, boolean adjustDomain, boolean legacyConfig, boolean withPrimaryServers) throws Exception {
        final File dir = OldVersionCopier.getOldVersionDir(version).toFile();
        return new MixedDomainTestSupport(version, testClass, domainConfig, primaryConfig,
                secondaryConfig, dir.getAbsolutePath(), profile, adjustDomain, legacyConfig, withPrimaryServers);
    }

    public static MixedDomainTestSupport create(String testClass, Version.AsVersion version, String domainConfig,
                                                String primaryConfig, String secondaryConfig, boolean adjustDomain,
                                                boolean legacyConfig) throws Exception {
        final File dir = OldVersionCopier.getOldVersionDir(version).toFile();
        return new MixedDomainTestSupport(version, testClass, domainConfig, primaryConfig,
                secondaryConfig, dir.getAbsolutePath(), "full-ha", adjustDomain, legacyConfig, false);
    }

    public void start() {
        if (adjustDomain) {
            startAndAdjust();
        } else {
            super.start();
        }

        if (!legacyConfig) {
            // The non-legacy config tests assume host=secondary/server=server-one is auto-start and running
            startSecondaryServer();
        }
    }

    private void startSecondaryServer() {
        DomainClient client = getDomainPrimaryLifecycleUtil().getDomainClient();
        PathElement hostElement = PathElement.pathElement("host", "secondary");

        try {
            PathAddress pa = PathAddress.pathAddress(hostElement, PathElement.pathElement("server-config", "server-one"));
            DomainTestUtils.executeForResult(Util.getUndefineAttributeOperation(pa, "auto-start"), client);
            DomainTestUtils.executeForResult(Util.createEmptyOperation("start", pa), client);
        } catch (IOException | MgmtOperationException e) {
            throw new RuntimeException(e);
        }

        long timeout = TimeoutUtil.adjust(20000);
        long expired = System.currentTimeMillis() + timeout;
        ModelNode op = Util.getReadAttributeOperation(PathAddress.pathAddress(hostElement, PathElement.pathElement("server", "server-one")), "server-state");
        do {
            try {
                ModelNode state = DomainTestUtils.executeForResult(op, client);
                if ("running".equalsIgnoreCase(state.asString())) {
                    return;
                }
            } catch (IOException | MgmtOperationException e) {
                // ignore and try again
            }

            try {
                TimeUnit.MILLISECONDS.sleep(250L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Assert.fail();
            }
        } while (System.currentTimeMillis() < expired);

        Assert.fail("Secondary server-one did not start within " + timeout + " ms");
    }

    private void configureSecondaryJavaHome() {
        // Look for properties pointing to a java home to use for the legacy host.
        // Look for homes for the max JVM version the host can handle, working back to the min it can handle.
        // We could start with the oldest and work forward, but that would likely result in all versions testing
        // against the oldest VM. Starting with the newest will increase coverage by increasing the probability
        // of different VM versions being used across the overall set of legacy host versions.
        String javaHome = null;
        for (int i = Math.min(version.getMaxVMVersion(), TEST_VM_VERSION - 1); i >= version.getMinVMVersion() && javaHome == null; i--) {
            javaHome = System.getProperty("jboss.test.legacy.host.java" + i + ".home");
        }

        if (javaHome != null) {
            WildFlyManagedConfiguration  cfg = getDomainSecondaryConfiguration();
            cfg.setJavaHome(javaHome);
            cfg.setControllerJavaHome(javaHome);
            System.out.println("Set legacy host controller to use " + javaHome + " as JAVA_HOME");
        } else {
            // Ignore the test if the secondary cannot run using the current VM version
            Assume.assumeTrue(TEST_VM_VERSION <= version.getMaxVMVersion());
            Assume.assumeTrue(TEST_VM_VERSION >= version.getMinVMVersion());
        }
    }

    private void startAndAdjust() {

        String jbossDomainServerArgsValue = null;
        try {
            //Start the primary in admin only  and reconfigure the domain with what
            //we want to test in the mixed domain and have the DomainAdjuster
            //strip down the domain model to something more workable. The domain
            //adjusters will also make adjustments for the legacy version being
            //tested.
            DomainLifecycleUtil primaryUtil = getDomainPrimaryLifecycleUtil();
            primaryUtil.getConfiguration().setAdminOnly(true);
            //primaryUtil.getConfiguration().addHostCommandLineProperty("-agentlib:jdwp=transport=dt_socket,address=8787,server=y,suspend=y");
            primaryUtil.start();
            if (legacyConfig) {
                LegacyConfigAdjuster.adjustForVersion(primaryUtil.getDomainClient(), version);
            } else {
                DomainAdjuster.adjustForVersion(primaryUtil.getDomainClient(), version, profile, withPrimaryServers);
            }

            //Now reload the primary in normal mode
            primaryUtil.executeAwaitConnectionClosed(Util.createEmptyOperation("reload", PathAddress.pathAddress(HOST, "primary")));
            primaryUtil.connect();
            primaryUtil.awaitHostController(System.currentTimeMillis());

            //Start the secondary hosts
            DomainLifecycleUtil secondaryUtil = getDomainSecondaryLifecycleUtil();
            if (secondaryUtil != null) {
                //secondaryUtil.getConfiguration().addHostCommandLineProperty("-agentlib:jdwp=transport=dt_socket,address=8787,server=y,suspend=y");
                secondaryUtil.start();
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static String copyDomainFile() {
        final Path originalDomainXml = loadFile("target", "wildfly", "domain", "configuration", "domain.xml");
        return copyDomainFile(originalDomainXml);
    }

    static String copyDomainFile(final Path originalDomainXml) {

        final Path targetDirectory = createDirectory("target", "test-classes", "copied-primary-config");
        final Path copiedDomainXml = targetDirectory.resolve("domain.xml");

        try {
            Files.copy(originalDomainXml, copiedDomainXml, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return STANDARD_DOMAIN_CONFIG;
    }

    static Path loadLegacyDomainXml(Version.AsVersion version) {
        String number = version.getVersion().replace('.', '-');
        final String fileName;
        switch (version.basename) {
            case Version.EAP:
                fileName = "eap-" + number + ".xml";
                break;
            case Version.WILDFLY:
            default:
                fileName = "wildfly-" + number + ".xml";
        }
        return loadFile("..", "integration", "manualmode", "src", "test", "resources", "legacy-configs", "domain", fileName);
    }

    private static Path loadFile(String first, String... parts) {
        final Path p = Paths.get(first, parts);
        Assert.assertTrue(p.toAbsolutePath() + " does not exist", Files.exists(p));
        return p;
    }


    private static Path createDirectory(String first, String... parts) {
        Path p = Paths.get(first, parts);
        try {
            Path dir = Files.createDirectories(p);
            Assert.assertTrue(Files.exists(dir));
            return dir;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
