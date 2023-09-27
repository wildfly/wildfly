/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.remote.classloading;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * Tests cross deployment remote EJB invocation, when the deployments cannot see each others classes
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
public class CrossDeploymentRemoteInvocationTestCase {


    @Deployment(name = "caller")
    public static Archive<?> caller() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "caller.jar");
        jar.addClasses(CrossDeploymentRemoteInvocationTestCase.class, RemoteInterface.class);
        return jar;
    }

    @Deployment(name = "callee", testable = false)
    public static Archive<?> callee() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "callee.jar");
        jar.addClasses(StatelessRemoteBean.class, RemoteInterface.class);
        return jar;
    }


    @ArquillianResource
    private InitialContext iniCtx;

    protected <T> T lookup(String beanName, Class<T> interfaceType) throws NamingException {
        return interfaceType.cast(iniCtx.lookup("java:global/callee/" + beanName + "!" + interfaceType.getName()));
    }

    @OperateOnDeployment("caller")
    @Test
    public void testCrossDeploymentInvocation() throws Exception {
        RemoteInterface bean = lookup("StatelessRemoteBean", RemoteInterface.class);
        Assert.assertEquals("hello bob", bean.hello("bob"));
    }
}
