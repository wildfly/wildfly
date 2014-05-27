/*
 *
 *  JBoss, Home of Professional Open Source.
 *  Copyright 2014, Red Hat, Inc., and individual contributors
 *  as indicated by the @author tags. See the copyright.txt file in the
 *  distribution for a full listing of individual contributors.
 *
 *  This is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as
 *  published by the Free Software Foundation; either version 2.1 of
 *  the License, or (at your option) any later version.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this software; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  02110-1301 USA, or see the FSF site: http://www.fsf.org.
 * /
 */

package org.jboss.as.jmx;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXPRESSIONS_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ObjectTypeAttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PrimitiveListAttributeDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleMapAttributeDefinition;
import org.jboss.as.controller.SimpleOperationDefinition;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.NonResolvingResourceDescriptionResolver;
import org.jboss.as.controller.operations.global.WriteAttributeHandlers;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author Tomaz Cerar (c) 2014 Red Hat Inc.
 */
public class ModelControllerResourceDefinition extends SimpleResourceDefinition {
    private final boolean allowExpressions;
    private final boolean forStandalone;
    private ModelNode complexValueType;
    protected ObjectTypeAttributeDefinition complex;
    private SimpleOperationDefinition COMPLEX_OP_DEF;


    private void init() {
        complexValueType = new ModelNode();
        complexValueType.get("int-value", DESCRIPTION).set("An int value");
        complexValueType.get("int-value", EXPRESSIONS_ALLOWED).set(allowExpressions);
        complexValueType.get("int-value", TYPE).set(ModelType.INT);
        complexValueType.get("bigdecimal-value", DESCRIPTION).set("A bigdecimal value");
        complexValueType.get("bigdecimal-value", TYPE).set(ModelType.BIG_DECIMAL);
        complexValueType.get("bigdecimal-value", EXPRESSIONS_ALLOWED).set(allowExpressions);

        SimpleAttributeDefinition intValue = createAttribute("int-value", ModelType.INT, allowExpressions);
        SimpleAttributeDefinition bigDecimal = createAttribute("bigdecimal-value", ModelType.BIG_DECIMAL, allowExpressions);

        complex = new ObjectTypeAttributeDefinition.Builder("complex", intValue, bigDecimal).build();
        AttributeDefinition param1 = new ObjectTypeAttributeDefinition.Builder("param1", intValue, bigDecimal).build();
        COMPLEX_OP_DEF = new SimpleOperationDefinitionBuilder("complex", new NonResolvingResourceDescriptionResolver())
                .addParameter(param1)
                .setReplyType(ModelType.OBJECT)
                .setReplyParameters(complex)
                .build();

    }


    public ModelControllerResourceDefinition(boolean allowExpressions, boolean forStandalone) {
        super(PathElement.pathElement("subsystem", "test"), new NonResolvingResourceDescriptionResolver(),
                TestSubystemAdd.INSTANCE,
                ReloadRequiredRemoveStepHandler.INSTANCE
        );
        this.allowExpressions = allowExpressions;
        this.forStandalone = forStandalone;
        init();
    }

    @Override
    public void registerOperations(ManagementResourceRegistration registration) {
        super.registerOperations(registration);

        // TODO for domain server we have to register ops as RUNTIME_ONLY or they get stripped from r-r-d results,
        // which is not ideal but not relevant to this test
        registration.registerOperationHandler(forStandalone ? VoidOperationNoParams.DEFINITION_STANDALONE : VoidOperationNoParams.DEFINITION_DOMAIN, VoidOperationNoParams.INSTANCE);
        IntOperationWithParams intOp = allowExpressions ? IntOperationWithParams.INSTANCE_EXPRESSIONS : IntOperationWithParams.INSTANCE_NO_EXPRESSIONS;
        registration.registerOperationHandler(forStandalone ? intOp.DEFINITION_STANDALONE : intOp.DEFINITION_DOMAIN, intOp);
        ComplexOperation op = new ComplexOperation(complexValueType);
        registration.registerOperationHandler(COMPLEX_OP_DEF, op);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);

        resourceRegistration.registerReadOnlyAttribute(createAttribute("ro-int", ModelType.INT, allowExpressions), null);
        addAttribute("undefined-int", ModelType.INT, resourceRegistration);
        addAttribute("int", ModelType.INT, resourceRegistration);
        addAttribute("bigint", ModelType.BIG_INTEGER, resourceRegistration);
        addAttribute("bigdec", ModelType.BIG_DECIMAL, resourceRegistration);
        addAttribute("boolean", ModelType.BOOLEAN, resourceRegistration);
        addAttribute("bytes", ModelType.BYTES, resourceRegistration);
        addAttribute("double", ModelType.DOUBLE, resourceRegistration);
        addAttribute("string", ModelType.STRING, resourceRegistration);
        addAttribute("long", ModelType.LONG, resourceRegistration);
        addAttribute("type", ModelType.TYPE, resourceRegistration);
        PrimitiveListAttributeDefinition list = new PrimitiveListAttributeDefinition.Builder("list", ModelType.INT).setAllowNull(true).setAllowExpression(allowExpressions).build();
        resourceRegistration.registerReadWriteAttribute(list, null, new ReloadRequiredWriteAttributeHandler(list));
        SimpleMapAttributeDefinition map = new SimpleMapAttributeDefinition.Builder("map", ModelType.INT, true).setAllowNull(true).setAllowExpression(allowExpressions).build();
        resourceRegistration.registerReadWriteAttribute(map, null, new ReloadRequiredWriteAttributeHandler(map));


        resourceRegistration.registerReadWriteAttribute(complex, null, new ComplexWriteAttributeHandler());
    }

    private static SimpleAttributeDefinition createAttribute(String name, ModelType type, boolean allowExpressions) {
        return new SimpleAttributeDefinitionBuilder(name, type, true)
                .setAllowExpression(allowExpressions)
                .build();
    }

    private void addAttribute(String name, ModelType type, ManagementResourceRegistration resourceRegistration) {
        AttributeDefinition attr = createAttribute(name, type, allowExpressions);
        //resourceRegistration.registerReadWriteAttribute(attr, null, new ReloadRequiredWriteAttributeHandler(attr));
        resourceRegistration.registerReadWriteAttribute(attr, null, new WriteAttributeHandlers.ModelTypeValidatingHandler(type, true, allowExpressions));
    }

    class ComplexWriteAttributeHandler extends ReloadRequiredWriteAttributeHandler {
        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            super.execute(context, operation);
        }
    }

    static class TestSubystemAdd extends AbstractAddStepHandler {
        static final TestSubystemAdd INSTANCE = new TestSubystemAdd();

        @Override
        protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
            model.get("ro-int").set(1);
            model.get("int").set(2);
            model.get("bigint").set(new BigInteger("3"));
            model.get("bigdec").set(new BigDecimal("4"));
            model.get("boolean").set(false);
            model.get("bytes").set(new byte[]{5, 6});
            model.get("double").set(7.0);
            model.get("string").set("8");
            model.get("list").add(new Integer(9));
            model.get("long").set(10L);
            model.get("type").set(ModelType.INT);
            model.get("map", "key1").set(11);
            model.get("map", "key2").set(12);
        }
    }

    static class VoidOperationNoParams implements OperationStepHandler {
        static final String OPERATION_NAME = "void-no-params";

        static final OperationDefinition DEFINITION_STANDALONE = new SimpleOperationDefinitionBuilder(VoidOperationNoParams.OPERATION_NAME, new NonResolvingResourceDescriptionResolver())
                .setReadOnly()
                .build();
        static final OperationDefinition DEFINITION_DOMAIN = new SimpleOperationDefinitionBuilder(VoidOperationNoParams.OPERATION_NAME, new NonResolvingResourceDescriptionResolver())
                .setReadOnly()
                .setRuntimeOnly()
                .build();
        static final VoidOperationNoParams INSTANCE = new VoidOperationNoParams();
        static final String OPERATION_JMX_NAME = "voidNoParams";
        boolean invoked;

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            invoked = true;
            context.stepCompleted();
        }
    }

    static class IntOperationWithParams implements OperationStepHandler {
        boolean allowExpressions;
        final SimpleAttributeDefinition param1 = createAttribute("param1", ModelType.LONG, allowExpressions);
        final PrimitiveListAttributeDefinition param2 = new PrimitiveListAttributeDefinition.Builder("param2", ModelType.STRING).setAllowExpression(allowExpressions).build();
        final SimpleMapAttributeDefinition param3 = new SimpleMapAttributeDefinition.Builder("param3", ModelType.INT, true)
                .setAllowExpression(allowExpressions)
                .build();
        final SimpleAttributeDefinition param4 = new SimpleAttributeDefinitionBuilder("param4", ModelType.INT)
                .setDefaultValue(new ModelNode(6))
                        //.setValidator(new IntRangeValidator(5,10,true,false)) //todo expressions & min/max dont match well
                .setAllowExpression(allowExpressions)
                .build();

        final SimpleAttributeDefinition param5 = new SimpleAttributeDefinitionBuilder("param5", ModelType.INT)
                .setAllowExpression(allowExpressions)
                .build();

        final OperationDefinition DEFINITION_STANDALONE = new SimpleOperationDefinitionBuilder(IntOperationWithParams.OPERATION_NAME, new NonResolvingResourceDescriptionResolver())
                .setParameters(param1, param2, param3, param4, param5)
                .setReplyType(ModelType.STRING)
                .build();
        final OperationDefinition DEFINITION_DOMAIN = new SimpleOperationDefinitionBuilder(IntOperationWithParams.OPERATION_NAME, new NonResolvingResourceDescriptionResolver())
                .setParameters(param1, param2, param3, param4, param5)
                .setReplyType(ModelType.STRING)
                .setRuntimeOnly()
                .build();

        static final IntOperationWithParams INSTANCE_NO_EXPRESSIONS = new IntOperationWithParams(false);
        static final IntOperationWithParams INSTANCE_EXPRESSIONS = new IntOperationWithParams(true);


        static final String OPERATION_NAME = "int-with-params";
        static final String OPERATION_JMX_NAME = "intWithParams";
        boolean invoked;


        private IntOperationWithParams(boolean allowExpressions) {
            this.allowExpressions = allowExpressions;
        }

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            invoked = true;
            long l = operation.get("param1").resolve().asLong() + context.readResource(PathAddress.EMPTY_ADDRESS).getModel().get("int").asInt() + operation.get("param3", "test").resolve().asInt();
            context.getResult().set(operation.get("param2").resolve().asList().get(0).asString() + l);
            context.stepCompleted();
        }
    }

    class ComplexOperation implements OperationStepHandler {
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

    }
}
