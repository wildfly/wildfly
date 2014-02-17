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

package org.jboss.as.test.integration.ejb.remote.byreference;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.ejb.remote.common.EJBManagementUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that the EJB subsystem can be configured for pass-by-reference semantics for in-vm invocations on
 * remote interfaces of EJBs
 *
 * @author Jaikiran Pai
 */
@RunWith(Arquillian.class)
@ServerSetup(RemoteInvocationByReferenceTestCase.RemoteInvocationByReferenceTestCaseSetup.class)
public class RemoteInvocationByReferenceTestCase {

    private static final String ARCHIVE_NAME = "in-vm-remote-interface-pass-by-reference-test";

    static class RemoteInvocationByReferenceTestCaseSetup implements ServerSetupTask {

        @Override
        public void setup(final ManagementClient managementClient, final String containerId) throws Exception {
            // setup pass-by-reference semantics
            // we do this here instead of a separate @BeforeClass method because of some weirdness
            // with the ordering of the @BeforeClass execution and deploying the deployment by Arquillian
            EJBManagementUtil.disablePassByValueForRemoteInterfaceInvocations(managementClient);
        }

        @Override
        public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {
            // switch back to the default pass-by-value semantics
            EJBManagementUtil.enablePassByValueForRemoteInterfaceInvocations(managementClient);
        }
    }

    @ArquillianResource
    private InitialContext iniCtx;

    @Deployment
    public static Archive<?> createDeployment() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar");
        jar.addClasses(StatelessRemoteBean.class, RemoteInterface.class, RemoteInvocationByReferenceTestCaseSetup.class);
        return jar;
    }

    protected <T> T lookup(String beanName, Class<T> interfaceType) throws NamingException {
        return interfaceType.cast(iniCtx.lookup("java:global/" + ARCHIVE_NAME + "/" + beanName + "!" + interfaceType.getName()));
    }

    /**
     * Test that invocation on a remote interface of an EJB uses pass-by-reference semantics
     *
     * @throws Exception
     */
    @Test
    public void testPassByReferenceSemanticsOnRemoteInterface() throws Exception {
        final String[] array = {"hello"};
        final RemoteInterface remote = lookup(StatelessRemoteBean.class.getSimpleName(), RemoteInterface.class);
        final String newValue = "foo";
        // invoke on the remote interface
        remote.modifyFirstElementOfArray(array, newValue);
        Assert.assertEquals("Invocation on remote interface of an EJB did *not* use pass-by-reference semantics", newValue, array[0]);
    }
}
