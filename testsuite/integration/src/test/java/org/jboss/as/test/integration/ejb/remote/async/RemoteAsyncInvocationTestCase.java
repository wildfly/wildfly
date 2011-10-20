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

package org.jboss.as.test.integration.ejb.remote.async;

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
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Simple remote ejb tests
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
public class RemoteAsyncInvocationTestCase {

    private static final String ARCHIVE_NAME = "RemoteInvocationTest";

    @Deployment
    public static Archive<?> deploy() {

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar");
        jar.addPackage(RemoteAsyncInvocationTestCase.class.getPackage());
        return jar;
    }

    @ArquillianResource
    private InitialContext iniCtx;

    protected <T> T lookup(String beanName, Class<T> interfaceType) throws NamingException {
        return interfaceType.cast(iniCtx.lookup("java:global/" + ARCHIVE_NAME + "/" + beanName + "!" + interfaceType.getName()));
    }

    @Test
    public void testRemoteAsyncInvocationByValue() throws Exception {
        StatelessRemoteBean.reset();
        String[] array = {"hello"};
        RemoteInterface remote = lookup(StatelessRemoteBean.class.getSimpleName(), RemoteInterface.class);
        remote.modifyArray(array);
        StatelessRemoteBean.startLatch.countDown();
        if (!StatelessRemoteBean.doneLatch.await(5, TimeUnit.SECONDS)) {
            throw new RuntimeException("Invocation was not asynchronous");
        }
        Assert.assertEquals("hello", array[0]);
    }

    @Test
    public void testRemoteAsyncInvocationByValueFromEjbInjcation() throws Exception {
        StatelessRemoteBean.reset();
        String[] array = {"hello"};
        StatelessRunningBean remote = lookup(StatelessRunningBean.class.getSimpleName(), StatelessRunningBean.class);
        remote.modifyArray(array);
        StatelessRemoteBean.startLatch.countDown();

        if (!StatelessRemoteBean.doneLatch.await(5, TimeUnit.SECONDS)) {
            throw new RuntimeException("Invocation was not asynchronous");
        }
        Assert.assertEquals("hello", array[0]);
    }

    @Test
    public void testLocalAsyncInvocationByValue() throws Exception {
        StatelessRemoteBean.reset();
        String[] array = {"hello"};
        LocalInterface remote = lookup(StatelessRemoteBean.class.getSimpleName(), LocalInterface.class);
        remote.passByReference(array);
        StatelessRemoteBean.startLatch.countDown();

        if (!StatelessRemoteBean.doneLatch.await(5, TimeUnit.SECONDS)) {
            throw new RuntimeException("Invocation was not asynchronous");
        }
        Assert.assertEquals("goodbye", array[0]);
    }

    @Test
    public void testReturnAsyncInvocationReturningValue() throws Exception {
        RemoteInterface remote = lookup(StatelessRemoteBean.class.getSimpleName(), RemoteInterface.class);
        Future<String> value = remote.hello();
        Assert.assertEquals("hello", value.get(5, TimeUnit.SECONDS));
    }
}
