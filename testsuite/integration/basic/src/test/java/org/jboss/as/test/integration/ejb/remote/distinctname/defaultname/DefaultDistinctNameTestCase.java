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

package org.jboss.as.test.integration.ejb.remote.distinctname.defaultname;

import javax.naming.Context;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.ejb.remote.common.EJBManagementUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that invocation on EJBs deployed in a deployment with distinct name works successfully
 *
 * @author Jaikiran Pai
 */
@ServerSetup(DefaultDistinctNameTestCase.DefaultDistinctNameTestCaseSetup.class)
@RunWith(Arquillian.class)
public class DefaultDistinctNameTestCase {

    @ArquillianResource
    private Context context;

    private static final String APP_NAME = "";
    private static final String DISTINCT_NAME = "distinct-name-in-jboss-ejb3-xml";
    private static final String MODULE_NAME = "remote-ejb-distinct-name-jar-test-case";

    static class DefaultDistinctNameTestCaseSetup implements ServerSetupTask {

        @Override
        public void setup(final ManagementClient managementClient, final String containerId) throws Exception {
            EJBManagementUtil.setDefaultDistinctName(managementClient, DISTINCT_NAME);
        }

        @Override
        public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {
            EJBManagementUtil.setDefaultDistinctName(managementClient, null);
        }
    }

    @Deployment(testable = false)
    public static JavaArchive createDeployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, MODULE_NAME + ".jar");
        jar.addPackage(DefaultDistinctNameTestCase.class.getPackage());
        return jar;
    }


    /**
     * Test that invocation on a stateless bean, deployed in a deployment with distinct-name, works fine
     *
     * @throws Exception
     */
    @Test
    public void testRemoteSLSBInvocation() throws Exception {
        final Echo bean = (Echo) context.lookup("ejb:" + APP_NAME + "/" + MODULE_NAME + "/" + DISTINCT_NAME
                + "/" + StatelessEcho.class.getSimpleName() + "!" + Echo.class.getName());
        Assert.assertNotNull("Lookup returned a null bean proxy", bean);
        final String msg = "Hello world from a really remote client!!!";
        final String echo = bean.echo(msg);
        Assert.assertEquals("Unexpected echo returned from remote stateless bean", msg, echo);
    }

    /**
     * Test that invocation on a stateful bean, deployed in a deployment with distinct-name, works fine
     *
     * @throws Exception
     */
    @Test
    public void testRemoteSFSBInvocation() throws Exception {
        final Echo bean = (Echo) context.lookup("ejb:" + APP_NAME + "/" + MODULE_NAME + "/" + DISTINCT_NAME
                + "/" + StatefulEcho.class.getSimpleName() + "!" + Echo.class.getName() + "?stateful");
        Assert.assertNotNull("Lookup returned a null bean proxy", bean);
        final String msg = "Hello world from a really remote client!!!";
        final String echo = bean.echo(msg);
        Assert.assertEquals("Unexpected echo returned from remote stateful bean", msg, echo);

    }

    /**
     * Test that invocation on a singleton bean, deployed in a deployment with distinct-name, works fine
     *
     * @throws Exception
     */
    @Test
    public void testRemoteSingletonInvocation() throws Exception {
        final Echo bean = (Echo) context.lookup("ejb:" + APP_NAME + "/" + MODULE_NAME + "/" + DISTINCT_NAME
                + "/" + SingletonEcho.class.getSimpleName() + "!" + Echo.class.getName());
        Assert.assertNotNull("Lookup returned a null bean proxy", bean);
        final String msg = "Hello world from a really remote client!!!";
        final String echo = bean.echo(msg);
        Assert.assertEquals("Unexpected echo returned from remote singleton bean", msg, echo);

    }
}
