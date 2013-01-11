/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.jmx;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILDREN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEFAULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXPRESSIONS_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MAX;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MIN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REPLY_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE_TYPE;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenMBeanAttributeInfo;
import javax.management.openmbean.OpenMBeanOperationInfo;
import javax.management.openmbean.OpenMBeanParameterInfo;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularType;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.xml.stream.XMLStreamException;

import junit.framework.Assert;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.operations.common.ResolveExpressionHandler;
import org.jboss.as.controller.operations.global.WriteAttributeHandlers;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.AttributeAccess.Storage;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.jmx.ModelControllerMBeanTestCase.TestExtension.ComplexOperation;
import org.jboss.as.jmx.ModelControllerMBeanTestCase.TestExtension.IntOperationWithParams;
import org.jboss.as.jmx.ModelControllerMBeanTestCase.TestExtension.VoidOperationNoParams;
import org.jboss.as.jmx.model.ModelControllerMBeanHelper;
import org.jboss.as.network.SocketBinding;
import org.jboss.as.remoting.EndpointService;
import org.jboss.as.remoting.RemotingServices;
import org.jboss.as.remoting.management.ManagementRemotingServices;
import org.jboss.as.subsystem.test.AbstractSubsystemTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.ControllerInitializer;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLMapper;
import org.junit.Test;
import org.xnio.OptionMap;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ModelControllerMBeanTestCase extends AbstractSubsystemTest {

    private static final String LAUNCH_TYPE = "launch-type";
    private static final String TYPE_STANDALONE = "STANDALONE";
    private static final String TYPE_DOMAIN = "DOMAIN";

    private static final String LEGACY_DOMAIN = "jboss.resolved";
    private final static ObjectName LEGACY_ROOT_NAME = ModelControllerMBeanHelper.createRootObjectName(LEGACY_DOMAIN);
    private final static ObjectName LEGACY_INTERFACE_NAME = createObjectName(LEGACY_DOMAIN + ":interface=test-interface");
    private final static ObjectName LEGACY_SOCKET_BINDING_GROUP_NAME = createObjectName(LEGACY_DOMAIN + ":socket-binding-group=test-socket-binding-group");
    private final static ObjectName LEGACY_SERVER_SOCKET_BINDING_NAME = createObjectName(LEGACY_DOMAIN + ":socket-binding-group=test-socket-binding-group,socket-binding=server");
    private final static ObjectName LEGACY_SERVER_SOCKET_BINDING_NAME_2 = createObjectName(LEGACY_DOMAIN + ":socket-binding=server,socket-binding-group=test-socket-binding-group");
    private final static ObjectName LEGACY_SUBSYSTEM_NAME = createObjectName(LEGACY_DOMAIN + ":subsystem=jmx");
    private final static ObjectName LEGACY_BAD_NAME = createObjectName(LEGACY_DOMAIN + ":type=bad");

    private static final String EXPR_DOMAIN = "jboss.as.expr";
    private final static ObjectName EXPR_ROOT_NAME = ModelControllerMBeanHelper.createRootObjectName(EXPR_DOMAIN);
    private final static ObjectName EXPR_INTERFACE_NAME = createObjectName(EXPR_DOMAIN + ":interface=test-interface");
    private final static ObjectName EXPR_SOCKET_BINDING_GROUP_NAME = createObjectName(EXPR_DOMAIN + ":socket-binding-group=test-socket-binding-group");
    private final static ObjectName EXPR_SERVER_SOCKET_BINDING_NAME = createObjectName(EXPR_DOMAIN + ":socket-binding-group=test-socket-binding-group,socket-binding=server");
    private final static ObjectName EXPR_SERVER_SOCKET_BINDING_NAME_2 = createObjectName(EXPR_DOMAIN + ":socket-binding=server,socket-binding-group=test-socket-binding-group");
    private final static ObjectName EXPR_SUBSYSTEM_NAME = createObjectName(EXPR_DOMAIN + ":subsystem=jmx");
    private final static ObjectName EXPR_BAD_NAME = createObjectName(LEGACY_DOMAIN + ":type=bad");

    public ModelControllerMBeanTestCase() {
        super(JMXExtension.SUBSYSTEM_NAME, new JMXExtension());
    }

    @Test
    public void testExposedMBeans() throws Exception {
        MBeanServerConnection connection = setupAndGetConnection(new BaseAdditionalInitialization(TYPE_STANDALONE));

        int count = connection.getMBeanCount();
        checkQueryMBeans(connection, count, null);
        checkQueryMBeans(connection, count, new ObjectName("*:*"));



        Set<ObjectInstance> filteredInstances = connection.queryMBeans(createObjectName(LEGACY_DOMAIN + ":socket-binding-group=*,*"),
                null);
        Set<ObjectName> filteredNames = connection.queryNames(createObjectName(LEGACY_DOMAIN + ":socket-binding-group=*,*"), null);
        Assert.assertEquals(2, filteredInstances.size());
        Assert.assertEquals(2, filteredNames.size());
        checkSameMBeans(filteredInstances, filteredNames);
        assertContainsNames(filteredNames, LEGACY_SOCKET_BINDING_GROUP_NAME, LEGACY_SERVER_SOCKET_BINDING_NAME);

        filteredInstances = connection.queryMBeans(createObjectName(EXPR_DOMAIN + ":socket-binding-group=*,*"),
                null);
        filteredNames = connection.queryNames(createObjectName(EXPR_DOMAIN + ":socket-binding-group=*,*"), null);
        Assert.assertEquals(2, filteredInstances.size());
        Assert.assertEquals(2, filteredNames.size());
        checkSameMBeans(filteredInstances, filteredNames);
        assertContainsNames(filteredNames, EXPR_SOCKET_BINDING_GROUP_NAME, EXPR_SERVER_SOCKET_BINDING_NAME);
        // TODO test with QueryExp
    }

    private void checkQueryMBeans(MBeanServerConnection connection, int count, ObjectName filter) throws Exception {
        Set<ObjectInstance> instances = connection.queryMBeans(filter, null);
        Set<ObjectName> objectNames = connection.queryNames(filter, null);
        Assert.assertEquals(count, instances.size());
        Assert.assertEquals(count, objectNames.size());

        checkSameMBeans(instances, objectNames);
        assertContainsNames(objectNames,
                LEGACY_ROOT_NAME, LEGACY_INTERFACE_NAME, LEGACY_SOCKET_BINDING_GROUP_NAME, LEGACY_SERVER_SOCKET_BINDING_NAME, LEGACY_SUBSYSTEM_NAME,
                EXPR_ROOT_NAME, EXPR_INTERFACE_NAME, EXPR_SOCKET_BINDING_GROUP_NAME, EXPR_SERVER_SOCKET_BINDING_NAME, EXPR_SUBSYSTEM_NAME);
    }

    @Test
    public void testGetObjectInstance() throws Exception {
        MBeanServerConnection connection = setupAndGetConnection(new BaseAdditionalInitialization(TYPE_STANDALONE));

        Assert.assertNotNull(connection.getObjectInstance(LEGACY_ROOT_NAME));
        Assert.assertEquals(LEGACY_ROOT_NAME, connection.getObjectInstance(LEGACY_ROOT_NAME).getObjectName());
        Assert.assertNotNull(connection.getObjectInstance(LEGACY_INTERFACE_NAME));
        Assert.assertNotNull(connection.getObjectInstance(LEGACY_SERVER_SOCKET_BINDING_NAME));
        Assert.assertNotNull(connection.getObjectInstance(LEGACY_SERVER_SOCKET_BINDING_NAME_2));
        try {
            connection.getObjectInstance(LEGACY_BAD_NAME);
            Assert.fail();
        } catch (InstanceNotFoundException expected) {
        }

        Assert.assertNotNull(connection.getObjectInstance(EXPR_ROOT_NAME));
        Assert.assertEquals(EXPR_ROOT_NAME, connection.getObjectInstance(EXPR_ROOT_NAME).getObjectName());
        Assert.assertNotNull(connection.getObjectInstance(EXPR_INTERFACE_NAME));
        Assert.assertNotNull(connection.getObjectInstance(EXPR_SERVER_SOCKET_BINDING_NAME));
        Assert.assertNotNull(connection.getObjectInstance(EXPR_SERVER_SOCKET_BINDING_NAME_2));
        try {
            connection.getObjectInstance(EXPR_BAD_NAME);
            Assert.fail();
        } catch (InstanceNotFoundException expected) {
        }
    }

    @Test
    public void testGetMBeanInfoStandalone() throws Exception {
        MBeanServerConnection connection = setupAndGetConnection(new MBeanInfoAdditionalInitialization(TYPE_STANDALONE, new TestExtension()));

        MBeanInfo info = connection.getMBeanInfo(LEGACY_ROOT_NAME);
        Assert.assertNotNull(info);

        //Make sure all occurrances of "-" have gone
        for (MBeanAttributeInfo attr : info.getAttributes()) {
            Assert.assertFalse(attr.getName().contains("-"));
        }
        for (MBeanOperationInfo op : info.getOperations()) {
            Assert.assertFalse(op.getName().contains("-"));
            for (MBeanParameterInfo param : op.getSignature()) {
                Assert.assertFalse(param.getName().contains("-"));
            }
        }

        // Make sure that the description gets set for things using resource
        // bundles
        info = connection.getMBeanInfo(createObjectName(LEGACY_DOMAIN + ":subsystem=jmx"));
        Assert.assertNotNull(info);
        Assert.assertEquals("The configuration of the JMX subsystem.", info.getDescription());

        info = connection.getMBeanInfo(createObjectName(LEGACY_DOMAIN + ":subsystem=test"));
        Assert.assertNotNull(info);
        Assert.assertEquals("A test subsystem", info.getDescription());

        checkMBeanInfoAttributes(info, true, false);

        MBeanOperationInfo[] operations = info.getOperations();
        Assert.assertEquals(3, operations.length);

        OpenMBeanOperationInfo op = findOperation(operations, VoidOperationNoParams.OPERATION_JMX_NAME);
        Assert.assertEquals(VoidOperationNoParams.OPERATION_JMX_NAME, op.getName());
        Assert.assertEquals("Test1", op.getDescription());
        Assert.assertEquals(0, op.getSignature().length);
        Assert.assertEquals(Void.class.getName(), op.getReturnType());

        op = findOperation(operations, IntOperationWithParams.OPERATION_JMX_NAME);
        Assert.assertEquals(IntOperationWithParams.OPERATION_JMX_NAME, op.getName());
        Assert.assertEquals("Test2", op.getDescription());
        Assert.assertEquals(String.class.getName(), op.getReturnType());
        Assert.assertEquals(5, op.getSignature().length);

        Assert.assertEquals("param1", op.getSignature()[0].getName());
        Assert.assertEquals("Param1", op.getSignature()[0].getDescription());
        Assert.assertEquals(Long.class.getName(), op.getSignature()[0].getType());

        Assert.assertEquals("param2", op.getSignature()[1].getName());
        Assert.assertEquals("Param2", op.getSignature()[1].getDescription());
        Assert.assertEquals(String[].class.getName(), op.getSignature()[1].getType());

        Assert.assertEquals("param3", op.getSignature()[2].getName());
        Assert.assertEquals("Param3", op.getSignature()[2].getDescription());
        Assert.assertEquals(TabularData.class.getName(), op.getSignature()[2].getType());
        assertMapType(assertCast(OpenMBeanParameterInfo.class, op.getSignature()[2]).getOpenType(), SimpleType.STRING, SimpleType.INTEGER);

        Assert.assertEquals("param4", op.getSignature()[3].getName());
        Assert.assertEquals("Param4", op.getSignature()[3].getDescription());
        Assert.assertEquals(Integer.class.getName(), op.getSignature()[3].getType());
        OpenMBeanParameterInfo parameterInfo = assertCast(OpenMBeanParameterInfo.class, op.getSignature()[3]);
        Assert.assertEquals(6, parameterInfo.getDefaultValue());
        Assert.assertEquals(5, parameterInfo.getMinValue());
        Assert.assertEquals(10, parameterInfo.getMaxValue());

        Assert.assertEquals("param5", op.getSignature()[4].getName());
        Assert.assertEquals("Param5", op.getSignature()[4].getDescription());
        Assert.assertEquals(Integer.class.getName(), op.getSignature()[4].getType());
        parameterInfo = assertCast(OpenMBeanParameterInfo.class, op.getSignature()[4]);
        Assert.assertNull(parameterInfo.getDefaultValue());
        Assert.assertNull(parameterInfo.getDefaultValue());
        Assert.assertEquals(new HashSet<Object>(Arrays.asList(3, 5, 7)), parameterInfo.getLegalValues());

        op = findOperation(operations, ComplexOperation.OPERATION_NAME);
        Assert.assertEquals(ComplexOperation.OPERATION_NAME, op.getName());
        Assert.assertEquals("Test3", op.getDescription());
        checkComplexTypeInfo(assertCast(CompositeType.class, op.getReturnOpenType()), false);
        Assert.assertEquals(1, op.getSignature().length);
        checkComplexTypeInfo(assertCast(CompositeType.class, assertCast(OpenMBeanParameterInfo.class, op.getSignature()[0]).getOpenType()), false);
    }

    @Test
    public void testGetMBeanInfoDomain() throws Exception {
        MBeanServerConnection connection = setupAndGetConnection(new MBeanInfoAdditionalInitialization(TYPE_DOMAIN, new TestExtension()));

        MBeanInfo info = connection.getMBeanInfo(LEGACY_ROOT_NAME);
        Assert.assertNotNull(info);

        //Make sure all occurrances of "-" have gone
        for (MBeanAttributeInfo attr : info.getAttributes()) {
            Assert.assertFalse(attr.getName().contains("-"));
        }
        for (MBeanOperationInfo op : info.getOperations()) {
            Assert.assertFalse(op.getName().contains("-"));
            for (MBeanParameterInfo param : op.getSignature()) {
                Assert.assertFalse(param.getName().contains("-"));
            }
        }

        // Make sure that the description gets set for things using resource
        // bundles
        info = connection.getMBeanInfo(createObjectName(LEGACY_DOMAIN + ":subsystem=jmx"));
        Assert.assertNotNull(info);
        Assert.assertEquals("The configuration of the JMX subsystem.", info.getDescription());

        info = connection.getMBeanInfo(createObjectName(LEGACY_DOMAIN + ":subsystem=test"));
        Assert.assertNotNull(info);
        Assert.assertEquals("A test subsystem", info.getDescription());

        //All attributes should be read-only
        checkMBeanInfoAttributes(info, false, false);


        MBeanOperationInfo[] operations = info.getOperations();
        Assert.assertEquals(1, operations.length);

        OpenMBeanOperationInfo op = findOperation(operations, VoidOperationNoParams.OPERATION_JMX_NAME);
        Assert.assertEquals(VoidOperationNoParams.OPERATION_JMX_NAME, op.getName());
        Assert.assertEquals("Test1", op.getDescription());
        Assert.assertEquals(0, op.getSignature().length);
        Assert.assertEquals(Void.class.getName(), op.getReturnType());
    }

    @Test
    public void testGetMBeanInfoExpressionsStandalone() throws Exception {
        MBeanServerConnection connection = setupAndGetConnection(new MBeanInfoAdditionalInitialization(TYPE_STANDALONE, new TestExtension(true)));

        MBeanInfo info = connection.getMBeanInfo(EXPR_ROOT_NAME);
        Assert.assertNotNull(info);

        //Make sure all occurrances of "-" have gone
        for (MBeanAttributeInfo attr : info.getAttributes()) {
            Assert.assertFalse(attr.getName().contains("-"));
        }
        for (MBeanOperationInfo op : info.getOperations()) {
            Assert.assertFalse(op.getName().contains("-"));
            for (MBeanParameterInfo param : op.getSignature()) {
                Assert.assertFalse(param.getName().contains("-"));
            }
        }

        // Make sure that the description gets set for things using resource
        // bundles
        info = connection.getMBeanInfo(createObjectName(EXPR_DOMAIN + ":subsystem=jmx"));
        Assert.assertNotNull(info);
        Assert.assertEquals("The configuration of the JMX subsystem.", info.getDescription());

        info = connection.getMBeanInfo(createObjectName(EXPR_DOMAIN + ":subsystem=test"));
        Assert.assertNotNull(info);
        Assert.assertEquals("A test subsystem", info.getDescription());

        checkMBeanInfoAttributes(info, true, true);

        MBeanOperationInfo[] operations = info.getOperations();
        Assert.assertEquals(3, operations.length);

        OpenMBeanOperationInfo op = findOperation(operations, VoidOperationNoParams.OPERATION_JMX_NAME);
        Assert.assertEquals(VoidOperationNoParams.OPERATION_JMX_NAME, op.getName());
        Assert.assertEquals("Test1", op.getDescription());
        Assert.assertEquals(0, op.getSignature().length);
        Assert.assertEquals(Void.class.getName(), op.getReturnType());

        op = findOperation(operations, IntOperationWithParams.OPERATION_JMX_NAME);
        Assert.assertEquals(IntOperationWithParams.OPERATION_JMX_NAME, op.getName());
        Assert.assertEquals("Test2", op.getDescription());
        Assert.assertEquals(String.class.getName(), op.getReturnType());
        Assert.assertEquals(5, op.getSignature().length);

        Assert.assertEquals("param1", op.getSignature()[0].getName());
        Assert.assertEquals("Param1", op.getSignature()[0].getDescription());
        Assert.assertEquals(String.class.getName(), op.getSignature()[0].getType());

        Assert.assertEquals("param2", op.getSignature()[1].getName());
        Assert.assertEquals("Param2", op.getSignature()[1].getDescription());
        Assert.assertEquals(String[].class.getName(), op.getSignature()[1].getType());

        Assert.assertEquals("param3", op.getSignature()[2].getName());
        Assert.assertEquals("Param3", op.getSignature()[2].getDescription());
        Assert.assertEquals(TabularData.class.getName(), op.getSignature()[2].getType());
        assertMapType(assertCast(OpenMBeanParameterInfo.class, op.getSignature()[2]).getOpenType(), SimpleType.STRING, SimpleType.STRING);

        Assert.assertEquals("param4", op.getSignature()[3].getName());
        Assert.assertEquals("Param4", op.getSignature()[3].getDescription());
        Assert.assertEquals(String.class.getName(), op.getSignature()[3].getType());
        OpenMBeanParameterInfo parameterInfo = assertCast(OpenMBeanParameterInfo.class, op.getSignature()[3]);
        Assert.assertNull(parameterInfo.getDefaultValue());
        Assert.assertNull(parameterInfo.getMinValue());
        Assert.assertNull(parameterInfo.getMaxValue());

        Assert.assertEquals("param5", op.getSignature()[4].getName());
        Assert.assertEquals("Param5", op.getSignature()[4].getDescription());
        Assert.assertEquals(String.class.getName(), op.getSignature()[4].getType());
        parameterInfo = assertCast(OpenMBeanParameterInfo.class, op.getSignature()[4]);
        Assert.assertNull(parameterInfo.getDefaultValue());
        Assert.assertNull(parameterInfo.getLegalValues());
        Assert.assertNull(parameterInfo.getDefaultValue());
        Assert.assertNull(parameterInfo.getLegalValues());

        op = findOperation(operations, ComplexOperation.OPERATION_NAME);
        Assert.assertEquals(ComplexOperation.OPERATION_NAME, op.getName());
        Assert.assertEquals("Test3", op.getDescription());
        checkComplexTypeInfo(assertCast(CompositeType.class, op.getReturnOpenType()), true);
        Assert.assertEquals(1, op.getSignature().length);
        checkComplexTypeInfo(assertCast(CompositeType.class, assertCast(OpenMBeanParameterInfo.class, op.getSignature()[0]).getOpenType()), true);
    }



    private void checkMBeanInfoAttributes(MBeanInfo info, boolean writable, boolean expressions) {
        //All attributes should be read-only
        MBeanAttributeInfo[] attributes = info.getAttributes();
        Assert.assertEquals(14, attributes.length);
        assertAttributeDescription(attributes[0], "roInt", expressions ? String.class.getName() : Integer.class.getName(), "A read-only int", true, false);
        assertAttributeDescription(attributes[1], "undefinedInt", expressions ? String.class.getName() : Integer.class.getName(), "A read-only int", true, writable);
        assertAttributeDescription(attributes[2], "int", expressions ? String.class.getName() : Integer.class.getName(), "A int", true, writable);
        assertAttributeDescription(attributes[3], "bigint", expressions ? String.class.getName() : BigInteger.class.getName(), "A big int", true, writable);
        assertAttributeDescription(attributes[4], "bigdec", expressions ? String.class.getName() : BigDecimal.class.getName(), "A big dec", true, writable);
        assertAttributeDescription(attributes[5], "boolean", expressions ? String.class.getName() : Boolean.class.getName(), "A boolean", true, writable);
        assertAttributeDescription(attributes[6], "bytes", byte[].class.getName(), "A bytes", true, writable);
        assertAttributeDescription(attributes[7], "double", expressions ? String.class.getName() : Double.class.getName(), "A double", true, writable);
        assertAttributeDescription(attributes[8], "string", expressions ? String.class.getName() : String.class.getName(), "A string", true, writable);
        assertAttributeDescription(attributes[9], "list", expressions ? String[].class.getName() : Integer[].class.getName(), "A list", true, writable);
        assertAttributeDescription(attributes[10], "long", expressions ? String.class.getName() : Long.class.getName(), "A long", true, writable);
        assertAttributeDescription(attributes[11], "type", String.class.getName(), "A type", true, writable);
        //type=OBJECT, value-type=a simple type -> a map
        assertAttributeDescription(attributes[12], "map", TabularData.class.getName(), "A map", true, writable);
        assertMapType(assertCast(OpenMBeanAttributeInfo.class, attributes[12]).getOpenType(), SimpleType.STRING, expressions ? SimpleType.STRING : SimpleType.INTEGER);

        checkComplexTypeInfo(assertCast(CompositeType.class, assertCast(OpenMBeanAttributeInfo.class, attributes[13]).getOpenType()), expressions);
    }


    @Test
    public void testReadWriteAttributeStandalone() throws Exception {
        MBeanServerConnection connection = setupAndGetConnection(new MBeanInfoAdditionalInitialization(TYPE_STANDALONE, new TestExtension()));

        ObjectName name = createObjectName(LEGACY_DOMAIN + ":subsystem=test");
        checkAttributeValues(connection, name, 1, null, 2, BigInteger.valueOf(3), BigDecimal.valueOf(4), false, new byte[] {5, 6}, 7.0, "8",
                Collections.singletonList(Integer.valueOf(9)), 10, ModelType.INT, "key1", 11, "key2", 12);
        Assert.assertNull(connection.getAttribute(name, "complex"));


        try {
            connection.setAttribute(name, new Attribute("roInt", 101));
            Assert.fail("roInt not writable");
        } catch (Exception expected) {
        }

        connection.setAttribute(name, new Attribute("int", 102));
        connection.setAttribute(name, new Attribute("undefinedInt", 103));
        connection.setAttribute(name, new Attribute("bigint", BigInteger.valueOf(104)));
        connection.setAttribute(name, new Attribute("bigdec", BigDecimal.valueOf(105)));
        connection.setAttribute(name, new Attribute("boolean", Boolean.TRUE));
        connection.setAttribute(name, new Attribute("bytes", new byte[] {106, 107}));
        connection.setAttribute(name, new Attribute("double", 108.0));
        connection.setAttribute(name, new Attribute("string", "109"));
        connection.setAttribute(name, new Attribute("list", new Integer[] {110}));
        connection.setAttribute(name, new Attribute("long", 111L));
        connection.setAttribute(name, new Attribute("type", ModelType.STRING.toString()));
        Map<String, Integer> map = new HashMap<String, Integer>();
        map.put("keyA", 112);
        map.put("keyB", 113);
        connection.setAttribute(name, new Attribute("map", map));
        MBeanInfo info = connection.getMBeanInfo(name);
        CompositeType complexType = assertCast(CompositeType.class, findAttribute(info.getAttributes(), "complex").getOpenType());
        connection.setAttribute(name, new Attribute("complex", createComplexData(connection, complexType, 1, BigDecimal.valueOf(2.0))));


        checkAttributeValues(connection, name, 1, 103, 102, BigInteger.valueOf(104), BigDecimal.valueOf(105), true, new byte[] {106, 107}, 108.0, "109",
                Collections.singletonList(Integer.valueOf(110)), 111, ModelType.STRING, "keyA", 112, "keyB", 113);
        CompositeData compositeData = assertCast(CompositeData.class, connection.getAttribute(name, "complex"));
        Assert.assertEquals(Integer.valueOf(1), compositeData.get("int-value"));
        Assert.assertEquals(BigDecimal.valueOf(2.0), compositeData.get("bigdecimal-value"));
    }

    @Test
    public void testReadWriteAttributeDomain() throws Exception {
        MBeanServerConnection connection = setupAndGetConnection(new MBeanInfoAdditionalInitialization(TYPE_DOMAIN, new TestExtension()));


        ObjectName name = createObjectName(LEGACY_DOMAIN + ":subsystem=test");

        checkAttributeValues(connection, name, 1, null, 2, BigInteger.valueOf(3), BigDecimal.valueOf(4), false, new byte[] {5, 6}, 7.0, "8",
                Collections.singletonList(Integer.valueOf(9)), 10, ModelType.INT, "key1", 11, "key2", 12);
        Assert.assertNull(connection.getAttribute(name, "complex"));


        try {
            connection.setAttribute(name, new Attribute("roInt", 101));
            Assert.fail("roInt not writable");
        } catch (Exception expected) {
        }
        try {
            connection.setAttribute(name, new Attribute("int", 102));
            Assert.fail("int not writable");
        } catch (Exception expected) {
        }
        try {
            connection.setAttribute(name, new Attribute("undefinedInt", 103));
            Assert.fail("undefinedInt not writable");
        } catch (Exception expected) {
        }
        try {
            //TODO BigInteger not working in current DMR version
            //connection.setAttribute(name, new Attribute("bigint", BigInteger.valueOf(104)));
            connection.setAttribute(name, new Attribute("bigdec", BigDecimal.valueOf(105)));
            Assert.fail("bigdec not writable");
        } catch (Exception expected) {
        }
        try {
            connection.setAttribute(name, new Attribute("boolean", Boolean.TRUE));
            Assert.fail("boolean not writable");
        } catch (Exception expected) {
        }
        try {
            connection.setAttribute(name, new Attribute("bytes", new byte[] {106, 107}));
            Assert.fail("bytes not writable");
        } catch (Exception expected) {
        }
        try {
            connection.setAttribute(name, new Attribute("double", 108.0));
            Assert.fail("double not writable");
        } catch (Exception expected) {
        }
        try {
            connection.setAttribute(name, new Attribute("string", "109"));
            Assert.fail("string not writable");
        } catch (Exception expected) {
        }
        try {
            connection.setAttribute(name, new Attribute("list", new Integer[] {110}));
            Assert.fail("list not writable");
        } catch (Exception expected) {
        }
        try {
            connection.setAttribute(name, new Attribute("long", 111L));
            Assert.fail("long not writable");
        } catch (Exception expected) {
        }
        try {
            connection.setAttribute(name, new Attribute("type", ModelType.STRING.toString()));
            Assert.fail("type not writable");
        } catch (Exception expected) {
        }
        try {
            Map<String, Integer> map = new HashMap<String, Integer>();
            map.put("keyA", 112);
            map.put("keyB", 113);
            connection.setAttribute(name, new Attribute("map", map));
            Assert.fail("map not writable");
        } catch (Exception expected) {
        }

        MBeanInfo info = connection.getMBeanInfo(name);
        CompositeType complexType = assertCast(CompositeType.class, findAttribute(info.getAttributes(), "complex").getOpenType());
        try {
            connection.setAttribute(name, new Attribute("complex", createComplexData(connection, complexType, 1, BigDecimal.valueOf(2.0))));
            Assert.fail("Complex not writable");
        } catch (Exception expected) {
        }

        checkAttributeValues(connection, name, 1, null, 2, BigInteger.valueOf(3), BigDecimal.valueOf(4), false, new byte[] {5, 6}, 7.0, "8",
                Collections.singletonList(Integer.valueOf(9)), 10, ModelType.INT, "key1", 11, "key2", 12);
        Assert.assertNull(connection.getAttribute(name, "complex"));
    }


    private void checkAttributeValues(MBeanServerConnection connection, ObjectName name,
            int roInt, Integer undefinedInt, int i, BigInteger bigInt, BigDecimal bigDecimal,
            boolean bool, byte[] bytes, double dbl, String s, List<Integer> list, long lng,
            ModelType type, String tblKey1, int tblValue1, String tblKey2, int tblValue2) throws Exception {
        Assert.assertEquals(roInt, assertCast(Integer.class, connection.getAttribute(name, "roInt")).intValue());
        if (undefinedInt == null) {
            Assert.assertNull(connection.getAttribute(name, "undefinedInt"));
        } else {
            Assert.assertEquals(undefinedInt, assertCast(Integer.class, connection.getAttribute(name, "undefinedInt")));
        }
        Assert.assertEquals(i, assertCast(Integer.class, connection.getAttribute(name, "int")).intValue());
        //TODO BigInteger not working in current DMR version
        //Assert.assertEquals(BigInteger.valueOf(3), assertCast(BigInteger.class, connection.getAttribute(name, "bigint")));
        Assert.assertEquals(bigDecimal, assertCast(BigDecimal.class, connection.getAttribute(name, "bigdec")));
        Assert.assertEquals(bool, assertCast(Boolean.class, connection.getAttribute(name, "boolean")).booleanValue());
        assertEqualByteArray(assertCast(byte[].class, connection.getAttribute(name, "bytes")), bytes);
        Assert.assertEquals(dbl, assertCast(Double.class, connection.getAttribute(name, "double")));
        Assert.assertEquals(s, assertCast(String.class, connection.getAttribute(name, "string")));

        Integer[] listValue = assertCast(Integer[].class, connection.getAttribute(name, "list"));
        Assert.assertEquals(list.size(), listValue.length);
        for (int ctr = 0 ; ctr < list.size() ; ctr++) {
            Assert.assertEquals(list.get(ctr), listValue[ctr]);
        }
        Assert.assertEquals(lng, assertCast(Long.class, connection.getAttribute(name, "long")).longValue());
        Assert.assertEquals(type, ModelType.valueOf(assertCast(String.class, connection.getAttribute(name, "type"))));
        TabularData tabularData = assertCast(TabularData.class, connection.getAttribute(name, "map"));
        Assert.assertEquals(2, tabularData.size());
        Assert.assertEquals(tblValue1, assertCast(Integer.class, tabularData.get(new Object[] {tblKey1}).get("value")).intValue());
        Assert.assertEquals(tblValue2, assertCast(Integer.class, tabularData.get(new Object[] {tblKey2}).get("value")).intValue());
    }

    @Test
    public void testReadWriteAttributeExpressionsStandalone() throws Exception {
        MBeanServerConnection connection = setupAndGetConnection(new MBeanInfoAdditionalInitialization(TYPE_STANDALONE, new TestExtension(true)));

        ObjectName name = createObjectName(EXPR_DOMAIN + ":subsystem=test");
        checkAttributeValues(connection, name, "1", null, "2", "3", "4", "false", new byte[] {5, 6}, "7.0", "8",
                Collections.singletonList("9"), "10", ModelType.INT.toString(), "key1", "11", "key2", "12");
        Assert.assertNull(connection.getAttribute(name, "complex"));


        try {
            connection.setAttribute(name, new Attribute("roInt", 101));
            Assert.fail("roInt not writable");
        } catch (Exception expected) {
        }

        connection.setAttribute(name, new Attribute("int", "${should.not.exist!!!!!:102}"));
        connection.setAttribute(name, new Attribute("undefinedInt", "${should.not.exist!!!!!:103}"));
        connection.setAttribute(name, new Attribute("bigint", "${should.not.exist!!!!!:104}"));
        connection.setAttribute(name, new Attribute("bigdec", "${should.not.exist!!!!!:105}"));
        connection.setAttribute(name, new Attribute("boolean", "${should.not.exist!!!!!:true}"));
        connection.setAttribute(name, new Attribute("bytes", new byte[] {106, 107}));
        connection.setAttribute(name, new Attribute("double", "${should.not.exist!!!!!:108.0}"));
        connection.setAttribute(name, new Attribute("string", "${should.not.exist!!!!!:109}"));
        connection.setAttribute(name, new Attribute("list", new String[] {"${should.not.exist!!!!!:110}"}));
        connection.setAttribute(name, new Attribute("long", "${should.not.exist!!!!!:111}"));
        connection.setAttribute(name, new Attribute("type", "${should.not.exist!!!!!:STRING}"));
        Map<String, String> map = new HashMap<String, String>();
        map.put("keyA", "${should.not.exist!!!!!:112}");
        map.put("keyB", "${should.not.exist!!!!!:113}");
        connection.setAttribute(name, new Attribute("map", map));
        MBeanInfo info = connection.getMBeanInfo(name);
        CompositeType complexType = assertCast(CompositeType.class, findAttribute(info.getAttributes(), "complex").getOpenType());
        connection.setAttribute(name, new Attribute("complex", createComplexData(connection, complexType,"${should.not.exist!!!!!:1}", "${should.not.exist!!!!!:2.0}")));


        checkAttributeValues(connection, name, "1", "${should.not.exist!!!!!:103}", "${should.not.exist!!!!!:102}", "${should.not.exist!!!!!:104}", "${should.not.exist!!!!!:105}", "${should.not.exist!!!!!:true}",
                new byte[] {106, 107}, "${should.not.exist!!!!!:108.0}", "${should.not.exist!!!!!:109}",
                Collections.singletonList("${should.not.exist!!!!!:110}"), "${should.not.exist!!!!!:111}", "${should.not.exist!!!!!:STRING}", "keyA", "${should.not.exist!!!!!:112}", "keyB", "${should.not.exist!!!!!:113}");
        CompositeData compositeData = assertCast(CompositeData.class, connection.getAttribute(name, "complex"));
        Assert.assertEquals("${should.not.exist!!!!!:1}", compositeData.get("int-value"));
        Assert.assertEquals("${should.not.exist!!!!!:2.0}", compositeData.get("bigdecimal-value"));
    }

    private void checkAttributeValues(MBeanServerConnection connection, ObjectName name,
            String roInt, String undefinedInt, String i, String bigInt, String bigDecimal,
            String bool, byte[] bytes, String dbl, String s, List<String> list, String lng,
            String type, String tblKey1, String tblValue1, String tblKey2, String tblValue2) throws Exception {
        Assert.assertEquals(roInt, assertCast(String.class, connection.getAttribute(name, "roInt")));
        if (undefinedInt == null) {
            Assert.assertNull(connection.getAttribute(name, "undefinedInt"));
        } else {
            Assert.assertEquals(undefinedInt, assertCast(String.class, connection.getAttribute(name, "undefinedInt")));
        }
        Assert.assertEquals(i, assertCast(String.class, connection.getAttribute(name, "int")));
        Assert.assertEquals(bigInt, assertCast(String.class, connection.getAttribute(name, "bigint")));
        Assert.assertEquals(bigDecimal, assertCast(String.class, connection.getAttribute(name, "bigdec")));
        Assert.assertEquals(bool, assertCast(String.class, connection.getAttribute(name, "boolean")));
        assertEqualByteArray(bytes, assertCast(byte[].class, connection.getAttribute(name, "bytes")));
        Assert.assertEquals(dbl, assertCast(String.class, connection.getAttribute(name, "double")));
        Assert.assertEquals(s, assertCast(String.class, connection.getAttribute(name, "string")));

        String[] listValue = assertCast(String[].class, connection.getAttribute(name, "list"));
        Assert.assertEquals(list.size(), listValue.length);
        for (int ctr = 0 ; ctr < list.size() ; ctr++) {
            Assert.assertEquals(list.get(ctr), listValue[ctr]);
        }
        Assert.assertEquals(lng, assertCast(String.class, connection.getAttribute(name, "long")));
        Assert.assertEquals(type, assertCast(String.class, connection.getAttribute(name, "type")));
        TabularData tabularData = assertCast(TabularData.class, connection.getAttribute(name, "map"));
        Assert.assertEquals(2, tabularData.size());
        Assert.assertEquals(tblValue1, assertCast(String.class, tabularData.get(new Object[] {tblKey1}).get("value")));
        Assert.assertEquals(tblValue2, assertCast(String.class, tabularData.get(new Object[] {tblKey2}).get("value")));
    }

    @Test
    public void testReadWriteAttributeListStandalone() throws Exception {
        MBeanServerConnection connection = setupAndGetConnection(new MBeanInfoAdditionalInitialization(TYPE_STANDALONE, new TestExtension()));

        ObjectName name = createObjectName(LEGACY_DOMAIN + ":subsystem=test");
        String[] attrNames = new String[] {"roInt", "int", "bigint", "bigdec", "boolean", "bytes", "double", "string", "list", "long", "type"};
        AttributeList list = connection.getAttributes(name, attrNames);
        Assert.assertEquals(list.size(), attrNames.length);

        checkAttributeList(attrNames, list, 1, 2, BigInteger.valueOf(3), BigDecimal.valueOf(4), false, new byte[] {5, 6}, 7.0, "8",
                Collections.singletonList(9), 10, ModelType.INT);

        list = new AttributeList();
        list.add(new Attribute("int", 102));
        list.add(new Attribute("bigint", BigInteger.valueOf(103)));
        list.add(new Attribute("bigdec", BigDecimal.valueOf(104)));
        list.add(new Attribute("boolean", true));
        list.add(new Attribute("bytes", new byte[] {105, 106}));
        list.add(new Attribute("double", 107.0));
        list.add(new Attribute("string", "108"));
        list.add(new Attribute("list", new Integer[] {109}));
        list.add(new Attribute("long", 110L));
        list.add(new Attribute("type", ModelType.STRING.toString()));
        connection.setAttributes(name, list);

        list = connection.getAttributes(name, attrNames);
        checkAttributeList(attrNames, list, 1, 102, BigInteger.valueOf(103), BigDecimal.valueOf(104), true, new byte[] {105, 106}, 107.0, "108",
                Collections.singletonList(109), 110, ModelType.STRING);
    }

    @Test
    public void testReadWriteAttributeListDomain() throws Exception {
        MBeanServerConnection connection = setupAndGetConnection(new MBeanInfoAdditionalInitialization(TYPE_DOMAIN, new TestExtension()));

        ObjectName name = createObjectName(LEGACY_DOMAIN + ":subsystem=test");
        String[] attrNames = new String[] {"roInt", "int", "bigint", "bigdec", "boolean", "bytes", "double", "string", "list", "long", "type"};
        AttributeList list = connection.getAttributes(name, attrNames);
        Assert.assertEquals(list.size(), attrNames.length);

        checkAttributeList(attrNames, list, 1, 2, BigInteger.valueOf(3), BigDecimal.valueOf(4), false, new byte[] {5, 6}, 7.0, "8",
                Collections.singletonList(9), 10, ModelType.INT);

        list = new AttributeList();
        try {
            list.add(new Attribute("int", 102));
            //TODO BigInteger not working in current DMR version
            //list.add(new Attribute("bigint", BigInteger.valueOf(103)));
            list.add(new Attribute("bigdec", BigDecimal.valueOf(104)));
            list.add(new Attribute("boolean", true));
            list.add(new Attribute("bytes", new byte[] {105, 106}));
            list.add(new Attribute("double", 107.0));
            list.add(new Attribute("string", "108"));
            list.add(new Attribute("list", new Integer[] {109}));
            list.add(new Attribute("long", 110L));
            list.add(new Attribute("type", ModelType.STRING.toString()));
            connection.setAttributes(name, list);
            Assert.fail("Should not have been able to set attributes");
        } catch (Exception expected) {
        }

        list = connection.getAttributes(name, attrNames);
        checkAttributeList(attrNames, list, 1, 2, BigInteger.valueOf(3), BigDecimal.valueOf(4), false, new byte[] {5, 6}, 7.0, "8",
                Collections.singletonList(9), 10, ModelType.INT);
    }

    private void checkAttributeList(String[] attrNames, AttributeList list, int roInt, int i, BigInteger bi, BigDecimal bd, boolean b,
            byte[] bytes, double d, String s, List<Integer> lst, long l, ModelType type) {
        Assert.assertEquals(list.size(), attrNames.length);

        Assert.assertEquals(roInt, assertGetFromList(Integer.class, list, "roInt").intValue());
        Assert.assertEquals(i, assertGetFromList(Integer.class, list, "int").intValue());
        Assert.assertEquals(bi, assertGetFromList(BigInteger.class, list, "bigint"));
        Assert.assertEquals(bd, assertGetFromList(BigDecimal.class, list, "bigdec"));
        Assert.assertEquals(b, assertGetFromList(Boolean.class, list, "boolean").booleanValue());
        assertEqualByteArray(assertGetFromList(byte[].class, list, "bytes"), bytes);
        Assert.assertEquals(d, assertGetFromList(Double.class, list, "double"));
        Assert.assertEquals(s, assertGetFromList(String.class, list, "string"));
        Integer[] listValue = assertGetFromList(Integer[].class, list, "list");
        Assert.assertEquals(lst.size(), listValue.length);
        for (int ctr = 0 ; ctr < lst.size() ; ctr++) {
            Assert.assertEquals(lst.get(ctr), listValue[ctr]);
        }
        Assert.assertEquals(l, assertGetFromList(Long.class, list, "long").longValue());
        Assert.assertEquals(type, ModelType.valueOf(assertGetFromList(String.class, list, "type")));
    }

    @Test
    public void testReadWriteAttributeListExpressionsStandalone() throws Exception {
        MBeanServerConnection connection = setupAndGetConnection(new MBeanInfoAdditionalInitialization(TYPE_STANDALONE, new TestExtension(true)));

        ObjectName name = createObjectName(EXPR_DOMAIN + ":subsystem=test");
        String[] attrNames = new String[] {"roInt", "int", "bigint", "bigdec", "boolean", "bytes", "double", "string", "list", "long", "type"};
        AttributeList list = connection.getAttributes(name, attrNames);
        Assert.assertEquals(list.size(), attrNames.length);

        checkAttributeList(attrNames, list, "1", "2", "3", "4", "false", new byte[] {5, 6}, "7.0", "8",
                Collections.singletonList("9"), "10", "INT");

        list = new AttributeList();
        list.add(new Attribute("int", "${should.not.exist!!!!!:102}"));
        list.add(new Attribute("bigint", "${should.not.exist!!!!!:103}"));
        list.add(new Attribute("bigdec", "${should.not.exist!!!!!:104}"));
        list.add(new Attribute("boolean", "${should.not.exist!!!!!:true}"));
        list.add(new Attribute("bytes", new byte[] {105, 106}));
        list.add(new Attribute("double", "${should.not.exist!!!!!:107.0}"));
        list.add(new Attribute("string", "${should.not.exist!!!!!:108}"));
        list.add(new Attribute("list", new String[] {"${should.not.exist!!!!!:109}"}));
        list.add(new Attribute("long", "${should.not.exist!!!!!:110L}"));
        list.add(new Attribute("type", "${should.not.exist!!!!!:STRING}"));
        connection.setAttributes(name, list);

        list = connection.getAttributes(name, attrNames);
        checkAttributeList(attrNames, list, "1", "${should.not.exist!!!!!:102}", "${should.not.exist!!!!!:103}", "${should.not.exist!!!!!:104}", "${should.not.exist!!!!!:true}", new byte[] {105, 106}, "${should.not.exist!!!!!:107.0}", "${should.not.exist!!!!!:108}",
                Collections.singletonList("${should.not.exist!!!!!:109}"), "${should.not.exist!!!!!:110L}", "${should.not.exist!!!!!:STRING}");
    }

    private void checkAttributeList(String[] attrNames, AttributeList list, String roInt, String i, String bi, String bd, String b,
            byte[] bytes, String d, String s, List<String> lst, String l, String type) {
        Assert.assertEquals(list.size(), attrNames.length);

        Assert.assertEquals(roInt, assertGetFromList(String.class, list, "roInt"));
        Assert.assertEquals(i, assertGetFromList(String.class, list, "int"));
        Assert.assertEquals(bi, assertGetFromList(String.class, list, "bigint"));
        Assert.assertEquals(bd, assertGetFromList(String.class, list, "bigdec"));
        Assert.assertEquals(b, assertGetFromList(String.class, list, "boolean"));
        assertEqualByteArray(assertGetFromList(byte[].class, list, "bytes"), bytes);
        Assert.assertEquals(d, assertGetFromList(String.class, list, "double"));
        Assert.assertEquals(s, assertGetFromList(String.class, list, "string"));
        String[] listValue = assertGetFromList(String[].class, list, "list");
        Assert.assertEquals(lst.size(), listValue.length);
        for (int ctr = 0 ; ctr < lst.size() ; ctr++) {
            Assert.assertEquals(lst.get(ctr), listValue[ctr]);
        }
        Assert.assertEquals(l, assertGetFromList(String.class, list, "long"));
        Assert.assertEquals(type, assertGetFromList(String.class, list, "type"));
    }

    @Test
    public void testInvokeOperationStandalone() throws Exception {
        MBeanServerConnection connection = setupAndGetConnection(new MBeanInfoAdditionalInitialization(TYPE_STANDALONE, new TestExtension()));

        ObjectName name = createObjectName(LEGACY_DOMAIN + ":subsystem=test");

        VoidOperationNoParams.INSTANCE.invoked = false;
        Assert.assertNull(connection.invoke(name, VoidOperationNoParams.OPERATION_JMX_NAME, null, null));
        Assert.assertTrue(VoidOperationNoParams.INSTANCE.invoked);

        String result = assertCast(String.class, connection.invoke(
                name,
                IntOperationWithParams.OPERATION_JMX_NAME,
                new Object[] {100L, new String[] {"A"}, Collections.singletonMap("test", 3), 5, 5},
                new String[] {Long.class.getName(), String[].class.getName(), Map.class.getName(), Integer.class.getName(), Integer.class.getName()}));
        Assert.assertEquals("A105", result);
        Assert.assertTrue(IntOperationWithParams.INSTANCE_NO_EXPRESSIONS.invoked);

        MBeanInfo info = connection.getMBeanInfo(name);
        CompositeType complexType = assertCast(CompositeType.class, findAttribute(info.getAttributes(), "complex").getOpenType());
        CompositeData complexData = createComplexData(connection, complexType, 5, BigDecimal.valueOf(10));
        Assert.assertEquals(complexData, assertCast(CompositeData.class, connection.invoke(
                name,
                ComplexOperation.OPERATION_NAME,
                new Object[] {complexData},
                new String[] {CompositeData.class.getName()})));
    }

    @Test
    public void testInvokeOperationDomain() throws Exception {
        MBeanServerConnection connection = setupAndGetConnection(new MBeanInfoAdditionalInitialization(TYPE_DOMAIN, new TestExtension()));

        ObjectName name = createObjectName(LEGACY_DOMAIN + ":subsystem=test");

        VoidOperationNoParams.INSTANCE.invoked = false;
        Assert.assertNull(connection.invoke(name, VoidOperationNoParams.OPERATION_JMX_NAME, new Object[0], new String[0]));
        Assert.assertTrue(VoidOperationNoParams.INSTANCE.invoked);

        try {
            connection.invoke(
                name,
                IntOperationWithParams.OPERATION_JMX_NAME,
                new Object[] {100L, new String[] {"A"}, Collections.singletonMap("test", 3), 5, 5},
                new String[] {Long.class.getName(), String[].class.getName(), Map.class.getName(), Integer.class.getName(), Integer.class.getName()});
            Assert.fail("Should not have been able to invoke method");
        } catch (Exception expected) {
        }

    }

    @Test
    public void testInvokeOperationExpressionsStandalone() throws Exception {
        MBeanServerConnection connection = setupAndGetConnection(new MBeanInfoAdditionalInitialization(TYPE_STANDALONE, new TestExtension(true)));

        ObjectName name = createObjectName(EXPR_DOMAIN + ":subsystem=test");

        VoidOperationNoParams.INSTANCE.invoked = false;
        Assert.assertNull(connection.invoke(name, VoidOperationNoParams.OPERATION_JMX_NAME, null, null));
        Assert.assertTrue(VoidOperationNoParams.INSTANCE.invoked);

        String result = assertCast(String.class, connection.invoke(
                name,
                IntOperationWithParams.OPERATION_JMX_NAME,
                new Object[] {"${should.not.exist!!!!!:100}", new String[] {"${should.not.exist!!!!!:A}"}, Collections.singletonMap("test", "${should.not.exist!!!!!:3}"), "${should.not.exist!!!!!:5}", "${should.not.exist!!!!!:5}"},
                new String[] {Long.class.getName(), String[].class.getName(), Map.class.getName(), Integer.class.getName(), Integer.class.getName()}));
        Assert.assertEquals("A105", result);
        Assert.assertTrue(IntOperationWithParams.INSTANCE_EXPRESSIONS.invoked);

        MBeanInfo info = connection.getMBeanInfo(name);
        CompositeType complexType = assertCast(CompositeType.class, findAttribute(info.getAttributes(), "complex").getOpenType());
        CompositeData complexData = createComplexData(connection, complexType, "${should.not.exist!!!!!:5}", "${should.not.exist!!!!!:10}");
        Assert.assertEquals(complexData, assertCast(CompositeData.class, connection.invoke(
                name,
                ComplexOperation.OPERATION_NAME,
                new Object[] {complexData},
                new String[] {CompositeData.class.getName()})));
    }


    @Test
    public void testAddMethodSingleFixedChild() throws Exception {
        final ObjectName testObjectName = createObjectName(LEGACY_DOMAIN + ":subsystem=test");
        final ObjectName childObjectName = createObjectName(LEGACY_DOMAIN + ":subsystem=test,single=only");
        MBeanServerConnection connection = setupAndGetConnection(new MBeanInfoAdditionalInitialization(TYPE_STANDALONE, new SubystemWithSingleFixedChildExtension()));

        Set<ObjectName> names = connection.queryNames(createObjectName(LEGACY_DOMAIN + ":subsystem=test,*"), null);
        Assert.assertEquals(1, names.size());
        Assert.assertTrue(names.contains(testObjectName));

        MBeanInfo subsystemInfo = connection.getMBeanInfo(testObjectName);
        Assert.assertEquals(0, subsystemInfo.getAttributes().length);
        Assert.assertEquals(1, subsystemInfo.getOperations().length);
        OpenMBeanOperationInfo op = findOperation(subsystemInfo.getOperations(), "addSingleOnly");
        Assert.assertEquals("Adds a child", op.getDescription());
        Assert.assertEquals(1, op.getSignature().length);
        Assert.assertEquals(Integer.class.getName(), op.getSignature()[0].getType());

        connection.invoke(testObjectName, "addSingleOnly", new Object[] {Integer.valueOf(123)}, new String[] {String.class.getName()});

        names = connection.queryNames(createObjectName(LEGACY_DOMAIN + ":subsystem=test,*"), null);
        Assert.assertEquals(2, names.size());
        Assert.assertTrue(names.contains(testObjectName));
        Assert.assertTrue(names.contains(childObjectName));

        subsystemInfo = connection.getMBeanInfo(testObjectName);
        Assert.assertEquals(0, subsystemInfo.getAttributes().length);
        Assert.assertEquals(1, subsystemInfo.getOperations().length);
        op = findOperation(subsystemInfo.getOperations(), "addSingleOnly");
        Assert.assertEquals("Adds a child", op.getDescription());
        Assert.assertEquals(1, op.getSignature().length);
        Assert.assertEquals(Integer.class.getName(), op.getSignature()[0].getType());

        MBeanInfo childInfo = connection.getMBeanInfo(childObjectName);
        Assert.assertEquals(1, childInfo.getAttributes().length);
        Assert.assertEquals(Integer.class.getName(), childInfo.getAttributes()[0].getType());
        Assert.assertEquals(1, childInfo.getOperations().length);
        op = findOperation(childInfo.getOperations(), REMOVE);
        Assert.assertEquals("Removes a child", op.getDescription());
        Assert.assertEquals(0, op.getSignature().length);

        Assert.assertEquals(Integer.valueOf(123), connection.getAttribute(childObjectName, "attr"));

        try {
            connection.invoke(testObjectName, "addSingleOnly", new Object[] {Integer.valueOf(123)}, new String[] {String.class.getName()});
            Assert.fail("Should not have been able to register a duplicate resource");
        } catch (Exception expected) {
        }

        connection.invoke(childObjectName, REMOVE, new Object[] {}, new String[] {});

        names = connection.queryNames(createObjectName(LEGACY_DOMAIN + ":subsystem=test,*"), null);
        Assert.assertEquals(1, names.size());
        Assert.assertTrue(names.contains(testObjectName));
    }

    @Test
    public void testAddMethodSiblingChildren() throws Exception {
        final ObjectName testObjectName = createObjectName(LEGACY_DOMAIN + ":subsystem=test");
        final ObjectName child1ObjectName = createObjectName(LEGACY_DOMAIN + ":subsystem=test,siblings=test1");
        final ObjectName child2ObjectName = createObjectName(LEGACY_DOMAIN + ":subsystem=test,siblings=test2");
        MBeanServerConnection connection = setupAndGetConnection(new MBeanInfoAdditionalInitialization(TYPE_STANDALONE, new SubystemWithSiblingChildrenChildExtension()));

        Set<ObjectName> names = connection.queryNames(createObjectName(LEGACY_DOMAIN + ":subsystem=test,*"), null);
        Assert.assertEquals(1, names.size());
        Assert.assertTrue(names.contains(testObjectName));

        MBeanInfo subsystemInfo = connection.getMBeanInfo(testObjectName);
        Assert.assertEquals(0, subsystemInfo.getAttributes().length);
        Assert.assertEquals(1, subsystemInfo.getOperations().length);
        OpenMBeanOperationInfo op = findOperation(subsystemInfo.getOperations(), "addSiblings");
        Assert.assertEquals("Adds a child", op.getDescription());
        Assert.assertEquals(2, op.getSignature().length);
        Assert.assertEquals(String.class.getName(), op.getSignature()[0].getType());
        Assert.assertEquals(Integer.class.getName(), op.getSignature()[1].getType());

        connection.invoke(testObjectName, "addSiblings", new Object[] {"test1", Integer.valueOf(123)}, new String[] {String.class.getName(), String.class.getName()});

        names = connection.queryNames(createObjectName(LEGACY_DOMAIN + ":subsystem=test,*"), null);
        Assert.assertEquals(2, names.size());
        Assert.assertTrue(names.contains(testObjectName));
        Assert.assertTrue(names.contains(child1ObjectName));

        subsystemInfo = connection.getMBeanInfo(testObjectName);
        Assert.assertEquals(0, subsystemInfo.getAttributes().length);
        Assert.assertEquals(1, subsystemInfo.getOperations().length);
        op = findOperation(subsystemInfo.getOperations(), "addSiblings");
        Assert.assertEquals("Adds a child", op.getDescription());
        Assert.assertEquals(2, op.getSignature().length);
        Assert.assertEquals(String.class.getName(), op.getSignature()[0].getType());
        Assert.assertEquals(Integer.class.getName(), op.getSignature()[1].getType());

        MBeanInfo childInfo = connection.getMBeanInfo(child1ObjectName);
        Assert.assertEquals(1, childInfo.getAttributes().length);
        Assert.assertEquals(Integer.class.getName(), childInfo.getAttributes()[0].getType());
        Assert.assertEquals(1, childInfo.getOperations().length);
        op = findOperation(childInfo.getOperations(), REMOVE);
        Assert.assertEquals("Removes a child", op.getDescription());
        Assert.assertEquals(0, op.getSignature().length);

        connection.invoke(testObjectName, "addSiblings", new Object[] {"test2", Integer.valueOf(456)}, new String[] {String.class.getName(), String.class.getName()});

        names = connection.queryNames(createObjectName(LEGACY_DOMAIN + ":subsystem=test,*"), null);
        Assert.assertEquals(3, names.size());
        Assert.assertTrue(names.contains(testObjectName));
        Assert.assertTrue(names.contains(child1ObjectName));
        Assert.assertTrue(names.contains(child2ObjectName));

        subsystemInfo = connection.getMBeanInfo(testObjectName);
        Assert.assertEquals(0, subsystemInfo.getAttributes().length);
        Assert.assertEquals(1, subsystemInfo.getOperations().length);
        op = findOperation(subsystemInfo.getOperations(), "addSiblings");
        Assert.assertEquals("Adds a child", op.getDescription());
        Assert.assertEquals(2, op.getSignature().length);
        Assert.assertEquals(String.class.getName(), op.getSignature()[0].getType());
        Assert.assertEquals(Integer.class.getName(), op.getSignature()[1].getType());

        childInfo = connection.getMBeanInfo(child1ObjectName);
        Assert.assertEquals(1, childInfo.getAttributes().length);
        Assert.assertEquals(Integer.class.getName(), childInfo.getAttributes()[0].getType());
        Assert.assertEquals(1, childInfo.getOperations().length);
        op = findOperation(childInfo.getOperations(), REMOVE);
        Assert.assertEquals("Removes a child", op.getDescription());
        Assert.assertEquals(0, op.getSignature().length);

        childInfo = connection.getMBeanInfo(child2ObjectName);
        Assert.assertEquals(1, childInfo.getAttributes().length);
        Assert.assertEquals(Integer.class.getName(), childInfo.getAttributes()[0].getType());
        Assert.assertEquals(1, childInfo.getOperations().length);
        op = findOperation(childInfo.getOperations(), REMOVE);
        Assert.assertEquals("Removes a child", op.getDescription());
        Assert.assertEquals(0, op.getSignature().length);

        Assert.assertEquals(Integer.valueOf(123), connection.getAttribute(child1ObjectName, "attr"));
        Assert.assertEquals(Integer.valueOf(456), connection.getAttribute(child2ObjectName, "attr"));

        connection.invoke(child1ObjectName, REMOVE, new Object[] {}, new String[] {});

        names = connection.queryNames(createObjectName(LEGACY_DOMAIN + ":subsystem=test,*"), null);
        Assert.assertEquals(2, names.size());
        Assert.assertTrue(names.contains(testObjectName));
        Assert.assertTrue(names.contains(child2ObjectName));

        connection.invoke(child2ObjectName, REMOVE, new Object[] {}, new String[] {});

        names = connection.queryNames(createObjectName(LEGACY_DOMAIN + ":subsystem=test,*"), null);
        Assert.assertEquals(1, names.size());
        Assert.assertTrue(names.contains(testObjectName));
    }

    @Test
    public void testResolveExpressions() throws Exception {
        MBeanServerConnection connection = setupAndGetConnection(new BaseAdditionalInitialization(TYPE_STANDALONE));
        System.clearProperty("jboss.test.resolve.expressions.test");
        Assert.assertEquals("123", connection.invoke(LEGACY_ROOT_NAME, "resolveExpression", new String[] {"${jboss.test.resolve.expressions.test:123}"}, new String[] {String.class.getName()}));

        try {
            connection.invoke(LEGACY_ROOT_NAME, "resolveExpression", new String[] {"${jboss.test.resolve.expressions.test}"}, new String[] {String.class.getName()});
            Assert.fail("Should not have been able to resolve non-existant property");
        } catch (Exception expected) {
        }

    }

    private OpenMBeanOperationInfo findOperation(MBeanOperationInfo[] ops, String name) {
        for (MBeanOperationInfo op : ops) {
            Assert.assertNotNull(op.getName());
            if (op.getName().equals(name)) {
                return assertCast(OpenMBeanOperationInfo.class, op);
            }
        }
        Assert.fail("No op called " + name);
        return null;
    }

    private OpenMBeanAttributeInfo findAttribute(MBeanAttributeInfo[] attrs, String name) {
        for (MBeanAttributeInfo attr : attrs) {
            Assert.assertNotNull(attr.getName());
            if (attr.getName().equals(name)) {
                return assertCast(OpenMBeanAttributeInfo.class, attr);
            }
        }
        Assert.fail("No attr called " + name);
        return null;
    }

    private void assertEqualByteArray(byte[] bytes, byte...expected) {
        Assert.assertEquals(expected.length, bytes.length);
        for (int i = 0 ; i < bytes.length ; i++) {
            Assert.assertEquals(expected[i], bytes[i]);
        }
    }

    private CompositeData createComplexData(MBeanServerConnection connection, CompositeType type, int intValue, BigDecimal bigDecimalValue) throws Exception {
        Map<String, Object> items = new HashMap<String, Object>();
        items.put("int-value", Integer.valueOf(intValue));
        //items.put("bigint-value", bigIntegerValue);
        items.put("bigdecimal-value", bigDecimalValue);
        CompositeDataSupport data = new CompositeDataSupport(type, items);
        return data;
    }

    private CompositeData createComplexData(MBeanServerConnection connection, CompositeType type, String intValue, String bigDecimalValue) throws Exception {
        Map<String, Object> items = new HashMap<String, Object>();
        items.put("int-value", intValue);
        //items.put("bigint-value", bigIntegerValue);
        items.put("bigdecimal-value", bigDecimalValue);
        CompositeDataSupport data = new CompositeDataSupport(type, items);
        return data;
    }

    private void assertMapType(OpenType<?> mapType, OpenType<?> keyType, OpenType<?> valueType) {
        TabularType type = assertCast(TabularType.class, mapType);
        Assert.assertEquals(1, type.getIndexNames().size());
        Assert.assertEquals("key", type.getIndexNames().get(0));
        Assert.assertEquals(2, type.getRowType().keySet().size());
        Assert.assertTrue(type.getRowType().keySet().contains("key"));
        Assert.assertTrue(type.getRowType().keySet().contains("value"));
        Assert.assertEquals(keyType, type.getRowType().getType("key"));
        Assert.assertEquals(valueType, type.getRowType().getType("value"));

    }

    private void checkComplexTypeInfo(CompositeType composite, boolean expressions) {
        Set<String> keys = composite.keySet();
        Assert.assertEquals(2, keys.size());
        assertCompositeType(composite, "int-value", expressions ? String.class.getName() : Integer.class.getName(), "An int value");
        assertCompositeType(composite, "bigdecimal-value", expressions ? String.class.getName() : BigDecimal.class.getName(), "A bigdecimal value");
    }


    private void assertAttributeDescription(MBeanAttributeInfo attribute, String name, String type, String description, boolean readable,
            boolean writable) {
        Assert.assertEquals(name, attribute.getName());
        Assert.assertEquals(description, attribute.getDescription());
        Assert.assertEquals(type, attribute.getType());
        Assert.assertEquals(readable, attribute.isReadable());
        Assert.assertEquals(writable, attribute.isWritable());
    }

    private OpenType<?> assertCompositeType(CompositeType composite, String name, String type, String description){
        return assertCompositeType(composite, name, type, description, true);
    }

    private OpenType<?> assertCompositeType(CompositeType composite, String name, String type, String description, boolean validateType){
        Assert.assertTrue(composite.keySet().contains(name));
        if (validateType) {
            Assert.assertEquals(type, composite.getType(name).getTypeName());
        }
        Assert.assertEquals(description, composite.getDescription(name));
        return composite.getType(name);
    }

    private static <T> T assertGetFromList(Class<T> clazz, AttributeList list, String name) {
        Object value = null;
        List<javax.management.Attribute> attrs = list.asList();
        for (Attribute attr : attrs) {
            if (attr.getName().equals(name)) {
                value = attr.getValue();
                break;
            }
        }
        Assert.assertNotNull(value);
        return assertCast(clazz, value);
    }

    private static <T> T assertCast(Class<T> clazz, Object value) {
        Assert.assertTrue("value " + value.getClass().getName() + " can not be changed to a " + clazz.getName(), clazz.isAssignableFrom(value.getClass()));
        return clazz.cast(value);
    }


    private void checkSameMBeans(Set<ObjectInstance> instances, Set<ObjectName> objectNames) {
        for (ObjectInstance instance : instances) {
            Assert.assertTrue("Does not contain " + instance.getObjectName(), objectNames.contains(instance.getObjectName()));
        }
    }

    private void assertContainsNames(Set<ObjectName> objectNames, ObjectName... expected) {
        for (ObjectName name : expected) {
            Assert.assertTrue("Does not contain " + name, objectNames.contains(name));
        }
    }

    private MBeanServerConnection setupAndGetConnection(BaseAdditionalInitialization additionalInitialization) throws Exception {

        // Parse the subsystem xml and install into the controller
        String subsystemXml = "<subsystem xmlns=\"" + Namespace.CURRENT.getUriString() + "\">"
                + "<expose-resolved-model domain-name=\"jboss.resolved\"/>"
                + "<expose-expression-model/>"
                + "<remoting-connector/>" + "</subsystem>"
                + additionalInitialization.getExtraXml();
        createKernelServicesBuilder(additionalInitialization).setSubsystemXml(subsystemXml).build();

        // Make sure that we can connect to the MBean server
        String host = "localhost";
        int port = 12345;
        String urlString = System
                .getProperty("jmx.service.url", "service:jmx:remoting-jmx://" + host + ":" + port);
        JMXServiceURL serviceURL = new JMXServiceURL(urlString);

        // TODO this is horrible - for some reason after the first test the
        // second time we
        // start the JMX connector it takes time for it to appear
        long end = System.currentTimeMillis() + 10000;
        while (true) {
            try {
                JMXConnector jmxConnector = JMXConnectorFactory.connect(serviceURL, null);
                return jmxConnector.getMBeanServerConnection();
            } catch (Exception e) {
                if (System.currentTimeMillis() >= end) {
                    throw new RuntimeException(e);
                }
                Thread.sleep(50);
            }
        }
    }

    private static ObjectName createObjectName(String s) {
        try {
            return new ObjectName(s);
        } catch (MalformedObjectNameException e) {
            throw new RuntimeException(e);
        }
    }


    private static class BaseAdditionalInitialization extends AdditionalInitialization {

        final String launchType;

        public BaseAdditionalInitialization(String launchType) {
            this.launchType = launchType;
        }

        @Override
        protected void initializeExtraSubystemsAndModel(ExtensionRegistry extensionRegistry, Resource rootResource,
                ManagementResourceRegistration rootRegistration) {
            rootResource.getModel().get(LAUNCH_TYPE).set(launchType);
            rootRegistration.registerOperationHandler(ResolveExpressionHandler.DEFINITION, ResolveExpressionHandler.INSTANCE);
        }

        @Override
        protected void setupController(ControllerInitializer controllerInitializer) {
            controllerInitializer.addSocketBinding("server", 12345);
            controllerInitializer.addPath("jboss.controller.temp.dir", System.getProperty("java.io.tmpdir"), null);
        }

        @Override
        protected void addExtraServices(final ServiceTarget target) {
            ManagementRemotingServices.installRemotingEndpoint(target, ManagementRemotingServices.MANAGEMENT_ENDPOINT, "loaclhost", EndpointService.EndpointType.MANAGEMENT, null, null);
            ServiceName tmpDirPath = ServiceName.JBOSS.append("server", "path", "jboss.controller.temp.dir");

            RemotingServices.installSecurityServices(target, "server", null, null, tmpDirPath, null, null);
            RemotingServices.installConnectorServicesForSocketBinding(target, ManagementRemotingServices.MANAGEMENT_ENDPOINT, "server", SocketBinding.JBOSS_BINDING_NAME.append("server"), OptionMap.EMPTY, null, null);
        }


        String getExtraXml() {
            return "";
        }
    }

    private static class MBeanInfoAdditionalInitialization extends BaseAdditionalInitialization {

        private final Extension extension;


        public MBeanInfoAdditionalInitialization(String launchType, Extension extension) {
            super(launchType);
            this.extension = extension;
        }

        @Override
        protected void addParsers(ExtensionRegistry extensionRegistry, XMLMapper xmlMapper) {
            extension.initializeParsers(extensionRegistry.getExtensionParsingContext("additional", xmlMapper));
        }

        @Override
        protected void initializeExtraSubystemsAndModel(ExtensionRegistry extensionRegistry, Resource rootResource,
                ManagementResourceRegistration rootRegistration) {
            super.initializeExtraSubystemsAndModel(extensionRegistry, rootResource, rootRegistration);
            extension.initialize(extensionRegistry.getExtensionContext("additional", false));
        }

        String getExtraXml() {
            return "<subsystem xmlns=\"" + TestExtension.NAMESPACE + "\"/>";
        }
    }

    static class TestExtension implements Extension {

        static final String NAMESPACE = "urn:jboss:mbean.model.test";

        private final boolean allowExpressions;

        public TestExtension() {
            this(false);
        }

        public TestExtension(boolean allowExpressions) {
            this.allowExpressions = allowExpressions;
        }

        @Override
        public void initialize(ExtensionContext context) {
            final ModelNode complexValueType = new ModelNode();
            complexValueType.get("int-value", DESCRIPTION).set("An int value");
            complexValueType.get("int-value", EXPRESSIONS_ALLOWED).set(allowExpressions);
            complexValueType.get("int-value", TYPE).set(ModelType.INT);
            complexValueType.get("bigdecimal-value", DESCRIPTION).set("A bigdecimal value");
            complexValueType.get("bigdecimal-value", TYPE).set(ModelType.BIG_DECIMAL);
            complexValueType.get("bigdecimal-value", EXPRESSIONS_ALLOWED).set(allowExpressions);


            final SubsystemRegistration subsystem = context.registerSubsystem("test", 1, 0, 0);
            final ManagementResourceRegistration registration = subsystem.registerSubsystemModel(new DescriptionProvider() {

                @Override
                public ModelNode getModelDescription(Locale locale) {
                    ModelNode node = new ModelNode();
                    node.get(DESCRIPTION).set("A test subsystem");
                    addAttribute(node, "ro-int", ModelType.INT, "A read-only int");
                    addAttribute(node, "undefined-int", ModelType.INT, "A read-only int");
                    addAttribute(node, "int", ModelType.INT, "A int");
                    addAttribute(node, "bigint", ModelType.BIG_INTEGER, "A big int");
                    addAttribute(node, "bigdec", ModelType.BIG_DECIMAL, "A big dec");
                    addAttribute(node, "boolean", ModelType.BOOLEAN, "A boolean");
                    addAttribute(node, "bytes", ModelType.BYTES, "A bytes");
                    addAttribute(node, "double", ModelType.DOUBLE, "A double");
                    addAttribute(node, "string", ModelType.STRING, "A string");
                    addValueTypeAttribute(node, "list", ModelType.LIST, new ModelNode().set(ModelType.INT), "A list");
                    addAttribute(node, "long", ModelType.LONG, "A long");
                    addAttribute(node, "type", ModelType.TYPE, "A type");
                    addValueTypeAttribute(node, "map", ModelType.OBJECT, new ModelNode().set(ModelType.INT), "A map");
                    addValueTypeAttribute(node, "complex", ModelType.OBJECT, complexValueType, "A complex value");


                    // TODO also add the types mentioned in
                    // MBeanInfoFactory.convertToMBeanType()
                    return node;
                }

                private ModelNode addAttribute(ModelNode node, String name, ModelType type, String description) {
                    ModelNode tgt = node.get(ATTRIBUTES, name);
                    tgt.get(TYPE).set(type);
                    tgt.get(DESCRIPTION).set(description);
                    if (allowExpressions) {
                        tgt.get(EXPRESSIONS_ALLOWED).set(true);
                    }
                    return node;
                }

                private void addValueTypeAttribute(ModelNode node, String name, ModelType type, ModelNode valueType, String description) {
                    ModelNode tgt = addAttribute(node, name, type, description);
                    tgt.get(ATTRIBUTES, name, VALUE_TYPE).set(valueType);
                }
            });
            // We always need to add an 'add' operation
            registration.registerOperationHandler(ADD, TestSubystemAdd.INSTANCE, TestSubystemAdd.INSTANCE, false);

            //Register the attributes
            OperationStepHandler expressionHandler = new WriteAttributeHandlers.StringLengthValidatingHandler(1, false, true);
            registration.registerReadOnlyAttribute("ro-int", null, Storage.CONFIGURATION);
            registration.registerReadWriteAttribute("undefined-int", null,
                    allowExpressions ? expressionHandler : new WriteAttributeHandlers.ModelTypeValidatingHandler(ModelType.INT), Storage.CONFIGURATION);
            registration.registerReadWriteAttribute("int", null,
                    allowExpressions ? expressionHandler : new WriteAttributeHandlers.ModelTypeValidatingHandler(ModelType.INT), Storage.CONFIGURATION);
            registration.registerReadWriteAttribute("bigint", null,
                    allowExpressions ? expressionHandler : new WriteAttributeHandlers.ModelTypeValidatingHandler(ModelType.BIG_INTEGER), Storage.CONFIGURATION);
            registration.registerReadWriteAttribute("bigdec", null,
                    allowExpressions ? expressionHandler : new WriteAttributeHandlers.ModelTypeValidatingHandler(ModelType.BIG_DECIMAL), Storage.CONFIGURATION);
            registration.registerReadWriteAttribute("boolean", null,
                    allowExpressions ? expressionHandler : new WriteAttributeHandlers.ModelTypeValidatingHandler(ModelType.BOOLEAN), Storage.CONFIGURATION);
            registration.registerReadWriteAttribute("bytes", null, new WriteAttributeHandlers.ModelTypeValidatingHandler(ModelType.BYTES), Storage.CONFIGURATION);
            registration.registerReadWriteAttribute("double", null,
                    allowExpressions ? expressionHandler : new WriteAttributeHandlers.ModelTypeValidatingHandler(ModelType.DOUBLE), Storage.CONFIGURATION);
            registration.registerReadWriteAttribute("string", null,
                    allowExpressions ? expressionHandler : new WriteAttributeHandlers.ModelTypeValidatingHandler(ModelType.STRING), Storage.CONFIGURATION);
            registration.registerReadWriteAttribute("list", null,
                    new WriteAttributeHandlers.ModelTypeValidatingHandler(ModelType.LIST), Storage.CONFIGURATION);
            registration.registerReadWriteAttribute("long", null,
                    allowExpressions ? expressionHandler : new WriteAttributeHandlers.ModelTypeValidatingHandler(ModelType.LONG), Storage.CONFIGURATION);
            registration.registerReadWriteAttribute("type", null,
                    allowExpressions ? expressionHandler : new WriteAttributeHandlers.ModelTypeValidatingHandler(ModelType.TYPE), Storage.CONFIGURATION);
            registration.registerReadWriteAttribute("map", null, new WriteAttributeHandlers.ModelTypeValidatingHandler(ModelType.OBJECT), Storage.CONFIGURATION);
            registration.registerReadWriteAttribute("complex", null, new ComplexWriteAttributeHandler(), Storage.CONFIGURATION);


            //Register the operation handlers
            registration.registerOperationHandler(VoidOperationNoParams.OPERATION_NAME, VoidOperationNoParams.INSTANCE, VoidOperationNoParams.INSTANCE, EnumSet.of(OperationEntry.Flag.READ_ONLY));
            IntOperationWithParams intOp = allowExpressions ? IntOperationWithParams.INSTANCE_EXPRESSIONS : IntOperationWithParams.INSTANCE_NO_EXPRESSIONS;
            registration.registerOperationHandler(IntOperationWithParams.OPERATION_NAME, intOp, intOp);
            ComplexOperation op = new ComplexOperation(complexValueType);
            registration.registerOperationHandler(ComplexOperation.OPERATION_NAME, op, op);

            // subsystem.registerXMLElementWriter(parser);
        }

        @Override
        public void initializeParsers(ExtensionParsingContext context) {
            context.setSubsystemXmlMapping("test", NAMESPACE, new TestExtensionParser());
        }

        static class TestExtensionParser implements XMLElementReader<List<ModelNode>> {
            @Override
            public void readElement(XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {
                reader.next();
                ModelNode add = new ModelNode();
                add.get(OP).set(ADD);
                add.get(OP_ADDR).set(PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, "test")).toModelNode());
                list.add(add);
            }
        }

        static class TestSubystemAdd extends AbstractAddStepHandler implements DescriptionProvider {
            static final TestSubystemAdd INSTANCE = new TestSubystemAdd();

            @Override
            protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
                model.get("ro-int").set(1);
                model.get("int").set(2);
                model.get("bigint").set(new BigInteger("3"));
                model.get("bigdec").set(new BigDecimal("4"));
                model.get("boolean").set(false);
                model.get("bytes").set(new byte[] {5, 6});
                model.get("double").set(7.0);
                model.get("string").set("8");
                model.get("list").add(new Integer(9));
                model.get("long").set(10L);
                model.get("type").set(ModelType.INT);
                model.get("map", "key1").set(11);
                model.get("map", "key2").set(12);
            }

            @Override
            public ModelNode getModelDescription(Locale locale) {
                ModelNode node = new ModelNode();
                node.get(NAME).set(ADD);
                node.get(DESCRIPTION).set("Add the test subsystem");
                return node;
            }
        }

        static class VoidOperationNoParams implements OperationStepHandler, DescriptionProvider {
            static final VoidOperationNoParams INSTANCE = new VoidOperationNoParams();
            static final String OPERATION_NAME = "void-no-params";
            static final String OPERATION_JMX_NAME = "voidNoParams";
            boolean invoked;

            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                invoked = true;
                context.stepCompleted();
            }

            @Override
            public ModelNode getModelDescription(Locale locale) {
                ModelNode node = new ModelNode();
                node.get(NAME).set(OPERATION_NAME);
                node.get(DESCRIPTION).set("Test1");
                return node;
            }
        }

        static class IntOperationWithParams implements OperationStepHandler, DescriptionProvider {
            static final IntOperationWithParams INSTANCE_NO_EXPRESSIONS = new IntOperationWithParams(false);
            static final IntOperationWithParams INSTANCE_EXPRESSIONS = new IntOperationWithParams(true);
            static final String OPERATION_NAME = "int-with-params";
            static final String OPERATION_JMX_NAME = "intWithParams";
            boolean invoked;
            boolean allowExpressions;

            public IntOperationWithParams(boolean allowExpressions) {
                this.allowExpressions = allowExpressions;
            }

            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                invoked = true;
                long l = operation.get("param1").resolve().asLong() + context.readResource(PathAddress.EMPTY_ADDRESS).getModel().get("int").asInt() + operation.get("param3", "test").resolve().asInt();
                context.getResult().set(operation.get("param2").resolve().asList().get(0).asString() + l);
                context.stepCompleted();
            }

            @Override
            public ModelNode getModelDescription(Locale locale) {
                ModelNode node = new ModelNode();
                node.get(NAME).set(OPERATION_NAME);
                node.get(DESCRIPTION).set("Test2");
                node.get(REQUEST_PROPERTIES, "param1", TYPE).set(ModelType.LONG);
                node.get(REQUEST_PROPERTIES, "param1", DESCRIPTION).set("Param1");
                node.get(REQUEST_PROPERTIES, "param1", EXPRESSIONS_ALLOWED).set(allowExpressions);
                node.get(REQUEST_PROPERTIES, "param2", TYPE).set(ModelType.LIST);
                node.get(REQUEST_PROPERTIES, "param2", VALUE_TYPE).set(ModelType.STRING);
                node.get(REQUEST_PROPERTIES, "param2", DESCRIPTION).set("Param2");
                node.get(REQUEST_PROPERTIES, "param2", EXPRESSIONS_ALLOWED).set(allowExpressions);
                node.get(REQUEST_PROPERTIES, "param3", TYPE).set(ModelType.OBJECT);
                node.get(REQUEST_PROPERTIES, "param3", VALUE_TYPE).set(ModelType.INT);
                node.get(REQUEST_PROPERTIES, "param3", DESCRIPTION).set("Param3");
                node.get(REQUEST_PROPERTIES, "param3", EXPRESSIONS_ALLOWED).set(allowExpressions);
                node.get(REQUEST_PROPERTIES, "param4", TYPE).set(ModelType.INT);
                node.get(REQUEST_PROPERTIES, "param4", DESCRIPTION).set("Param4");
                node.get(REQUEST_PROPERTIES, "param4", EXPRESSIONS_ALLOWED).set(allowExpressions);
                node.get(REQUEST_PROPERTIES, "param4", DEFAULT).set(6);
                node.get(REQUEST_PROPERTIES, "param4", MIN).set(5);
                node.get(REQUEST_PROPERTIES, "param4", MAX).set(10);
                node.get(REQUEST_PROPERTIES, "param5", TYPE).set(ModelType.INT);
                node.get(REQUEST_PROPERTIES, "param5", DESCRIPTION).set("Param5");
                node.get(REQUEST_PROPERTIES, "param5", EXPRESSIONS_ALLOWED).set(allowExpressions);
                node.get(REQUEST_PROPERTIES, "param5", ALLOWED).add(3);
                node.get(REQUEST_PROPERTIES, "param5", ALLOWED).add(5);
                node.get(REQUEST_PROPERTIES, "param5", ALLOWED).add(7);
                node.get(REPLY_PROPERTIES, TYPE).set(ModelType.STRING);
                node.get(REPLY_PROPERTIES, DESCRIPTION).set("Return");
                return node;
            }
        }

        class ComplexOperation implements OperationStepHandler, DescriptionProvider {
            static final String OPERATION_NAME = "complex";
            final ModelNode complexValueType;

            public ComplexOperation(ModelNode complexValueType) {
                this.complexValueType = complexValueType;
            }

            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                context.getResult().set(operation.get("param1"));
                context.stepCompleted();
            }

            @Override
            public ModelNode getModelDescription(Locale locale) {
                ModelNode node = new ModelNode();
                node.get(NAME).set(OPERATION_NAME);
                node.get(DESCRIPTION).set("Test3");
                node.get(REQUEST_PROPERTIES, "param1", TYPE).set(ModelType.OBJECT);
                node.get(REQUEST_PROPERTIES, "param1", DESCRIPTION).set("Param1");
                node.get(REQUEST_PROPERTIES, "param1", VALUE_TYPE).set(complexValueType);
                node.get(REPLY_PROPERTIES, TYPE).set(ModelType.OBJECT);
                node.get(REPLY_PROPERTIES, DESCRIPTION).set("Return");
                node.get(REPLY_PROPERTIES, VALUE_TYPE).set(complexValueType);
                return node;
            }
        }

        class ComplexWriteAttributeHandler extends WriteAttributeHandlers.WriteAttributeOperationHandler {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                super.execute(context, operation);
            }
        }
    }

    static class SubsystemWithChildrenExtension implements Extension {

        static final String NAMESPACE = "urn:jboss:mbean.model.test";

        @Override
        public void initialize(ExtensionContext context) {

            final SubsystemRegistration subsystem = context.registerSubsystem("test", 1, 0, 0);
            final ManagementResourceRegistration registration = subsystem.registerSubsystemModel(new DescriptionProvider() {

                @Override
                public ModelNode getModelDescription(Locale locale) {
                    ModelNode node = new ModelNode();
                    node.get(DESCRIPTION).set("A test subsystem");
                    node.get(CHILDREN, "single", DESCRIPTION).set("An only child");
                    node.get(CHILDREN, "siblings", DESCRIPTION).set("One of many");


                    // TODO also add the types mentioned in
                    // MBeanInfoFactory.convertToMBeanType()
                    return node;
                }
            });
            // We always need to add an 'add' operation
            registration.registerOperationHandler(ADD, TestSubystemAdd.INSTANCE, TestSubystemAdd.INSTANCE, false);

            final ManagementResourceRegistration singleRegistration = registration.registerSubModel(getChildElement() ,new DescriptionProvider() {

                @Override
                public ModelNode getModelDescription(Locale locale) {
                    ModelNode node = new ModelNode();
                    node.get(DESCRIPTION).set("An only child");
                    node.get(ATTRIBUTES, "attr", TYPE).set(ModelType.INT);
                    node.get(ATTRIBUTES, "attr", DESCRIPTION).set("Only child int");
                    return node;
                }
            });
            singleRegistration.registerOperationHandler(ADD, TestChildAdd.INSTANCE, TestChildAdd.INSTANCE);
            singleRegistration.registerOperationHandler(REMOVE, TestChildRemove.INSTANCE, TestChildRemove.INSTANCE);
        }

        PathElement getChildElement() {
            return null;
        }

        @Override
        public void initializeParsers(ExtensionParsingContext context) {
            context.setSubsystemXmlMapping("test", NAMESPACE, new TestExtensionParser());
        }

        static class TestExtensionParser implements XMLElementReader<List<ModelNode>> {
            @Override
            public void readElement(XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {
                reader.next();
                ModelNode add = new ModelNode();
                add.get(OP).set(ADD);
                add.get(OP_ADDR).set(PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, "test")).toModelNode());
                list.add(add);
            }
        }

        static class TestSubystemAdd extends AbstractAddStepHandler implements DescriptionProvider {
            static final TestSubystemAdd INSTANCE = new TestSubystemAdd();

            @Override
            protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
            }

            @Override
            public ModelNode getModelDescription(Locale locale) {
                ModelNode node = new ModelNode();
                node.get(NAME).set(ADD);
                node.get(DESCRIPTION).set("Add the test subsystem");
                return node;
            }
        }

        static class TestChildAdd extends AbstractAddStepHandler implements DescriptionProvider {
            static final TestChildAdd INSTANCE = new TestChildAdd();

            @Override
            protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
                model.get("attr").set(operation.get("attr"));

            }

            @Override
            public ModelNode getModelDescription(Locale locale) {
                ModelNode node = new ModelNode();
                node.get(NAME).set(ADD);
                node.get(DESCRIPTION).set("Adds a child");
                node.get(REQUEST_PROPERTIES, "attr", TYPE).set(ModelType.INT);
                node.get(REQUEST_PROPERTIES, "attr", DESCRIPTION).set("The attribute value");
                return node;
            }
        }

        static class TestChildRemove extends AbstractRemoveStepHandler implements DescriptionProvider {
            static final TestChildRemove INSTANCE = new TestChildRemove();

            @Override
            public ModelNode getModelDescription(Locale locale) {
                ModelNode node = new ModelNode();
                node.get(NAME).set(REMOVE);
                node.get(DESCRIPTION).set("Removes a child");
                return node;
            }
        }
    }

    static class SubystemWithSingleFixedChildExtension extends SubsystemWithChildrenExtension {
        @Override
        PathElement getChildElement() {
            return PathElement.pathElement("single", "only");
        }
    }

    static class SubystemWithSiblingChildrenChildExtension extends SubsystemWithChildrenExtension {
        @Override
        PathElement getChildElement() {
            return PathElement.pathElement("siblings");
        }
    }

}
