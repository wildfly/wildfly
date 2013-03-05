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
package org.jboss.as.controller.test;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTE_VALUE_WRITTEN_NOTIFICATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NOTIFICATION_TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ACCESS_TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILDREN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MIN_LENGTH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MIN_OCCURS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MODEL_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NILLABLE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NOTIFICATIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROFILE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_NAMES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_RESOURCES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_TYPES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_OPERATION_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_OPERATION_NAMES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUIRED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESOURCE_ADDED_NOTIFICATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESOURCE_REMOVED_NOTIFICATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE_TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.ResourceBuilder;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.NonResolvingResourceDescriptionResolver;
import org.jboss.as.controller.operations.global.GlobalOperationHandlers;
import org.jboss.as.controller.operations.global.WriteAttributeHandlers;
import org.jboss.as.controller.registry.AttributeAccess.AccessType;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;

/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public abstract class AbstractGlobalOperationsTestCase extends AbstractControllerTestBase {

    private final AccessType expectedRwAttributeAccess;

    private static final OperationDefinition SETUP_OP_DEF = new SimpleOperationDefinitionBuilder("setup", new NonResolvingResourceDescriptionResolver())
            .setPrivateEntry()
            .build();

    protected AbstractGlobalOperationsTestCase() {
        super();
        this.expectedRwAttributeAccess = AccessType.READ_WRITE;
    }

    protected AbstractGlobalOperationsTestCase(ProcessType processType, AccessType expectedRwAttributeAccess) {
        super(processType);
        this.expectedRwAttributeAccess = expectedRwAttributeAccess;
    }

    private static AttributeDefinition createAttribute(String name, ModelType type) {
        return SimpleAttributeDefinitionBuilder.create(name, type).build();
    }

    private static AttributeDefinition createMetric(String name, ModelType type) {
        return SimpleAttributeDefinitionBuilder.create(name, type).setStorageRuntime().build();
    }

    private static OperationDefinition createOperationDefinition(String name, AttributeDefinition... parameters) {
        return new SimpleOperationDefinitionBuilder(name, new NonResolvingResourceDescriptionResolver())
                .setParameters(parameters)
                .build();
    }

    @Override
    protected void initModel(Resource rootResource, ManagementResourceRegistration rootRegistration) {
        GlobalOperationHandlers.registerGlobalOperations(rootRegistration, processType);
        rootRegistration.registerOperationHandler(SETUP_OP_DEF, new OperationStepHandler() {
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


                model.get("profile", "profileA", "subsystem", "subsystem2", "bigdecimal").set(new BigDecimal(100));
                model.get("profile", "profileA", "subsystem", "subsystem2", "biginteger").set(new BigInteger("101"));
                model.get("profile", "profileA", "subsystem", "subsystem2", "boolean").set(true);
                model.get("profile", "profileA", "subsystem", "subsystem2", "bytes").set(new byte[]{1, 2, 3});
                model.get("profile", "profileA", "subsystem", "subsystem2", "double").set(Double.MAX_VALUE);
                model.get("profile", "profileA", "subsystem", "subsystem2", "expression").setExpression("{expr}");
                model.get("profile", "profileA", "subsystem", "subsystem2", "int").set(102);
                model.get("profile", "profileA", "subsystem", "subsystem2", "list").add("l1A");
                model.get("profile", "profileA", "subsystem", "subsystem2", "list").add("l1B");
                model.get("profile", "profileA", "subsystem", "subsystem2", "long").set(Long.MAX_VALUE);
                model.get("profile", "profileA", "subsystem", "subsystem2", "object", "value").set("objVal");
                model.get("profile", "profileA", "subsystem", "subsystem2", "property").set(new Property("prop1", new ModelNode().set("value1")));
                model.get("profile", "profileA", "subsystem", "subsystem2", "string1").set("s1");
                model.get("profile", "profileA", "subsystem", "subsystem2", "string2").set("s2");
                model.get("profile", "profileA", "subsystem", "subsystem2", "type").set(ModelType.TYPE);


                model.get("profile", "profileB", "name").set("Profile B");

                model.get("profile", "profileC", "subsystem", "subsystem4");
                model.get("profile", "profileC", "subsystem", "subsystem5", "name").set("Test");

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

        ManagementResourceRegistration profileSub1Reg = profileReg.registerSubModel(PathElement.pathElement("subsystem", "subsystem1"), new DescriptionProvider() {

            @Override
            public ModelNode getModelDescription(Locale locale) {
                ModelNode node = new ModelNode();
                node.get(DESCRIPTION).set("A test subsystem 1");
                node.get(ATTRIBUTES, "attr1", TYPE).set(ModelType.LIST);
                node.get(ATTRIBUTES, "attr1", VALUE_TYPE).set(ModelType.INT);
                node.get(ATTRIBUTES, "attr1", DESCRIPTION).set("The values");
                node.get(ATTRIBUTES, "attr1", REQUIRED).set(true);
                node.get(ATTRIBUTES, "read-only", TYPE).set(ModelType.INT);
                node.get(ATTRIBUTES, "read-only", DESCRIPTION).set("A r/o int");
                node.get(ATTRIBUTES, "read-only", REQUIRED).set(false);
                node.get(ATTRIBUTES, "metric1", TYPE).set(ModelType.INT);
                node.get(ATTRIBUTES, "metric1", DESCRIPTION).set("A random metric");
                node.get(ATTRIBUTES, "read-write", TYPE).set(ModelType.INT);
                node.get(ATTRIBUTES, "metric2", TYPE).set(ModelType.INT);
                node.get(ATTRIBUTES, "read-write", DESCRIPTION).set("A r/w int");
                node.get(ATTRIBUTES, "read-write", REQUIRED).set(false);
                node.get(CHILDREN, "type1", DESCRIPTION).set("The children1");
                node.get(CHILDREN, "type1", MIN_OCCURS).set(1);
                node.get(CHILDREN, "type1", MODEL_DESCRIPTION);
                node.get(CHILDREN, "type2", DESCRIPTION).set("The children2");
                node.get(CHILDREN, "type2", MIN_OCCURS).set(1);
                node.get(CHILDREN, "type2", MODEL_DESCRIPTION);
                return node;
            }
        });

        profileSub1Reg.registerReadOnlyAttribute(createAttribute("read-only", ModelType.INT), null);
        profileSub1Reg.registerReadWriteAttribute(createAttribute("read-write", ModelType.INT), null, new WriteAttributeHandlers.ModelTypeValidatingHandler(ModelType.INT));
        profileSub1Reg.registerMetric(createMetric("metric1", ModelType.INT), TestMetricHandler.INSTANCE);
        profileSub1Reg.registerMetric(createMetric("metric2", ModelType.INT), TestMetricHandler.INSTANCE);

        ResourceDefinition profileSub1RegType1Def = ResourceBuilder.Factory.create(PathElement.pathElement("type1", "*"),
                new NonResolvingResourceDescriptionResolver())
                .addReadOnlyAttribute(createAttribute("name", ModelType.STRING))
                .addReadOnlyAttribute(createAttribute("value", ModelType.INT))
                .build();
        profileSub1Reg.registerSubModel(profileSub1RegType1Def);

        ResourceDefinition profileSub1RegType2Def = ResourceBuilder.Factory.create(PathElement.pathElement("type2", "other"),
                new NonResolvingResourceDescriptionResolver())
                .addReadOnlyAttribute(createAttribute("name", ModelType.STRING))
                .addReadOnlyAttribute(SimpleAttributeDefinitionBuilder.create("default", ModelType.STRING).setDefaultValue(new ModelNode("Default string")).build())
                .build();
        profileSub1Reg.registerSubModel(profileSub1RegType2Def);

        ManagementResourceRegistration profileASub2Reg = profileReg.registerSubModel(PathElement.pathElement("subsystem", "subsystem2"), new DescriptionProvider() {

            @Override
            public ModelNode getModelDescription(Locale locale) {
                ModelNode node = new ModelNode();
                node.get(DESCRIPTION).set("A test subsystem 2");
                node.get(ATTRIBUTES, "bigdecimal", TYPE).set(ModelType.BIG_DECIMAL);
                node.get(ATTRIBUTES, "bigdecimal", DESCRIPTION).set("A big decimal");
                node.get(ATTRIBUTES, "bigdecimal", REQUIRED).set(true);
                node.get(ATTRIBUTES, "biginteger", TYPE).set(ModelType.BIG_DECIMAL);
                node.get(ATTRIBUTES, "biginteger", DESCRIPTION).set("A big integer");
                node.get(ATTRIBUTES, "biginteger", REQUIRED).set(true);
                node.get(ATTRIBUTES, "boolean", TYPE).set(ModelType.BOOLEAN);
                node.get(ATTRIBUTES, "boolean", DESCRIPTION).set("A boolean");
                node.get(ATTRIBUTES, "boolean", REQUIRED).set(true);
                node.get(ATTRIBUTES, "bytes", TYPE).set(ModelType.BYTES);
                node.get(ATTRIBUTES, "bytes", DESCRIPTION).set("A bytes");
                node.get(ATTRIBUTES, "bytes", REQUIRED).set(true);
                node.get(ATTRIBUTES, "double", TYPE).set(ModelType.DOUBLE);
                node.get(ATTRIBUTES, "double", DESCRIPTION).set("A double");
                node.get(ATTRIBUTES, "double", REQUIRED).set(true);
                node.get(ATTRIBUTES, "expression", TYPE).set(ModelType.EXPRESSION);
                node.get(ATTRIBUTES, "expression", DESCRIPTION).set("A double");
                node.get(ATTRIBUTES, "expression", REQUIRED).set(true);
                node.get(ATTRIBUTES, "int", TYPE).set(ModelType.INT);
                node.get(ATTRIBUTES, "int", DESCRIPTION).set("A int");
                node.get(ATTRIBUTES, "int", REQUIRED).set(true);
                node.get(ATTRIBUTES, "list", TYPE).set(ModelType.LIST);
                node.get(ATTRIBUTES, "list", VALUE_TYPE).set(ModelType.STRING);
                node.get(ATTRIBUTES, "list", DESCRIPTION).set("A list");
                node.get(ATTRIBUTES, "list", REQUIRED).set(true);
                node.get(ATTRIBUTES, "long", TYPE).set(ModelType.LONG);
                node.get(ATTRIBUTES, "long", DESCRIPTION).set("A long");
                node.get(ATTRIBUTES, "long", REQUIRED).set(true);
                node.get(ATTRIBUTES, "object", TYPE).set(ModelType.OBJECT);
                node.get(ATTRIBUTES, "object", VALUE_TYPE).set(ModelType.STRING);
                node.get(ATTRIBUTES, "object", DESCRIPTION).set("A object");
                node.get(ATTRIBUTES, "object", REQUIRED).set(true);
                node.get(ATTRIBUTES, "property", TYPE).set(ModelType.PROPERTY);
                node.get(ATTRIBUTES, "property", VALUE_TYPE).set(ModelType.STRING);
                node.get(ATTRIBUTES, "property", DESCRIPTION).set("A property");
                node.get(ATTRIBUTES, "property", REQUIRED).set(true);
                node.get(ATTRIBUTES, "string1", TYPE).set(ModelType.STRING);
                node.get(ATTRIBUTES, "string1", DESCRIPTION).set("A string");
                node.get(ATTRIBUTES, "string1", REQUIRED).set(true);
                node.get(ATTRIBUTES, "string2", TYPE).set(ModelType.STRING);
                node.get(ATTRIBUTES, "string2", DESCRIPTION).set("A string");
                node.get(ATTRIBUTES, "string2", REQUIRED).set(true);
                node.get(ATTRIBUTES, "type", TYPE).set(ModelType.TYPE);
                node.get(ATTRIBUTES, "type", DESCRIPTION).set("A type");
                node.get(ATTRIBUTES, "type", REQUIRED).set(true);


                return node;
            }
        });

        profileASub2Reg.registerReadWriteAttribute(createAttribute("long", ModelType.LONG), null, new WriteAttributeHandlers.ModelTypeValidatingHandler(ModelType.LONG, false));

        ManagementResourceRegistration profileBSub3Reg = profileReg.registerSubModel(PathElement.pathElement("subsystem", "subsystem3"), new DescriptionProvider() {

            @Override
            public ModelNode getModelDescription(Locale locale) {
                ModelNode node = new ModelNode();
                node.get(DESCRIPTION).set("A test subsystem 1");
                node.get(ATTRIBUTES, "attr1", TYPE).set(ModelType.INT);
                node.get(ATTRIBUTES, "attr1", DESCRIPTION).set("The value");
                node.get(ATTRIBUTES, "attr1", REQUIRED).set(true);
                node.get(CHILDREN).setEmptyObject();
                return node;
            }
        });

        profileSub1Reg.registerOperationHandler(createOperationDefinition("testA1-1", createAttribute("paramA1", ModelType.INT)),
                new OperationStepHandler() {
                    @Override
                    public void execute(OperationContext context, ModelNode operation) {
                    }
                }
        );

        profileSub1Reg.registerOperationHandler(createOperationDefinition("testA1-2", createAttribute("paramA2", ModelType.STRING)),
                new OperationStepHandler() {
                    @Override
                    public void execute(OperationContext context, ModelNode operation) {
                    }
                }
        );


        profileASub2Reg.registerOperationHandler(createOperationDefinition("testA2", createAttribute("paramB", ModelType.LONG)),
                new OperationStepHandler() {

                    @Override
                    public void execute(OperationContext context, ModelNode operation) {
                    }
                }
        );

        ManagementResourceRegistration profileCSub4Reg = profileReg.registerSubModel(PathElement.pathElement("subsystem", "subsystem4"), new DescriptionProvider() {

            @Override
            public ModelNode getModelDescription(Locale locale) {
                ModelNode node = new ModelNode();
                node.get(DESCRIPTION).set("A subsystem");
                node.get(ATTRIBUTES, "name", TYPE).set(ModelType.STRING);
                node.get(ATTRIBUTES, "name", DESCRIPTION).set("The name of the thing");
                node.get(ATTRIBUTES, "name", REQUIRED).set(false);
                node.get(CHILDREN, "type1", DESCRIPTION).set("The children1");
                node.get(CHILDREN, "type1", MIN_OCCURS).set(0);
                node.get(CHILDREN, "type1", MODEL_DESCRIPTION);
                return node;
            }
        });


        ManagementResourceRegistration profileCSub5Reg = profileReg.registerSubModel(PathElement.pathElement("subsystem", "subsystem5"), new DescriptionProvider() {

            @Override
            public ModelNode getModelDescription(Locale locale) {
                ModelNode node = new ModelNode();
                node.get(DESCRIPTION).set("A subsystem");
                node.get(ATTRIBUTES, "name", TYPE).set(ModelType.STRING);
                node.get(ATTRIBUTES, "name", DESCRIPTION).set("The name of the thing");
                node.get(ATTRIBUTES, "name", REQUIRED).set(false);
                node.get(CHILDREN, "type1", DESCRIPTION).set("The children1");
                node.get(CHILDREN, "type1", MIN_OCCURS).set(0);
                node.get(CHILDREN, "type1", MODEL_DESCRIPTION);
                return node;
            }
        });
        profileCSub5Reg.registerReadOnlyAttribute(createAttribute("name", ModelType.STRING), new OperationStepHandler() {

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
    }

    /**
     * Override to get the actual result from the response.
     */
    public ModelNode executeForResult(ModelNode operation) throws OperationFailedException {
        ModelNode rsp = getController().execute(operation, null, null, null);
        if (FAILED.equals(rsp.get(OUTCOME).asString())) {
            throw new OperationFailedException(rsp.get(FAILURE_DESCRIPTION));
        }
        return rsp.get(RESULT);
    }

    static class TestMetricHandler implements OperationStepHandler {
        static final TestMetricHandler INSTANCE = new TestMetricHandler();
        private static final Random random = new Random();

        @Override
        public void execute(final OperationContext context, final ModelNode operation) {
            context.getResult().set(random.nextInt());
            context.stepCompleted();
        }

    }

    protected void checkRootNodeDescription(ModelNode result, boolean recursive, boolean operations, boolean notifications) {
        assertEquals("description", result.require(DESCRIPTION).asString());
        assertEquals("profile", result.require(CHILDREN).require(PROFILE).require(DESCRIPTION).asString());

        if (operations) {
            assertTrue(result.require(OPERATIONS).isDefined());
            Set<String> ops = result.require(OPERATIONS).keys();
            assertTrue(ops.contains(READ_ATTRIBUTE_OPERATION));
            assertTrue(ops.contains(READ_CHILDREN_NAMES_OPERATION));
            assertTrue(ops.contains(READ_CHILDREN_TYPES_OPERATION));
            assertTrue(ops.contains(READ_OPERATION_DESCRIPTION_OPERATION));
            assertTrue(ops.contains(READ_OPERATION_NAMES_OPERATION));
            assertTrue(ops.contains(READ_RESOURCE_DESCRIPTION_OPERATION));
            assertTrue(ops.contains(READ_RESOURCE_OPERATION));
            assertEquals(processType != ProcessType.DOMAIN_SERVER, ops.contains(WRITE_ATTRIBUTE_OPERATION));
            for (String op : ops) {
                assertEquals(op, result.require(OPERATIONS).require(op).require(OPERATION_NAME).asString());
            }
        } else {
            assertFalse(result.get(OPERATIONS).isDefined());
        }

        if (notifications) {
            assertTrue(result.require(NOTIFICATIONS).isDefined());
            Set<String> notifs = result.require(NOTIFICATIONS).keys();
            assertTrue(notifs.contains(RESOURCE_ADDED_NOTIFICATION));
            assertTrue(notifs.contains(RESOURCE_REMOVED_NOTIFICATION));
            assertEquals(processType != ProcessType.DOMAIN_SERVER, notifs.contains(ATTRIBUTE_VALUE_WRITTEN_NOTIFICATION));
            for (String notif : notifs) {
                assertEquals(notif, result.require(NOTIFICATIONS).require(notif).require(NOTIFICATION_TYPE).asString());
            }
        }

        if (!recursive) {
            assertFalse(result.require(CHILDREN).require(PROFILE).require(MODEL_DESCRIPTION).isDefined());
            return;
        }
        assertTrue(result.require(CHILDREN).require(PROFILE).require(MODEL_DESCRIPTION).isDefined());
        assertEquals(1, result.require(CHILDREN).require(PROFILE).require(MODEL_DESCRIPTION).keys().size());
        checkProfileNodeDescription(result.require(CHILDREN).require(PROFILE).require(MODEL_DESCRIPTION).require("*"), true, operations);
    }

    protected void checkProfileNodeDescription(ModelNode result, boolean recursive, boolean operations) {
        assertEquals(ModelType.STRING, result.require(ATTRIBUTES).require(NAME).require(TYPE).asType());
        assertEquals(false, result.require(ATTRIBUTES).require(NAME).require(NILLABLE).asBoolean());
        assertEquals(1, result.require(ATTRIBUTES).require(NAME).require(MIN_LENGTH).asInt());
        assertEquals("subsystem", result.require(CHILDREN).require(SUBSYSTEM).require(DESCRIPTION).asString());
        if (!recursive) {
            assertFalse(result.require(CHILDREN).require(SUBSYSTEM).require(MODEL_DESCRIPTION).isDefined());
            return;
        }
        assertTrue(result.require(CHILDREN).require(SUBSYSTEM).require(MODEL_DESCRIPTION).isDefined());
        assertEquals(5, result.require(CHILDREN).require(SUBSYSTEM).require(MODEL_DESCRIPTION).keys().size());
        checkSubsystem1Description(result.require(CHILDREN).require(SUBSYSTEM).require(MODEL_DESCRIPTION).require("subsystem1"), recursive, operations);
    }

    protected void checkSubsystem1Description(ModelNode result, boolean recursive, boolean operations) {
        assertNotNull(result);

        assertEquals(ModelType.LIST, result.require(ATTRIBUTES).require("attr1").require(TYPE).asType());
        assertEquals(ModelType.INT, result.require(ATTRIBUTES).require("attr1").require(VALUE_TYPE).asType());
        assertEquals("The values", result.require(ATTRIBUTES).require("attr1").require(DESCRIPTION).asString());
        assertTrue(result.require(ATTRIBUTES).require("attr1").require(REQUIRED).asBoolean());
        assertEquals(AccessType.READ_ONLY.toString(), result.require(ATTRIBUTES).require("attr1").get(ACCESS_TYPE).asString());
        assertEquals(ModelType.INT, result.require(ATTRIBUTES).require("read-only").require(TYPE).asType());
        assertEquals("A r/o int", result.require(ATTRIBUTES).require("read-only").require(DESCRIPTION).asString());
        assertFalse(result.require(ATTRIBUTES).require("read-only").require(REQUIRED).asBoolean());
        assertEquals(AccessType.READ_ONLY.toString(), result.require(ATTRIBUTES).require("read-only").get(ACCESS_TYPE).asString());
        assertEquals(ModelType.INT, result.require(ATTRIBUTES).require("metric1").require(TYPE).asType());
        assertEquals("A random metric", result.require(ATTRIBUTES).require("metric1").require(DESCRIPTION).asString());
        assertEquals(AccessType.METRIC.toString(), result.require(ATTRIBUTES).require("metric1").get(ACCESS_TYPE).asString());
        assertEquals(AccessType.METRIC.toString(), result.require(ATTRIBUTES).require("metric2").get(ACCESS_TYPE).asString());
        assertEquals(ModelType.INT, result.require(ATTRIBUTES).require("read-write").require(TYPE).asType());
        assertEquals("A r/w int", result.require(ATTRIBUTES).require("read-write").require(DESCRIPTION).asString());
        assertFalse(result.require(ATTRIBUTES).require("read-write").require(REQUIRED).asBoolean());
        assertEquals(expectedRwAttributeAccess.toString(), result.require(ATTRIBUTES).require("read-write").get(ACCESS_TYPE).asString());

        assertEquals("The children1", result.require(CHILDREN).require("type1").require(DESCRIPTION).asString());
        assertEquals(1, result.require(CHILDREN).require("type1").require(MIN_OCCURS).asInt());

        assertEquals("The children1", result.require(CHILDREN).require("type1").require(DESCRIPTION).asString());
        assertEquals("The children2", result.require(CHILDREN).require("type2").require(DESCRIPTION).asString());
        assertEquals(1, result.require(CHILDREN).require("type2").require(MIN_OCCURS).asInt());
        assertEquals(1, result.require(CHILDREN).require("type2").require(MIN_OCCURS).asInt());

        if (operations) {
            assertTrue(result.require(OPERATIONS).isDefined());
            Set<String> ops = result.require(OPERATIONS).keys();
            assertEquals(processType == ProcessType.DOMAIN_SERVER ? 8 : 12, ops.size());
            boolean runtimeOnly = processType != ProcessType.DOMAIN_SERVER;
            assertEquals(runtimeOnly, ops.contains("testA1-1"));
            assertEquals(runtimeOnly, ops.contains("testA1-2"));
            assertTrue(ops.contains(READ_RESOURCE_OPERATION));
            assertTrue(ops.contains(READ_ATTRIBUTE_OPERATION));
            assertTrue(ops.contains(READ_RESOURCE_DESCRIPTION_OPERATION));
            assertTrue(ops.contains(READ_CHILDREN_NAMES_OPERATION));
            assertTrue(ops.contains(READ_CHILDREN_TYPES_OPERATION));
            assertTrue(ops.contains(READ_CHILDREN_RESOURCES_OPERATION));
            assertTrue(ops.contains(READ_OPERATION_NAMES_OPERATION));
            assertTrue(ops.contains(READ_OPERATION_DESCRIPTION_OPERATION));
            assertEquals(runtimeOnly, ops.contains(WRITE_ATTRIBUTE_OPERATION));

        } else {
            assertFalse(result.get(OPERATIONS).isDefined());
        }

        if (!recursive) {
            assertFalse(result.require(CHILDREN).require("type1").require(MODEL_DESCRIPTION).isDefined());
            assertFalse(result.require(CHILDREN).require("type2").require(MODEL_DESCRIPTION).isDefined());
            return;
        }

        checkType1Description(result.require(CHILDREN).require("type1").require(MODEL_DESCRIPTION).require("*"));
        checkType2Description(result.require(CHILDREN).require("type2").require(MODEL_DESCRIPTION).require("other"));
    }

    protected void checkType1Description(ModelNode result) {
        assertNotNull(result);
        assertEquals(ModelType.STRING, result.require(ATTRIBUTES).require("name").require(TYPE).asType());
        assertEquals("name", result.require(ATTRIBUTES).require("name").require(DESCRIPTION).asString());
        assertFalse(result.require(ATTRIBUTES).require("name").require(NILLABLE).asBoolean());
        assertEquals(ModelType.INT, result.require(ATTRIBUTES).require("value").require(TYPE).asType());
        assertEquals("value", result.require(ATTRIBUTES).require("value").require(DESCRIPTION).asString());
        assertFalse(result.require(ATTRIBUTES).require("value").require(NILLABLE).asBoolean());
        //TODO should the inherited ops be picked up?
        if (result.hasDefined(OPERATIONS)) {
            assertTrue(result.require(OPERATIONS).isDefined());
            Set<String> ops = result.require(OPERATIONS).keys();
            assertEquals(processType == ProcessType.DOMAIN_SERVER ? 8 : 10, ops.size());
            assertTrue(ops.contains(READ_RESOURCE_OPERATION));
            assertTrue(ops.contains(READ_ATTRIBUTE_OPERATION));
            assertTrue(ops.contains(READ_RESOURCE_DESCRIPTION_OPERATION));
            assertTrue(ops.contains(READ_CHILDREN_NAMES_OPERATION));
            assertTrue(ops.contains(READ_CHILDREN_TYPES_OPERATION));
            assertTrue(ops.contains(READ_CHILDREN_RESOURCES_OPERATION));
            assertTrue(ops.contains(READ_OPERATION_NAMES_OPERATION));
            assertTrue(ops.contains(READ_OPERATION_DESCRIPTION_OPERATION));
            assertEquals(processType != ProcessType.DOMAIN_SERVER, ops.contains(WRITE_ATTRIBUTE_OPERATION));

        }

    }

    protected void checkType2Description(ModelNode result) {
        assertNotNull(result);
        assertEquals("description", result.require(DESCRIPTION).asString());
        assertEquals(ModelType.STRING, result.require(ATTRIBUTES).require("name").require(TYPE).asType());
        assertEquals("name", result.require(ATTRIBUTES).require("name").require(DESCRIPTION).asString());
        assertFalse(result.require(ATTRIBUTES).require("name").require(NILLABLE).asBoolean());
        if (result.hasDefined(OPERATIONS)) {
            assertTrue(result.require(OPERATIONS).isDefined());
            Set<String> ops = result.require(OPERATIONS).keys();
            assertEquals(processType == ProcessType.DOMAIN_SERVER ? 8 : 10, ops.size());
            assertTrue(ops.contains(READ_RESOURCE_OPERATION));
            assertTrue(ops.contains(READ_ATTRIBUTE_OPERATION));
            assertTrue(ops.contains(READ_RESOURCE_DESCRIPTION_OPERATION));
            assertTrue(ops.contains(READ_CHILDREN_NAMES_OPERATION));
            assertTrue(ops.contains(READ_CHILDREN_TYPES_OPERATION));
            assertTrue(ops.contains(READ_CHILDREN_RESOURCES_OPERATION));
            assertTrue(ops.contains(READ_OPERATION_NAMES_OPERATION));
            assertTrue(ops.contains(READ_OPERATION_DESCRIPTION_OPERATION));
            assertEquals(processType != ProcessType.DOMAIN_SERVER, ops.contains(WRITE_ATTRIBUTE_OPERATION));
        }
    }

    protected ModelNode createOperation(String operationName, String... address) {
        ModelNode operation = new ModelNode();
        operation.get(OP).set(operationName);
        if (address.length > 0) {
            for (String addr : address) {
                operation.get(OP_ADDR).add(addr);
            }
        } else {
            operation.get(OP_ADDR).setEmptyList();
        }

        return operation;
    }

    protected List<String> modelNodeListToStringList(List<ModelNode> nodes) {
        List<String> result = new ArrayList<String>();
        for (ModelNode node : nodes) {
            result.add(node.asString());
        }
        return result;
    }

}
