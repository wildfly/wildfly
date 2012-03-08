/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2012, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.ejb.management.deployments;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.ejb3.subsystem.deployment.EJBComponentType;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import static junit.framework.Assert.assertEquals;
import static org.jboss.as.test.integration.ejb.management.deployments.EjbJarRuntimeResourceTestBase.MODULE_NAME;
import static org.jboss.as.test.integration.ejb.management.deployments.EjbJarRuntimeResourceTestBase.componentAddress;
import static org.jboss.as.test.integration.ejb.management.deployments.EjbJarRuntimeResourceTestBase.executeOperation;
import static org.jboss.as.test.integration.ejb.management.deployments.EjbJarRuntimeResourceTestBase.getEJBJar;
import static org.junit.Assert.assertTrue;

/**
 * Tests whether the invocation statistics actually make sense.
 *
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class EjbInvocationStatisticsTestCase {
    @ContainerResource
    private ManagementClient managementClient;

    private static InitialContext context;

    @BeforeClass
    public static void beforeClass() throws Exception {
        final Hashtable props = new Hashtable();
        props.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
        context = new InitialContext(props);
    }

    @Deployment
    public static Archive<?> deployment() {
        return getEJBJar();
    }

    @Test
    public void testSingleton() throws Exception {
        validateBean(EJBComponentType.SINGLETON, ManagedSingletonBean.class);
    }

    @Test
    public void testSFSB() throws Exception {
        validateBean(EJBComponentType.STATEFUL, ManagedStatefulBean.class);
    }

    @Test
    public void testSLSB() throws Exception {
        validateBean(EJBComponentType.STATELESS, ManagedStatelessBean.class);
    }

    private void validateBean(final EJBComponentType type, final Class<?> beanClass) throws Exception {
        final String name = beanClass.getSimpleName();
        final ModelNode address = componentAddress(EjbJarRuntimeResourcesTestCase.BASE_ADDRESS, type, name).toModelNode();
        address.protect();
        {
            final ModelNode result = executeOperation(managementClient, ModelDescriptionConstants.READ_RESOURCE_OPERATION, address);
            assertEquals(0L, result.get("execution-time").asLong());
            assertEquals(0L, result.get("invocations").asLong());
            assertEquals(0L, result.get("peak-concurrent-invocations").asLong());
            assertEquals(0L, result.get("wait-time").asLong());
        }
        final BusinessInterface bean = (BusinessInterface) context.lookup("ejb:/" + MODULE_NAME + "//" + name + "!" + BusinessInterface.class.getName() + (type == EJBComponentType.STATEFUL ? "?stateful" : ""));
        bean.doIt();
        {
            final ModelNode result = executeOperation(managementClient, ModelDescriptionConstants.READ_RESOURCE_OPERATION, address);
            assertTrue(result.get("execution-time").asLong() >= 50L);
            assertEquals(1L, result.get("invocations").asLong());
            assertEquals(1L, result.get("peak-concurrent-invocations").asLong());
            assertTrue(result.get("wait-time").asLong() >= 0L);
        }        
    }
}
