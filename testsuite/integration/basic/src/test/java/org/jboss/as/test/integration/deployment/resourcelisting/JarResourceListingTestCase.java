/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.test.integration.deployment.resourcelisting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.shared.ResourceListingUtils;
import org.jboss.logging.Logger;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class JarResourceListingTestCase {

    private static final Logger log = Logger.getLogger(JarResourceListingTestCase.class);

    @Deployment
    public static Archive<?> deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class);
        jar.addClasses(JarResourceListingTestCase.class, ResourceListingUtils.class);
        jar.add(EmptyAsset.INSTANCE, "META-INF/example.txt");
        jar.add(EmptyAsset.INSTANCE, "META-INF/properties/nested.properties");
        jar.addAsManifestResource(EmptyAsset.INSTANCE, "MANIFEST.MF"); // shrink wrapper creates it in default, this way it is more clear that it's there
        jar.add(EmptyAsset.INSTANCE, "example2.txt");
        jar.addAsResource(JarResourceListingTestCase.class.getPackage(), "TextFile1.txt", "TextFile1.txt");
        return jar;
    }


    @Test()
    public void testRecursiveJARResourceRetrieval() {
        log.trace("Test non-recursive listing of resources in JAR deployment");
        doTestJARResourceRetrieval(true, "/");

    }

    @Test()
    public void testNonRecursiveJARResourceRetrieval() {
        log.trace("Test recursive listing of resources in JAR deployment");
        doTestJARResourceRetrieval(false, "/");
    }

    @Test()
    public void testRecursiveJARResourceRetrievalForSpecifiedRootDir() {
        log.trace("Test recursive listing of resources in JAR deployment for root dir /META-INF");
        doTestJARResourceRetrieval(true, "/META-INF");
    }

    @Test()
    public void testNonRecursiveJARResourceRetrievalForSpecifiedRootDir() {
        log.trace("Test non-recursive listing of resources in JAR deployment for root dir /META-INF");
        doTestJARResourceRetrieval(false, "/META-INF");
    }

    private void doTestJARResourceRetrieval(boolean recursive, String rootDir) {
        ModuleClassLoader classLoader = (ModuleClassLoader) getClass().getClassLoader();
        List<String> foundResources = ResourceListingUtils.listResources(classLoader, rootDir, recursive);

        // only resources in JAR library should be listed
        List<String> resourcesInDeployment = new ArrayList<>();
        resourcesInDeployment.add(ResourceListingUtils.classToPath(JarResourceListingTestCase.class));
        resourcesInDeployment.add(ResourceListingUtils.classToPath(ResourceListingUtils.class));
        resourcesInDeployment.add("META-INF/example.txt");
        resourcesInDeployment.add("META-INF/properties/nested.properties");
        resourcesInDeployment.add("META-INF/MANIFEST.MF");
        resourcesInDeployment.add("example2.txt");
        resourcesInDeployment.add("TextFile1.txt");

        ResourceListingUtils.filterResources(resourcesInDeployment, rootDir, !recursive);

        Collections.sort(foundResources);
        Collections.sort(resourcesInDeployment);

        log.trace("List of expected resources:");
        for (String expectedResource : resourcesInDeployment) {
            log.trace(expectedResource);
        }
        log.trace("List of found resources: ");
        for (String foundResource : foundResources) {
            log.trace(foundResource);
        }

        Assert.assertArrayEquals("Not all resources from JAR archive are correctly listed", resourcesInDeployment.toArray(), foundResources.toArray());
    }

}
