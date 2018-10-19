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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.jboss.as.controller.client.helpers.domain.DomainClient;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.domain.management.util.DomainLifecycleUtil;
import org.jboss.as.test.integration.domain.management.util.DomainTestUtils;
import org.jboss.as.test.integration.domain.management.util.WildFlyManagedConfiguration;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.modules.LocalModuleLoader;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.jboss.modules.Resource;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * This test verifies it is possible to get the list of previous release extensions using the host-excludes definition
 * included in the current domain.xml.
 * <p>
 *
 * The test fails if it finds a missing extension in the excluded-extensions section, there is a host-exclude name
 * in domain.xml undefined in this test or we are excluding more extensions than the necessary.
 * <p>
 *
 * @author Yeray Borges
 */
public class HostExcludesTestCase extends BuildConfigurationTestBase {

    private static DomainLifecycleUtil masterUtils;
    private static DomainClient masterClient;
    private static WildFlyManagedConfiguration masterConfig;

    /**
     * Maintains the list of expected extensions for each host-exclude name
     * for previous releases. This must be corrected on each new host-exclude id added
     * on the current release.
     */
    private enum ExtensionConf {
        WILDFLY_10_0("WildFly10.0", Arrays.asList(
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
        )),
        WILDFLY_10_1("WildFly10.1", WILDFLY_10_0),
        WILDFLY_11_0("WildFly11.0", WILDFLY_10_1, Arrays.asList(
                "org.wildfly.extension.core-management",
                "org.wildfly.extension.discovery",
                "org.wildfly.extension.elytron"
        )),
        WILDFLY_12_0("WildFly12.0", WILDFLY_11_0),
        WILDFLY_13_0("WildFly13.0", WILDFLY_12_0, Arrays.asList(
                "org.wildfly.extension.ee-security"
        )),
        WILDFLY_14_0("WildFly14.0", WILDFLY_13_0, Arrays.asList(
                "org.wildfly.extension.datasources-agroal",
                "org.wildfly.extension.microprofile.config-smallrye",
                "org.wildfly.extension.microprofile.health-smallrye",
                "org.wildfly.extension.microprofile.opentracing-smallrye"
        )),


        EAP62("EAP62", Arrays.asList(
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
                "org.jboss.as.jaxr",
                "org.jboss.as.jaxrs",
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
                // This module was added in EAP70, but we move it to the EAP62 extension list to allow the test passing
                // without adding it to the host-exclude section. We don't want to expose it in the host-exclude.
                "org.wildfly.extension.mod_cluster"
        )),
        EAP63("EAP63", EAP62, Arrays.asList(
                "org.wildfly.extension.picketlink"
        )),
        EAP64("EAP64", EAP63),
        EAP64z("EAP64z", EAP64),
        EAP70("EAP70", EAP64z, Arrays.asList(
                "org.wildfly.extension.batch.jberet",
                "org.wildfly.extension.bean-validation",
                "org.wildfly.extension.clustering.singleton",
                "org.wildfly.extension.io",
                "org.wildfly.extension.messaging-activemq",
                "org.wildfly.extension.request-controller",
                "org.wildfly.extension.rts",
                "org.wildfly.extension.security.manager",
                "org.wildfly.extension.undertow",
                "org.wildfly.iiop-openjdk"
        )),
        EAP71("EAP71", EAP70, Arrays.asList(
                "org.wildfly.extension.core-management",
                "org.wildfly.extension.discovery",
                "org.wildfly.extension.elytron"
        ));

        private final String name;
        private final Set<String> extensions = new HashSet<>();
        private static final Map<String, ExtensionConf> MAP;

        ExtensionConf(String name, List<String> addedExtensions) {
            this(name, null, addedExtensions, null);
        }

        ExtensionConf(String name, ExtensionConf parent) {
            this(name, parent, null, null);
        }

        ExtensionConf(String name, ExtensionConf parent, List<String> addedExtensions) {
            this(name, parent, addedExtensions, null);
        }

        /**
         * Main constructor
         *
         * @param name Host exclude name to define
         * @param parent A parent extension definition
         * @param addedExtensions Extensions added on the server release referred by this host exclude name
         * @param removedExtensions Extensions added on the server release referred by this host exclude name
         */
        ExtensionConf(String name, ExtensionConf parent, List<String> addedExtensions, List<String> removedExtensions) {
            this.name = name;
            if (addedExtensions != null) {
                this.extensions.addAll(addedExtensions);
            }
            if (parent != null && parent.extensions != null) {
                this.extensions.addAll(parent.extensions);
            }
            if (removedExtensions != null) {
                this.extensions.removeAll(removedExtensions);
            }
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

        public Set<String> getExtensions() {
            return extensions;
        }

        public static Set<String> getNames() {
            return MAP.keySet();
        }
    }

    @BeforeClass
    public static void setUp() throws IOException {
        masterConfig = createConfiguration("domain.xml", "host-master.xml", HostExcludesTestCase.class.getSimpleName());
        masterUtils = new DomainLifecycleUtil(masterConfig);
        masterUtils.start();
        masterClient = masterUtils.getDomainClient();
    }

    @AfterClass
    public static void tearDown() {
        if (masterUtils != null) {
            masterUtils.stop();
        }
    }

    @Test
    public void testHostExcludes() throws IOException, MgmtOperationException, ModuleLoadException {
        final Path moduleDir = Paths.get(masterConfig.getModulePath()).resolve("system").resolve("layers").resolve("base");
        LocalModuleLoader ml = new LocalModuleLoader(new File[]{moduleDir.normalize().toFile()});

        Set<String> availableExtensions = retrieveAvailableExtensions(ml);

        ModelNode op = Util.getEmptyOperation(READ_CHILDREN_RESOURCES_OPERATION, null);
        op.get(CHILD_TYPE).set(EXTENSION);

        ModelNode result = DomainTestUtils.executeForResult(op, masterClient);

        Set<String> currentExtensions = new HashSet<>();
        for (Property prop : result.asPropertyList()) {
            currentExtensions.add(prop.getName());
        }

        //Check we are able to retrieve all current extensions defined for the server
        if (!availableExtensions.containsAll(currentExtensions)){
            currentExtensions.removeAll(availableExtensions);
            fail(String.format ("The following extensions defined in domain.xml cannot be retrieve by this test %s . " +
                    "It could lead in a false negative test result, check HostExcludesTestCase.retrieveAvailableExtensions method",currentExtensions));
        }

        op = Util.getEmptyOperation(READ_CHILDREN_RESOURCES_OPERATION, null);
        op.get(CHILD_TYPE).set(HOST_EXCLUDE);

        result = DomainTestUtils.executeForResult(op, masterClient);

        for (Property prop : result.asPropertyList()) {
            String name = prop.getName();

            ModelNode value = prop.getValue();
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

            //check that available extensions - excluded extensions = extensions in a previous release.
            Set<String> expectedExtensions = ExtensionConf.forName(name).getExtensions();

            Set<String> extensionsUnderTest = new HashSet<>(availableExtensions);
            extensionsUnderTest.removeAll(excludedExtensions);

            if (expectedExtensions.size() > extensionsUnderTest.size()) {
                expectedExtensions.removeAll(extensionsUnderTest);
                fail(String.format("These exclusions are not required for %s host-exclude: %s", name, expectedExtensions));
            }

            if ( extensionsUnderTest.size() != expectedExtensions.size() ){
                extensionsUnderTest.removeAll(expectedExtensions);
                fail(String.format("Found missing extensions for %s host-exclude: %s", name, extensionsUnderTest));
            }
        }
    }

    /**
     * Retrieve the list of all modules which export locally a resource that implements a org.jboss.as.controller.Extension.
     * This list is considered the list of all available extensions that can be added to a server.
     *
     * It is assumed that the module which is added as an extension has the org.jboss.as.controller.Extension service as
     * a local resource.
     *
     * @param ml
     * @throws ModuleLoadException
     */
    private Set<String> retrieveAvailableExtensions(ModuleLoader ml) throws ModuleLoadException {
        Set<String> result = new HashSet<>();
        Iterator<String> moduleNames = ml.iterateModules((String) null, true);
        while (moduleNames.hasNext()) {
            String moduleName = moduleNames.next();
            Module module = ml.loadModule(moduleName);
            List<Resource> resources = module.getClassLoader().loadResourceLocal("META-INF/services/org.jboss.as.controller.Extension");
            if (!resources.isEmpty()) {
                result.add(moduleName);
            }
        }

        return result;
    }
}
