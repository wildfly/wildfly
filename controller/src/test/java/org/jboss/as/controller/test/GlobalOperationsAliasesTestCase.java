/*
 * Copyright (C) 2014 Red Hat, inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package org.jboss.as.controller.test;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES_ONLY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILDREN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILD_TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INCLUDE_RUNTIME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MIN_LENGTH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MODEL_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NILLABLE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_NAMES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_RESOURCES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_TYPES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_OPERATION_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_OPERATION_NAMES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RECURSIVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.controller.test.AbstractControllerTestBase.createModel;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelOnlyWriteAttributeHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceBuilder;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.NonResolvingResourceDescriptionResolver;
import org.jboss.as.controller.operations.global.GlobalNotifications;
import org.jboss.as.controller.operations.global.GlobalOperationHandlers;
import org.jboss.as.controller.registry.AliasEntry;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;
import org.junit.Assert;
import org.junit.Test;


/**
 *
 * @author <a href="mailto:ehugonne@redhat.com">Emmanuel Hugonnet</a> (c) 2014 Red Hat, inc.
 */
public class GlobalOperationsAliasesTestCase extends AbstractGlobalOperationsTestCase {

    @Override
    protected void initModel(Resource rootResource, ManagementResourceRegistration rootRegistration) {
        GlobalOperationHandlers.registerGlobalOperations(rootRegistration, processType);
        GlobalNotifications.registerGlobalNotifications(rootRegistration, processType);

        rootRegistration.registerOperationHandler(TestUtils.SETUP_OPERATION_DEF, new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                final ModelNode model = new ModelNode();
                //Atttributes
                model.get("profile", "profileA", "subsystem", "subsystem1", "attr1").add(1);
                model.get("profile", "profileA", "subsystem", "subsystem1", "attr1").add(2);
                //Children
                model.get("profile", "profileA", "subsystem", "subsystem1", "type1", "thing1", "name").set("Name11");
                model.get("profile", "profileA", "subsystem", "subsystem1", "type1", "thing1", "value").set("201");
                model.get("profile", "profileA", "subsystem", "subsystem1", "type1", "thing2", "name").set("Name12");
                model.get("profile", "profileA", "subsystem", "subsystem1", "type1", "thing2", "value").set("202");
                model.get("profile", "profileA", "subsystem", "subsystem1", "type2", "other", "name").set("Name2");

                model.get("profile", "profileB", "name").set("Profile B");

                model.get("profile", "profileC", "subsystem", "subsystem4");
                model.get("profile", "profileC", "subsystem", "subsystem5", "name").set("Test");

                model.get("profile", "profileD", "subsystem", "subsystem3");
                model.get("profile", "profileD", "subsystem", "subsystem3", "service", "squatter1", "name").set("TestSquatter1");
                model.get("profile", "profileD", "subsystem", "subsystem3", "service", "squatter1", "thing1").set("squatter");
                model.get("profile", "profileD", "subsystem", "subsystem3", "service", "squatter3", "name").set("TestSquatter3");
                model.get("profile", "profileD", "subsystem", "subsystem3", "service", "squatter3", "thing3").set("squatter");

                createModel(context, model);

                context.stepCompleted();
            }
        }
        );

        ResourceDefinition profileDef = ResourceBuilder.Factory.create(PathElement.pathElement("profile", "*"),
                new NonResolvingResourceDescriptionResolver())
                .addReadOnlyAttribute(SimpleAttributeDefinitionBuilder.create("name", ModelType.STRING, false).setMinSize(1).build())
                .build();

        ManagementResourceRegistration profileReg = rootRegistration.registerSubModel(profileDef);

        ManagementResourceRegistration profileSub1Reg = profileReg.registerSubModel(new Subsystem1RootResource());
        profileReg.registerAlias(
                PathElement.pathElement("subsystem-test"),
                new AliasEntry(profileSub1Reg) {
                    @Override
                    public PathAddress convertToTargetAddress(PathAddress address) {
                        List<PathElement> list = new ArrayList<PathElement>();
                        for (PathElement element : address) {
                            if ("subsystem-test".equals(element.getKey())) {
                                if (element.getValue() != null) {
                                    list.add(PathElement.pathElement("subsystem", element.getValue()));
                                } else {
                                    list.add(PathElement.pathElement("subsystem"));
                                }
                            } else {
                                list.add(element);
                            }
                        }
                        return PathAddress.pathAddress(list);
                    }
                });
        profileReg.registerAlias(
                PathElement.pathElement("alias"),
                new AliasEntry(profileSub1Reg) {
                    @Override
                    public PathAddress convertToTargetAddress(PathAddress address) {
                        List<PathElement> list = new ArrayList<PathElement>();
                        for (PathElement element : address) {
                            if ("alias".equals(element.getKey())) {
                                if (element.getValue() != null) {
                                    list.add(PathElement.pathElement("subsystem", element.getValue()));
                                } else {
                                    list.add(PathElement.pathElement("subsystem"));
                                }
                            } else {
                                list.add(element);
                            }
                        }
                        return PathAddress.pathAddress(list);
                    }
                });

        ManagementResourceRegistration profileASub2Reg = profileReg.registerSubModel(
                new SimpleResourceDefinition(PathElement.pathElement("subsystem", "subsystem2"), new NonResolvingResourceDescriptionResolver()));
        AttributeDefinition longAttr = TestUtils.createAttribute("long", ModelType.LONG);
        profileASub2Reg.registerReadWriteAttribute(longAttr, null, new ModelOnlyWriteAttributeHandler(longAttr));

        ManagementResourceRegistration profileBSub3Reg = profileReg.registerSubModel(
                new SimpleResourceDefinition(PathElement.pathElement("subsystem", "subsystem3"), new NonResolvingResourceDescriptionResolver()));
        ManagementResourceRegistration squatter1Reg = profileBSub3Reg.registerSubModel(
                new SimpleResourceDefinition(PathElement.pathElement("service", "squatter1"), new NonResolvingResourceDescriptionResolver()));
        AttributeDefinition squatter1Name = TestUtils.createAttribute("name", ModelType.STRING);
        squatter1Reg.registerReadWriteAttribute(squatter1Name, null, new ModelOnlyWriteAttributeHandler(squatter1Name));
        AttributeDefinition squatter1Thing = TestUtils.createAttribute("thing1", ModelType.STRING);
        squatter1Reg.registerReadWriteAttribute(squatter1Thing, null, new ModelOnlyWriteAttributeHandler(squatter1Thing));

        ManagementResourceRegistration squatter3Reg = profileBSub3Reg.registerSubModel(
                new SimpleResourceDefinition(PathElement.pathElement("service", "squatter3"), new NonResolvingResourceDescriptionResolver()));
        AttributeDefinition squatter3Name = TestUtils.createAttribute("name", ModelType.STRING);
        squatter3Reg.registerReadWriteAttribute(squatter3Name, null, new ModelOnlyWriteAttributeHandler(squatter3Name));
        AttributeDefinition squatter3Thing = TestUtils.createAttribute("thing3", ModelType.LONG);
        squatter3Reg.registerReadWriteAttribute(squatter3Thing, null, new ModelOnlyWriteAttributeHandler(squatter3Thing));
        profileBSub3Reg.registerAlias(
                PathElement.pathElement("service", "squatter2"),
                new AliasEntry(squatter3Reg) {
                    @Override
                    public PathAddress convertToTargetAddress(PathAddress address) {
                        List<PathElement> list = new ArrayList<PathElement>();
                        for (PathElement element : address) {
                            if ("service".equals(element.getKey()) && "squatter2".equals(element.getValue())) {
                                list.add(PathElement.pathElement("service", "squatter3"));
                            } else {
                                list.add(element);
                            }
                        }
                        return PathAddress.pathAddress(list);
                    }
                });
        profileBSub3Reg.registerAlias(
                PathElement.pathElement("alias", "squatter1"),
                new AliasEntry(squatter1Reg) {
                    @Override
                    public PathAddress convertToTargetAddress(PathAddress address) {
                        List<PathElement> list = new ArrayList<PathElement>();
                        for (PathElement element : address) {
                            if ("alias".equals(element.getKey()) && "squatter1".equals(element.getValue())) {
                                list.add(PathElement.pathElement("service", "squatter1"));
                            } else {
                                list.add(element);
                            }
                        }
                        return PathAddress.pathAddress(list);
                    }
                });
        profileSub1Reg.registerOperationHandler(TestUtils.createOperationDefinition("testA1-1", TestUtils.createAttribute("paramA1", ModelType.INT)),
                new OperationStepHandler() {
                    @Override
                    public void execute(OperationContext context, ModelNode operation) {
                    }
                }
        );

        profileSub1Reg.registerOperationHandler(TestUtils.createOperationDefinition("testA1-2", TestUtils.createAttribute("paramA2", ModelType.STRING)),
                new OperationStepHandler() {
                    @Override
                    public void execute(OperationContext context, ModelNode operation) {
                    }
                }
        );

        profileASub2Reg.registerOperationHandler(TestUtils.createOperationDefinition("testA2", TestUtils.createAttribute("paramB", ModelType.LONG)),
                new OperationStepHandler() {

                    @Override
                    public void execute(OperationContext context, ModelNode operation) {
                    }
                }
        );

        ManagementResourceRegistration profileCSub4Reg = profileReg.registerSubModel(
                new SimpleResourceDefinition(PathElement.pathElement("subsystem", "subsystem4"), new NonResolvingResourceDescriptionResolver()));

        ManagementResourceRegistration profileCSub5Reg = profileReg.registerSubModel(
                new SimpleResourceDefinition(PathElement.pathElement("subsystem", "subsystem5"), new NonResolvingResourceDescriptionResolver()));
        profileCSub5Reg.registerReadOnlyAttribute(TestUtils.createAttribute("name", ModelType.STRING), new OperationStepHandler() {

            @Override
            public void execute(OperationContext context, ModelNode operation) {
                context.getResult().set("Overridden by special read handler");
                context.stepCompleted();
            }
        });

        ResourceDefinition profileCSub5Type1RegDef = ResourceBuilder.Factory.create(PathElement.pathElement("type1", "thing1"),
                new NonResolvingResourceDescriptionResolver())
                .build();
        ManagementResourceRegistration profileCSub5Type1Reg = profileCSub5Reg.registerSubModel(profileCSub5Type1RegDef);

        /*ManagementResourceRegistration profileCSub6Reg = profileReg.registerSubModel(
                new SimpleResourceDefinition(PathElement.pathElement("subsystem", "subsystem6"), new NonResolvingResourceDescriptionResolver()));

        profileCSub6Reg.registerOperationHandler(TestUtils.createOperationDefinition("testA", true),
                new OperationStepHandler() {

                    @Override
                    public void execute(OperationContext context, ModelNode operation) {
                    }
                }
        );*/
    }

    @Test
    public void testRecursiveReadSubModelOperationSimple() throws Exception {
        ModelNode operation = createOperation(READ_RESOURCE_OPERATION, "profile", "profileA", "subsystem", "subsystem1");
        operation.get(RECURSIVE).set(true);

        ModelNode result = executeForResult(operation);
        assertNotNull(result);
        checkRecursiveSubsystem1(result);
        assertFalse(result.get("metric1").isDefined());

        // Query runtime metrics
        operation = createOperation(READ_RESOURCE_OPERATION, "profile", "profileA", "subsystem", "subsystem1");
        operation.get(INCLUDE_RUNTIME).set(true);
        result = executeForResult(operation);

        assertTrue(result.get("metric1").isDefined());
        assertTrue(result.get("metric2").isDefined());
    }

    @Test
    public void testReadChildrenTypes() throws Exception {
        ModelNode operation = createOperation(READ_CHILDREN_TYPES_OPERATION, "profile", "profileA");

        ModelNode result = executeForResult(operation);
        assertNotNull(result);
        assertEquals(ModelType.LIST, result.getType());
        assertEquals(1, result.asList().size());
        assertEquals("subsystem", result.asList().get(0).asString());

        operation = createOperation(READ_CHILDREN_TYPES_OPERATION, "profile", "profileA");
        operation.get(ModelDescriptionConstants.INCLUDE_ALIASES).set(true);
        result = executeForResult(operation);
        assertNotNull(result);
        assertEquals(ModelType.LIST, result.getType());
        assertEquals(3, result.asList().size());
        assertEquals("alias", result.asList().get(0).asString());
        assertEquals("subsystem", result.asList().get(1).asString());
        assertEquals("subsystem-test", result.asList().get(2).asString());

        operation = createOperation(READ_CHILDREN_TYPES_OPERATION, "profile", "profileA", "subsystem", "subsystem1");
        result = executeForResult(operation);
        assertNotNull(result);
        assertEquals(ModelType.LIST, result.getType());
        assertEquals(2, result.asList().size());
        List<String> stringList = modelNodeListToStringList(result.asList());
        assertTrue(Arrays.asList("type1", "type2").containsAll(stringList));

        operation = createOperation(READ_CHILDREN_TYPES_OPERATION, "profile", "profileA", "subsystem", "non-existent");
        try {
            result = executeForResult(operation);
            fail("Expected error for non-existent child");
        } catch (OperationFailedException expected) {
        }

        operation = createOperation(READ_CHILDREN_TYPES_OPERATION, "profile", "profileC", "subsystem", "subsystem4");
        result = executeForResult(operation);
        assertNotNull(result);
        assertEquals(ModelType.LIST, result.getType());
        assertTrue(result.asList().isEmpty());

        operation = createOperation(READ_CHILDREN_TYPES_OPERATION, "profile", "profileC", "subsystem", "subsystem5");
        result = executeForResult(operation);
        assertNotNull(result);
        assertEquals(ModelType.LIST, result.getType());
        assertEquals(1, result.asList().size());
        assertEquals("type1", result.asList().get(0).asString());
    }

    @Test
    public void testReadChildrenResources() throws Exception {
        ModelNode operation = createOperation(READ_CHILDREN_RESOURCES_OPERATION, "profile", "profileA");
        operation.get(CHILD_TYPE).set("subsystem");
        operation.get(INCLUDE_RUNTIME).set(true);

        ModelNode result = executeForResult(operation);
        assertNotNull(result);
        assertEquals(ModelType.OBJECT, result.getType());
        assertEquals(1, result.asList().size());
        ModelNode subsystem1 = null;
        for (Property property : result.asPropertyList()) {
            if ("subsystem1".equals(property.getName())) {
                subsystem1 = property.getValue();
            }
        }
        assertNotNull(subsystem1);
        checkNonRecursiveSubsystem1(subsystem1, true);

        operation = createOperation(READ_CHILDREN_RESOURCES_OPERATION, "profile", "profileA", "subsystem-test", "subsystem1");
        operation.get(CHILD_TYPE).set("type2");
        result = executeForResult(operation);
        assertNotNull(result);
        assertEquals(ModelType.OBJECT, result.getType());
        assertEquals(1, result.asList().size());
        ModelNode other = null;
        for (Property property : result.asPropertyList()) {
            if ("other".equals(property.getName())) {
                other = property.getValue();
            }
        }
        assertNotNull(other);
        assertEquals("Name2", other.require(NAME).asString());

        operation.get(CHILD_TYPE).set("non-existent-child");
        try {
            result = executeForResult(operation);
            fail("Expected error for non-existent child");
        } catch (OperationFailedException expected) {
        }

        operation = createOperation(READ_CHILDREN_RESOURCES_OPERATION, "profile", "profileC", "subsystem", "subsystem4");
        operation.get(CHILD_TYPE).set("type1");
        try {
            result = executeForResult(operation);
            fail("Expected error for type1 child under subsystem4");
        } catch (OperationFailedException expected) {
        }

        operation = createOperation(READ_CHILDREN_RESOURCES_OPERATION, "profile", "profileC", "subsystem", "subsystem5");
        operation.get(CHILD_TYPE).set("type1");
        result = executeForResult(operation);
        assertNotNull(result);
        assertEquals(ModelType.OBJECT, result.getType());
        assertTrue(result.asList().isEmpty());
    }

    @Test
    public void testSquatterReadResourceDescription() throws Exception {
        ModelNode operation = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION);
        operation.get(RECURSIVE).set(true);
        ModelNode result = executeForResult(operation);
        checkRootNodeDescription(result, true, false, false);
        assertFalse(result.get(OPERATIONS).isDefined());
        operation = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION, "profile", "profileD");
        operation.get(RECURSIVE).set(true);
        result = executeForResult(operation);
        checkProfileNodeDescription(result, true, false, false);

        operation = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION, "profile", "profileD", "subsystem", "subsystem3");
        operation.get(RECURSIVE).set(true);
        result = executeForResult(operation);
        checkRecursiveSubsystem3Description(result, false, false, false);

        operation = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION, "profile", "profileD", "subsystem", "subsystem3");
        operation.get(RECURSIVE).set(true);
        operation.get(ModelDescriptionConstants.INCLUDE_ALIASES).set(true);
        result = executeForResult(operation);
        checkRecursiveSubsystem3Description(result, true, false, false);

        operation = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION, "profile", "profileD");
        operation.get(ModelDescriptionConstants.INCLUDE_ALIASES).set(true);
        result = executeForResult(operation);
        checkProfileNodeDescriptionWithAliases(result, false, false, false);
    }

    protected void checkRecursiveSubsystem3Description(ModelNode result, boolean withAlias, boolean operations, boolean notifications) {
        assertNotNull(result);
        assertEquals(ModelType.STRING, result.require(CHILDREN).require("service").require(MODEL_DESCRIPTION).require("squatter1").require(ATTRIBUTES).require("name").require(TYPE).asType());
        assertEquals(ModelType.STRING, result.require(CHILDREN).require("service").require(MODEL_DESCRIPTION).require("squatter1").require(ATTRIBUTES).require("thing1").require(TYPE).asType());

        assertEquals(ModelType.STRING, result.require(CHILDREN).require("service").require(MODEL_DESCRIPTION).require("squatter3").require(ATTRIBUTES).require("name").require(TYPE).asType());
        assertEquals(ModelType.LONG, result.require(CHILDREN).require("service").require(MODEL_DESCRIPTION).require("squatter3").require(ATTRIBUTES).require("thing3").require(TYPE).asType());

        if (withAlias) {
            assertEquals(ModelType.STRING, result.require(CHILDREN).require("service").require(MODEL_DESCRIPTION).require("squatter2").require(ATTRIBUTES).require("name").require(TYPE).asType());
            assertEquals(ModelType.LONG, result.require(CHILDREN).require("service").require(MODEL_DESCRIPTION).require("squatter2").require(ATTRIBUTES).require("thing3").require(TYPE).asType());
            assertEquals(ModelType.STRING, result.require(CHILDREN).require("alias").require(MODEL_DESCRIPTION).require("squatter1").require(ATTRIBUTES).require("name").require(TYPE).asType());
            assertEquals(ModelType.STRING, result.require(CHILDREN).require("alias").require(MODEL_DESCRIPTION).require("squatter1").require(ATTRIBUTES).require("thing1").require(TYPE).asType());

        }
    }

    @Test
    public void testReadChildrenResourcesRecursive() throws Exception {
        ModelNode operation = createOperation(READ_CHILDREN_RESOURCES_OPERATION, "profile", "profileA");
        operation.get(CHILD_TYPE).set("subsystem");
        operation.get(RECURSIVE).set(true);

        ModelNode result = executeForResult(operation);
        assertNotNull(result);
        assertEquals(ModelType.OBJECT, result.getType());
        assertEquals(1, result.asList().size());
        ModelNode subsystem1 = null;
        for (Property property : result.asPropertyList()) {
            if ("subsystem1".equals(property.getName())) {
                subsystem1 = property.getValue();
            }
        }
        assertNotNull(subsystem1);
        checkRecursiveSubsystem1(subsystem1);

        operation = createOperation(READ_CHILDREN_RESOURCES_OPERATION, "profile", "profileA", "subsystem-test", "subsystem1");
        operation.get(CHILD_TYPE).set("type2");
        result = executeForResult(operation);
        assertNotNull(result);
        assertEquals(ModelType.OBJECT, result.getType());
        assertEquals(1, result.asList().size());
        ModelNode other = null;
        for (Property property : result.asPropertyList()) {
            if ("other".equals(property.getName())) {
                other = property.getValue();
            }
        }
        assertNotNull(other);
        assertEquals("Name2", other.require(NAME).asString());

        operation.get(CHILD_TYPE).set("non-existent-child");
        try {
            result = executeForResult(operation);
            fail("Expected error for non-existent child");
        } catch (OperationFailedException expected) {
        }

        operation = createOperation(READ_CHILDREN_RESOURCES_OPERATION, "profile", "profileC", "subsystem", "subsystem4");
        operation.get(CHILD_TYPE).set("type1");
        try {
            result = executeForResult(operation);
            fail("Expected error for type1 child under subsystem4");
        } catch (OperationFailedException expected) {
        }

        operation = createOperation(READ_CHILDREN_RESOURCES_OPERATION, "profile", "profileC", "subsystem", "subsystem5");
        operation.get(CHILD_TYPE).set("type1");
        result = executeForResult(operation);
        assertNotNull(result);
        assertEquals(ModelType.OBJECT, result.getType());
        assertTrue(result.asList().isEmpty());
    }

    @Test
    public void testReadResourceDescriptionOperation() throws Exception {
        ModelNode operation = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION);
        ModelNode result = executeForResult(operation);
        checkRootNodeDescription(result, false, false, false);
        assertFalse(result.get(OPERATIONS).isDefined());

        operation = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION, "profile", "profileA");
        result = executeForResult(operation);
        checkProfileNodeDescription(result, false, false, false);

        operation = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION, "profile", "profileA");
        operation.get(ModelDescriptionConstants.INCLUDE_ALIASES).set(true);
        result = executeForResult(operation);
        checkProfileNodeDescriptionWithAliases(result, false, false, false);

        operation = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION, "profile", "profileA", "subsystem", "subsystem1");
        result = executeForResult(operation);
        checkSubsystem1Description(result, false, false, false);
        assertFalse(result.get(OPERATIONS).isDefined());

        operation = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION, "profile", "profileA", "subsystem", "subsystem1", "type1", "thing1");
        result = executeForResult(operation);
        checkType1Description(result);
        assertFalse(result.get(OPERATIONS).isDefined());

        operation = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION, "profile", "profileA", "subsystem", "subsystem1", "type1", "thing2");
        result = executeForResult(operation);
        checkType1Description(result);
        assertFalse(result.get(OPERATIONS).isDefined());

        operation = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION, "profile", "profileA", "subsystem", "subsystem1", "type2", "other");
        result = executeForResult(operation);
        checkType2Description(result);
        assertFalse(result.get(OPERATIONS).isDefined());
    }

    @Test
    public void testReadRecursiveResourceDescriptionOperation() throws Exception {
        ModelNode operation = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION);
        operation.get(RECURSIVE).set(true);
        ModelNode result = executeForResult(operation);
        checkRootNodeDescription(result, true, false, false);
        assertFalse(result.get(OPERATIONS).isDefined());

        operation = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION, "profile", "profileA");
        operation.get(RECURSIVE).set(true);
        result = executeForResult(operation);
        checkProfileNodeDescription(result, true, false, false);

        operation = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION, "profile", "profileA", "subsystem", "subsystem1");
        operation.get(RECURSIVE).set(true);
        result = executeForResult(operation);
        checkSubsystem1Description(result, true, false, false);
        assertFalse(result.get(OPERATIONS).isDefined());

        operation = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION, "profile", "profileA", "subsystem", "subsystem1", "type1", "thing1");
        operation.get(RECURSIVE).set(true);
        result = executeForResult(operation);
        checkType1Description(result);
        assertFalse(result.get(OPERATIONS).isDefined());

        operation = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION, "profile", "profileA", "subsystem", "subsystem1", "type1", "thing2");
        operation.get(RECURSIVE).set(true);
        result = executeForResult(operation);
        checkType1Description(result);
        assertFalse(result.get(OPERATIONS).isDefined());

        operation = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION, "profile", "profileA", "subsystem", "subsystem1", "type2", "other");
        operation.get(RECURSIVE).set(true);
        result = executeForResult(operation);
        checkType2Description(result);
        assertFalse(result.get(OPERATIONS).isDefined());
    }

    @Test
    public void testReadResourceDescriptionWithOperationsOperation() throws Exception {
        ModelNode operation = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION);
        operation.get(OPERATIONS).set(true);
        ModelNode result = executeForResult(operation);
        checkRootNodeDescription(result, false, true, false);
        assertTrue(result.require(OPERATIONS).isDefined());
        Set<String> ops = result.require(OPERATIONS).keys();
        assertTrue(ops.contains(READ_ATTRIBUTE_OPERATION));
        assertTrue(ops.contains(READ_CHILDREN_NAMES_OPERATION));
        assertTrue(ops.contains(READ_CHILDREN_TYPES_OPERATION));
        assertTrue(ops.contains(READ_OPERATION_DESCRIPTION_OPERATION));
        assertTrue(ops.contains(READ_OPERATION_NAMES_OPERATION));
        assertTrue(ops.contains(READ_RESOURCE_DESCRIPTION_OPERATION));
        assertTrue(ops.contains(READ_RESOURCE_OPERATION));
        for (String op : ops) {
            assertEquals(op, result.require(OPERATIONS).require(op).require(OPERATION_NAME).asString());
        }

        operation = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION, "profile", "profileA", "subsystem", "subsystem1");
        operation.get(OPERATIONS).set(true);
        result = executeForResult(operation);
        checkSubsystem1Description(result, false, true, false);

        operation = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION, "profile", "profileA", "subsystem", "subsystem1", "type1", "thing1");
        operation.get(OPERATIONS).set(true);
        result = executeForResult(operation);
        checkType1Description(result);

        operation = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION, "profile", "profileA", "subsystem", "subsystem1", "type1", "thing2");
        operation.get(OPERATIONS).set(true);
        result = executeForResult(operation);
        checkType1Description(result);

        operation = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION, "profile", "profileA", "subsystem", "subsystem1", "type2", "other");
        operation.get(OPERATIONS).set(true);
        result = executeForResult(operation);
        checkType2Description(result);
    }

    @Test
    public void testRecursiveReadResourceDescriptionWithOperationsOperation() throws Exception {
        ModelNode operation = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION);
        operation.get(OPERATIONS).set(true);
        operation.get(RECURSIVE).set(true);
        ModelNode result = executeForResult(operation);
        checkRootNodeDescription(result, true, true, false);

        operation = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION, "profile", "profileA", "subsystem", "subsystem1");
        operation.get(OPERATIONS).set(true);
        operation.get(RECURSIVE).set(true);
        result = executeForResult(operation);
        checkSubsystem1Description(result, true, true, false);

        operation = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION, "profile", "profileA", "subsystem", "subsystem1", "type1", "thing1");
        operation.get(OPERATIONS).set(true);
        operation.get(RECURSIVE).set(true);
        result = executeForResult(operation);
        checkType1Description(result);

        operation = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION, "profile", "profileA", "subsystem", "subsystem1", "type1", "thing2");
        operation.get(OPERATIONS).set(true);
        operation.get(RECURSIVE).set(true);
        result = executeForResult(operation);
        checkType1Description(result);

        operation = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION, "profile", "profileA", "subsystem", "subsystem1", "type2", "other");
        operation.get(OPERATIONS).set(true);
        operation.get(RECURSIVE).set(true);
        result = executeForResult(operation);
        checkType2Description(result);
    }

    @Test
    public void testReadResourceAttributesOnly() throws Exception {
        ModelNode operation = createOperation(READ_RESOURCE_OPERATION);
        operation.get(ATTRIBUTES_ONLY).set(true);
        ModelNode result = executeForResult(operation);
        Assert.assertEquals(0, result.keys().size());

        operation.get(OP_ADDR).add("profile", "profileB");
        result = executeForResult(operation);
        Assert.assertEquals(1, result.keys().size());
        Assert.assertEquals("Profile B", result.get("name").asString());

        operation.get(OP_ADDR).setEmptyList().add("profile", "profileA").add("subsystem", "subsystem1");
        result = executeForResult(operation);
        Assert.assertEquals(3, result.keys().size());
        List<ModelNode> list = result.get("attr1").asList();
        Assert.assertEquals(2, list.size());
        Assert.assertEquals(1, list.get(0).asInt());
        Assert.assertEquals(2, list.get(1).asInt());
        assertTrue(result.has("read-only"));
        assertTrue(result.has("read-write"));
        assertFalse(result.hasDefined("read-only"));
        assertFalse(result.hasDefined("read-write"));

        operation.get(RECURSIVE).set(true);
        executeForFailure(operation);
    }

    private void checkNonRecursiveSubsystem1(ModelNode result, boolean includeRuntime) {
        assertEquals(includeRuntime ? 7 : 5, result.keys().size());
        ModelNode content = result.require("attr1");
        List<ModelNode> list = content.asList();
        assertEquals(2, list.size());
        assertEquals(1, list.get(0).asInt());
        assertEquals(2, list.get(1).asInt());
        assertTrue(result.has("read-only"));
        assertTrue(result.has("read-write"));
        assertFalse(result.hasDefined("read-only"));
        assertFalse(result.hasDefined("read-write"));
        assertEquals(2, result.require("type1").keys().size());
        assertTrue(result.require("type1").has("thing1"));
        assertFalse(result.require("type1").get("thing1").isDefined());
        assertTrue(result.require("type1").has("thing2"));
        assertFalse(result.require("type1").get("thing2").isDefined());
        assertEquals(1, result.require("type2").keys().size());
        assertTrue(result.require("type2").has("other"));
        assertFalse(result.require("type2").get("other").isDefined());
        if (includeRuntime) {
            assertEquals(ModelType.INT, result.require("metric1").getType());
            assertEquals(ModelType.INT, result.require("metric2").getType());
        }
    }

    private void checkRecursiveSubsystem1(ModelNode result) {
        assertEquals(5, result.keys().size());
        List<ModelNode> list = result.require("attr1").asList();
        assertEquals(2, list.size());
        assertEquals(1, list.get(0).asInt());
        assertEquals(2, list.get(1).asInt());
        assertTrue(result.has("read-only"));
        assertTrue(result.has("read-write"));
        assertFalse(result.hasDefined("read-only"));
        assertFalse(result.hasDefined("read-write"));
        assertEquals("Name11", result.require("type1").require("thing1").require("name").asString());
        assertEquals(201, result.require("type1").require("thing1").require("value").asInt());
        assertEquals("Name12", result.require("type1").require("thing2").require("name").asString());
        assertEquals(202, result.require("type1").require("thing2").require("value").asInt());
        assertEquals("Name2", result.require("type2").require("other").require("name").asString());
    }

    protected void checkProfileNodeDescriptionWithAliases(ModelNode result, boolean recursive, boolean operations, boolean notifications) {
        checkProfileNodeDescription(result, recursive, operations, notifications);
        assertEquals(ModelType.STRING, result.require(ATTRIBUTES).require(NAME).require(TYPE).asType());
        assertEquals(false, result.require(ATTRIBUTES).require(NAME).require(NILLABLE).asBoolean());
        assertEquals(1, result.require(ATTRIBUTES).require(NAME).require(MIN_LENGTH).asInt());
        assertEquals("subsystem-test", result.require(CHILDREN).require("subsystem-test").require(DESCRIPTION).asString());
        assertEquals("alias", result.require(CHILDREN).require("alias").require(DESCRIPTION).asString());
        if (!recursive) {
            assertFalse(result.require(CHILDREN).require("subsystem-test").require(MODEL_DESCRIPTION).isDefined());
            assertFalse(result.require(CHILDREN).require("alias").require(MODEL_DESCRIPTION).isDefined());
            return;
        }
        assertTrue(result.require(CHILDREN).require("alias").require(MODEL_DESCRIPTION).isDefined());
        assertTrue(result.require(CHILDREN).require("subsystem-test").require(MODEL_DESCRIPTION).isDefined());
        assertEquals(6, result.require(CHILDREN).require("subsystem-test").require(MODEL_DESCRIPTION).keys().size());
        assertEquals(6, result.require(CHILDREN).require("alias").require(MODEL_DESCRIPTION).keys().size());
        checkSubsystem1Description(result.require(CHILDREN).require("subsystem-test").require(MODEL_DESCRIPTION).require("subsystem1"), recursive, operations, notifications);
    }
}
