/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.embedded.demos.ejb3;

import org.jboss.arquillian.api.Deployment;
import org.jboss.arquillian.api.Run;
import org.jboss.arquillian.api.RunModeType;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.demos.ejb3.archive.session.singleton.CallTrackerSingletonBean;
import org.jboss.as.demos.ejb3.archive.session.singleton.SimpleSingletonBean;
import org.jboss.as.demos.ejb3.archive.session.singleton.SimpleSingletonLocal;
import org.jboss.as.demos.ejb3.archive.session.singleton.StartupSingleton;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.naming.Context;
import javax.naming.InitialContext;

/**
 * Testcase for testing the basic functionality of a EJB3 singleton session bean.
 *
 * @author Jaikiran Pai
 */
@RunWith(Arquillian.class)
@Run(RunModeType.IN_CONTAINER)
public class SingletonBeanTestCase {

    @Deployment
    public static JavaArchive createDeployment() {
        // create the ejb jar
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "ejb3-singleton-bean-example.jar");
        jar.addManifestResource("archives/ejb3-example.jar/META-INF/MANIFEST.MF", "MANIFEST.MF");
        jar.addPackage(SimpleSingletonBean.class.getPackage());
        jar.addPackage(StartupSingleton.class.getPackage());
        return jar;
    }

    /**
     * Test a basic invocation on @Singleton bean
     *
     * @throws Exception
     */
    @Test
    public void testSingletonEJB() throws Exception {
        Context ctx = new InitialContext();
        SimpleSingletonLocal singletonBean = (SimpleSingletonLocal) ctx.lookup("java:global/ejb3-singleton-bean-example/" + SimpleSingletonBean.class.getSimpleName() + "!" + SimpleSingletonLocal.class.getName());
        final int NUM_TIMES = 10;
        for (int i = 0; i < NUM_TIMES; i++) {
            singletonBean.increment();
        }
        Assert.assertEquals("Unexpected count returned from singleton bean", NUM_TIMES, singletonBean.getCount());

    }

    @Test
    public void testStartupSingleton() throws Exception {
        Context ctx = new InitialContext();
        CallTrackerSingletonBean callTrackerSingletonBean = (CallTrackerSingletonBean) ctx.lookup("java:global/ejb3-singleton-bean-example/" + CallTrackerSingletonBean.class.getSimpleName() + "!" + CallTrackerSingletonBean.class.getName());
        Assert.assertTrue("@Startup singleton bean was not created", callTrackerSingletonBean.wasStartupSingletonBeanCreated());

    }

    @Test
    public void testReadOnlySingleton() throws Exception {
        Context ctx = new InitialContext();

    }

}
