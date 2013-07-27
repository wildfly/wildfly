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

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.deployment.classloading.war.WebInfLibClass;
import org.jboss.logging.Logger;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.modules.Resource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@RunWith(Arquillian.class)
public class EarResourceListingTestCase {

    private static final Logger log = Logger.getLogger(EarResourceListingTestCase.class);
    private static final String innerWarArchiveName = "innerWarDeployment.jar";

    @Deployment
    public static Archive<?> deploy() {
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class);
        JavaArchive earLib = ShrinkWrap.create(JavaArchive.class);
        earLib.addClasses(EarResourceListingTestCase.class, ResourceListingUtils.class);
        earLib.addAsManifestResource(EmptyAsset.INSTANCE, "emptyJarLibResource.properties");
        earLib.addAsManifestResource(EmptyAsset.INSTANCE, "properties/nestedJarLib.properties");
        ear.addAsLibraries(earLib);

        WebArchive war = ShrinkWrap.create(WebArchive.class, innerWarArchiveName);
        war.addClass(TestA.class);
        war.add(EmptyAsset.INSTANCE, "META-INF/example.txt");
        war.add(EmptyAsset.INSTANCE, "META-INF/properties/nested.properties");
        war.add(EmptyAsset.INSTANCE, "example2.txt");
        war.addAsResource(EarResourceListingTestCase.class.getPackage(), "TextFile1.txt", "TextFile1.txt");
        war.addAsWebInfResource(EarResourceListingTestCase.class.getPackage(), "web.xml", "web.xml");
        JavaArchive libJar = ShrinkWrap.create(JavaArchive.class);
        libJar.addClass(WebInfLibClass.class);
        war.addAsLibraries(libJar);
        ear.addAsModules(libJar, war);
        ear.addAsManifestResource(EmptyAsset.INSTANCE, "MANIFEST.MF");
        ear.addAsResource(EmptyAsset.INSTANCE, "emptyEarResource");

        ear.addAsManifestResource(new StringAsset("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "\n" +
                "<application xmlns=\"http://java.sun.com/xml/ns/javaee\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                "   xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/application_6.xsd\" version=\"6\">\n" +
                "\n" +
                "  <module>\n" +
                "     <web>\n" +
                "       <web-uri>"+war.getName()+"</web-uri>\n" +
                "       <context-root>"+war.getName().replace(".war", "")+"</context-root>\n" +
                "     </web>\n" +
                "   </module>\n" +
                "\n" +
                "  <module>\n" +
                "     <java>"+libJar.getName()+"</java>\n" +
                "  </module>\n" +
                "\n" +
                "</application>"), "application.xml");

        ear.addAsManifestResource(new StringAsset("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<jboss-deployment-structure> \n" +
                "  <ear-subdeployments-isolated>false</ear-subdeployments-isolated>\n" +
                "  <sub-deployment name=\""+war.getName()+"\"/>\n" +
                "</jboss-deployment-structure>"), "jboss-deployment-structure.xml");

//        ear.as(org.jboss.shrinkwrap.api.exporter.ZipExporter.class).exportTo(new java.io.File("/tmp/" + ear.getName()), true);
        return ear;
    }

    @Test()
    public void testRecursiveEARResourceRetrieval() {
        log.info("Test non-recursive listing of resources in EAR deployment");
        doTestEARResourceRetrieval(true, "/");

    }

    @Test()
    public void testNonRecursiveEARResourceRetrieval() {
        log.info("Test recursive listing of resources in EAR deployment");
        doTestEARResourceRetrieval(false, "/");
    }

    @Test()
    public void testRecursiveEARResourceRetrievalForSpecifiedRootDir() {
        log.info("Test recursive listing of resources in EAR deployment for root dir /META-INF");
        doTestEARResourceRetrieval(true, "/META-INF");
    }

    @Test()
    public void testNonRecursiveEARResourceRetrievalForSpecifiedRootDir() {
        log.info("Test non-recursive listing of resources in EAR deployment for root dir /META-INF");
        doTestEARResourceRetrieval(false, "/META-INF");
    }

    private void doTestEARResourceRetrieval(boolean recursive, String rootDir) {
        ModuleClassLoader classLoader = (ModuleClassLoader) getClass().getClassLoader();
        List<String> foundResources = ResourceListingUtils.listResources(classLoader, rootDir, recursive);

        // only resources in EAR library should be listed
        List<String> resourcesInDeployment = new ArrayList<>();
        resourcesInDeployment.add(ResourceListingUtils.classToPath(EarResourceListingTestCase.class));
        resourcesInDeployment.add(ResourceListingUtils.classToPath(ResourceListingUtils.class));
        resourcesInDeployment.add("META-INF/emptyJarLibResource.properties");
        resourcesInDeployment.add("META-INF/properties/nestedJarLib.properties");

        ResourceListingUtils.filterResources(resourcesInDeployment, rootDir, !recursive);

        Collections.sort(foundResources);
        Collections.sort(resourcesInDeployment);

        log.info("List of expected resources:");
        for (String expectedResource : resourcesInDeployment) {
            log.info(expectedResource);
        }
        log.info("List of found resources: ");
        for (String foundResource : foundResources) {
            log.info(foundResource);
        }

        Assert.assertArrayEquals("Not all resources from EAR archive are correctly listed", resourcesInDeployment.toArray(), foundResources.toArray());
    }


}
