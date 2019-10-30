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
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.PropertyPermission;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;

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
        jar.addClass(TimeoutUtil.class);
        jar.addAsManifestResource(createPermissionsXmlAsset(
                new PropertyPermission("ts.timeout.factor", "read")
        ), "permissions.xml");
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

    /**
     * Tests that an exception thrown from a async EJB invocation, leads to the returned
     * {@link Future} to be marked as {@link Future#isDone() done}
     *
     * @throws Exception
     * @see <a href="https://issues.jboss.org/browse/WFLY-9624">WFLY-9624</a> for more details
     */
    @Test
    public void testFutureDoneInLocalAsyncInvocation() throws Exception {
        final LocalInterface localBean = lookup(StatelessRemoteBean.class.getSimpleName(), LocalInterface.class);
        final Future<Void> future = localBean.alwaysFail();
        // try a few times to make sure it's "done". it should be done immediately, so trying just
        // a few times should be fine
        for (int i = 0; i < 5; i++) {
            if (future.isDone()) {
                break;
            }
            // wait for a while
            Thread.sleep(TimeoutUtil.adjust(500));
        }
        Assert.assertTrue("Async invocation which was expected to fail immediately, wasn't considered \"done\"", future.isDone());
        try {
            // once the future is marked as done, the get should immediately return, but we
            // do send the timeouts here, just so that we don't end up blocking in this testcase
            // due to any bugs in the implementation of the returned future
            future.get(1, TimeUnit.SECONDS);
            Assert.fail("Async invocation was expected to throw an application exception, but it didn't");
        } catch (ExecutionException ee) {
            // we expect a AppException
            if (!(ee.getCause() instanceof AppException)) {
                throw ee;
            }
        }
    }
}
