/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.domain;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILD_TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXCLUDED_EXTENSIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST_EXCLUDE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_RESOURCES_OPERATION;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.jboss.as.controller.client.helpers.domain.DomainClient;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.domain.management.util.DomainLifecycleUtil;
import org.jboss.as.test.integration.domain.management.util.DomainTestUtils;
import org.jboss.as.test.integration.domain.management.util.WildFlyManagedConfiguration;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.as.test.shared.util.AssumeTestGroupUtil;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.logging.Logger;
import org.jboss.modules.LocalModuleLoader;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.Resource;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * This test verifies it is possible to get the list of previous release extensions using the host-excludes definition
 * included in the current domain.xml.
 * <p>
 * The test fails if it finds a missing extension in the excluded-extensions section, there is a host-exclude name
 * in domain.xml undefined in this test or we are excluding more extensions than the necessary.
 * <p>
 * It also maintains the configuration of the current release, see ExtensionConf.CURRENT. When an extension is added or
 * removed on the current release, that extension must be tracked down on the ExtensionConf.CURRENT object. Once the
 * current release moves to the next mayor, if the ExtensionConf.CURRENT has extensions added or removed during the
 * current development cycle, the test will fail, forcing us to create new ExtensionConf objects for each previous
 * releases not defined in the test and point out ExtensionConf.CURRENT to the new current release without any
 * additional / removed extensions.
 *
 * @author Yeray Borges
 */
public class HostExcludesTestCase extends BuildConfigurationTestBase {

    private static DomainLifecycleUtil primaryUtils;
    private static DomainClient primaryClient;
    private static WildFlyManagedConfiguration primaryConfig;
    private static final  BiFunction<Set<String>, Set<String>, Set<String>> diff = (a, b) -> a.stream().filter(e -> !b.contains(e)).collect(Collectors.toSet());
    private final boolean isFullDistribution = AssumeTestGroupUtil.isFullDistribution();
    private final boolean isPreviewGalleonPack = AssumeTestGroupUtil.isWildFlyPreview();

    private static final String MAJOR = "30.";

    /**
     * Maintains the list of expected extensions for each host-exclude name for previous releases.
     * Each enum entry represents the list of extensions that are available on the excluded host.
     * It assumes that the hosts builds are always full builds, including the MP extensions if they exist.
     * This must be corrected on each new host-exclude id added on the current release.
     */
    private enum ExtensionConf {
        WILDFLY_10_0("WildFly10.0", null, Arrays.asList(
                "org.jboss.as.appclient",
                "org.jboss.as.clustering.infinispan",
                "org.jboss.as.clustering.jgroups",
                "org.jboss.as.cmp",
                "org.jboss.as.configadmin",
                "org.jboss.as.connector",
                "org.jboss.as.deployment-scanner",
                "org.jboss.as.ee",
                "org.jboss.as.ejb3",
                "org.jboss.as.jacorb",
                "org.jboss.as.jaxrs",
                "org.jboss.as.jaxr",
                "org.jboss.as.jdr",
                "org.jboss.as.jmx",
                "org.jboss.as.jpa",
                "org.jboss.as.jsf",
                "org.jboss.as.jsr77",
                "org.jboss.as.logging",
                "org.jboss.as.mail",
                "org.jboss.as.messaging",
                "org.jboss.as.modcluster",
                "org.jboss.as.naming",
                "org.jboss.as.pojo",
                "org.jboss.as.remoting",
                "org.jboss.as.sar",
                "org.jboss.as.security",
                "org.jboss.as.threads",
                "org.jboss.as.transactions",
                "org.jboss.as.web",
                "org.jboss.as.webservices",
                "org.jboss.as.weld",
                "org.jboss.as.xts",
                "org.keycloak.keycloak-adapter-subsystem",
                "org.wildfly.extension.batch.jberet",
                "org.wildfly.extension.bean-validation",
                "org.wildfly.extension.clustering.singleton",
                "org.wildfly.extension.io",
                "org.wildfly.extension.messaging-activemq",
                "org.wildfly.extension.mod_cluster",
                "org.wildfly.extension.picketlink",
                "org.wildfly.extension.request-controller",
                "org.wildfly.extension.rts",
                "org.wildfly.extension.security.manager",
                "org.wildfly.extension.undertow",
                "org.wildfly.iiop-openjdk"
        ), false),
        WILDFLY_10_1("WildFly10.1", WILDFLY_10_0, false),
        WILDFLY_11_0("WildFly11.0", WILDFLY_10_1, Arrays.asList(
                "org.wildfly.extension.core-management",
                "org.wildfly.extension.discovery",
                "org.wildfly.extension.elytron"
        ), false),
        WILDFLY_12_0("WildFly12.0", WILDFLY_11_0, false),
        WILDFLY_13_0("WildFly13.0", WILDFLY_12_0, List.of(
                "org.wildfly.extension.ee-security"
        ), false),
        WILDFLY_14_0("WildFly14.0", WILDFLY_13_0, Arrays.asList(
                "org.wildfly.extension.datasources-agroal",
                "org.wildfly.extension.microprofile.config-smallrye",
                "org.wildfly.extension.microprofile.health-smallrye",
                "org.wildfly.extension.microprofile.opentracing-smallrye"
        ), false),
        WILDFLY_15_0("WildFly15.0", WILDFLY_14_0, List.of(
                "org.wildfly.extension.microprofile.metrics-smallrye"
        ), false),
        WILDFLY_16_0("WildFly16.0", WILDFLY_15_0, List.of(
                // This extension was added in WF17, however we add it here because WF16/WF17/WF18 use the same management
                // kernel API, which is 10.0.0. Adding a host-exclusion for this extension on WF16 could affect to WF17/WF18
                // We decided to add the host-exclusion only for WF15 and below. It means potentially a DC running on WF17
                // with an WF16 as secondary will not exclude this extension. It is not a problem at all since mixed domains in
                // WildFly is not supported.
                "org.wildfly.extension.clustering.web"
        ), false),
        WILDFLY_17_0("WildFly17.0", WILDFLY_16_0, false),
        WILDFLY_18_0("WildFly18.0", WILDFLY_17_0, false),
        WILDFLY_19_0("WildFly19.0", WILDFLY_18_0, Arrays.asList(
                "org.wildfly.extension.microprofile.fault-tolerance-smallrye",
                "org.wildfly.extension.microprofile.jwt-smallrye",
                "org.wildfly.extension.microprofile.openapi-smallrye"
        ), false),
        WILDFLY_20_0("WildFly20.0", WILDFLY_19_0, false),
        WILDFLY_21_0("WildFly21.0", WILDFLY_20_0, false),
        WILDFLY_22_0("WildFly22.0", WILDFLY_21_0, Arrays.asList(
                "org.wildfly.extension.health",
                "org.wildfly.extension.metrics"
        ), false),
        WILDFLY_23_0("WildFly23.0", WILDFLY_22_0, Arrays.asList(
                "org.wildfly.extension.microprofile.reactive-messaging-smallrye",
                "org.wildfly.extension.microprofile.reactive-streams-operators-smallrye"
        ), false),
        WILDFLY_24_0("WildFly24.0", WILDFLY_23_0, true),
        WILDFLY_25_0("WildFly25.0", WILDFLY_24_0, Arrays.asList(
                "org.wildfly.extension.elytron-oidc-client",
                "org.wildfly.extension.opentelemetry"
        ), true),
        WILDFLY_26_0("WildFly26.0", WILDFLY_25_0, null, Arrays.asList(
                "org.jboss.as.cmp",
                "org.jboss.as.jaxr",
                "org.jboss.as.configadmin"
        ), true),
        WILDFLY_27_0("WildFly27.0", WILDFLY_26_0, Arrays.asList(
                "org.wildfly.extension.clustering.ejb",
                "org.wildfly.extension.datasources-agroal"
        ), true),
        WILDFLY_28_0("WildFly28.0", WILDFLY_27_0, Arrays.asList(
                "org.wildfly.extension.micrometer",
                "org.wildfly.extension.microprofile.lra-coordinator",
                "org.wildfly.extension.microprofile.lra-participant",
                "org.wildfly.extension.microprofile.telemetry"
        ), true),
        WILDFLY_29_0("WildFly29.0", WILDFLY_28_0, List.of(), List.of(
                "org.jboss.as.jacorb",
                "org.jboss.as.messaging",
                "org.jboss.as.web"
                ), true),
        CURRENT(MAJOR, WILDFLY_29_0, getCurrentAddedExtensions(), getCurrentRemovedExtensions(), true);

        private static List<String> getCurrentAddedExtensions() {
            // If an extension is added to this list, also check if it is supplied only by wildfly-galleon-pack. If so, add it also
            // to the internal mpExtensions Set defined on this class.
            // Don't add here extensions supplied only by the wildfly-preview-feature-pack because we are not tracking different releases
            // of wildfly preview. In such a case, add them to previewExtensions set defined below.
            return List.of();
        }
        private static List<String> getCurrentRemovedExtensions() {
            // TODO If we decide to remove these modules from WFP, uncomment this.
            // See https://issues.redhat.com/browse/WFLY-16686
            /*
            if (AssumeTestGroupUtil.isWildFlyPreview()) {
                return Arrays.asList(
                        "org.jboss.as.jsr77",
                        "org.wildfly.extension.picketlink",
                        "org.jboss.as.security"
                        );
            }
            */
            return List.of();
        }

        private final String name;
        private final Set<String> extensions = new HashSet<>();
        private final Set<String> removed = new HashSet<>();
        private static final Map<String, ExtensionConf> MAP;
        private final boolean modified;
        private final boolean supported;

        // List of extensions added by the wildfly-galleon-pack
        private final Set<String> mpExtensions = new HashSet<>(Arrays.asList(
                "org.wildfly.extension.micrometer",
                "org.wildfly.extension.microprofile.config-smallrye",
                "org.wildfly.extension.microprofile.health-smallrye",
                "org.wildfly.extension.microprofile.metrics-smallrye",
                "org.wildfly.extension.microprofile.fault-tolerance-smallrye",
                "org.wildfly.extension.microprofile.jwt-smallrye",
                "org.wildfly.extension.microprofile.openapi-smallrye",
                "org.wildfly.extension.microprofile.reactive-messaging-smallrye",
                "org.wildfly.extension.microprofile.reactive-streams-operators-smallrye",
                "org.wildfly.extension.microprofile.lra-coordinator",
                "org.wildfly.extension.microprofile.lra-participant",
                "org.wildfly.extension.microprofile.telemetry"
        ));

        // List of extensions added only by the WildFly Preview
        // We do not track changes between different versions of WildFly Preview.
        // From the point of view of this test, all extensions added in WildFly Preview are always extensions
        // added in the latest release of WildFly Preview. It is out of the scope of Host Exclusion test
        // to compute on which WildFly Preview was added such a new extension and track the Host Exclusions between
        // different WildFly Preview releases.
        private final Set<String> previewExtensions = new HashSet<>(Arrays.asList(
        ));

        ExtensionConf(String name, ExtensionConf parent, boolean supported) {
            this(name, parent, null, null, supported);
        }

        ExtensionConf(String name, ExtensionConf parent, List<String> addedExtensions, boolean supported) {
            this(name, parent, addedExtensions, null, supported);
        }

        /**
         * Main constructor
         *  @param name Host exclude name to define
         * @param parent A parent extension definition
         * @param addedExtensions Extensions added on the server release referred by this host exclude name
         * @param removedExtensions Extensions removed on the server release referred by this host exclude name
         * @param supported whether the given release is supported as a secondary host in a mixed domain
         */
        ExtensionConf(String name, ExtensionConf parent, List<String> addedExtensions, List<String> removedExtensions, boolean supported) {
            this.name = name;
            this.modified = (addedExtensions != null && !addedExtensions.isEmpty()) || (removedExtensions != null && !removedExtensions.isEmpty());
            if (addedExtensions != null) {
                this.extensions.addAll(addedExtensions);
            }
            if (parent != null) {
                this.extensions.addAll(parent.extensions);
                this.removed.addAll(parent.removed);
            }
            if (removedExtensions != null) {
                this.extensions.removeAll(removedExtensions);
                this.removed.addAll(removedExtensions);
            }
            this.supported = supported;
        }

        static {
            final Map<String, ExtensionConf> map = new HashMap<>();
            for (ExtensionConf element : values()) {
                final String name = element.name;
                if (name != null) map.put(name, element);
            }
            MAP = map;
        }

        public static ExtensionConf forName(String name) {
            return MAP.get(name);
        }

        public Set<String> getRemovedExtensions() {
            return removed;
        }

        public Set<String> getExtensions(boolean isFullDistribution, boolean isPreview) {
            if (this.name.equals(MAJOR) && isPreview) {
                this.extensions.addAll(previewExtensions);
            }
            if (!isFullDistribution && !isPreview) {
                return diff.apply(extensions, mpExtensions);
            }
            return extensions;
        }

        public boolean isModified() {
            return modified;
        }

        public boolean isSupported() {
            return supported;
        }

        public String getName() {
            return name;
        }
    }

    @BeforeClass
    public static void setUp() throws IOException {
        primaryConfig = createConfiguration("domain.xml", "host-primary.xml", HostExcludesTestCase.class.getSimpleName());
        primaryUtils = new DomainLifecycleUtil(primaryConfig);
        primaryUtils.start();
        primaryClient = primaryUtils.getDomainClient();
    }

    @AfterClass
    public static void tearDown() {
        if (primaryUtils != null) {
            primaryUtils.stop();
        }
    }

    @Test
    public void testHostExcludes() throws IOException, MgmtOperationException {
        Set<String> availableExtensions = retrieveAvailableExtensions();

        ModelNode op = Util.getEmptyOperation(READ_CHILDREN_RESOURCES_OPERATION, null);
        op.get(CHILD_TYPE).set(EXTENSION);

        ModelNode result = DomainTestUtils.executeForResult(op, primaryClient);

        Set<String> currentExtensions = new HashSet<>();
        for (Property prop : result.asPropertyList()) {
            currentExtensions.add(prop.getName());
        }

        //Check we are able to retrieve at minimum all the extensions defined for the current server
        if (!availableExtensions.containsAll(currentExtensions)) {
            currentExtensions.removeAll(availableExtensions);
            fail(String.format("The following extensions defined in domain.xml cannot be retrieved by this test %s . " +
                    "It could lead in a false negative test result, check HostExcludesTestCase.retrieveAvailableExtensions method", currentExtensions));
        }

        // Check that the list of all available extensions is in the ExtensionConf.CURRENT configuration
        Set<String> current = ExtensionConf.forName(MAJOR).getExtensions(isFullDistribution, isPreviewGalleonPack);
        if (!current.equals(availableExtensions)) {
            Set<String> extensionsAdded = diff.apply(current, availableExtensions);
            Set<String> extensionsRemoved = diff.apply(availableExtensions, current);
            fail(String.format("The following extensions %s have been removed on the current release. Remove them on ExtensionConf.CURRENT object defined in this test. " +
                    "The following extensions %s have been added on the current release. Add them to ExtensionConf.CURRENT object defined in this test.", extensionsAdded, extensionsRemoved));
        }

        // If the ExtensionConf.CURRENT has extensions removed / added and the version it represents no longer
        // points out to the actual, then we need to create new ExtensionConf(s) for each of the previous releases
        // not included in this test.
        if (ExtensionConf.CURRENT.isModified()) {
            op = Util.getReadAttributeOperation(null, "product-version");
            result = DomainTestUtils.executeForResult(op, primaryClient);
            if (!result.asString().startsWith(ExtensionConf.CURRENT.getName())) {
                fail(String.format("The ExtensionConf.CURRENT has extensions added or removed but it no longer points to the current release. " +
                        "Modify this test adding new ExtensionConf enums for each previous releases undefined in this test by using the list of extensions added or removed on ExtensionConf.CURRENT." +
                        "Then remove all the extensions from ExtensionConf.CURRENT enum and correct the MAJOR number accordingly to point out to the current release."));
            }
        }

        op = Util.getEmptyOperation(READ_CHILDREN_RESOURCES_OPERATION, null);
        op.get(CHILD_TYPE).set(HOST_EXCLUDE);

        result = DomainTestUtils.executeForResult(op, primaryClient);

        Set<String> processedExclusionsIds = new HashSet<>();
        for (Property prop : result.asPropertyList()) {
            String name = prop.getName();

            List<String> excludedExtensions = prop.getValue().get(EXCLUDED_EXTENSIONS)
                    .asListOrEmpty()
                    .stream()
                    .map(p -> p.asString())
                    .collect(Collectors.toList());

            //check duplicated extensions
            Assert.assertTrue(String.format (
                            "There are duplicated extensions declared for %s host-exclude", name),
                    excludedExtensions.size() == new HashSet<>(excludedExtensions).size()
            );

            //check we have defined the current host-exclude configuration in the test
            ExtensionConf confPrevRelease = ExtensionConf.forName(name);
            Assert.assertNotNull(String.format(
                            "This host-exclude name is not defined in this test: %s", name),
                    confPrevRelease);

            //check that available extensions - excluded extensions = expected extensions in a previous release - removed
            Set<String> expectedExtensions = ExtensionConf.forName(name).getExtensions(isFullDistribution, isPreviewGalleonPack);
            expectedExtensions.removeAll(ExtensionConf.forName(MAJOR).getRemovedExtensions());

            Set<String> extensionsUnderTest = new HashSet<>(availableExtensions);
            extensionsUnderTest.removeAll(excludedExtensions);

            if (expectedExtensions.size() > extensionsUnderTest.size()) {
                expectedExtensions.removeAll(extensionsUnderTest);
                fail(String.format("These extensions are expected to be available after applying the %s host-exclude configuration to the extensions supplied by this server release: %s", name, expectedExtensions));
            }

            if ( extensionsUnderTest.size() != expectedExtensions.size() ){
                extensionsUnderTest.removeAll(expectedExtensions);
                fail(String.format("These extensions are missing on the %s host-exclude: %s", name, extensionsUnderTest));
            }

            processedExclusionsIds.add(name);
        }

        // Verifies all the exclusions Id added as configurations for this test are defined as host exclusions in the current server release
        for(ExtensionConf extensionConf : ExtensionConf.values()) {
            if (extensionConf != ExtensionConf.CURRENT && extensionConf.isSupported() && !processedExclusionsIds.contains(extensionConf.getName())) {
                Set<String> extensions = extensionConf.getExtensions(isFullDistribution, isPreviewGalleonPack);
                extensions.removeAll(ExtensionConf.forName(MAJOR).getRemovedExtensions());
                if (!extensions.equals(availableExtensions)) {
                    fail(String.format("The %s exclusion id is not defined as host exclusion for the current release.", extensionConf.getName()));
                }
            }
        }
    }

    /**
     * Retrieve the list of all modules which export locally a resource that implements org.jboss.as.controller.Extension.
     * This list is considered the list of all available extensions that can be added to a server.
     *
     * It is assumed that the module which is added as an extension has the org.jboss.as.controller.Extension service as
     * a local resource.
     */
    private Set<String> retrieveAvailableExtensions() throws IOException {
        final Set<String> result = new HashSet<>();
        LocalModuleLoader ml = new LocalModuleLoader(getModuleRoots());
        Iterator<String> moduleNames = ml.iterateModules((String) null, true);
        while (moduleNames.hasNext()) {
            String moduleName = moduleNames.next();
            Module module;
            try {
                module = ml.loadModule(moduleName);
                List<Resource> resources = module.getClassLoader().loadResourceLocal("META-INF/services/org.jboss.as.controller.Extension");
                if (!resources.isEmpty()) {
                    result.add(moduleName);
                }
            } catch (ModuleLoadException e) {
                Logger.getLogger(HostExcludesTestCase.class).warn("Failed to load module " + moduleName +
                        " to check if it is an extension", e);
            }
        }
        return result;
    }

    private static File[] getModuleRoots() throws IOException {
        Path layersRoot = Paths.get(primaryConfig.getModulePath()) .resolve("system").resolve("layers");
        DirectoryStream.Filter<Path> filter = entry -> {
            File f = entry.toFile();
            return f.isDirectory() && !f.isHidden();
        };
        List<File> result = new ArrayList<>();
        for (Path path : Files.newDirectoryStream(layersRoot, filter)) {
            result.add(path.toFile());
        }
        return result.toArray(new File[0]);
    }
}
