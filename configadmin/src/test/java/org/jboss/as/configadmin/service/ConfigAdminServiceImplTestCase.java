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
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

import org.jboss.as.configadmin.parser.ConfigAdminExtension;
import org.jboss.as.configadmin.parser.ConfigAdminState;
import org.jboss.as.configadmin.parser.ModelConstants;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.value.ImmediateValue;
import org.jboss.msc.value.InjectedValue;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * @author David Bosschaert
 */
public class ConfigAdminServiceImplTestCase {
    @Test
    public void testPutConfiguration() throws Exception {
        // Set up some mocks
        ConfigAdminState mockState = Mockito.mock(ConfigAdminState.class);

        ModelNode expectedAddr = new ModelNode();
        expectedAddr.add(new ModelNode().set(ModelDescriptionConstants.SUBSYSTEM, ConfigAdminExtension.SUBSYSTEM_NAME));
        expectedAddr.add(new ModelNode().set(ModelConstants.CONFIGURATION, "a.b.c"));
        ModelNode expectedOp = Util.getEmptyOperation(ModelDescriptionConstants.ADD, expectedAddr);
        ModelNode entries = new ModelNode();
        entries.get(ConfigAdminService.SOURCE_PROPERTY_KEY).set(ConfigAdminService.FROM_NONDMR_SOURCE_VALUE);
        entries.get("a.key").set("A Value");
        expectedOp.get(ModelConstants.ENTRIES).set(entries);

        ModelNode success = new ModelNode();
        success.set(ModelDescriptionConstants.OUTCOME, ModelDescriptionConstants.SUCCESS);

        ModelControllerClient mockControllerClient = Mockito.mock(ModelControllerClient.class);
        Mockito.when(mockControllerClient.execute(expectedOp)).thenReturn(success);

        // Initialize the ConfigAdminServiceImpl object
        ConfigAdminServiceImpl cas = createConfigAdminServiceImpl();
        setControllerClient(cas, mockControllerClient);
        injectSubsystemState(cas, mockState);
        setSynchronousExecutor(cas);

        // Register listener
        TestConfigAdminListener testListener = new TestConfigAdminListener();
        cas.addListener(testListener);

        Hashtable<String, String> config = new Hashtable<String, String>();
        config.put("a.key", "A Value");

        assertEquals("Precondition", 0, testListener.pidList.size());
        // Call the operation
        assertNull(cas.putConfiguration("a.b.c", config));

        // Verify expected behaviour
        Mockito.verify(mockState).putConfiguration("a.b.c", config);
        assertEquals(Arrays.asList("a.b.c"), testListener.pidList);
        assertEquals(Arrays.asList((Object) config), testListener.propList);
    }

    @Test
    public void testUpdateConfiguration() throws Exception {
        Dictionary<String, String> initial = new Hashtable<String, String>();
        initial.put("some.key", "some value");

        // Set up some mocks
        ConfigAdminState mockState = Mockito.mock(ConfigAdminState.class);
        Mockito.when(mockState.getConfiguration("a.b.c")).thenReturn(initial);

        ModelNode expectedAddr = new ModelNode();
        expectedAddr.add(new ModelNode().set(ModelDescriptionConstants.SUBSYSTEM, ConfigAdminExtension.SUBSYSTEM_NAME));
        expectedAddr.add(new ModelNode().set(ModelConstants.CONFIGURATION, "a.b.c"));
        ModelNode expectedOp = Util.getEmptyOperation(ModelConstants.UPDATE, expectedAddr);
        ModelNode entries = new ModelNode();
        entries.get(ConfigAdminService.SOURCE_PROPERTY_KEY).set(ConfigAdminService.FROM_NONDMR_SOURCE_VALUE);
        entries.get("a.key").set("A Value");
        expectedOp.get(ModelConstants.ENTRIES).set(entries);

        ModelNode success = new ModelNode();
        success.set(ModelDescriptionConstants.OUTCOME, ModelDescriptionConstants.SUCCESS);

        ModelControllerClient mockControllerClient = Mockito.mock(ModelControllerClient.class);
        Mockito.when(mockControllerClient.execute(expectedOp)).thenReturn(success);

        // Initialize the ConfigAdminServiceImpl object
        ConfigAdminServiceImpl cas = createConfigAdminServiceImpl();
        setControllerClient(cas, mockControllerClient);
        injectSubsystemState(cas, mockState);
        setSynchronousExecutor(cas);

        // Register listener
        TestConfigAdminListener testListener = new TestConfigAdminListener();
        cas.addListener(testListener);

        Hashtable<String, String> config = new Hashtable<String, String>();
        config.put("a.key", "A Value");

        assertEquals("Precondition", 0, testListener.pidList.size());
        // Call the operation
        assertEquals(initial, cas.putConfiguration("a.b.c", config));

        // Verify expected behaviour
        Mockito.verify(mockState).putConfiguration("a.b.c", config);
        assertEquals(Arrays.asList("a.b.c"), testListener.pidList);
        assertEquals(Arrays.asList((Object) config), testListener.propList);
    }

    @Test
    public void testPutConfigurationFromDMR() throws Exception {
        // Set up some mocks
        ConfigAdminState mockState = Mockito.mock(ConfigAdminState.class);

        // Initialize the ConfigAdminServiceImpl object
        ConfigAdminServiceImpl cas = createConfigAdminServiceImpl();
        injectSubsystemState(cas, mockState);
        setSynchronousExecutor(cas);

        Hashtable<String, String> config = new Hashtable<String, String>();
        config.put("a.b.c.d.e.f.g", "a value");

        // Call the operation
        cas.putConfigurationFromDMR("someconfig", config);

        // Verify expected behaviour
        Dictionary<String, String> expected = new Hashtable<String, String>(config);
        expected.put(ConfigAdminService.SOURCE_PROPERTY_KEY, ConfigAdminService.FROM_DMR_SOURCE_VALUE);
        Mockito.verify(mockState).putConfiguration("someconfig", expected);
    }

    @Test
    public void testRemoveConfiguration() throws Exception {
        Hashtable<String, String> initialConfig = new Hashtable<String, String>();
        initialConfig.put("x", "y");

        // Set up some mocks
        ConfigAdminState mockState = Mockito.mock(ConfigAdminState.class);
        Mockito.when(mockState.getConfiguration("abc")).thenReturn(initialConfig);

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
        injectSubsystemState(cas, mockState);
        setSynchronousExecutor(cas);

        // Register listener
        TestConfigAdminListener testListener = new TestConfigAdminListener();
        cas.addListener(testListener);

        assertEquals("Precondition", 0, testListener.pidList.size());
        // Call the remove operation
        assertEquals(initialConfig, cas.removeConfiguration("abc"));

        // Verify expected behaviour
        Mockito.verify(mockState).removeConfiguration("abc");
        assertEquals(Arrays.asList("abc"), testListener.pidList);
        assertEquals(Arrays.asList((Object) null), testListener.propList);
    }

    @Test
    public void testRemoveConfigurationFromDMR() throws Exception {
        ConfigAdminState mockState = Mockito.mock(ConfigAdminState.class);

        // Initialize the ConfigAdminServiceImpl object
        ConfigAdminServiceImpl cas = createConfigAdminServiceImpl();
        injectSubsystemState(cas, mockState);
        setSynchronousExecutor(cas);

        // Register listener
        TestConfigAdminListener testListener = new TestConfigAdminListener();
        cas.addListener(testListener);

        assertEquals("Precondition", 0, testListener.pidList.size());
        // Call the remove operation
        cas.removeConfigurationFromDMR("xx.yy");

        // Verify expected behaviour
        Mockito.verify(mockState).removeConfiguration("xx.yy");
        assertEquals(Arrays.asList("xx.yy"), testListener.pidList);
        assertEquals(Arrays.asList((String) null), testListener.propList);
    }

    private static ConfigAdminServiceImpl createConfigAdminServiceImpl() throws Exception {
        Constructor<ConfigAdminServiceImpl> ctor = ConfigAdminServiceImpl.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        return ctor.newInstance();
    }

    private static void injectSubsystemState(ConfigAdminServiceImpl cas, ConfigAdminState mockState) throws Exception {
        Field field = cas.getClass().getDeclaredField("injectedSubsystemState");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        InjectedValue<ConfigAdminState> injected = (InjectedValue<ConfigAdminState>) field.get(cas);
        injected.setValue(new ImmediateValue<ConfigAdminState>(mockState));
    }

    private static void setControllerClient(ConfigAdminServiceImpl cas, ModelControllerClient value) throws Exception {
        Field field = cas.getClass().getDeclaredField("controllerClient");
        field.setAccessible(true);
        unFinal(field);
        field.set(cas, value);
    }

    private static void setSynchronousExecutor(ConfigAdminServiceImpl cas) throws Exception {
        Field field = cas.getClass().getDeclaredField("executor");
        field.setAccessible(true);
        unFinal(field);
        field.set(cas, new TestSynchronousExecutor());
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

    private static class TestSynchronousExecutor implements Executor {
        @Override
        public void execute(Runnable command) {
            command.run();
        }
    }
}
