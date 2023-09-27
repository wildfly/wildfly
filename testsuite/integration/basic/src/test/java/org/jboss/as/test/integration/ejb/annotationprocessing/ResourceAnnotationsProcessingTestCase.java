/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.annotationprocessing;

import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class ResourceAnnotationsProcessingTestCase {

    private static final String ABSTRACT_REFERENCING_INTERFACE_WITHOUT_IMPL = "abstract-referencing-interface-without-impl";
    private static final String INTERFACE_REFERENCING_INTERFACE_WITHOUT_IMPL = "interface-referencing-interface-without-impl";
    private static final String EJBS_REFERENCING_INTERFACE_WITH_IMPL = "ejbs-referencing-interface-with-impl";
    private static final String EJBS_REFERENCING_INTERFACE_WITHOUT_IMPL = "ejbs-referencing-interface-without-impl";
    private static final String CLASSES_REFERENCING_INTERFACE_WITH_IMPL = "classes-referencing-interface-with-impl";
    private static final String CLASSES_REFERENCING_INTERFACE_WITHOUT_IMPL = "classes-referencing-interface-without-impl";

    @ArquillianResource
    private Deployer deployer;

    @Deployment(name = ABSTRACT_REFERENCING_INTERFACE_WITHOUT_IMPL, managed = false)
    public static WebArchive createDeployment1() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class,
                ABSTRACT_REFERENCING_INTERFACE_WITHOUT_IMPL + ".war");
        war.addClass(ReferencedBeanInterface.class);
        war.addClass(AbstractReferencingBeanA.class);
        return war;
    }

    @Deployment(name = INTERFACE_REFERENCING_INTERFACE_WITHOUT_IMPL, managed = false)
    public static WebArchive createDeployment2() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class,
                INTERFACE_REFERENCING_INTERFACE_WITHOUT_IMPL + ".war");
        war.addClass(ReferencedBeanInterface.class);
        war.addClass(ReferencingBeanBInterface.class);
        return war;
    }

    @Deployment(name = EJBS_REFERENCING_INTERFACE_WITH_IMPL, managed = false)
    public static WebArchive createDeployment3() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class,
                EJBS_REFERENCING_INTERFACE_WITH_IMPL + ".war");
        war.addClass(ReferencedBeanInterface.class);
        war.addClass(ReferencedBean.class);
        war.addClass(AbstractReferencingBeanA.class);
        war.addClass(ReferencingComponentA.class);
        war.addClass(ReferencingBeanBInterface.class);
        war.addClass(ReferencingComponentB.class);
        return war;
    }

    @Deployment(name = EJBS_REFERENCING_INTERFACE_WITHOUT_IMPL, managed = false)
    public static WebArchive createDeployment4() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class,
                EJBS_REFERENCING_INTERFACE_WITHOUT_IMPL + ".war");
        war.addClass(ReferencedBeanInterface.class);
        war.addClass(AbstractReferencingBeanA.class);
        war.addClass(ReferencingComponentA.class);
        war.addClass(ReferencingBeanBInterface.class);
        war.addClass(ReferencingComponentB.class);
        return war;
    }

    @Deployment(name = CLASSES_REFERENCING_INTERFACE_WITH_IMPL, managed = false)
    public static WebArchive createDeployment5() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class,
                CLASSES_REFERENCING_INTERFACE_WITH_IMPL + ".war");
        war.addClass(ReferencedBeanInterface.class);
        war.addClass(ReferencedBean.class);
        war.addClass(AbstractReferencingBeanA.class);
        war.addClass(ReferencingClassA.class);
        war.addClass(ReferencingBeanBInterface.class);
        war.addClass(ReferencingClassB.class);
        return war;
    }

    @Deployment(name = CLASSES_REFERENCING_INTERFACE_WITHOUT_IMPL, managed = false)
    public static WebArchive createDeployment6() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class,
                CLASSES_REFERENCING_INTERFACE_WITHOUT_IMPL + ".war");
        war.addClass(ReferencedBeanInterface.class);
        war.addClass(AbstractReferencingBeanA.class);
        war.addClass(ReferencingClassA.class);
        war.addClass(ReferencingBeanBInterface.class);
        war.addClass(ReferencingClassB.class);
        return war;
    }

    @Test
    public void testAbstractReferencingInterfaceWithoutImpl() {
        tryDeployment(ABSTRACT_REFERENCING_INTERFACE_WITHOUT_IMPL);
    }

    @Test
    public void testInterfaceReferencingInterfaceWithoutImpl() {
        tryDeployment(INTERFACE_REFERENCING_INTERFACE_WITHOUT_IMPL);
    }

    @Test
    public void testEjbsReferencingInterfaceWithImpl() {
        tryDeployment(EJBS_REFERENCING_INTERFACE_WITH_IMPL);
    }

    @Test(expected = DeploymentException.class)
    public void testEjbsReferencingInterfaceWithoutImpl() {
        tryDeployment(EJBS_REFERENCING_INTERFACE_WITHOUT_IMPL);
    }

    @Test
    public void testClassesReferencingInterfaceWithImpl() {
        tryDeployment(CLASSES_REFERENCING_INTERFACE_WITH_IMPL);
    }

    @Test(expected = DeploymentException.class)
    public void testClassesReferencingInterfaceWithoutImpl() {
        tryDeployment(CLASSES_REFERENCING_INTERFACE_WITHOUT_IMPL);
    }

    private void tryDeployment(String name) {
        deployer.deploy(name);
        deployer.undeploy(name);
    }
}
