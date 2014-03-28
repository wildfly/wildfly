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

import java.math.BigDecimal;
import java.math.BigInteger;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsNull.notNullValue;
import org.jboss.as.controller.ControlledProcessState;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.ResourceBuilder;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_REQUIRES_RELOAD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROCESS_STATE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESPONSE_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import org.jboss.as.controller.descriptions.NonResolvingResourceDescriptionResolver;
import org.jboss.as.controller.operations.global.GlobalOperationHandlers;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.ValueExpression;
import static org.junit.Assert.assertThat;
import org.junit.Test;

/**
 *
 * @author <a href="mailto:ehugonne@redhat.com">Emmanuel Hugonnet</a> (c) 2014 Red Hat, inc.
 */
public class WriteAttributeOperationTestCase extends AbstractControllerTestBase {

    private static final String BOOLEAN_ATT_NAME = "boolean-att";
    private static final String LONG_ATT_NAME = "long-att";
    private static final String STRING_ATT_NAME = "string-att";
    private static final String DOUBLE_ATT_NAME = "double-att";
    private static final String INT_ATT_NAME = "int-att";
    private static final String BYTES_ATT_NAME = "bytes-att";
    private static final String BIGINT_ATT_NAME = "bigint-att";
    private static final String BIGDEC_ATT_NAME = "bigdec-att";

    private static final OperationDefinition SETUP_OP_DEF = new SimpleOperationDefinitionBuilder("setup", new NonResolvingResourceDescriptionResolver())
            .setPrivateEntry()
            .build();

    protected static final SimpleAttributeDefinition LONG_ATT = new SimpleAttributeDefinitionBuilder(LONG_ATT_NAME, ModelType.LONG)
            .setAllowNull(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setAllowExpression(true)
            .setMaxSize(1)
            .build();

    protected static final SimpleAttributeDefinition DOUBLE_ATT = new SimpleAttributeDefinitionBuilder(DOUBLE_ATT_NAME, ModelType.DOUBLE)
            .setAllowNull(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setAllowExpression(true)
            .setMaxSize(1)
            .build();

    protected static final SimpleAttributeDefinition BOOLEAN_ATT = new SimpleAttributeDefinitionBuilder(BOOLEAN_ATT_NAME, ModelType.BOOLEAN)
            .setAllowNull(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setDefaultValue(new ModelNode(true))
            .setAllowExpression(true)
            .setMaxSize(1)
            .build();

    protected static final SimpleAttributeDefinition STRING_ATT = new SimpleAttributeDefinitionBuilder(STRING_ATT_NAME, ModelType.STRING)
            .setAllowNull(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setAllowExpression(true)
            .setMaxSize(1)
            .build();

    protected static final SimpleAttributeDefinition INT_ATT = new SimpleAttributeDefinitionBuilder(INT_ATT_NAME, ModelType.INT)
            .setAllowNull(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setAllowExpression(true)
            .setMaxSize(1)
            .build();

    protected static final SimpleAttributeDefinition BYTES_ATT = new SimpleAttributeDefinitionBuilder(BYTES_ATT_NAME, ModelType.BYTES)
            .setAllowNull(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setAllowExpression(true)
            .setMaxSize(1)
            .build();

    protected static final SimpleAttributeDefinition BIGINT_ATT = new SimpleAttributeDefinitionBuilder(BIGINT_ATT_NAME, ModelType.BIG_INTEGER)
            .setAllowNull(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setAllowExpression(true)
            .setMaxSize(1)
            .build();

    protected static final SimpleAttributeDefinition BIGDEC_ATT = new SimpleAttributeDefinitionBuilder(BIGDEC_ATT_NAME, ModelType.BIG_DECIMAL)
            .setAllowNull(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setAllowExpression(true)
            .setMaxSize(1)
            .build();

    private static final OperationStepHandler handler = new ReloadRequiredWriteAttributeHandler(
            BOOLEAN_ATT, LONG_ATT, STRING_ATT, DOUBLE_ATT, INT_ATT, BYTES_ATT, BIGINT_ATT, BIGDEC_ATT);

    @Override
    protected void initModel(Resource rootResource, ManagementResourceRegistration rootRegistration) {
        System.setProperty("boolean-value", "true");
        System.setProperty("long-value", "1000");
        System.setProperty("string-value", "wildfly");
        System.setProperty("double-value", "1.0");
        System.setProperty("int-value", "100");
        System.setProperty("bytes-value", "wildfly");
        System.setProperty("bigint-value", "100");
        System.setProperty("bigdec-value", "10.0");

        GlobalOperationHandlers.registerGlobalOperations(rootRegistration, processType);
        rootRegistration.registerOperationHandler(SETUP_OP_DEF, new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                final ModelNode model = new ModelNode();
                //Atttributes
                model.get("profile", "profileA", "subsystem", "subsystem1", BOOLEAN_ATT_NAME).set(true);
                model.get("profile", "profileA", "subsystem", "subsystem1", LONG_ATT_NAME).set(1000L);
                model.get("profile", "profileB", "subsystem", "subsystem1", BOOLEAN_ATT_NAME).set(new ValueExpression("${boolean-value}"));
                model.get("profile", "profileB", "subsystem", "subsystem1", LONG_ATT_NAME).set(new ValueExpression("${long-value}"));
                model.get("profile", "profilType", "subsystem", "subsystem1", BOOLEAN_ATT_NAME).set(true);
                model.get("profile", "profilType", "subsystem", "subsystem1", LONG_ATT_NAME).set(1000L);
                model.get("profile", "profilType", "subsystem", "subsystem1", STRING_ATT_NAME).set("wildfly");
                model.get("profile", "profilType", "subsystem", "subsystem1", DOUBLE_ATT_NAME).set(1.0D);
                model.get("profile", "profilType", "subsystem", "subsystem1", INT_ATT_NAME).set(100);
                model.get("profile", "profilType", "subsystem", "subsystem1", BYTES_ATT_NAME).set("wildfly".getBytes(UTF_8));
                model.get("profile", "profilType", "subsystem", "subsystem1", BIGINT_ATT_NAME).set(new BigInteger("100"));
                model.get("profile", "profilType", "subsystem", "subsystem1", BIGDEC_ATT_NAME).set(new BigDecimal("10.0"));
                createModel(context, model);
                context.stepCompleted();
            }
        }
        );

        ResourceDefinition profileDef = ResourceBuilder.Factory.create(PathElement.pathElement("profile", "*"),
                new NonResolvingResourceDescriptionResolver())
                .addReadOnlyAttribute(SimpleAttributeDefinitionBuilder.create("name", ModelType.STRING, false).setMinSize(1).build())
                .pushChild(PathElement.pathElement("subsystem", "subsystem1"))
                .addReadWriteAttribute(BOOLEAN_ATT, null, handler)
                .addReadWriteAttribute(LONG_ATT, null, handler)
                .addReadWriteAttribute(STRING_ATT, null, handler)
                .addReadWriteAttribute(DOUBLE_ATT, null, handler)
                .addReadWriteAttribute(INT_ATT, null, handler)
                .addReadWriteAttribute(BYTES_ATT, null, handler)
                .addReadWriteAttribute(BIGINT_ATT, null, handler)
                .addReadWriteAttribute(BIGDEC_ATT, null, handler)
                .pop()
                .build();
        rootRegistration.registerSubModel(profileDef);
    }

    @Test
    public void testWriteReloadAttribute() throws Exception {

        //Just make sure it works as expected for an existant resource
        ModelNode operation = createOperation(READ_ATTRIBUTE_OPERATION, "profile", "profileA", "subsystem", "subsystem1");
        operation.get(NAME).set(BOOLEAN_ATT_NAME);
        ModelNode result = executeForResult(operation);
        assertThat(result, is(notNullValue()));
        assertThat(result.getType(), is(ModelType.BOOLEAN));
        assertThat(result.asBoolean(), is(true));

        operation = createOperation(READ_ATTRIBUTE_OPERATION, "profile", "profileA", "subsystem", "subsystem1");
        operation.get(NAME).set(LONG_ATT_NAME);
        result = executeForResult(operation);
        assertThat(result, is(notNullValue()));
        assertThat(result.getType(), is(ModelType.LONG));
        assertThat(result.asLong(), is(1000L));

        ModelNode write = createOperation(WRITE_ATTRIBUTE_OPERATION, "profile", "profileA", "subsystem", "subsystem1");
        write.get(NAME).set(BOOLEAN_ATT_NAME);
        write.get(VALUE).set(false);
        result = executeCheckNoFailure(write);
        assertThat(result, is(notNullValue()));
        assertThat(result.get(RESPONSE_HEADERS, OPERATION_REQUIRES_RELOAD).asBoolean(), is(true));
        assertThat(result.get(RESPONSE_HEADERS, PROCESS_STATE).asString(), is(ControlledProcessState.State.RELOAD_REQUIRED.toString()));

        write = createOperation(WRITE_ATTRIBUTE_OPERATION, "profile", "profileA", "subsystem", "subsystem1");
        write.get(NAME).set(LONG_ATT_NAME);
        write.get(VALUE).set(10000L);
        result = executeCheckNoFailure(write);
        assertThat(result, is(notNullValue()));
        assertThat(result.get(RESPONSE_HEADERS, OPERATION_REQUIRES_RELOAD).asBoolean(), is(true));
        assertThat(result.get(RESPONSE_HEADERS, PROCESS_STATE).asString(), is(ControlledProcessState.State.RELOAD_REQUIRED.toString()));
    }

    @Test
    public void testWriteReloadAttributeWithoutChange() throws Exception {

        //Just make sure it works as expected for an existant resource
        ModelNode operation = createOperation(READ_ATTRIBUTE_OPERATION, "profile", "profileA", "subsystem", "subsystem1");
        operation.get(NAME).set(BOOLEAN_ATT_NAME);
        ModelNode result = executeForResult(operation);
        assertThat(result, is(notNullValue()));
        assertThat(result.getType(), is(ModelType.BOOLEAN));
        assertThat(result.asBoolean(), is(true));

        operation = createOperation(READ_ATTRIBUTE_OPERATION, "profile", "profileA", "subsystem", "subsystem1");
        operation.get(NAME).set(LONG_ATT_NAME);
        result = executeForResult(operation);
        assertThat(result, is(notNullValue()));
        assertThat(result.getType(), is(ModelType.LONG));
        assertThat(result.asLong(), is(1000L));

        ModelNode rewrite = createOperation(WRITE_ATTRIBUTE_OPERATION, "profile", "profileA", "subsystem", "subsystem1");
        rewrite.get(NAME).set(BOOLEAN_ATT_NAME);
        rewrite.get(VALUE).set(true);
        result = executeCheckNoFailure(rewrite);
        assertThat(result, is(notNullValue()));
        assertThat(result.get(RESPONSE_HEADERS, OPERATION_REQUIRES_RELOAD).isDefined(), is(false));
        assertThat(result.get(RESPONSE_HEADERS, PROCESS_STATE).isDefined(), is(false));

        rewrite = createOperation(WRITE_ATTRIBUTE_OPERATION, "profile", "profileA", "subsystem", "subsystem1");
        rewrite.get(NAME).set(LONG_ATT_NAME);
        rewrite.get(VALUE).set(1000L);
        result = executeCheckNoFailure(rewrite);
        assertThat(result, is(notNullValue()));
        assertThat(result.get(RESPONSE_HEADERS, OPERATION_REQUIRES_RELOAD).isDefined(), is(false));
        assertThat(result.get(RESPONSE_HEADERS, PROCESS_STATE).isDefined(), is(false));
    }

    @Test
    public void testWriteReloadAttributeOnExpression() throws Exception {
        //Just make sure it works as expected for an existant resource
        ModelNode operation = createOperation(READ_ATTRIBUTE_OPERATION, "profile", "profileB", "subsystem", "subsystem1");
        operation.get(NAME).set(BOOLEAN_ATT_NAME);
        ModelNode result = executeForResult(operation);
        assertThat(result, is(notNullValue()));
        assertThat(result.getType(), is(ModelType.EXPRESSION));
        assertThat(result.asExpression().resolveString(), is("true"));

        ModelNode rewrite = createOperation(WRITE_ATTRIBUTE_OPERATION, "profile", "profileB", "subsystem", "subsystem1");
        rewrite.get(NAME).set(BOOLEAN_ATT_NAME);
        rewrite.get(VALUE).set(true);
        result = executeCheckNoFailure(rewrite);
        assertThat(result, is(notNullValue()));
        assertThat(result.get(RESPONSE_HEADERS, OPERATION_REQUIRES_RELOAD).isDefined(), is(true));
        assertThat(result.get(RESPONSE_HEADERS, PROCESS_STATE).isDefined(), is(true));
    }

    @Test
    public void testWriteReloadAttributeExpressionOverExpression() throws Exception {
        //Just make sure it works as expected for an existant resource
        ModelNode operation = createOperation(READ_ATTRIBUTE_OPERATION, "profile", "profileB", "subsystem", "subsystem1");
        operation.get(NAME).set(BOOLEAN_ATT_NAME);
        ModelNode result = executeForResult(operation);
        assertThat(result, is(notNullValue()));
        assertThat(result.getType(), is(ModelType.EXPRESSION));
        assertThat(result.asExpression().resolveString(), is("true"));

        ModelNode rewrite = createOperation(WRITE_ATTRIBUTE_OPERATION, "profile", "profileB", "subsystem", "subsystem1");
        rewrite.get(NAME).set(BOOLEAN_ATT_NAME);
        rewrite.get(VALUE).set("${boolean-value}");
        result = executeCheckNoFailure(rewrite);
        assertThat(result, is(notNullValue()));
        assertThat(result.get(RESPONSE_HEADERS, OPERATION_REQUIRES_RELOAD).isDefined(), is(true));
        assertThat(result.get(RESPONSE_HEADERS, PROCESS_STATE).isDefined(), is(true));
    }

    @Test
    public void testWriteReloadAttributeValueOverExpression() throws Exception {
        //Just make sure it works as expected for an existant resource
        ModelNode operation = createOperation(READ_ATTRIBUTE_OPERATION, "profile", "profileB", "subsystem", "subsystem1");
        operation.get(NAME).set(BOOLEAN_ATT_NAME);
        ModelNode result = executeForResult(operation);
        assertThat(result, is(notNullValue()));
        assertThat(result.getType(), is(ModelType.EXPRESSION));
        assertThat(result.asExpression().resolveString(), is("true"));

        ModelNode rewrite = createOperation(WRITE_ATTRIBUTE_OPERATION, "profile", "profileB", "subsystem", "subsystem1");
        rewrite.get(NAME).set(BOOLEAN_ATT_NAME);
        rewrite.get(VALUE).set(true);
        result = executeCheckNoFailure(rewrite);
        assertThat(result, is(notNullValue()));
        assertThat(result.get(RESPONSE_HEADERS, OPERATION_REQUIRES_RELOAD).isDefined(), is(true));
        assertThat(result.get(RESPONSE_HEADERS, PROCESS_STATE).isDefined(), is(true));
    }

    @Test
    public void testWriteReloadBooleanAttributeExpressionOverValue() throws Exception {
        //Just make sure it works as expected for an existant resource
        ModelNode operation = createOperation(READ_ATTRIBUTE_OPERATION, "profile", "profilType", "subsystem", "subsystem1");
        operation.get(NAME).set(BOOLEAN_ATT_NAME);
        ModelNode result = executeForResult(operation);
        assertThat(result, is(notNullValue()));
        assertThat(result.getType(), is(ModelType.BOOLEAN));
        assertThat(result.asBoolean(), is(true));

        ModelNode rewrite = createOperation(WRITE_ATTRIBUTE_OPERATION, "profile", "profilType", "subsystem", "subsystem1");
        rewrite.get(NAME).set(BOOLEAN_ATT_NAME);
        rewrite.get(VALUE).set("${boolean-value}");
        result = executeCheckNoFailure(rewrite);
        assertThat(result, is(notNullValue()));
        assertThat(result.get(RESPONSE_HEADERS, OPERATION_REQUIRES_RELOAD).isDefined(), is(false));
        assertThat(result.get(RESPONSE_HEADERS, PROCESS_STATE).isDefined(), is(false));

        operation = createOperation(READ_ATTRIBUTE_OPERATION, "profile", "profilType", "subsystem", "subsystem1");
        operation.get(NAME).set(BOOLEAN_ATT_NAME);
        result = executeForResult(operation);
        assertThat(result, is(notNullValue()));
        assertThat(result.getType(), is(ModelType.EXPRESSION));
        assertThat(result.asExpression().resolveString(), is("true"));
    }

    @Test
    public void testWriteReloadLongAttributeExpressionOverValue() throws Exception {
        ModelNode operation = createOperation(READ_ATTRIBUTE_OPERATION, "profile", "profilType", "subsystem", "subsystem1");
        operation.get(NAME).set(LONG_ATT_NAME);
        ModelNode result = executeForResult(operation);
        assertThat(result, is(notNullValue()));
        assertThat(result.getType(), is(ModelType.LONG));
        assertThat(result.asLong(), is(1000L));

        ModelNode rewrite = createOperation(WRITE_ATTRIBUTE_OPERATION, "profile", "profilType", "subsystem", "subsystem1");
        rewrite.get(NAME).set(LONG_ATT_NAME);
        rewrite.get(VALUE).set("${long-value}");
        result = executeCheckNoFailure(rewrite);
        assertThat(result, is(notNullValue()));
        assertThat(result.get(RESPONSE_HEADERS, OPERATION_REQUIRES_RELOAD).isDefined(), is(false));
        assertThat(result.get(RESPONSE_HEADERS, PROCESS_STATE).isDefined(), is(false));

        operation = createOperation(READ_ATTRIBUTE_OPERATION, "profile", "profilType", "subsystem", "subsystem1");
        operation.get(NAME).set(LONG_ATT_NAME);
        result = executeForResult(operation);
        assertThat(result, is(notNullValue()));
        assertThat(result.getType(), is(ModelType.EXPRESSION));
        assertThat(result.asExpression().resolveString(), is("1000"));
    }

    @Test
    public void testWriteReloadDoubleAttributeExpressionOverValue() throws Exception {
        ModelNode operation = createOperation(READ_ATTRIBUTE_OPERATION, "profile", "profilType", "subsystem", "subsystem1");
        operation.get(NAME).set(DOUBLE_ATT_NAME);
        ModelNode result = executeForResult(operation);
        assertThat(result, is(notNullValue()));
        assertThat(result.getType(), is(ModelType.DOUBLE));
        assertThat(result.asDouble(), is(1.0D));

        ModelNode rewrite = createOperation(WRITE_ATTRIBUTE_OPERATION, "profile", "profilType", "subsystem", "subsystem1");
        rewrite.get(NAME).set(DOUBLE_ATT_NAME);
        rewrite.get(VALUE).set("${double-value}");
        result = executeCheckNoFailure(rewrite);
        assertThat(result, is(notNullValue()));
        assertThat(result.get(RESPONSE_HEADERS, OPERATION_REQUIRES_RELOAD).isDefined(), is(false));
        assertThat(result.get(RESPONSE_HEADERS, PROCESS_STATE).isDefined(), is(false));

        operation = createOperation(READ_ATTRIBUTE_OPERATION, "profile", "profilType", "subsystem", "subsystem1");
        operation.get(NAME).set(DOUBLE_ATT_NAME);
        result = executeForResult(operation);
        assertThat(result, is(notNullValue()));
        assertThat(result.getType(), is(ModelType.EXPRESSION));
        assertThat(result.asExpression().resolveString(), is("1.0"));
    }

    @Test
    public void testWriteReloadBytesAttributeExpressionOverValue() throws Exception {
        ModelNode operation = createOperation(READ_ATTRIBUTE_OPERATION, "profile", "profilType", "subsystem", "subsystem1");
        operation.get(NAME).set(STRING_ATT_NAME);
        ModelNode result = executeForResult(operation);
        assertThat(result, is(notNullValue()));
        assertThat(result.getType(), is(ModelType.STRING));
        assertThat(new String(result.asBytes(), UTF_8), is("wildfly"));

        ModelNode rewrite = createOperation(WRITE_ATTRIBUTE_OPERATION, "profile", "profilType", "subsystem", "subsystem1");
        rewrite.get(NAME).set(STRING_ATT_NAME);
        rewrite.get(VALUE).set("${bytes-value}");
        result = executeCheckNoFailure(rewrite);
        assertThat(result, is(notNullValue()));
        assertThat(result.get(RESPONSE_HEADERS, OPERATION_REQUIRES_RELOAD).isDefined(), is(false));
        assertThat(result.get(RESPONSE_HEADERS, PROCESS_STATE).isDefined(), is(false));

        operation = createOperation(READ_ATTRIBUTE_OPERATION, "profile", "profilType", "subsystem", "subsystem1");
        operation.get(NAME).set(STRING_ATT_NAME);
        result = executeForResult(operation);
        assertThat(result, is(notNullValue()));
        assertThat(result.getType(), is(ModelType.EXPRESSION));
        assertThat(result.asExpression().resolveString(), is("wildfly"));
    }

    @Test
    public void testWriteReloadIntAttributeExpressionOverValue() throws Exception {
        ModelNode operation = createOperation(READ_ATTRIBUTE_OPERATION, "profile", "profilType", "subsystem", "subsystem1");
        operation.get(NAME).set(INT_ATT_NAME);
        ModelNode result = executeForResult(operation);
        assertThat(result, is(notNullValue()));
        assertThat(result.getType(), is(ModelType.INT));
        assertThat(result.asInt(), is(100));

        ModelNode rewrite = createOperation(WRITE_ATTRIBUTE_OPERATION, "profile", "profilType", "subsystem", "subsystem1");
        rewrite.get(NAME).set(INT_ATT_NAME);
        rewrite.get(VALUE).set("${int-value}");
        result = executeCheckNoFailure(rewrite);
        assertThat(result, is(notNullValue()));
        assertThat(result.get(RESPONSE_HEADERS, OPERATION_REQUIRES_RELOAD).isDefined(), is(false));
        assertThat(result.get(RESPONSE_HEADERS, PROCESS_STATE).isDefined(), is(false));

        operation = createOperation(READ_ATTRIBUTE_OPERATION, "profile", "profilType", "subsystem", "subsystem1");
        operation.get(NAME).set(INT_ATT_NAME);
        result = executeForResult(operation);
        assertThat(result, is(notNullValue()));
        assertThat(result.getType(), is(ModelType.EXPRESSION));
        assertThat(result.asExpression().resolveString(), is("100"));
    }
    
    @Test
    public void testWriteReloadBigintAttributeExpressionOverValue() throws Exception {
        ModelNode operation = createOperation(READ_ATTRIBUTE_OPERATION, "profile", "profilType", "subsystem", "subsystem1");
        operation.get(NAME).set(BIGINT_ATT_NAME);
        ModelNode result = executeForResult(operation);
        assertThat(result, is(notNullValue()));
        assertThat(result.getType(), is(ModelType.BIG_INTEGER));
        assertThat(result.asBigInteger(), is(new BigInteger("100")));

        ModelNode rewrite = createOperation(WRITE_ATTRIBUTE_OPERATION, "profile", "profilType", "subsystem", "subsystem1");
        rewrite.get(NAME).set(BIGINT_ATT_NAME);
        rewrite.get(VALUE).set("${bigint-value}");
        result = executeCheckNoFailure(rewrite);
        assertThat(result, is(notNullValue()));
        assertThat(result.get(RESPONSE_HEADERS, OPERATION_REQUIRES_RELOAD).isDefined(), is(false));
        assertThat(result.get(RESPONSE_HEADERS, PROCESS_STATE).isDefined(), is(false));

        operation = createOperation(READ_ATTRIBUTE_OPERATION, "profile", "profilType", "subsystem", "subsystem1");
        operation.get(NAME).set(BIGINT_ATT_NAME);
        result = executeForResult(operation);
        assertThat(result, is(notNullValue()));
        assertThat(result.getType(), is(ModelType.EXPRESSION));
        assertThat(result.asExpression().resolveString(), is("100"));
    }

    @Test
    public void testWriteReloadBigdecAttributeExpressionOverValue() throws Exception {
        ModelNode operation = createOperation(READ_ATTRIBUTE_OPERATION, "profile", "profilType", "subsystem", "subsystem1");
        operation.get(NAME).set(BIGDEC_ATT_NAME);
        ModelNode result = executeForResult(operation);
        assertThat(result, is(notNullValue()));
        assertThat(result.getType(), is(ModelType.BIG_DECIMAL));
        assertThat(result.asBigDecimal(), is(new BigDecimal("10.0")));

        ModelNode rewrite = createOperation(WRITE_ATTRIBUTE_OPERATION, "profile", "profilType", "subsystem", "subsystem1");
        rewrite.get(NAME).set(BIGDEC_ATT_NAME);
        rewrite.get(VALUE).set("${bigdec-value}");
        result = executeCheckNoFailure(rewrite);
        assertThat(result, is(notNullValue()));
        assertThat(result.get(RESPONSE_HEADERS, OPERATION_REQUIRES_RELOAD).isDefined(), is(false));
        assertThat(result.get(RESPONSE_HEADERS, PROCESS_STATE).isDefined(), is(false));

        operation = createOperation(READ_ATTRIBUTE_OPERATION, "profile", "profilType", "subsystem", "subsystem1");
        operation.get(NAME).set(BIGDEC_ATT_NAME);
        result = executeForResult(operation);
        assertThat(result, is(notNullValue()));
        assertThat(result.getType(), is(ModelType.EXPRESSION));
        assertThat(result.asExpression().resolveString(), is("10.0"));
    }
}
