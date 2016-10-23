/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.weld.modules;

import java.io.File;
import java.net.URL;

import javax.ejb.EJB;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.module.util.TestModule;
import org.jboss.as.test.shared.ModuleUtils;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests the deployment of simple enterprise application depending on some external WFLY module. The external WFLY module
 * defines beans and contains an extension (application scoped by default) which can be injected into
 * <ul>
 * <li>module itself</li>
 * <li>utility library in application</li>
 * <li>EJB sub-deployment in application</li>
 * <li>WAR sub-deployment in application</li>
 * </ul>
 *
 * @see WFLY-1746
 *
 * @author Petr Andreev
 *
 */
@RunWith(Arquillian.class)
public class PortableExtensionInExternalModuleTestCase {

    private static final Logger log = Logger.getLogger(PortableExtensionInExternalModuleTestCase.class.getName());

    private static final String MANIFEST = "MANIFEST.MF";

    private static final String MODULE_NAME = "portable-extension";
    private static TestModule testModule;

    @Inject
    private PortableExtension extension;

    /**
     * The CDI-style EJB injection into the the test-case does not work!
     */
    @EJB(mappedName = "java:global/test/ejb-subdeployment/PortableExtensionSubdeploymentLookup")
    private PortableExtensionLookup ejbInjectionTarget;

    public static void doSetup() throws Exception {
        URL url = PortableExtension.class.getResource(MODULE_NAME + "-module.xml");
        File moduleXmlFile = new File(url.toURI());
        testModule = new TestModule("test." + MODULE_NAME, moduleXmlFile);
        testModule.addResource("portable-extension.jar")
            .addClasses(PortableExtension.class, PortableExtensionLookup.class, PortableExtensionModuleLookup.class)
            .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
        testModule.addClassCallback(ModuleUtils.ENTERPRISE_INJECT);
        testModule.create();

    }

    @AfterClass
    public static void tearDown() throws Exception {
        testModule.remove();
    }

    @Deployment
    public static EnterpriseArchive getDeployment() throws Exception {
        doSetup();

        JavaArchive library = ShrinkWrap.create(JavaArchive.class, "library.jar")
                .addClasses(PortableExtensionLibraryLookup.class)
                .addAsManifestResource(newBeans11Descriptor("annotated"), "beans.xml");

        JavaArchive ejbSubdeployment = ShrinkWrap.create(JavaArchive.class, "ejb-subdeployment.jar")
                .addClass(PortableExtensionSubdeploymentLookup.class)
                // the CDI injection does not work neither with empty nor with 'all'!
                .addAsManifestResource(newBeans11Descriptor("all"), "beans.xml");

        WebArchive webSubdeployment = ShrinkWrap.create(WebArchive.class, "web-subdeployment.war")
                .addClass(PortableExtensionInExternalModuleTestCase.class)
                .addClass(TestModule.class)
                .addAsWebInfResource(newBeans11Descriptor("annotated"), "beans.xml");

        return ShrinkWrap.create(EnterpriseArchive.class, "test.ear").addAsLibrary(library).addAsModule(ejbSubdeployment)
                .addAsModule(webSubdeployment)
                // add EAR-wide dependency on external module
                .addAsManifestResource(getModuleDependencies(), MANIFEST);
        // Adding the deployment structure does not work: the interface PortableExtensionLookup is not on application class-path
        // .addAsApplicationResource(getDeploymentStructure(), "jboss-deployment-structure.xml");

    }

    @Test
    public void testInModule(PortableExtensionModuleLookup injectionTarget) {
        assertPortableExtensionInjection(injectionTarget);
    }

    @Test
    public void testInLibrary(PortableExtensionLibraryLookup injectionTarget) {
        assertPortableExtensionInjection(injectionTarget);
    }

    /**
     * The injectionTarget argument is not injected for EJB!
     *
     * @param injectionTarget
     */
    @Test
    public void testInEjbSubdeployment(PortableExtensionSubdeploymentLookup injectionTarget) {
        assertPortableExtensionInjection(this.ejbInjectionTarget);
    }

    @Test
    public void testInWarSubdeployment() {
        Assert.assertNotNull(extension);
        BeanManager beanManager = extension.getBeanManager();
        Assert.assertNotNull(beanManager);
    }

    private void assertPortableExtensionInjection(PortableExtensionLookup injectionTarget) {
        Assert.assertNotNull(injectionTarget);
        PortableExtension extension = injectionTarget.getPortableExtension();
        Assert.assertNotNull(extension);
        BeanManager beanManager = extension.getBeanManager();
        Assert.assertNotNull(beanManager);
    }

    private static StringAsset newBeans11Descriptor(String mode) {
        return new StringAsset("<beans bean-discovery-mode=\"" + mode + "\" version=\"1.1\"/>");
    }

    private static Asset getModuleDependencies() {
        return new StringAsset("Dependencies: test." + MODULE_NAME + " meta-inf\n");
    }

    @SuppressWarnings("unused")
    private static Asset jbossDeploymentStructure() {
        return new StringAsset("<jboss-deployment-structure xmlns=\"urn:jboss:deployment-structure:1.2\">\n"
                + "<deployment>\n<dependencies>\n<module name=\"test." + MODULE_NAME
                + "\" meta-inf=\"import\"/>\n</dependencies>\n</deployment>\n</jboss-deployment-structure>");
    }

    @SuppressWarnings("unused")
    private static StringAsset emptyEjbJar() {
        return new StringAsset(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<ejb-jar xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\" \n"
                        + "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \n"
                        + "         xsi:schemaLocation=\"http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/ejb-jar_3_2.xsd\"\n"
                        + "         version=\"3.2\">\n\n</ejb-jar>");
    }
}