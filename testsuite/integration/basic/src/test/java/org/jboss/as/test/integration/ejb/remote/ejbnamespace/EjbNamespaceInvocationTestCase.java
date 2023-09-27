/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.remote.ejbnamespace;

import org.jboss.arquillian.container.test.api.Deployment;
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
 * Simple remote ejb tests
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
public class EjbNamespaceInvocationTestCase {

    private static final String APP_NAME = "";
    private static final String MODULE_NAME = "RemoteInvocationTest";

    @Deployment
    public static Archive<?> deploy() {

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, MODULE_NAME + ".jar");
        jar.addPackage(EjbNamespaceInvocationTestCase.class.getPackage());
        return jar;
    }

    @ArquillianResource
    private InitialContext iniCtx;

    @Test
    public void testDirectLookup() throws Exception {
        RemoteInterface bean = lookupEjb(StatelessRemoteBean.class.getSimpleName(), RemoteInterface.class);
        Assert.assertEquals("hello", bean.hello());
    }

    @Test
    public void testAnnotationInjection() throws Exception {
        SimpleEjb bean = lookup(SimpleEjb.class.getSimpleName(), SimpleEjb.class);
        Assert.assertEquals("hello", bean.hello());
    }


    protected <T> T lookupEjb(String beanName, Class<T> interfaceType) throws NamingException {
        return interfaceType.cast(iniCtx.lookup("ejb:" + APP_NAME + "/" + MODULE_NAME + "//" + beanName + "!" + interfaceType.getName()));
    }

    protected <T> T lookup(String beanName, Class<T> interfaceType) throws NamingException {
        return interfaceType.cast(iniCtx.lookup("java:global/" + MODULE_NAME + "/" + beanName + "!" + interfaceType.getName()));
    }

}
