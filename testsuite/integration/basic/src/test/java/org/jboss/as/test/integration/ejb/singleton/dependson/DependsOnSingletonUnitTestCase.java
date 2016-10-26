/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
