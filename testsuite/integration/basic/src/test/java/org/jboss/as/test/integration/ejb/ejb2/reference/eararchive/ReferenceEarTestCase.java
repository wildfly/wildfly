/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.ejb2.reference.eararchive;

import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Migration test from EJB Testsuite (reference21_30) to AS7 [JIRA JBQA-5483]. Test for EJB3.0/EJB2.1 references
 *
 * @author William DeCoste, Ondrej Chaloupka
 */
@RunWith(Arquillian.class)
public class ReferenceEarTestCase {

    @ArquillianResource
    InitialContext ctx;

    @Deployment
    public static Archive<?> deployment() {
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "multideploy-reference21_31-test.ear");

        final JavaArchive jar1 = ShrinkWrap.create(JavaArchive.class, "multideploy-ejb2.jar")
                .addClasses(
                        Test2.class,
                        Test2Bean.class,
                        Test2Home.class);
        jar1.addClass(ReferenceEarTestCase.class);
        jar1.addAsManifestResource(ReferenceEarTestCase.class.getPackage(), "jboss-ejb3-ejb2.xml", "jboss-ejb3.xml");
        jar1.addAsManifestResource(ReferenceEarTestCase.class.getPackage(), "ejb-jar-ejb2.xml", "ejb-jar.xml");

        final JavaArchive jar2 = ShrinkWrap.create(JavaArchive.class, "multideploy-ejb3.jar")
                .addClasses(
                        Test3.class,
                        Test3Business.class,
                        Test3Home.class,
                        Test3Bean.class);
        jar2.addAsManifestResource(ReferenceEarTestCase.class.getPackage(), "jboss-ejb3-ejb3.xml", "jboss-ejb3.xml");
        jar2.addAsManifestResource(ReferenceEarTestCase.class.getPackage(), "ejb-jar-ejb3.xml", "ejb-jar.xml");

        ear.addAsModule(jar1);
        ear.addAsModule(jar2);
        return ear;
    }

    @Test
    public void testEjbInjection2() throws Exception {

        Test3Home test3Home = (Test3Home) ctx.lookup("java:app/multideploy-ejb3/Test3!" + Test3Home.class.getName());
        Test3 test3 = test3Home.create();
        Assert.assertNotNull(test3);
        test3.testAccess();

        Test2Home home = (Test2Home) ctx.lookup("java:app/multideploy-ejb2/ejb_Test2!" + Test2Home.class.getName());
        Assert.assertNotNull(home);
        Test2 test2 = home.create();
        Assert.assertNotNull(test2);
        test2.testAccess();
    }

    @Test
    public void testEjbInjection3() throws Exception {

        Test3Business test3 = (Test3Business) ctx.lookup("java:app/multideploy-ejb3/Test3!" + Test3Business.class.getName());
        Assert.assertNotNull(test3);
        test3.testAccess();

        Test2Home home = (Test2Home) ctx.lookup("java:app/multideploy-ejb2/ejb_Test2!" + Test2Home.class.getName());
        Assert.assertNotNull(home);
        Test2 test2 = home.create();
        Assert.assertNotNull(test2);
        test2.testAccess();
    }
}
