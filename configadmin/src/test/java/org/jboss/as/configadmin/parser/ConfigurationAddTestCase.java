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
package org.jboss.as.configadmin.parser;

import static org.junit.Assert.assertNull;

import java.lang.reflect.Field;
import java.util.Hashtable;
import java.util.Map;

import org.jboss.as.configadmin.service.ConfigAdminService;
import org.jboss.as.configadmin.service.ConfigAdminServiceImpl;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.ImmediateValue;
import org.jboss.msc.value.InjectedValue;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * @author David Bosschaert
 */
public class ConfigurationAddTestCase {
    @Before
    public void setUp() throws Exception {
        clearInitializationService();
    }

    @After
    public void tearDown() throws Exception {
        clearInitializationService();
    }

    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void testConfigAdminPresent() throws Exception {
        // Set up some mock objects
        ConfigAdminServiceImpl mockCAS = Mockito.mock(ConfigAdminServiceImpl.class);

        ServiceController mockCASServiceController = Mockito.mock(ServiceController.class);
        Mockito.when(mockCASServiceController.getValue()).thenReturn(mockCAS);

        ServiceRegistry mockServiceRegistry = Mockito.mock(ServiceRegistry.class);
        Mockito.when(mockServiceRegistry.getService(ConfigAdminService.SERVICE_NAME)).thenReturn(mockCASServiceController);

        OperationContext mockOperationContext = Mockito.mock(OperationContext.class);
        Mockito.when(mockOperationContext.getServiceRegistry(true)).thenReturn(mockServiceRegistry);

        // Create the operation model node
        Hashtable<String, String> dict = new Hashtable<String, String>();
        dict.put("x.y", "a b");
        ModelNode operation = getOperationModelNode("some.config", dict);

        // Invoke the Add operation
        ConfigurationAdd.INSTANCE.performRuntime(mockOperationContext, operation, null, null, null);

        // Verify the results
        Mockito.verify(mockCAS).putConfigurationFromDMR("some.config", dict);
        assertNull(getInitializationService());
    }

    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void testConfigAdminArrivesLater() throws Exception {
        // Set up some mock objects
        ServiceRegistry mockServiceRegistry = Mockito.mock(ServiceRegistry.class);

        ServiceBuilder mockBuilder = Mockito.mock(ServiceBuilder.class);

        ServiceTarget mockServiceTarget = Mockito.mock(ServiceTarget.class);
        Mockito.when(mockServiceTarget.addService(
                Mockito.eq(ServiceName.JBOSS.append("configadmin", "data_initialization")),
                Mockito.any(Service.class))).
            thenReturn(mockBuilder);

        OperationContext mockOperationContext = Mockito.mock(OperationContext.class);
        Mockito.when(mockOperationContext.getServiceRegistry(true)).thenReturn(mockServiceRegistry);
        Mockito.when(mockOperationContext.getServiceTarget()).thenReturn(mockServiceTarget);

        // Create the operation model node
        Hashtable<String, String> values = new Hashtable<String, String>();
        values.put("a", "aa");
        values.put("b", "bb");
        ModelNode operation = getOperationModelNode("a.b.c", values);

        // Invoke the Add operation
        ConfigurationAdd.INSTANCE.performRuntime(mockOperationContext, operation, null, null, null);

        // Check that the service that depends on the Config Admin Service has been created
        Mockito.verify(mockBuilder).addDependency(
                Mockito.eq(ConfigAdminService.SERVICE_NAME),
                Mockito.eq(ConfigAdminService.class),
                Mockito.any(Injector.class));
        Mockito.verify(mockBuilder).install();

        // Set up the mock Config Admin Service
        ConfigAdminServiceImpl mockCAS = Mockito.mock(ConfigAdminServiceImpl.class);
        ConfigurationAdd.InitializeConfigAdminService initSvc = getInitializationService();
        Field injectedCASField = initSvc.getClass().getDeclaredField("injectedConfigAdminService");
        injectedCASField.setAccessible(true);
        InjectedValue<ConfigAdminService> injectedCAS = (InjectedValue<ConfigAdminService>) injectedCASField.get(initSvc);
        injectedCAS.setValue(new ImmediateValue<ConfigAdminService>(mockCAS));

        // Invoke the operation again
        Hashtable<String, String> values2 = new Hashtable<String, String>();
        values2.put("x", "x");
        values2.put(ConfigAdminService.SOURCE_PROPERTY_KEY, ConfigAdminService.FROM_NONDMR_SOURCE_VALUE);
        ModelNode op2 = getOperationModelNode("xx", values2);
        ConfigurationAdd.INSTANCE.performRuntime(mockOperationContext, op2, null, null, null);

        initSvc.start(null);

        Mockito.verify(mockCAS).putConfigurationFromDMR("a.b.c", values);
        Mockito.verify(mockCAS).putConfigurationFromDMR("xx", values2);
    }

    private ModelNode getOperationModelNode(String pid, Map<String, String> props) {
        ModelNode addr = new ModelNode();
        addr.add(new ModelNode().set(ModelDescriptionConstants.SUBSYSTEM, ConfigAdminExtension.SUBSYSTEM_NAME));
        addr.add(new ModelNode().set(ModelConstants.CONFIGURATION, pid));
        ModelNode operation = Util.getEmptyOperation(ModelDescriptionConstants.ADD, addr);
        ModelNode entries = new ModelNode();

        for (Map.Entry<String, String> entry : props.entrySet()) {
            entries.get(entry.getKey()).set(entry.getValue());
        }
        operation.get(ModelConstants.ENTRIES).set(entries);
        return operation;
    }

    private ConfigurationAdd.InitializeConfigAdminService getInitializationService() throws Exception {
        Field field = ConfigurationAdd.class.getDeclaredField("initializationService");
        field.setAccessible(true);
        return (ConfigurationAdd.InitializeConfigAdminService) field.get(ConfigurationAdd.INSTANCE);
    }

    private void clearInitializationService() throws Exception {
        Field field = ConfigurationAdd.class.getDeclaredField("initializationService");
        field.setAccessible(true);
        field.set(ConfigurationAdd.INSTANCE, null);
    }
}
