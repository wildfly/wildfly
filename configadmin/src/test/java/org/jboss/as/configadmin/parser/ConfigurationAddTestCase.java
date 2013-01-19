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

import junit.framework.Assert;
import org.jboss.as.configadmin.ConfigAdmin;
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

        // Create the operation model node
        Hashtable<String, String> dict = new Hashtable<String, String>();
        dict.put("x.y", "a b");
        ModelNode operation = getOperationModelNode("some.config", dict);
        ModelNode model = new ModelNode();
        ConfigurationAdd.INSTANCE.populateModel(operation, model);

        ModelNode propValue = model.get(ConfigurationResource.ENTRIES.getName(), "x.y");
        Assert.assertEquals(dict.get("x.y"), propValue.asString());

        // Set up some mock objects
        ConfigAdminServiceImpl mockCAS = Mockito.mock(ConfigAdminServiceImpl.class);

        ServiceController mockCASServiceController = Mockito.mock(ServiceController.class);
        Mockito.when(mockCASServiceController.getValue()).thenReturn(mockCAS);

        ServiceRegistry mockServiceRegistry = Mockito.mock(ServiceRegistry.class);
        Mockito.when(mockServiceRegistry.getService(ConfigAdmin.SERVICE_NAME)).thenReturn(mockCASServiceController);

        OperationContext mockOperationContext = Mockito.mock(OperationContext.class);
        Mockito.when(mockOperationContext.resolveExpressions(propValue)).thenReturn(propValue);
        Mockito.when(mockOperationContext.getServiceRegistry(true)).thenReturn(mockServiceRegistry);

        // Invoke the Add operation
        ConfigurationAdd.INSTANCE.performRuntime(mockOperationContext, operation, model, null, null);

        // Verify the results
        Mockito.verify(mockCAS).putConfigurationInternal("some.config", dict);
        assertNull(getInitializationService());
    }

    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void testConfigAdminArrivesLater() throws Exception {

        // Create the operation model node
        Hashtable<String, String> values = new Hashtable<String, String>();
        values.put("a", "aa");
        values.put("b", "bb");
        ModelNode operation = getOperationModelNode("a.b.c", values);
        ModelNode model = new ModelNode();
        ConfigurationAdd.INSTANCE.populateModel(operation, model);

        ModelNode aValue = model.get(ConfigurationResource.ENTRIES.getName(), "a");
        Assert.assertEquals(values.get("a"), aValue.asString());
        ModelNode bValue = model.get(ConfigurationResource.ENTRIES.getName(), "b");
        Assert.assertEquals(values.get("b"), bValue.asString());

        // Set up some mock objects
        ServiceRegistry mockServiceRegistry = Mockito.mock(ServiceRegistry.class);

        ServiceBuilder mockBuilder = Mockito.mock(ServiceBuilder.class);

        ServiceTarget mockServiceTarget = Mockito.mock(ServiceTarget.class);
        Mockito.when(mockServiceTarget.addService(
                Mockito.eq(ServiceName.JBOSS.append("configadmin", "data_initialization")),
                Mockito.any(Service.class))).
            thenReturn(mockBuilder);

        OperationContext mockOperationContext = Mockito.mock(OperationContext.class);
        Mockito.when(mockOperationContext.resolveExpressions(aValue)).thenReturn(aValue);
        Mockito.when(mockOperationContext.resolveExpressions(bValue)).thenReturn(bValue);
        Mockito.when(mockOperationContext.getServiceRegistry(true)).thenReturn(mockServiceRegistry);
        Mockito.when(mockOperationContext.getServiceTarget()).thenReturn(mockServiceTarget);

        // Invoke the Add operation
        ConfigurationAdd.INSTANCE.performRuntime(mockOperationContext, operation, model, null, null);

        // Check that the service that depends on the Config Admin Service has been created
        Mockito.verify(mockBuilder).addDependency(
                Mockito.eq(ConfigAdmin.SERVICE_NAME),
                Mockito.eq(ConfigAdmin.class),
                Mockito.any(Injector.class));
        Mockito.verify(mockBuilder).install();

        // Set up the mock Config Admin Service
        ConfigAdminServiceImpl mockCAS = Mockito.mock(ConfigAdminServiceImpl.class);
        ConfigurationAdd.InitializeConfigAdminService initSvc = getInitializationService();
        Field injectedCASField = initSvc.getClass().getDeclaredField("injectedConfigAdminService");
        injectedCASField.setAccessible(true);
        InjectedValue<ConfigAdmin> injectedCAS = (InjectedValue<ConfigAdmin>) injectedCASField.get(initSvc);
        injectedCAS.setValue(new ImmediateValue<ConfigAdmin>(mockCAS));

        // Invoke the operation again
        Hashtable<String, String> values2 = new Hashtable<String, String>();
        values2.put("x", "x");
        ModelNode op2 = getOperationModelNode("xx", values2);
        ModelNode mod2 = new ModelNode();
        ConfigurationAdd.INSTANCE.populateModel(op2, mod2);

        ModelNode xValue = mod2.get(ConfigurationResource.ENTRIES.getName(), "x");
        Assert.assertEquals(values2.get("x"), xValue.asString());

        Mockito.when(mockOperationContext.resolveExpressions(xValue)).thenReturn(xValue);

        ConfigurationAdd.INSTANCE.performRuntime(mockOperationContext, op2, mod2, null, null);

        initSvc.start(null);

        Mockito.verify(mockCAS).putConfigurationInternal("a.b.c", values);
        Mockito.verify(mockCAS).putConfigurationInternal("xx", values2);
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
