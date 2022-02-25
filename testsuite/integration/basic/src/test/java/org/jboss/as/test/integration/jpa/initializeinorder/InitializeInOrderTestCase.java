/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2021, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.jpa.initializeinorder;

import static org.junit.Assert.assertTrue;


import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that <initialize-in-order> works as expected.
 *
 * @author Scott Marlow (clone of test written by Stuart Douglas)
 */
@RunWith(Arquillian.class)
public class InitializeInOrderTestCase {

    private static final String ARCHIVE_NAME = "InitializeInOrderTestCase";

    @Deployment
    public static Archive<?> deployment() {
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, ARCHIVE_NAME + ".ear");
        ear.addAsResource(InitializeInOrderTestCase.class.getPackage(), "application.xml", "application.xml");
        final JavaArchive sharedJar = ShrinkWrap.create(JavaArchive.class, "shared.jar");
        sharedJar.addClasses(InitializeInOrderTestCase.class,
                TestState.class,
                Employee.class,
                MyListener.class,
                SingletonCMT.class
                );
        ear.addAsLibraries(sharedJar);

        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "ejb.jar");
        jar.addClasses(MyEjb.class);
        jar.addAsManifestResource(InitializeInOrderTestCase.class.getPackage(), "persistence.xml", "persistence.xml");
        ear.addAsModule(jar);

        final JavaArchive jar2 = ShrinkWrap.create(JavaArchive.class, "ejb2.jar");
        jar2.addClasses(MyEjb2.class);
        jar2.addClass(SFSBCMT.class);
        jar2.addClass(AbstractCMTBean.class);
        jar2.addClass(Employee.class);
        jar2.addAsManifestResource(InitializeInOrderTestCase.class.getPackage(), "persistence.xml", "persistence.xml");
        ear.addAsModule(jar2);


        final WebArchive war = ShrinkWrap.create(WebArchive.class, "web.war");
        war.addClasses(MyServlet.class, CdiJpaInjectingBean.class, QualifyEntityManagerFactory.class, CdiJpaInjectingBean.class);
        war.addAsResource( InitializeInOrderTestCase.class.getPackage(), "persistence.xml", "META-INF/persistence.xml");
        war.addAsWebInfResource(new StringAsset("<beans bean-discovery-mode=\"all\"></beans>"), "beans.xml");
        ear.addAsModule(war);

        return ear;
    }

    @Test
    public void testPostConstruct() throws NamingException {
        Assert.assertEquals("check of initOrder (" + TestState.getInitOrder().toString() + ") size is expected to be 3 but was " + TestState.getInitOrder().size(), 3, TestState.getInitOrder().size());
        Assert.assertEquals("MyServlet", TestState.getInitOrder().get(0));
        Assert.assertEquals("MyEjb", TestState.getInitOrder().get(1));
        TestState.clearInitOrder();
    }

    /**
     * Tests that the entity listeners are correctly invoked and have access to the java:comp/EJBContext
     * when an entity is persisted via a stateful CMT bean
     *
     * @throws Exception
     */
    @Test
    public void testSFSBCMT() throws Exception {
        MyListener.setInvocationCount(0);
        // java:global/InitializeInOrderTestCase/ejb2/SFSBCMT
        SFSBCMT cmt = lookup("ejb2/SFSBCMT", SFSBCMT.class);
        doCMTTest(cmt, 2);
    }

    /**
     * Tests that both MyEJB + MyEJB2 have access to the persistence context defined in the other EJB
     *
     * @throws Exception
     */
    @Test
    public void testSubDeploymentsHaveAllPersistenceUnits() throws Exception {
        // java:global/InitializeInOrderTestCase/ejb2/SFSBCMT
        MyEjb myEjb = lookup("ejb/MyEjb", MyEjb.class);
        MyEjb2 myEjb2 = lookup("ejb2/MyEjb2", MyEjb2.class);
        assertTrue(myEjb.hasPersistenceContext());
        assertTrue(myEjb2.hasPersistenceContext());

    }

    /**
     * Tests that the entity listeners are correctly invoked and have access to the java:comp/EJBContext
     * when an entity is persisted via a CMT bean
     *
     * @param cmtBean The CMT bean
     * @throws Exception
     */
    private void doCMTTest(final AbstractCMTBean cmtBean, final int empId) throws Exception {
        cmtBean.createEmployee("Alfred E. Neuman", "101010 Mad Street", empId);
        Employee emp = cmtBean.getEmployeeNoTX(empId);
        cmtBean.updateEmployee(emp);
        assertTrue("could not load added employee", emp != null);
        assertTrue("EntityListener wasn't invoked twice as expected, instead " + MyListener.getInvocationCount(), 2 == MyListener.getInvocationCount());
    }

    @Test
    public void testInjectedPersistenceContext() throws Exception {
        Assert.assertTrue("CdiJpaInjectingBean should be true but is",
                TestState.isJpaInjectingBeanAvailable());

        Assert.assertTrue("injected EntityManagerFactory should not be null but is",
                TestState.isEntityManagerFactoryAvailable());

        Assert.assertTrue("injected EntityManager should not be null but is",
                TestState.isEntityManagerAvailable());

    }


    @ArquillianResource
    private static InitialContext iniCtx;

    protected <T> T lookup(String beanName, Class<T> interfaceType) throws NamingException {
        return interfaceType.cast(iniCtx.lookup("java:global/" + ARCHIVE_NAME + "/" + beanName + "!" + interfaceType.getName()));
    }

    protected <T> T rawLookup(String name, Class<T> interfaceType) throws NamingException {
        return interfaceType.cast(iniCtx.lookup(name));
    }

}
