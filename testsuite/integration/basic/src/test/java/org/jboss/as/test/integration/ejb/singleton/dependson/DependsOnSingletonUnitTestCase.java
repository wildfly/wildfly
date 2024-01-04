/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.singleton.dependson;

import java.util.ArrayList;
import java.util.List;
import org.jboss.logging.Logger;
import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Ondrej Chaloupka
 */
@RunWith(Arquillian.class)
public class DependsOnSingletonUnitTestCase {
    private static final Logger log = Logger.getLogger(DependsOnSingletonUnitTestCase.class.getName());

    @ArquillianResource
    InitialContext ctx;

    @ArquillianResource
    Deployer deployer;

    @Deployment(name = "callcounter", order = 0, managed = true, testable = true)
    public static Archive<?> deployCallcounter() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "callcounter.jar");
        jar.addClass(CallCounterSingleton.class);
        jar.addClass(DependsOnSingletonUnitTestCase.class);
        return jar;
    }

    @Deployment(name = "ear", order = 1, managed = false, testable = false)
    public static Archive<?> deployDependsOn() {
        JavaArchive jarOne = ShrinkWrap.create(JavaArchive.class, "one.jar");
        jarOne.addClass(SingletonOne.class);

        JavaArchive jarTwo = ShrinkWrap.create(JavaArchive.class, "two.jar");
        jarTwo.addClass(SingletonTwo.class);

        JavaArchive jarThree = ShrinkWrap.create(JavaArchive.class, "three.jar");
        jarThree.addClass(SingletonThree.class);

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "dependson-test.ear");
        ear.addAsModule(jarOne);
        ear.addAsModule(jarTwo);
        ear.addAsModule(jarThree);
        ear.addAsManifestResource(DependsOnSingletonUnitTestCase.class.getPackage(), "application.xml", "application.xml");
        ear.addAsManifestResource(new StringAsset("Dependencies: deployment.callcounter.jar \n"), "MANIFEST.MF");
        return ear;
    }

    @Test
    @OperateOnDeployment("callcounter")
    public void testDependsOn() throws Exception {
        CallCounterSingleton singleton = (CallCounterSingleton) ctx.lookup("java:module/CallCounterSingleton");

        deployer.deploy("ear");
        deployer.undeploy("ear");

        List<String> expectedOrder = new ArrayList<String>();
        expectedOrder.add("SingletonOne");
        expectedOrder.add("SingletonTwo");
        expectedOrder.add("SingletonThree");
        expectedOrder.add("SingletonThree");
        expectedOrder.add("SingletonTwo");
        expectedOrder.add("SingletonOne");
        Assert.assertEquals(expectedOrder, singleton.getCalls());
    }
}
