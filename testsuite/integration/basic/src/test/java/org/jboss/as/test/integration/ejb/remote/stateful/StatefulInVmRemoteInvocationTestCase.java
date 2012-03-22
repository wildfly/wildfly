/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ejb.remote.stateful;

import javax.ejb.NoSuchEJBException;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests cross deployment remote EJB invocation, when the deployments cannot see each others classes
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
public class StatefulInVmRemoteInvocationTestCase {


    @Deployment
    public static Archive<?> caller() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "stateful.jar");
        jar.addPackage(StatefulInVmRemoteInvocationTestCase.class.getPackage());
        return jar;
    }


    @ArquillianResource
    private InitialContext iniCtx;

    protected <T> T lookup(String beanName, Class<T> interfaceType) throws NamingException {
        return interfaceType.cast(iniCtx.lookup("java:global/stateful/" + beanName + "!" + interfaceType.getName()));
    }

    @Test
    public void testCrossDeploymentInvocation() throws Exception {
        RemoteInterface bean = lookup(StatefulAddingBean.class.getSimpleName(), RemoteInterface.class);
        bean.add(10);
        bean.add(10);
        Assert.assertEquals(20, bean.get());
        bean.remove();
        try {
            bean.add(10);
            Assert.fail("Expected EJB to be removed");
        } catch (NoSuchEJBException expected) {

        }
    }

    /**
     * Test bean returning a value object with a transient field.  Will test that the transient field is set to null
     * (just like java serialization would do)
     * instead of a non-null value (non-null came ValueWrapper class initializer if this fails).
     *
     * @throws Exception
     */
    @Test
    public void testValueObjectWithTransientField() throws Exception {
        RemoteInterface bean = lookup(StatefulAddingBean.class.getSimpleName(), RemoteInterface.class);
        Assert.assertNull("transient field should be serialized as null but was '" + bean.getValue().getShouldBeNilAfterUnmarshalling() +"'",
                bean.getValue().getShouldBeNilAfterUnmarshalling());
        bean.remove();
    }

}
