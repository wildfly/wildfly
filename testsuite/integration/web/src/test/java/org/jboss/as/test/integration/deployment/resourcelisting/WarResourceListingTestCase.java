/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.deployment.resourcelisting;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.common.WebInfLibClass;
import org.jboss.as.test.shared.ResourceListingUtils;
import org.jboss.logging.Logger;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@RunWith(Arquillian.class)
public class WarResourceListingTestCase {

    private static final Logger log = Logger.getLogger(WarResourceListingTestCase.class);
    private static final String jarLibName = "innerJarLibrary.jar";


    /**
     * @return war archive with different types of web.xml
     */
    @Deployment
    public static Archive<?> deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class);
        war.addClass(WarResourceListingTestCase.class);
        war.addClass(ResourceListingUtils.class);
        war.add(EmptyAsset.INSTANCE, "META-INF/properties/nested.properties");
        war.add(EmptyAsset.INSTANCE, "META-INF/example.txt");
        war.add(EmptyAsset.INSTANCE, "example2.txt");
        war.addAsResource(WarResourceListingTestCase.class.getPackage(), "TextFile1.txt", "TextFile1.txt");
        war.addAsWebInfResource(WarResourceListingTestCase.class.getPackage(), "web.xml", "web.xml");
        JavaArchive libJar = ShrinkWrap.create(JavaArchive.class, jarLibName);
        libJar.addClass(WebInfLibClass.class);
        war.addAsLibraries(libJar);
        return war;
    }

    @Test()
    public void testRecursiveResourceRetrieval() {
        log.trace("Test recursive listing of resources");
        doTestResourceRetrieval(true, "/");

    }

    @Test()
    public void testNonRecursiveResourceRetrieval() {
        log.trace("Test nonrecursive listing of resources");
        doTestResourceRetrieval(false, "/");
    }

    @Test()
    public void testRecursiveResourceRetrievalForSpecifiedRootDir() {
        log.trace("Test recursive listing of resources in specific directory");
        doTestResourceRetrieval(true, "/WEB-INF");
    }

    @Test()
    public void testNonRecursiveResourceRetrievalForSpecifiedRootDir() {
        log.trace("Test recursive listing of resources in specific directory");
        doTestResourceRetrieval(false, "/WEB-INF");
    }

    @Test()
    public void testDirectResourceRetrieval() {
        log.trace("Test accessing resources using getResource method");

        ModuleClassLoader classLoader = (ModuleClassLoader) getClass().getClassLoader();

        // checking that resource under META-INF is accessible
        URL manifestResource = classLoader.getResource("META-INF/example.txt");

        assertNotNull("Resource in META-INF should be accessible", manifestResource);

        // checking that resource under META-INF is accessible
        URL nestedManifestResource = classLoader.getResource("META-INF/properties/nested.properties");
        assertNotNull("Nested resource should be also accessible", nestedManifestResource);

        // checking that resource which is not under META-INF is not accessible
        URL nonManifestResource = classLoader.getResource("example2.txt");
        assertNull("Resource in the root of WAR shouldn't be accessible", nonManifestResource);
    }

    /**
     * Based on provided parameters it filters which resources should be available and which not and tests if the retrieved resources matches this list
     * @param recursive also a nested/recursive resources are counted
     * @param rootDir represents the filtering by root directory (only resources in the specified root dir are taken into account
     */
    private void doTestResourceRetrieval(boolean recursive, String rootDir) {
        ModuleClassLoader classLoader = (ModuleClassLoader) getClass().getClassLoader();

        List<String> resourcesInDeployment = getActualResourcesInWar(recursive, rootDir);

        List<String> foundResources = ResourceListingUtils.listResources(classLoader, rootDir, recursive);

        Collections.sort(foundResources);

        log.trace("List of expected resources:");
        for (String expectedResource : resourcesInDeployment) {
            log.trace(expectedResource);
        }
        log.trace("List of found resources: ");
        for (String foundResource : foundResources) {
            log.trace(foundResource);
        }

        Assert.assertArrayEquals("Not all resources from WAR archive are correctly listed", resourcesInDeployment.toArray(), foundResources.toArray());
    }

    /**
     * Returns all the resources in WAR archive
     *
     * @param recursive -- if even recursive resources (taken from rootDir) shall be provided
     * @param rootDir   -- can be used for getting resources only from specific rootDir
     * @return list of resources in WAR filtered based on specified arguments
     */
    public static List<String> getActualResourcesInWar(boolean recursive, String rootDir) {

        String[] resourcesInWar = new String[]{
                "META-INF/example.txt",
                "META-INF/MANIFEST.MF",
                // Added by WildFly Arquillian jmx-as7 protocol
                "META-INF/test-description.properties",
                "META-INF/properties/nested.properties",
                "TextFile1.txt",

        };
        List<String> actualResources = new ArrayList<String>(Arrays.asList(resourcesInWar));
        Class[] clazzes = new Class[] {
                WarResourceListingTestCase.class,
                ResourceListingUtils.class,
                WebInfLibClass.class
        };

        for (Class clazz : clazzes) {
            actualResources.add(ResourceListingUtils.classToPath(clazz));
        }

        ResourceListingUtils.filterResources(actualResources, rootDir, !recursive);

        Collections.sort(actualResources);
        return actualResources;
    }

}
