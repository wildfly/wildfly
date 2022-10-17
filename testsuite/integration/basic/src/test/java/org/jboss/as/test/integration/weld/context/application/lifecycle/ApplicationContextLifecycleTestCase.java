/*
 * JBoss, Home of Professional Open Source
 * Copyright 2019, Red Hat Inc., and individual contributors as indicated
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

package org.jboss.as.test.integration.weld.context.application.lifecycle;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.PropertyPermission;
import java.util.concurrent.TimeUnit;

import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;

/**
 * Tests possible deployment scenarios for CDI ApplicationScoped events, and usage of other EE technologies in such event handlers.
 * @author emmartins
 */
@RunWith(Arquillian.class)
public class ApplicationContextLifecycleTestCase {

    private static final String TEST_BEANS_LIB_JAR = "TEST_BEANS_LIB_JAR";
    private static final String TEST_BEANS_EJB_JAR = "TEST_BEANS_EJB_JAR";
    private static final String TEST_BEANS_EAR = "TEST_BEANS_EAR";
    private static final String TEST_BEANS_WAR = "TEST_BEANS_WAR";
    private static final String TEST_RESULTS = "TEST_RESULTS";

    @ArquillianResource
    public Deployer deployer;

    @Deployment(name = TEST_BEANS_EJB_JAR, managed = false)
    public static Archive<JavaArchive> deployJar() {
        final JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, TEST_BEANS_EJB_JAR +".jar")
                .addClasses(Ejb.class, Bean.class, Mdb.class, TestResults.class, Utils.class, TimeoutUtil.class)
                .addAsManifestResource(createPermissionsXmlAsset(
                    new PropertyPermission(TimeoutUtil.FACTOR_SYS_PROP, "read")), "permissions.xml");
        return ejbJar;
    }

    @Deployment(name = TEST_BEANS_EAR, managed = false)
    public static Archive<EnterpriseArchive> deployEar() {
        final JavaArchive libJar = ShrinkWrap.create(JavaArchive.class, TEST_BEANS_LIB_JAR +".jar")
                .addClasses(TestResults.class, Utils.class);
        final JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, TEST_BEANS_EJB_JAR +".jar")
                .addClasses(Ejb.class, Bean.class, Mdb.class);
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, TEST_BEANS_EAR +".ear")
                .addAsLibrary(libJar)
                .addAsModule(ejbJar);
        return ear;
    }

    @Deployment(name = TEST_BEANS_WAR, managed = false)
    public static Archive<WebArchive> deployWar() {
        final JavaArchive libJar = ShrinkWrap.create(JavaArchive.class, TEST_BEANS_LIB_JAR +".jar")
                .addClasses(TestResults.class, Utils.class);
        final WebArchive war = ShrinkWrap.create(WebArchive.class, TEST_BEANS_WAR +".war")
                .addAsLibrary(libJar)
                .addClasses(Servlet.class, Bean.class, Mdb.class);
        return war;
    }

    @Deployment
    public static Archive<?> deployTestResults() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, TEST_RESULTS +".jar");
        jar.addClasses(TestResultsBean.class, TestResults.class, TimeoutUtil.class);
        jar.addAsManifestResource(createPermissionsXmlAsset(
                new PropertyPermission(TimeoutUtil.FACTOR_SYS_PROP, "read")), "permissions.xml");
        return jar;
    }

    @ArquillianResource
    private InitialContext initialContext;

    @Test
    public void testJar() throws NamingException, InterruptedException {
        final TestResults testResults = (TestResults) initialContext.lookup("java:global/"+ TEST_RESULTS +"/"+ TestResultsBean.class.getSimpleName()+"!"+ TestResults.class.getName());
        // setup initialized test
        testResults.setup(2);
        // deploy app
        deployer.deploy(TEST_BEANS_EJB_JAR);
        // await and assert initialized results
        testResults.await(TimeoutUtil.adjust(5), TimeUnit.SECONDS);
        Assert.assertTrue(testResults.isCdiBeanInitialized());
        Assert.assertTrue(testResults.isEjbBeanInitialized());
        // NOTE: before destroyed and destroyed atm only guaranteed for web deployments, uncomment all once that's solved
        // setup before destroyed and destroyed test
        //testResults.setup(4);
        // undeploy app
        deployer.undeploy(TEST_BEANS_EJB_JAR);
        // await and assert before destroyed and destroyed results
        //testResults.await(TimeoutUtil.adjust(5), TimeUnit.SECONDS);
        //Assert.assertTrue(testResults.isCdiBeanBeforeDestroyed());
        //Assert.assertTrue(testResults.isCdiBeanDestroyed());
        //Assert.assertTrue(testResults.isEjbBeanBeforeDestroyed());
        //Assert.assertTrue(testResults.isEjbBeanDestroyed());
    }

    @Test
    public void testEar() throws NamingException, InterruptedException {
        final TestResults testResults = (TestResults) initialContext.lookup("java:global/"+ TEST_RESULTS +"/"+ TestResultsBean.class.getSimpleName()+"!"+ TestResults.class.getName());
        // setup initialized test
        testResults.setup(2);
        // deploy app
        deployer.deploy(TEST_BEANS_EAR);
        // await and assert initialized results
        testResults.await(TimeoutUtil.adjust(5), TimeUnit.SECONDS);
        Assert.assertTrue(testResults.isCdiBeanInitialized());
        Assert.assertTrue(testResults.isEjbBeanInitialized());
        // NOTE: before destroyed and destroyed atm only guaranteed for web deployments, uncomment all once that's solved
        // setup before destroyed and destroyed test
        //testResults.setup(4);
        // undeploy app
        deployer.undeploy(TEST_BEANS_EAR);
        // await and assert before destroyed and destroyed results
        //testResults.await(TimeoutUtil.adjust(5), TimeUnit.SECONDS);
        //Assert.assertTrue(testResults.isCdiBeanBeforeDestroyed());
        //Assert.assertTrue(testResults.isCdiBeanDestroyed());
        //Assert.assertTrue(testResults.isEjbBeanBeforeDestroyed());
        //Assert.assertTrue(testResults.isEjbBeanDestroyed());
    }

    @Test
    public void testWar() throws NamingException, InterruptedException {
        final TestResults testResults = (TestResults) initialContext.lookup("java:global/"+ TEST_RESULTS +"/"+ TestResultsBean.class.getSimpleName()+"!"+ TestResults.class.getName());
        // setup initialized test
        testResults.setup(2);
        // deploy app
        deployer.deploy(TEST_BEANS_WAR);
        // await and assert initialized results
        testResults.await(TimeoutUtil.adjust(5), TimeUnit.SECONDS);
        Assert.assertTrue(testResults.isCdiBeanInitialized());
        Assert.assertTrue(testResults.isServletInitialized());
        // setup before destroyed and destroyed test
        testResults.setup(4);
        // undeploy app
        deployer.undeploy(TEST_BEANS_WAR);
        // await and assert before destroyed and destroyed results
        testResults.await(TimeoutUtil.adjust(5), TimeUnit.SECONDS);
        Assert.assertTrue(testResults.isCdiBeanBeforeDestroyed());
        Assert.assertTrue(testResults.isCdiBeanDestroyed());
        Assert.assertTrue(testResults.isServletBeforeDestroyed());
        Assert.assertTrue(testResults.isServletDestroyed());
    }
}
