/*
 * Copyright 2019 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.test.integration.deployment.moduleslisting;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.LIST_MODULES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.domain.management.ModelDescriptionConstants.VERBOSE;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * List modules which are on deployment’s classpath
 * /deployment=application_war_ear_name:list-modules(verbose=false|true)
 * @author <a href="mailto:szhantem@redhat.com">Sultan Zhantemirov</a> (c) 2019 Red Hat, inc.
 */
@RunWith(Arquillian.class)
@RunAsClient
public class DeploymentModulesListTestCase {

    private static final String NODE_TYPE = "deployment";
    private static final String INNER_JAR_ARCHIVE_NAME = "inner-jar-lib.jar";
    private static final String EXAMPLE_MODULE_TO_EXCLUDE = "ibm.jdk";
    private static final String INNER_WAR_ARCHIVE_NAME = "list-modules.war";
    private static final String EAR_DEPLOYMENT_NAME = "list-modules-ear-test.ear";
    private static final String USER_MODULE = "org.hibernate";
    private static final String CUSTOM_SLOT = "6.0";

    @ContainerResource
    private static ManagementClient managementClient;

    @Deployment
    public static Archive<?> deploy() {

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, EAR_DEPLOYMENT_NAME);
        JavaArchive earLib = ShrinkWrap.create(JavaArchive.class);
        earLib.addClass(DeploymentModulesListTestCase.class);
        earLib.addAsManifestResource(EmptyAsset.INSTANCE, "emptyJarLibResource.properties");
        earLib.addAsManifestResource(EmptyAsset.INSTANCE, "properties/nestedJarLib.properties");
        ear.addAsLibraries(earLib);

        WebArchive war = ShrinkWrap.create(WebArchive.class, INNER_WAR_ARCHIVE_NAME);
        war.addClass(DeploymentModulesListTestCase.class);
        war.add(EmptyAsset.INSTANCE, "META-INF/example.txt");
        war.add(EmptyAsset.INSTANCE, "META-INF/properties/nested.properties");

        JavaArchive libJar = ShrinkWrap.create(JavaArchive.class, INNER_JAR_ARCHIVE_NAME);
        libJar.addClass(DeploymentModulesListTestCase.class);

        war.addAsLibraries(libJar);
        ear.addAsModules(libJar, war);
        ear.add(new StringAsset(prepareJBossEarDeploymentStructure()),"/META-INF/jboss-deployment-structure.xml");
        ear.addAsResource(EmptyAsset.INSTANCE, "emptyEarResource");

        return ear;
    }

    @Test
    public void listEarModulesNonVerbose() throws Throwable {
        this.listEarModules(false);
    }

    @Test
    public void listEarModulesVerbose() throws Throwable {
        this.listEarModules(true);
    }

    private void listEarModules(boolean verbose) throws Throwable {
        final ModelNode operation = new ModelNode();
        operation.get(OP).set(LIST_MODULES);
        operation.get(OP_ADDR).set(PathAddress.parseCLIStyleAddress("/" + NODE_TYPE + "=" + EAR_DEPLOYMENT_NAME + "/subdeployment=" + INNER_WAR_ARCHIVE_NAME).toModelNode());

        if (verbose) {
            operation.get(VERBOSE).set(Boolean.TRUE.toString());
        }

        final ModelNode operationResult = managementClient.getControllerClient().execute(operation);

        // check whether the operation was successful
        assertTrue(Operations.isSuccessfulOutcome(operationResult));

        // check standard/detailed output
        if (!verbose) {
            // check whether modules are ordered alphabetically
            assertTrue(isOrderedAlphabetically(operationResult));
            // check module presence
            assertTrue(checkModulesListPresence(operationResult, "deployment." + EAR_DEPLOYMENT_NAME));
            // check user defined module with custom slot
            assertTrue(checkModulesListPresence(operationResult, USER_MODULE + ":" + CUSTOM_SLOT));
            // check module absence
            assertFalse(checkModulesListPresence(operationResult, EXAMPLE_MODULE_TO_EXCLUDE));
            // check system and user dependencies presence
            assertTrue(checkModulesListNonEmptiness(operationResult));
        } else {
            // check other attributes presence only
            assertTrue(checkDetailedOutput(operationResult));
        }
    }

    /**
     * Checks given module presence in the "list-modules" command output.
     * @param operationResult - operation object to extract result from
     * @param moduleName - name of the module expected to be present
     * @return true if given module is present in any (system, local, user) list of module dependencies
     */
    private boolean checkModulesListPresence(ModelNode operationResult, String moduleName) {
        boolean isModulePresent = false;

        for (Property dependenciesGroup : operationResult.get(RESULT).asPropertyList()) {
            List<Property> list = dependenciesGroup
                    .getValue()
                    .asPropertyList()
                    .stream()
                    .filter(dependency -> dependency.getValue().asString().equalsIgnoreCase(moduleName))
                    .collect(Collectors.toList());
            if (list.size() > 0) isModulePresent = true;
        }

        return isModulePresent;
    }

    /**
     * Checks whether the module output information contains at least one of the "optional", "export" and "import-services" attributes.
     * @param operationResult - operation object to extract result from
     * @return true if detailed output is present
     */
    private boolean checkDetailedOutput(ModelNode operationResult) {
        boolean isDetailedOutput = false;

        for (Property dependenciesGroup : operationResult.get(RESULT).asPropertyList()) {
            for (ModelNode dependency : dependenciesGroup.getValue().asList()) {
                isDetailedOutput = dependency
                        .asPropertyList()
                        .stream()
                        .map(Property::getName)
                        .anyMatch(attributeName ->
                                attributeName.equalsIgnoreCase("optional") ||
                                        attributeName.equalsIgnoreCase("import-services") ||
                                        attributeName.equalsIgnoreCase("export")
                        );
            }
        }

        return isDetailedOutput;
    }

    /**
     * Checks whether both system and user dependencies lists are not empty.
     * @param operationResult - operation object to extract result from
     * @return true if both system and user dependencies lists are not empty
     */
    private boolean checkModulesListNonEmptiness(ModelNode operationResult) {
        boolean isSystemDependenciesPresent = false;
        boolean isUserDependenciesPresent = false;
        for (Property dependenciesGroup : operationResult.get(RESULT).asPropertyList()) {
            if (dependenciesGroup.getName().equalsIgnoreCase("system-dependencies")) {
                // check system dependencies list non-emptiness
                isSystemDependenciesPresent = !dependenciesGroup.getValue().asPropertyList().isEmpty();
            }
            if (dependenciesGroup.getName().equalsIgnoreCase("user-dependencies")) {
                // check system dependencies list non-emptiness
                isUserDependenciesPresent = !dependenciesGroup.getValue().asPropertyList().isEmpty();
            }
        }

        return isSystemDependenciesPresent && isUserDependenciesPresent;
    }

    private static String prepareJBossEarDeploymentStructure() {
        return "<jboss-deployment-structure>\n" +
                "  <deployment>\n" +
                "       <exclusions>\n" +
                "           <module name=\"" + EXAMPLE_MODULE_TO_EXCLUDE + "\"/>\n" +
                "       </exclusions>\n" +
                "  </deployment>\n" +
                "  <sub-deployment name=\"" + INNER_WAR_ARCHIVE_NAME + "\">\n" +
                "       <exclusions>\n" +
                "           <module name=\"" + EXAMPLE_MODULE_TO_EXCLUDE + "\"/>\n" +
                "       </exclusions>\n" +
                "       <dependencies>\n" +
                "           <module name=\"" + USER_MODULE + "\" slot=\"" + CUSTOM_SLOT + "\"/>\n" +
                "       </dependencies>\n" +
                "   </sub-deployment>\n" +
                "</jboss-deployment-structure>\n";
    }

    private boolean isOrderedAlphabetically(ModelNode operationResult) {
        List<String> dependenciesList;
        List<Property> list;
        boolean isSorted = true;

        for (Property dependenciesGroup : operationResult.get(RESULT).asPropertyList()) {
            dependenciesList =  new ArrayList<>();
            list = dependenciesGroup.getValue().asPropertyList();
            for (Property dependency : list) {
                dependenciesList.add(dependency.getValue().asString());
            }
            isSorted = isSorted(dependenciesList);
        }

        return isSorted;
    }

    private boolean isSorted(List<String> list) {
        boolean sorted = true;

        for (int i = 1; i < list.size(); i++) {
            if (list.get(i - 1).compareTo(list.get(i)) > 0) {
                sorted = false;
            }
        }

        return sorted;
    }
}
