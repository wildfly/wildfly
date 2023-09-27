/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.remote.byvalue;

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
public class RemoteInvocationByValueTestCase {

    private static final String ARCHIVE_NAME = "RemoteInvocationTest";

    @Deployment
    public static Archive<?> deploy() {

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar");
        jar.addPackage(RemoteInvocationByValueTestCase.class.getPackage());
        return jar;
    }

    @ArquillianResource
    private InitialContext iniCtx;

    protected <T> T lookup(String beanName, Class<T> interfaceType) throws NamingException {
        return interfaceType.cast(iniCtx.lookup("java:global/" + ARCHIVE_NAME + "/" + beanName + "!" + interfaceType.getName()));
    }

    @Test
    public void testInvocationOnRemoteInterface() throws Exception {
        String[] array = {"hello"};
        RemoteInterface remote = lookup(StatelessRemoteBean.class.getSimpleName(), RemoteInterface.class);
        remote.modifyArray(array);
        Assert.assertEquals("hello", array[0]);
        LocalInterface local = lookup(StatelessRemoteBean.class.getSimpleName(), LocalInterface.class);
        local.modifyArray(array);
        Assert.assertEquals("goodbye", array[0]);
    }
}
