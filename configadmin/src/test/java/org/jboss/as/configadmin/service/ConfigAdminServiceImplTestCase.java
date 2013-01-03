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
package org.jboss.as.configadmin.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import junit.framework.Assert;

import org.jboss.as.configadmin.ConfigAdminListener;
import org.jboss.as.configadmin.parser.ConfigAdminExtension;
import org.jboss.as.configadmin.parser.ModelConstants;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * @author David Bosschaert
 */
public class ConfigAdminServiceImplTestCase {
    @Test
    public void testPutConfiguration() throws Exception {

        ModelNode expectedAddr = new ModelNode();
        expectedAddr.add(new ModelNode().set(ModelDescriptionConstants.SUBSYSTEM, ConfigAdminExtension.SUBSYSTEM_NAME));
        expectedAddr.add(new ModelNode().set(ModelConstants.CONFIGURATION, "a.b.c"));
        ModelNode expectedOp = Util.getEmptyOperation(ModelDescriptionConstants.ADD, expectedAddr);
        ModelNode entries = new ModelNode();
        entries.get("a.key").set("A Value");
        expectedOp.get(ModelConstants.ENTRIES).set(entries);

        ModelNode success = new ModelNode();
        success.set(ModelDescriptionConstants.OUTCOME, ModelDescriptionConstants.SUCCESS);

        ModelControllerClient mockControllerClient = Mockito.mock(ModelControllerClient.class);
        Mockito.when(mockControllerClient.execute(expectedOp)).thenReturn(success);

        // Initialize the ConfigAdminServiceImpl object
        ConfigAdminServiceImpl cas = createConfigAdminServiceImpl();
        setControllerClient(cas, mockControllerClient);
        setSynchronousExecutor(cas);

        // Register listener
        TestConfigAdminListener testListener = new TestConfigAdminListener();
        cas.addListener(testListener);

        Hashtable<String, String> config = new Hashtable<String, String>();
        config.put("a.key", "A Value");

        assertEquals("Precondition", 0, testListener.pidList.size());
        assertNull(cas.putConfiguration("a.b.c", config));
    }

    @Test
    public void testUpdateConfiguration() throws Exception {
        Dictionary<String, String> initial = new Hashtable<String, String>();
        initial.put("some.key", "some value");

        ModelNode expectedAddr = new ModelNode();
        expectedAddr.add(new ModelNode().set(ModelDescriptionConstants.SUBSYSTEM, ConfigAdminExtension.SUBSYSTEM_NAME));
        expectedAddr.add(new ModelNode().set(ModelConstants.CONFIGURATION, "a.b.c"));
        ModelNode expectedOp = Util.getEmptyOperation(ModelConstants.UPDATE, expectedAddr);
        ModelNode entries = new ModelNode();
        entries.get("a.key").set("A Value");
        expectedOp.get(ModelConstants.ENTRIES).set(entries);

        ModelNode success = new ModelNode();
        success.set(ModelDescriptionConstants.OUTCOME, ModelDescriptionConstants.SUCCESS);

        ModelControllerClient mockControllerClient = Mockito.mock(ModelControllerClient.class);
        Mockito.when(mockControllerClient.execute(expectedOp)).thenReturn(success);

        // Initialize the ConfigAdminServiceImpl object
        ConfigAdminServiceImpl cas = createConfigAdminServiceImpl();
        setControllerClient(cas, mockControllerClient);
        setSynchronousExecutor(cas);

        // Register listener
        TestConfigAdminListener testListener = new TestConfigAdminListener();
        cas.addListener(testListener);

        Hashtable<String, String> config = new Hashtable<String, String>();
        config.put("a.key", "A Value");

        assertEquals("Precondition", 0, testListener.pidList.size());
        assertNull(cas.putConfiguration("a.b.c", config));
    }

    @Test
    public void testRemoveConfiguration() throws Exception {
        Hashtable<String, String> initialConfig = new Hashtable<String, String>();
        initialConfig.put("x", "y");

        ModelNode expectedAddr = new ModelNode();
        expectedAddr.add(new ModelNode().set(ModelDescriptionConstants.SUBSYSTEM, ConfigAdminExtension.SUBSYSTEM_NAME));
        expectedAddr.add(new ModelNode().set(ModelConstants.CONFIGURATION, "abc"));
        ModelNode expectedOp = Util.getEmptyOperation(ModelDescriptionConstants.REMOVE, expectedAddr);

        ModelNode success = new ModelNode();
        success.set(ModelDescriptionConstants.OUTCOME, ModelDescriptionConstants.SUCCESS);

        ModelControllerClient mockControllerClient = Mockito.mock(ModelControllerClient.class);
        Mockito.when(mockControllerClient.execute(expectedOp)).thenReturn(success);

        // Initialize the ConfigAdminServiceImpl object
        ConfigAdminServiceImpl cas = createConfigAdminServiceImpl();
        setControllerClient(cas, mockControllerClient);
        setSynchronousExecutor(cas);

        // Register listener
        TestConfigAdminListener testListener = new TestConfigAdminListener();
        cas.addListener(testListener);

        assertEquals("Precondition", 0, testListener.pidList.size());
        assertNull(cas.removeConfiguration("abc"));
    }

    @Test
    public void testPutConfigurationInternal() throws Exception {

        // Initialize the ConfigAdminServiceImpl object
        ConfigAdminServiceImpl cas = createConfigAdminServiceImpl();
        setSynchronousExecutor(cas);

        // Call the operation
        Hashtable<String, String> config = new Hashtable<String, String>();
        config.put("a.b.c.d.e.f.g", "a value");
        cas.putConfigurationInternal("someconfig", config);

        // Verify expected behaviour
        Assert.assertEquals("a value", cas.getConfiguration("someconfig").get("a.b.c.d.e.f.g"));
    }

    @Test
    public void testRemoveConfigurationInternal() throws Exception {

        // Initialize the ConfigAdminServiceImpl object
        ConfigAdminServiceImpl cas = createConfigAdminServiceImpl();
        setSynchronousExecutor(cas);

        Hashtable<String, String> config = new Hashtable<String, String>();
        config.put("a.b.c.d.e.f.g", "a value");
        cas.putConfigurationInternal("xx.yy", config);

        // Register listener
        TestConfigAdminListener testListener = new TestConfigAdminListener();
        cas.addListener(testListener);

        assertEquals("Precondition", 1, testListener.pidList.size());

        // Call the remove operation
        cas.removeConfigurationInternal("xx.yy");

        // Verify expected behaviour
        Assert.assertNull(cas.getConfiguration("xx.yy"));
        assertEquals(Arrays.asList("xx.yy", "xx.yy"), testListener.pidList);
        assertNull(testListener.propList.get(1));
    }

    private static ConfigAdminServiceImpl createConfigAdminServiceImpl() throws Exception {
        Constructor<ConfigAdminServiceImpl> ctor = ConfigAdminServiceImpl.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        return ctor.newInstance();
    }

    private static void setControllerClient(ConfigAdminServiceImpl cas, ModelControllerClient value) throws Exception {
        Field field = cas.getClass().getDeclaredField("controllerClient");
        field.setAccessible(true);
        unFinal(field);
        field.set(cas, value);
    }

    private static void setSynchronousExecutor(ConfigAdminServiceImpl cas) throws Exception {
        ExecutorService executor = Mockito.mock(ExecutorService.class);
        Field field = cas.getClass().getDeclaredField("mgmntOperationExecutor");
        field.setAccessible(true);
        unFinal(field);
        field.set(cas, new TestSynchronousExecutor(executor));
        field = cas.getClass().getDeclaredField("asyncListnersExecutor");
        field.setAccessible(true);
        unFinal(field);
        field.set(cas, new TestSynchronousExecutor(executor));
    }

    private static void unFinal(Field field) throws NoSuchFieldException, IllegalAccessException {
        Field modifiers = Field.class.getDeclaredField("modifiers");
        modifiers.setAccessible(true);
        modifiers.setInt(field, field.getModifiers() & ~Modifier.FINAL);
    }

    private static class TestConfigAdminListener implements ConfigAdminListener {
        List<String> pidList = new ArrayList<String>();
        List<Dictionary<String, String>> propList = new ArrayList<Dictionary<String, String>>();

        @Override
        public void configurationModified(String pid, Dictionary<String, String> props) {
            pidList.add(pid);
            propList.add(props);
        }

        @Override
        public Set<String> getPIDs() {
            return null;
        }
    }

    private static class TestSynchronousExecutor implements ExecutorService {

        ExecutorService delegate;

        TestSynchronousExecutor(ExecutorService delegate) {
            this.delegate = delegate;
        }

        @Override
        public void execute(Runnable command) {
            command.run();
        }

        public Future<?> submit(Runnable task) {
            task.run();
            return null;
        }

        public void shutdown() {
            delegate.shutdown();
        }

        public List<Runnable> shutdownNow() {
            return delegate.shutdownNow();
        }

        public boolean isShutdown() {
            return delegate.isShutdown();
        }

        public boolean isTerminated() {
            return delegate.isTerminated();
        }

        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            return delegate.awaitTermination(timeout, unit);
        }

        public <T> Future<T> submit(Callable<T> task) {
            throw new UnsupportedOperationException();
        }

        public <T> Future<T> submit(Runnable task, T result) {
            throw new UnsupportedOperationException();
        }

        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
            throw new UnsupportedOperationException();
        }

        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
            throw new UnsupportedOperationException();
        }

        public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
            throw new UnsupportedOperationException();
        }

        public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            throw new UnsupportedOperationException();
        }
    }
}
