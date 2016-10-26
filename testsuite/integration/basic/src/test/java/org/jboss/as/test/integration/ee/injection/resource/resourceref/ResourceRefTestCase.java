/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ee.injection.resource.resourceref;

import java.net.URL;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.jms.auxiliary.CreateQueueSetupTask;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;



/**
 * Tests that @Resource bindings on interceptors that are applied to multiple
 * components without their own naming context work properly, and do not try
 * and create two duplicate bindings in the same namespace.
 *
 * Migration test from EJB Testsuite (ejbthree-1823, ejbthree-1858) to AS7 [JIRA JBQA-5483].
 * - ResourceHandler when resource-ref type is not specified.
 * - EJBContext is configured through ejb-jar.xml as a resource-env-ref.
 *
 * @author Stuart Douglas, Jaikiran Pai, Ondrej Chaloupka
 */
@RunWith(Arquillian.class)
@ServerSetup(CreateQueueSetupTask.class)
public class ResourceRefTestCase {

    @Deployment
    public static Archive<?> deployment() {
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "resourcerref.ear");

        WebArchive war = ShrinkWrap.create(WebArchive.class, "managed-bean.war");
        war.addAsWebInfResource(ResourceRefTestCase.class.getPackage(),"web.xml", "web.xml");
        war.addClasses(ResourceRefTestCase.class, DatasourceManagedBean.class, CreateQueueSetupTask.class);

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "resource-ref-test.jar");
        jar.addClasses(ResourceRefBean.class, ResourceRefRemote.class, StatelessBean.class, StatelessBeanRemote.class, ResUrlChecker.class, ResUrlCheckerBean.class);
        jar.addAsManifestResource(ResourceRefTestCase.class.getPackage(),"jboss-ejb3.xml", "jboss-ejb3.xml");
        jar.addAsManifestResource(ResourceRefTestCase.class.getPackage(),"ejb-jar.xml", "ejb-jar.xml");

        ear.addAsModule(jar);
        ear.addAsModule(war);

        return ear;
    }

    @Test
    public void testCorrectBinding() throws NamingException {
        InitialContext context = new InitialContext();
        Object result = context.lookup("java:module/env/ds");
        Assert.assertTrue(result instanceof DataSource);
    }

    @Test
    public void testInjection() throws NamingException {
        InitialContext context = new InitialContext();
        DatasourceManagedBean bean = (DatasourceManagedBean)context.lookup("java:module/datasourceManagedBean");
        Assert.assertNotNull(bean.getDataSource());
    }

    /**
     * Test that a resource-ref entry with a res-type does not throw an NPE. Furthermore, the test additional provides a
     * mappedName for the resource-ref in which case the resource ref will be created in the ENC.
     *
     * @throws Exception
     */
    @Test
    public void testResourceRefEntriesWithoutResType() throws Exception {
        // lookup the bean
        InitialContext context = new InitialContext();
        ResourceRefRemote bean = (ResourceRefRemote) context.lookup("java:app/resource-ref-test/" + ResourceRefBean.class.getSimpleName() + "!" + ResourceRefRemote.class.getName());
        Assert.assertNotNull("Bean returned from JNDI is null", bean);

        // test datasource resource-ref which does not have a res-type specified
        boolean result = bean.isDataSourceAvailableInEnc();
        Assert.assertTrue("Datasource not bound in ENC of the bean", result);
    }

    /**
     * Test that the resources configured through resource-env-ref are bound
     * correctly
     *
     * @throws Exception
     */
    @Test
    public void testResourceEnvRefWithoutInjectionTarget() throws Exception {
        InitialContext context = new InitialContext();
       StatelessBeanRemote bean = (StatelessBeanRemote) context.lookup("java:app/resource-ref-test/"+StatelessBean.class.getSimpleName() + "!" + StatelessBeanRemote.class.getName());
       // check EJBContext through resource-env-ref was handled
       Assert.assertTrue("resource-env-ref did not handle EJBContext", bean.isEJBContextAvailableThroughResourceEnvRef());
       // check UserTransaction through resource-env-ref was handled
       Assert.assertTrue("resource-env-ref did not handle UserTransaction", bean
             .isUserTransactionAvailableThroughResourceEnvRef());
       // check some other resource through resource-env-ref was handled
       Assert.assertTrue("resource-env-ref did not setup the other resource in java:comp/env of the bean", bean
             .isOtherResourceAvailableThroughResourceEnvRef());
    }

    @Test
    public void test2() throws Exception {
        ResUrlChecker bean = (ResUrlChecker) new InitialContext().lookup("java:app/resource-ref-test/ResUrlCheckerBean");
        // defined in jboss.xml
        URL expected = new URL("http://somewhere/url2");
        URL actual = bean.getURL2();
        Assert.assertEquals(expected, actual);
    }
}
