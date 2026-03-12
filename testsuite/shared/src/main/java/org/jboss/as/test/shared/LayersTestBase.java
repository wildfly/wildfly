/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.shared;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jboss.as.test.layers.LayersTest;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Base class for tests of Galleon layers provided by WildFly feature packs as well as of the proper exposure and use
 * of JBoss Modules modules by those feature packs. Different subclasses will extend this class to support testing
 * of different feature packs.
 * <p/>
 * This class and its subclasses are somewhat misnamed as they test more than installations
 * that have been slimmed to only provide certain Galleon layers. They also include tests that test
 * un-slimmed installations, i.e. {@link #testUnreferencedModules()} and {@link #testDefaultConfigs()}.
 * <p/>
 * These tests cover a number of topics:
 * <ol>
 *     <li>Ensuring that each layer can be used by itself to provision a server that can boot without errors.
 *     See {@link #testLayersBoot()}</li>
 *     <li>Ensuring that layers don't provision 'banned' modules. See {@link #checkBannedModules()}</li>
 *     <li>Ensuring that all modules are associated with at least one layer unless an explanation as to why not
 *     is provided. Discourages the inclusion of functionality that is only available in an un-slimmed server.
 *     See {@link #testLayersModuleUse()}</li>
 *     <li>Testing that all modules in an un-slimmed server are needed to provide functionality defined
 *     in the standard {@code standalone.xml}, unless an explanation as to why not is provided. Discourages
 *     the accumulation of no-longer-necessary modules. See {@link #testUnreferencedModules()}</li>
 * </ol>
 * <p>
 * These tests assume that before running the maven {@code test} phase the {@code pom.xml} for the maven module that
 * includes a subclass of this class will have provisioned a number of WildFly installations in a directory
 * whose location is available via the {@code layers.installation.root} system property.
 * <ol>
 *     <li>One installation for each layer provided by the feature pack being tested, not including other layers
 *     provided by other feature packs the feature pack under test depends upon. Each layer should have an
 *     installation that only provides that layer. There may also be other installations that provide more than one layer.
 *     Each of these installations is tested by the {@link #testLayersBoot()} test and the
 *     {@link #checkBannedModules()} test.</li>
 *     <li>One installation that has not been slimmed and that therefore should include all JBoss Module modules
 *     available from the feature pack. This installation is called the {@code test-standalone-reference} installation.
 *     It is used in both the {@link #getExpectedUnreferenced()} and {@link #testLayersModuleUse()} tests.
 *     </li>
 *     <li>One installation that has been slimmed using layers, but where the list of layers includes all of those
 *     that can be used with the feature pack (including ones provided by a dependency feature pack), excluding any
 *     that are an alternative to one of the others. This installation is called the {@code test-all-layers} installation.
 *     It is used in the {@link #testLayersModuleUse()} test.
 *     </li>
 * </ol>
 * <p/>
 * In addition, a set of installations will have been provisioned in a directory whose location is available via the
 * {@code std.default.install.root} system property. There will be one installation for each of the main 'standard'
 * configuration files -- {@code standalone.xml}, {@code standalone-ha.xml}, {@code standalone-full.xml} and
 * {@code standalone-full-ha.xml}. These installations are tested by the {@link #testDefaultConfigs()} test.
 * <p/>
 * Subclasses must implement the {@link #getExpectedUnreferenced()} and {@link #getExpectedUnusedInAllLayers()} methods
 * to provide lists of modules that meet the criteria described in the javadoc for each method. This class provides
 * constants of type {@code String[]} that define names of modules that are applicable to different cases, i.e.
 * relevant to one method or the other or both, and relevant to a particular feature pack or another or a combination.
 * The expectation is subclasses will implement {@link #getExpectedUnreferenced()} and
 * {@link #getExpectedUnusedInAllLayers()} by concatenating together the arrays that are relevant to the feature pack(s)
 * tested by the subclass.
 * <p/>
 * When adding elements to the various String[] constants, maintainers should group the elements based on the
 * reason why they are being added and preface each group with an explanatory comment.
 */
public abstract class LayersTestBase {

    /**
     * Gets the expected set of packages that are not referenced from an un-slimmed server's module graph
     * but that need to be provisioned.
     * <p/>
     * This is the expected set of modules found when scanning the {@code test-standalone-reference} installation
     * that are not referenced (i.e. depended upon) directly or transitively from a standalone server's root module
     * or from one of the extensions used in standalone.xml.
     * <p/>
     * See the various constants with "NOT_REFERENCED" in their name for examples of such modules and the reason
     * why they are not referenced.
     */
    protected abstract Set<String> getExpectedUnreferenced();

    /**
     * Gets the expected set of packages that are provisioned by the {@code test-standalone-reference} installation
     * but not used in the {@code test-all-layers} installation.
     * <p/>
     * This is the expected set of not provisioned modules when all layers are provisioned; i.e.
     * those that are not associated with any layer included in the {@code test-all-layers} installation.
     * <p/>
     * See the various constants with "NO_LAYER" in their name for examples of such modules and the reason why
     * they are not associated with a layer used in the {@code test-all-layers} installation.
     */
    protected abstract Set<String> getExpectedUnusedInAllLayers();

    /**
     * Packages that are always expected to be included in the return value of {@link #getExpectedUnusedInAllLayers()}.
     */
    public static final String[] NO_LAYER_COMMON = {
            // Alternative messaging protocols besides the std Artemis core protocol
            // Use of these depends on an attribute value setting
            "org.apache.activemq.artemis.protocol.amqp",
            "org.apache.activemq.artemis.protocol.hornetq",
            "org.apache.activemq.artemis.protocol.stomp",
            // Legacy client not associated with any layer
            "org.hornetq.client",
            // TODO we need to add an xts layer
            "org.jboss.as.xts",
    };

    /**
     * Included in the return value of {@link #getExpectedUnusedInAllLayers()}
     * only when testing provisioning directly from the wildfly-ee feature pack.
     */
    public static final String[] NO_LAYER_WILDFLY_EE = {
            // In 'wildfly-ee' this is only a dep of org.apache.activemq.artemis.protocol.amqp,
            // which is not part of test-all-layers. It is used in a layer in 'wildfly' and 'wildfly-preview'
            "org.apache.qpid.proton",
            // 'preview' stability module so not yet included in "all-layers" installation
            "jakarta.data.api",
    };

    /**
     * Included in the return value of {@link #getExpectedUnusedInAllLayers()}
     * when testing provisioning from the wildfly or wildfly-preview feature packs.
     * Use this array for items common between the two feature packs.
     */
    public static final String[] NO_LAYER_EXPANSION = {
            // Legacy subsystems for which we will not provide layers
            "org.wildfly.extension.microprofile.metrics-smallrye",
            "org.wildfly.extension.microprofile.opentracing-smallrye",
            // TODO WFLY-16586 microprofile-reactive-streams-operators layer should provision this
            "org.wildfly.reactive.dep.jts",
    };

    /**
     * Included in the return value of {@link #getExpectedUnusedInAllLayers()}
     * only when testing provisioning from the wildfly feature pack.
     */
    public static final String[] NO_LAYER_WILDFLY = {
            // Preview stability 'mvc-krazo' layer cannot be provisioned in OOTB standard wildfly
            "org.wildfly.extension.mvc-krazo",
            "jakarta.mvc.api",
            "org.eclipse.krazo.core",
            "org.eclipse.krazo.resteasy",
            // 'preview' stability extension so not yet included in
            // the "all-layers" installation
            "org.wildfly.extension.jakarta.data",
            "jakarta.data.api",
    };

    /**
     * Included in the return value of {@link #getExpectedUnusedInAllLayers()}
     * only when testing provisioning from the wildfly-preview feature pack.
     */
    public static final String[] NO_LAYER_WILDFLY_PREVIEW = {
            // WFP standard config uses Micrometer instead of WF Metrics
            "org.wildfly.extension.metrics",
            // Not usable with EE 11
            "org.wildfly.extension.security.manager",
    };

    /**
     * Packages that are always expected to be included in the return value of {@link #getExpectedUnreferenced()}.
     */
    public static final String[] NOT_REFERENCED_COMMON = {
            // injected by ee
            "org.wildfly.naming",
            // Injected by jaxrs
            "org.jboss.resteasy.resteasy-json-binding-provider",
            // Injected by jaxrs and also depended upon by narayana-rts, which is part of the non-OOTB rts subsystem
            "org.jboss.resteasy.resteasy-json-p-provider",
            // The console ui content is not part of the kernel nor is it provided by an extension
            "org.jboss.as.console",
            // tooling
            "org.jboss.as.domain-add-user",
            // injected by server in UndertowHttpManagementService
            "org.jboss.as.domain-http-error-context",
            // injected by jsf
            "org.jboss.as.jsf-injection",
            // Brought by galleon FP config
            "org.jboss.as.product",
            // Brought by galleon FP config
            "org.jboss.as.standalone",
            // injected by logging
            "org.jboss.logging.jul-to-slf4j-stub",
            // Webservices tooling
            "org.jboss.ws.tools.common",
            "org.jboss.ws.tools.wsconsume",
            "org.jboss.ws.tools.wsprovide",
            "gnu.getopt",
            // Elytron tooling
            "org.wildfly.security.elytron-tool",
            // bootable jar runtime
            "org.wildfly.bootable-jar",
            // Dynamically added by ee-security and mp-jwt-smallrye DUPs but not referenced by subsystems.
            "org.wildfly.security.jakarta.security",
            // injected by sar
            "org.jboss.as.system-jmx",
            // Loaded reflectively by the jboss fork impl of jakarta.xml.soap.FactoryFinder
            "org.jboss.ws.saaj-impl",
            // TODO just a testsuite utility https://wildfly.zulipchat.com/#narrow/stream/174184-wildfly-developers/topic/org.2Ejboss.2Ews.2Ecxf.2Ests.20module
            "org.jboss.ws.cxf.sts",
            // WFLY-13520 Unreferenced Infinispan modules available for applications to depend upon
            "org.infinispan.cdi.common",
            "org.infinispan.cdi.embedded",
            "org.infinispan.cdi.remote",
            "org.infinispan.query",
            // WFLY-8770 jgroups-aws layer modules needed to configure the aws.S3_PING protocol are not referenced
            "org.jgroups.aws",
            "software.amazon.awssdk.s3",
            // Extension not included in the default config
            "org.jboss.mod_cluster.container.spi",
            "org.jboss.mod_cluster.core",
            "org.jboss.mod_cluster.load.spi",
            "org.wildfly.extension.elytron.jaas-realm",
            "org.wildfly.extension.mod_cluster",
            "org.wildfly.mod_cluster.undertow",
            // Brought by galleon ServerRootResourceDefinition
            "wildflyee.api",
    };


    /**
     * Included in the return value of {@link #getExpectedUnreferenced()}
     * only when testing provisioning directly from the wildfly-ee feature pack.
     */
    public static final String[] NOT_REFERENCED_WILDFLY_EE = {
            // Extension not included in the default config
            "org.wildfly.extension.micrometer",
            "org.wildfly.micrometer.deployment",
            "io.micrometer",
            "io.opentelemetry.proto",
            "io.prometheus",
    };


    /**
     * Included in the return value of {@link #getExpectedUnreferenced()}
     * when testing provisioning from the wildfly or wildfly-preview feature packs.
     * Use this array for items common between the two feature packs.
     */
    public static final String[] NOT_REFERENCED_EXPANSION = {
            // Extension not included in the default config
            "org.wildfly.extension.microprofile.fault-tolerance-smallrye",
            "org.wildfly.microprofile.fault-tolerance-smallrye.deployment",
            "io.smallrye.fault-tolerance",
            "org.eclipse.microprofile.fault-tolerance.api",
            // Extension not included in the default config
            "org.wildfly.extension.microprofile.health-smallrye",
            "org.eclipse.microprofile.health.api",
            "io.smallrye.health",
            // Extension not included in the default config
            "org.wildfly.extension.microprofile.lra-coordinator",
            "org.wildfly.extension.microprofile.lra-participant",
            "org.jboss.narayana.lra.lra-service-base",
            "org.jboss.narayana.lra.lra-coordinator",
            "org.jboss.narayana.lra.lra-participant",
            "org.eclipse.microprofile.lra.api",
            "io.smallrye.stork",
            // Extension not included in the default config
            "org.wildfly.extension.microprofile.openapi-smallrye",
            // Extension not included in the default config
            "org.wildfly.extension.microprofile.reactive-messaging-smallrye",
            // Extension not included in the default config
            "org.wildfly.extension.microprofile.telemetry",
            // Extension not included in the default config
            "org.wildfly.extension.microprofile.reactive-streams-operators-smallrye",
            "org.wildfly.reactive.mutiny.reactive-streams-operators.cdi-provider",
            "io.vertx.client",
            // Extension not included in the default config
            "org.wildfly.extension.microprofile.metrics-smallrye",
            // Extension not included in the default config
            "org.wildfly.extension.microprofile.opentracing-smallrye",
            // Extension not included in the default config
            "org.wildfly.extension.mvc-krazo",
            "jakarta.mvc.api",
            "org.eclipse.krazo.core",
            "org.eclipse.krazo.resteasy",
            // Injected by jaxrs subsystem
            "org.jboss.resteasy.microprofile.config",
            "org.jboss.resteasy.resteasy-client-microprofile",
            // Extension not included in the default config (from wildfly-ee)
            "org.wildfly.extension.micrometer",
            "org.wildfly.micrometer.deployment",
            "io.micrometer",
            "io.opentelemetry.proto",
            "io.prometheus",
    };

    /**
     * Included in the return value of {@link #getExpectedUnreferenced()}
     * only when testing provisioning from the wildfly feature pack.
     */
    public static final String[] NOT_REFERENCED_WILDFLY = {
            // Extension not included in the default config
            "org.wildfly.extension.mvc-krazo",
            "jakarta.mvc.api",
            "org.eclipse.krazo.core",
            "org.eclipse.krazo.resteasy",
            // 'preview' stability extension so not yet included in the standard configs
            "org.wildfly.extension.jakarta.data",
    };

    /**
     * Included in the return value of {@link #getExpectedUnreferenced()}
     * only when testing provisioning from the wildfly-preview feature pack.
     */
    public static final String[] NOT_REFERENCED_WILDFLY_PREVIEW = {
            // Extension not included in the default config
            "org.wildfly.extension.metrics",
            // Extension not included in the default config
            "org.wildfly.extension.mvc-krazo",
            "jakarta.mvc.api",
            "org.eclipse.krazo.core",
            "org.eclipse.krazo.resteasy",
            // Not usable with EE 11
            "org.wildfly.extension.security.manager",
            // Not needed for WildFly as this module is effectively replaced by org.hibernate.models.hibernate-models
            "org.hibernate.commons-annotations",
            "org.wildfly.extension.vertx"
    };

    /**
     * Packages that are always expected to be included in the return value of both
     * {@link #getExpectedUnusedInAllLayers()} and {@link #getExpectedUnreferenced()}.
     */
    public static final String[] NO_LAYER_OR_REFERENCE_COMMON = {
            // deprecated and unused
            "ibm.jdk",
            "javax.api",
            "javax.sql.api",
            "javax.xml.stream.api",
            "sun.jdk",
            "sun.scripting",
            // Special support status -- wildfly-elytron-http-stateful-basic
            "org.wildfly.security.http.sfbasic",
            // test-all-layers installation is non-ha and does not include layers that provide jgroups
            "org.jboss.as.clustering.jgroups",
            // TODO we need to add an agroal layer
            "org.wildfly.extension.datasources-agroal",
            "io.agroal",
            // Legacy subsystems for which we will not provide layers.
            // Not in the ootb standalone.xml extension list
            "org.wildfly.extension.picketlink",
            "org.jboss.as.jsr77",
            "org.keycloak.keycloak-adapter-subsystem",
            // end legacy subsystems with no layer ^^^
            // Legacy extension not in ootb standalone.xml extension list
            // and not in test-all-layers as it is admin-only
            "org.jboss.as.security",
            // TODO move eclipse link support to an external feature pack
            "org.eclipse.persistence",
            // RA not associated with any layer
            "org.jboss.genericjms",
            // Appclient support is not provided by a layer
            "org.jboss.as.appclient",
            "org.jboss.metadata.appclient",
            // TODO WFLY-16576 -- cruft?
            "org.bouncycastle",
            // This was brought in as part an RFE, WFLY-10632 & WFLY-10636. While the module is currently marked as private,
            // for now we should keep this module.
            "org.jboss.resteasy.resteasy-rxjava2",
            // TODO these implement SPIs from RESTEasy or JBoss WS but I don't know how they integrate
            // as there is no ref to them in any module.xml nor any in WF java code.
            // Perhaps via deployment descriptor? In any case, no layer provides them
            "org.wildfly.security.jakarta.client.resteasy",
            "org.wildfly.security.jakarta.client.webservices",
            // TODO we need to add an rts layer
            "org.wildfly.extension.rts",
            "org.jboss.narayana.rts",
    };

    /**
     * Included in the return value of both {@link #getExpectedUnusedInAllLayers()}
     * and {@link #getExpectedUnreferenced()}, but only when testing provisioning
     * directly from the wildfly-ee feature pack.
     */
    public static final String[] NO_LAYER_OR_REFERENCE_WILDFLY_EE = {
            // In wildfly-ee only referenced by the
            // unused-in-all-layers org.jboss.resteasy.resteasy-rxjava2
            "io.reactivex.rxjava2.rxjava",
            // Downstream uses this in installations provisioned with wildfly-ee but upstream does not.
            // To make life easier downstream we include it in wildfly-ee.
            "com.fasterxml.jackson.dataformat.jackson-dataformat-yaml",
            // 'preview' stability extension so not yet included in
            // the "all-layers" installation or the standard configs
            "org.wildfly.extension.jakarta.data"
    };

    /**
     * Utility method to combine various module name arrays into sets for use
     * as parameters to this class' methods.
     *
     * @param first the first array to combine. Cannot be {@code null}
     * @param others other arrays to combine. Can be {@code null}
     * @return a set containing all of the elements in the arrays
     */
    @SuppressWarnings("SameParameterValue")
    protected static Set<String> concatArrays(String[] first, String[]... others) {
        // TODO Delegate to LayersTest.concatArrays once WFCORE-6481 is integrated
        if (others == null || others.length == 0) {
            return new HashSet<>(Arrays.asList(first));
        } else {
            Stream<String> stream = Arrays.stream(first);
            for (String[] array : others) {
                stream = Stream.concat(stream, Arrays.stream(array));
            }
            return stream.collect(Collectors.toSet());
        }
    }

    /**
     * A HashMap to configure a banned module.
     * They key is the banned module name, the value is an optional List with the installation names that are allowed to
     * provision the banned module. The presence of the module in these installations will be ignored.
     * <p>
     * Notice the allowed installation names does not distinguish between different parent names, e.g. test-all-layers here means
     * allowing root/test-all-layers and servletRoot/test-all-layers.
     */
    private static final HashMap<String, List<String>> BANNED_MODULES_CONF = new HashMap<>(){{
        put("org.jboss.as.security", Arrays.asList("test-all-layers-jpa-distributed", "test-all-layers", "legacy-security", "test-standalone-reference"));
    }};

    protected static String root;
    private static String defaultConfigsRoot;
    private static LayersTest.ScanContext scanContext;

    @BeforeClass
    public static void setUp() {
        root = System.getProperty("layers.install.root");
        defaultConfigsRoot = System.getProperty("std.default.install.root");
        scanContext = new LayersTest.ScanContext(root);
    }

    /**
     * If the {@code layers.delete.installations} system property is set to {@code true},
     * deletes all the provisioned installations.
     */
    @AfterClass
    public static void cleanUp() {
        boolean delete = Boolean.getBoolean("layers.delete.installations");
        if(delete) {
            File[] installations = new File(root).listFiles(File::isDirectory);
            if (installations != null) {
                for (File f : installations) {
                    LayersTest.recursiveDelete(f.toPath());
                }
            }
            installations = new File(defaultConfigsRoot).listFiles(File::isDirectory);
            if (installations != null) {
                for (File f : installations) {
                    LayersTest.recursiveDelete(f.toPath());
                }
            }
        }
    }

    /**
     * Checks that the installations found in the given {@code layers.install.root} directory can all be started
     * without errors, i.e. with the {@code WFLYSRV0025} log message in the server's stdout stream.
     * <p>
     * The @{code test-standalone-reference} installation is not tested as that kind of installation is heavily
     * tested elsewhere.
     *
     * @throws Exception on failure
     */
    @Test
    public void testLayersBoot() throws Exception {
        LayersTest.testLayersBoot(root);
    }

    /**
     * Checks that all modules that were provisioned in the @{code test-standalone-reference} installation are also
     * provisioned in @{test-all-layers}, except those included in the {@link #getExpectedUnusedInAllLayers()} set.
     * <p/>
     * The goals of this test are to check for new modules that should be provided by layers but currently are not
     * and to encourage inclusion of existing modules not used in a layer to have an associated layer.
     * <p/>
     * See the class javadoc for a description of the @{code test-standalone-reference} and @{test-all-layers} installations.
     *
     * @throws Exception on failure
     */
    @Test
    public void testLayersModuleUse() throws Exception {
        LayersTest.testLayersModuleUse(getExpectedUnusedInAllLayers(), scanContext);
    }

    /**
     * Checks that all modules in the @{code test-standalone-reference} installation are referenced
     * (i.e. directly or transitively depended upon) from the installation root module or the extension modules
     * configured in standalone.xml, except those included in the {@link #getExpectedUnreferenced()} set.
     * <p/>
     * The goal of this test is to prevent the accumulation of 'orphaned' modules that are not usable.
     * <p/>
     * See the class javadoc for a description of the @{code test-standalone-reference} installation.
     *
     * @throws Exception on failure
     */
    @Test
    public void testUnreferencedModules() throws Exception {
        LayersTest.testUnreferencedModules(getExpectedUnreferenced(), scanContext);
    }

    /**
     * Checks that none of the installations found in the given {@code layers.install.root} directory include modules
     * marked as 'banned'.
     *
     * @throws Exception on failure
     */
    @Test
    public void checkBannedModules() throws Exception {
        final HashMap<String, String> results = LayersTest.checkBannedModules(root, BANNED_MODULES_CONF);
        Assert.assertTrue("The following banned modules were provisioned " + results, results.isEmpty());
    }

    /**
     * Checks that the installations found in the given {@code std.default.install.root} directory can be started
     * without errors, i.e. with the {@code WFLYSRV0025} log message in the server's stdout stream.
     *
     * @throws Exception on failure
     */
    @Test
    public void testDefaultConfigs() throws Exception {
        LayersTest.testLayersBoot(defaultConfigsRoot);
    }
}
