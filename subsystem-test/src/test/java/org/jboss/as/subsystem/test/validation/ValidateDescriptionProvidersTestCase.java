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
package org.jboss.as.subsystem.test.validation;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ACCESS_TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILDREN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HEAD_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MAX;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MAX_LENGTH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MIN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MIN_LENGTH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MIN_OCCURS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MODEL_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAMESPACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REPLY_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUIRED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STORAGE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TAIL_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE_TYPE;

import java.util.List;

import junit.framework.Assert;

import org.jboss.as.controller.registry.AttributeAccess.AccessType;
import org.jboss.as.controller.registry.AttributeAccess.Storage;
import org.jboss.as.subsystem.test.ModelDescriptionValidator;
import org.jboss.as.subsystem.test.ModelDescriptionValidator.ValidationConfiguration;
import org.jboss.as.subsystem.test.ModelDescriptionValidator.ValidationFailure;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.junit.Test;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ValidateDescriptionProvidersTestCase {

    static final String REPLY = "reply-properties";

    static final String ROOT_ATTR = "root-attr";
    static final String ROOT_OP = "root-op";
    static final String PARAM = "param";
    static final String CHILD_TYPE = "child-type";
    static final String CHILD_NAME = "child-name";
    static final String CHILD_ATTR = "child-attr";
    static final String CHILD_OP = "child-op";

    static final ModelNode ROOT_ADDRESS = new ModelNode().setEmptyList();
    static final ModelNode CHILD_ADDRESS = new ModelNode().add(CHILD_TYPE, CHILD_NAME);

    @Test
    public void testSuccessfulCoreModelSimpleType() {
        ModelNode description = createSubsystemSkeleton(ModelType.STRING, (ModelNode)null);
        List<ValidationFailure> errors = validate(description, null);
        Assert.assertEquals(errors.toString(), 0, errors.size());
    }

    @Test
    public void testSuccessfulCoreModelListWithSimpleValueType() {
        ModelNode description = createSubsystemSkeleton(ModelType.LIST, ModelType.INT);
        List<ValidationFailure> errors = validate(description, null);
        Assert.assertEquals(errors.toString(), 0, errors.size());
    }

    @Test
    public void testSuccessfulCoreModelObjectWithSimpleValueType() {
        ModelNode description = createSubsystemSkeleton(ModelType.OBJECT, ModelType.BOOLEAN);
        List<ValidationFailure> errors = validate(description, null);
        Assert.assertEquals(errors.toString(), 0, errors.size());
    }

    @Test
    public void testSuccessfulCoreModelListWithEmptyReplyPropertiesAndRequestProperties() {
        ModelNode description = createSubsystemSkeleton(ModelType.LIST, ModelType.INT);
        description.get(OPERATIONS, ROOT_OP, REQUEST_PROPERTIES).setEmptyObject();
        description.get(OPERATIONS, ROOT_OP, REPLY_PROPERTIES).setEmptyObject();
        description.get(CHILDREN, CHILD_TYPE, MODEL_DESCRIPTION, CHILD_NAME, OPERATIONS, CHILD_OP, REQUEST_PROPERTIES).setEmptyObject();
        description.get(CHILDREN, CHILD_TYPE, MODEL_DESCRIPTION, CHILD_NAME, OPERATIONS, CHILD_OP, REPLY_PROPERTIES).setEmptyObject();

        List<ValidationFailure> errors = validate(description, null);
        Assert.assertEquals(errors.toString(), 0, errors.size());
    }

    @Test
    public void testSuccessfulCoreModelListWithUndefinedReplyPropertiesAndRequestProperties() {
        ModelNode description = createSubsystemSkeleton(ModelType.LIST, ModelType.INT);
        description.get(OPERATIONS, ROOT_OP).remove(REQUEST_PROPERTIES);
        description.get(OPERATIONS, ROOT_OP).remove(REPLY_PROPERTIES);
        description.get(CHILDREN, CHILD_TYPE, MODEL_DESCRIPTION, CHILD_NAME, OPERATIONS, CHILD_OP).remove(REQUEST_PROPERTIES);
        description.get(CHILDREN, CHILD_TYPE, MODEL_DESCRIPTION, CHILD_NAME, OPERATIONS, CHILD_OP).remove(REPLY_PROPERTIES);

        List<ValidationFailure> errors = validate(description, null);
        Assert.assertEquals(errors.toString(), 0, errors.size());
    }

    @Test
    public void testSuccessfulCoreModelObjectWithComplexValueType() {
        ModelNode complex = new ModelNode();
        complex.get("first", TYPE).set(ModelType.LONG);
        complex.get("second", TYPE).set(ModelType.BOOLEAN);
        complex.get("third", TYPE).set(ModelType.OBJECT);
        complex.get("third", VALUE_TYPE).set(ModelType.INT      );
        ModelNode description = createSubsystemSkeleton(ModelType.OBJECT, complex);
        List<ValidationFailure> errors = validate(description, null);
        Assert.assertEquals(errors.toString(), 0, errors.size());
    }

    @Test
    public void testSuccessfulCoreModelSimpleTypeMinMax() {
        ModelNode description = createSubsystemSkeleton(ModelType.INT, (ModelNode)null);
        description.get(ATTRIBUTES, ROOT_ATTR, MIN).set(5);
        description.get(ATTRIBUTES, ROOT_ATTR, MAX).set(5);
        description.get(OPERATIONS, ROOT_OP, REQUEST_PROPERTIES, PARAM, MIN).set(5);
        description.get(OPERATIONS, ROOT_OP, REQUEST_PROPERTIES, PARAM, MAX).set(5);
        description.get(OPERATIONS, ROOT_OP, REPLY_PROPERTIES, MAX).set(5);
        List<ValidationFailure> errors = validate(description, null);
        Assert.assertEquals(errors.toString(), 0, errors.size());
    }

    @Test
    public void testObjectNoValueType() {
        ModelNode description = createSubsystemSkeleton(ModelType.OBJECT, (ModelNode)null);
        List<ValidationFailure> errors = validate(description, null);
        Assert.assertEquals(errors.toString(), 6, errors.size());

        assertAttributeFailure(errors.get(0), ROOT_ADDRESS, ROOT_ATTR);
        assertOperationParameterFailure(errors.get(1), ROOT_ADDRESS, ROOT_OP, PARAM);
        assertOperationParameterFailure(errors.get(2), ROOT_ADDRESS, ROOT_OP, REPLY);
        assertAttributeFailure(errors.get(3), CHILD_ADDRESS, CHILD_ATTR);
        assertOperationParameterFailure(errors.get(4), CHILD_ADDRESS, CHILD_OP, PARAM);
        assertOperationParameterFailure(errors.get(5), CHILD_ADDRESS, CHILD_OP, REPLY);
    }

    @Test
    public void testBadOperationNames() {
        ModelNode description = createSubsystemSkeleton(ModelType.INT, (ModelNode)null);
        description.get(OPERATIONS, ROOT_OP).remove(OPERATION_NAME);
        description.get(CHILDREN, CHILD_TYPE, MODEL_DESCRIPTION, CHILD_NAME, OPERATIONS, CHILD_OP, OPERATION_NAME).set("Wrong");
        List<ValidationFailure> errors = validate(description, null);
        Assert.assertEquals(2, errors.size());
        assertOperationFailure(errors.get(0), ROOT_ADDRESS, ROOT_OP);
        assertOperationFailure(errors.get(1), CHILD_ADDRESS, CHILD_OP);
    }

    @Test
    public void testModelWithUndefinedElements() {
        ModelNode description = createSubsystemSkeleton(ModelType.LIST, ModelType.INT);
        description.get(NAMESPACE).set(new ModelNode());
        description.get(ATTRIBUTES, ROOT_ATTR, MIN);
        description.get(OPERATIONS, ROOT_OP, REQUEST_PROPERTIES, PARAM, MIN);
        description.get(CHILDREN, CHILD_TYPE, MIN_OCCURS);
        description.get(CHILDREN, CHILD_TYPE, MODEL_DESCRIPTION, CHILD_NAME, ATTRIBUTES, CHILD_ATTR, MIN);
        description.get(CHILDREN, CHILD_TYPE, MODEL_DESCRIPTION, CHILD_NAME, OPERATIONS, CHILD_OP, REQUEST_PROPERTIES, PARAM, MIN);
        description.get(CHILDREN, CHILD_TYPE, MODEL_DESCRIPTION, "EmptyChild");


        List<ValidationFailure> errors = validate(description, null);
        Assert.assertEquals(errors.toString(), 7, errors.size());
        assertFailure(errors.get(0), ROOT_ADDRESS);
        assertAttributeFailure(errors.get(1), ROOT_ADDRESS, ROOT_ATTR);
        assertOperationParameterFailure(errors.get(2), ROOT_ADDRESS, ROOT_OP, PARAM);
        assertFailure(errors.get(3), ROOT_ADDRESS);
        assertAttributeFailure(errors.get(4), CHILD_ADDRESS, CHILD_ATTR);
        assertOperationParameterFailure(errors.get(5), CHILD_ADDRESS, CHILD_OP, PARAM);
        assertFailure(errors.get(6), ROOT_ADDRESS.clone().add(CHILD_TYPE, "EmptyChild"));
    }

    @Test
    public void testMissingDescriptions() {
        ModelNode description = createSubsystemSkeleton(ModelType.INT, (ModelNode)null);

        description.remove(DESCRIPTION);
        description.get(ATTRIBUTES, ROOT_ATTR).remove(DESCRIPTION);
        description.get(OPERATIONS, ROOT_OP).remove(DESCRIPTION);
        description.get(OPERATIONS, ROOT_OP, REQUEST_PROPERTIES, PARAM).remove(DESCRIPTION);
        description.get(CHILDREN, CHILD_TYPE).remove(DESCRIPTION);
        description.get(CHILDREN, CHILD_TYPE, MODEL_DESCRIPTION, CHILD_NAME).remove(DESCRIPTION);
        description.get(CHILDREN, CHILD_TYPE, MODEL_DESCRIPTION, CHILD_NAME, ATTRIBUTES, CHILD_ATTR).remove(DESCRIPTION);
        description.get(CHILDREN, CHILD_TYPE, MODEL_DESCRIPTION, CHILD_NAME, OPERATIONS, CHILD_OP).remove(DESCRIPTION);
        description.get(CHILDREN, CHILD_TYPE, MODEL_DESCRIPTION, CHILD_NAME, OPERATIONS, CHILD_OP, REQUEST_PROPERTIES, PARAM).remove(DESCRIPTION);

        List<ValidationFailure> errors = validate(description, null);
        Assert.assertEquals(9, errors.size());
        assertFailure(errors.get(0), ROOT_ADDRESS);
        assertAttributeFailure(errors.get(1), ROOT_ADDRESS, ROOT_ATTR);
        assertOperationFailure(errors.get(2), ROOT_ADDRESS, ROOT_OP);
        assertOperationParameterFailure(errors.get(3), ROOT_ADDRESS, ROOT_OP, PARAM);
        assertFailure(errors.get(4), ROOT_ADDRESS);
        assertFailure(errors.get(5), CHILD_ADDRESS);
        assertAttributeFailure(errors.get(6), CHILD_ADDRESS, CHILD_ATTR);
        assertOperationFailure(errors.get(7), CHILD_ADDRESS, CHILD_OP);
        assertOperationParameterFailure(errors.get(8), CHILD_ADDRESS, CHILD_OP, PARAM);
    }

    @Test
    public void testIntMinMax() {
        ModelNode description = createSubsystemSkeleton(ModelType.INT, (ModelNode)null);
        description.get(ATTRIBUTES, ROOT_ATTR, MIN).set(5);
        description.get(ATTRIBUTES, ROOT_ATTR, MAX).set(5);
        description.get(CHILDREN, CHILD_TYPE, MODEL_DESCRIPTION, CHILD_NAME, OPERATIONS, CHILD_OP, REQUEST_PROPERTIES, PARAM, MIN).set(5);
        description.get(CHILDREN, CHILD_TYPE, MODEL_DESCRIPTION, CHILD_NAME, OPERATIONS, CHILD_OP, REQUEST_PROPERTIES, PARAM, MAX).set(5);
        description.get(CHILDREN, CHILD_TYPE, MODEL_DESCRIPTION, CHILD_NAME, OPERATIONS, CHILD_OP, REPLY_PROPERTIES, MAX).set(10);

        List<ValidationFailure> errors = validate(description, null);
        Assert.assertEquals(0, errors.size());
    }

    @Test
    public void testStringMinMax() {
        ModelNode description = createSubsystemSkeleton(ModelType.STRING, (ModelNode)null);
        description.get(ATTRIBUTES, ROOT_ATTR, MIN).set(5);
        description.get(ATTRIBUTES, ROOT_ATTR, MAX).set(5);
        description.get(CHILDREN, CHILD_TYPE, MODEL_DESCRIPTION, CHILD_NAME, OPERATIONS, CHILD_OP, REQUEST_PROPERTIES, PARAM, MIN).set(5);
        description.get(CHILDREN, CHILD_TYPE, MODEL_DESCRIPTION, CHILD_NAME, OPERATIONS, CHILD_OP, REQUEST_PROPERTIES, PARAM, MAX).set(5);
        description.get(CHILDREN, CHILD_TYPE, MODEL_DESCRIPTION, CHILD_NAME, OPERATIONS, CHILD_OP, REPLY_PROPERTIES, MAX).set(10);

        List<ValidationFailure> errors = validate(description, null);
        Assert.assertEquals(5, errors.size());

        assertAttributeFailure(errors.get(0), ROOT_ADDRESS, ROOT_ATTR);
        assertAttributeFailure(errors.get(1), ROOT_ADDRESS, ROOT_ATTR);
        assertOperationParameterFailure(errors.get(2), CHILD_ADDRESS, CHILD_OP, PARAM);
        assertOperationParameterFailure(errors.get(3), CHILD_ADDRESS, CHILD_OP, PARAM);
        assertOperationParameterFailure(errors.get(4), CHILD_ADDRESS, CHILD_OP, REPLY);
    }

    @Test
    public void testLongBadMinMax() {
        ModelNode description = createSubsystemSkeleton(ModelType.LONG, (ModelNode)null);
        description.get(OPERATIONS, ROOT_OP, REQUEST_PROPERTIES, PARAM, MIN).set("Hello");
        description.get(OPERATIONS, ROOT_OP, REQUEST_PROPERTIES, PARAM, MAX).set("Hello");
        description.get(OPERATIONS, ROOT_OP, REPLY_PROPERTIES, MIN).set("Hello");
        description.get(CHILDREN, CHILD_TYPE, MODEL_DESCRIPTION, CHILD_NAME, ATTRIBUTES, CHILD_ATTR, MIN).set("Hello");
        description.get(CHILDREN, CHILD_TYPE, MODEL_DESCRIPTION, CHILD_NAME, ATTRIBUTES, CHILD_ATTR, MAX).set("Hello");

        List<ValidationFailure> errors = validate(description, null);
        Assert.assertEquals(errors.toString(), 5, errors.size());

        assertOperationParameterFailure(errors.get(0), ROOT_ADDRESS, ROOT_OP, PARAM);
        assertOperationParameterFailure(errors.get(1), ROOT_ADDRESS, ROOT_OP, PARAM);
        assertOperationParameterFailure(errors.get(2), ROOT_ADDRESS, ROOT_OP, REPLY);
        assertAttributeFailure(errors.get(3), CHILD_ADDRESS, CHILD_ATTR);
        assertAttributeFailure(errors.get(4), CHILD_ADDRESS, CHILD_ATTR);
    }

    @Test
    public void testStringMinMaxLength() {
        ModelNode description = createSubsystemSkeleton(ModelType.STRING, (ModelNode)null);
        description.get(ATTRIBUTES, ROOT_ATTR, MIN_LENGTH).set(5);
        description.get(ATTRIBUTES, ROOT_ATTR, MAX_LENGTH).set(5);
        description.get(CHILDREN, CHILD_TYPE, MODEL_DESCRIPTION, CHILD_NAME, OPERATIONS, CHILD_OP, REQUEST_PROPERTIES, PARAM, MIN_LENGTH).set(5);
        description.get(CHILDREN, CHILD_TYPE, MODEL_DESCRIPTION, CHILD_NAME, OPERATIONS, CHILD_OP, REQUEST_PROPERTIES, PARAM, MAX_LENGTH).set(5);
        description.get(CHILDREN, CHILD_TYPE, MODEL_DESCRIPTION, CHILD_NAME, OPERATIONS, CHILD_OP, REPLY_PROPERTIES, MAX_LENGTH).set(10);

        List<ValidationFailure> errors = validate(description, null);
        Assert.assertEquals(0, errors.size());
    }

    @Test
    public void testListMinMaxLength() {
        ModelNode description = createSubsystemSkeleton(ModelType.LIST, ModelType.INT);
        description.get(ATTRIBUTES, ROOT_ATTR, MIN_LENGTH).set(5);
        description.get(ATTRIBUTES, ROOT_ATTR, MAX_LENGTH).set(5);
        description.get(CHILDREN, CHILD_TYPE, MODEL_DESCRIPTION, CHILD_NAME, OPERATIONS, CHILD_OP, REQUEST_PROPERTIES, PARAM, MIN_LENGTH).set(5);
        description.get(CHILDREN, CHILD_TYPE, MODEL_DESCRIPTION, CHILD_NAME, OPERATIONS, CHILD_OP, REQUEST_PROPERTIES, PARAM, MAX_LENGTH).set(5);
        description.get(CHILDREN, CHILD_TYPE, MODEL_DESCRIPTION, CHILD_NAME, OPERATIONS, CHILD_OP, REPLY_PROPERTIES, MAX_LENGTH).set(10);

        List<ValidationFailure> errors = validate(description, null);
        Assert.assertEquals(0, errors.size());
    }

    @Test
    public void testIntMinMaxLength() {
        ModelNode description = createSubsystemSkeleton(ModelType.INT, (ModelType)null);
        description.get(ATTRIBUTES, ROOT_ATTR, MIN_LENGTH).set(5);
        description.get(ATTRIBUTES, ROOT_ATTR, MAX_LENGTH).set(5);
        description.get(CHILDREN, CHILD_TYPE, MODEL_DESCRIPTION, CHILD_NAME, OPERATIONS, CHILD_OP, REQUEST_PROPERTIES, PARAM, MIN_LENGTH).set(5);
        description.get(CHILDREN, CHILD_TYPE, MODEL_DESCRIPTION, CHILD_NAME, OPERATIONS, CHILD_OP, REQUEST_PROPERTIES, PARAM, MAX_LENGTH).set(5);
        description.get(CHILDREN, CHILD_TYPE, MODEL_DESCRIPTION, CHILD_NAME, OPERATIONS, CHILD_OP, REPLY_PROPERTIES, MAX_LENGTH).set(10);

        List<ValidationFailure> errors = validate(description, null);
        Assert.assertEquals(5, errors.size());

        assertAttributeFailure(errors.get(0), ROOT_ADDRESS, ROOT_ATTR);
        assertAttributeFailure(errors.get(1), ROOT_ADDRESS, ROOT_ATTR);
        assertOperationParameterFailure(errors.get(2), CHILD_ADDRESS, CHILD_OP, PARAM);
        assertOperationParameterFailure(errors.get(3), CHILD_ADDRESS, CHILD_OP, PARAM);
        assertOperationParameterFailure(errors.get(4), CHILD_ADDRESS, CHILD_OP, REPLY);
    }

    @Test
    public void testStringBadMinMaxLength() {
        ModelNode description = createSubsystemSkeleton(ModelType.STRING, (ModelType)null);
        description.get(ATTRIBUTES, ROOT_ATTR, MIN_LENGTH).set("Bad");
        description.get(ATTRIBUTES, ROOT_ATTR, MAX_LENGTH).set("Bad");
        description.get(CHILDREN, CHILD_TYPE, MODEL_DESCRIPTION, CHILD_NAME, OPERATIONS, CHILD_OP, REQUEST_PROPERTIES, PARAM, MIN_LENGTH).set("Bad");
        description.get(CHILDREN, CHILD_TYPE, MODEL_DESCRIPTION, CHILD_NAME, OPERATIONS, CHILD_OP, REQUEST_PROPERTIES, PARAM, MAX_LENGTH).set("Bad");
        description.get(CHILDREN, CHILD_TYPE, MODEL_DESCRIPTION, CHILD_NAME, OPERATIONS, CHILD_OP, REPLY_PROPERTIES, MAX_LENGTH).set("Bad");

        List<ValidationFailure> errors = validate(description, null);
        Assert.assertEquals(5, errors.size());

        assertAttributeFailure(errors.get(0), ROOT_ADDRESS, ROOT_ATTR);
        assertAttributeFailure(errors.get(1), ROOT_ADDRESS, ROOT_ATTR);
        assertOperationParameterFailure(errors.get(2), CHILD_ADDRESS, CHILD_OP, PARAM);
        assertOperationParameterFailure(errors.get(3), CHILD_ADDRESS, CHILD_OP, PARAM);
        assertOperationParameterFailure(errors.get(4), CHILD_ADDRESS, CHILD_OP, REPLY);
    }

    @Test
    public void testBadAttributeAndParameterArbitraryDescriptors() {
        ModelNode description = createSubsystemSkeleton(ModelType.STRING, (ModelType)null);
        description.get(ATTRIBUTES, ROOT_ATTR, "bad").set("Bad");
        description.get(ATTRIBUTES, ROOT_ATTR, "no good").set("Bad");
        description.get(CHILDREN, CHILD_TYPE, MODEL_DESCRIPTION, CHILD_NAME, OPERATIONS, CHILD_OP, REQUEST_PROPERTIES, PARAM, "bad").set("Bad");
        description.get(CHILDREN, CHILD_TYPE, MODEL_DESCRIPTION, CHILD_NAME, OPERATIONS, CHILD_OP, REQUEST_PROPERTIES, PARAM, "no good").set("Bad");
        description.get(CHILDREN, CHILD_TYPE, MODEL_DESCRIPTION, CHILD_NAME, OPERATIONS, CHILD_OP, REPLY_PROPERTIES, "bad").set("Bad");

        List<ValidationFailure> errors = validate(description, null);
        Assert.assertEquals(5, errors.size());

        assertAttributeFailure(errors.get(0), ROOT_ADDRESS, ROOT_ATTR);
        assertAttributeFailure(errors.get(1), ROOT_ADDRESS, ROOT_ATTR);
        assertOperationParameterFailure(errors.get(2), CHILD_ADDRESS, CHILD_OP, PARAM);
        assertOperationParameterFailure(errors.get(3), CHILD_ADDRESS, CHILD_OP, PARAM);
        assertOperationParameterFailure(errors.get(4), CHILD_ADDRESS, CHILD_OP, REPLY);
    }

    @Test
    public void testConfiguredAttributeAndParameterArbitraryDescriptors() {
        ModelNode description = createSubsystemSkeleton(ModelType.STRING, (ModelType)null);
        description.get(ATTRIBUTES, ROOT_ATTR, "added").set("ok");
        description.get(OPERATIONS, ROOT_OP, REQUEST_PROPERTIES, PARAM, "added").set("ok");
        description.get(OPERATIONS, ROOT_OP, REPLY_PROPERTIES, "added").set("ok");
        description.get(CHILDREN, CHILD_TYPE, MODEL_DESCRIPTION, CHILD_NAME, ATTRIBUTES, CHILD_ATTR, "added").set("ok");
        description.get(CHILDREN, CHILD_TYPE, MODEL_DESCRIPTION, CHILD_NAME, OPERATIONS, CHILD_OP, REQUEST_PROPERTIES, PARAM, "added").set("ok");
        description.get(CHILDREN, CHILD_TYPE, MODEL_DESCRIPTION, CHILD_NAME, OPERATIONS, CHILD_OP, REPLY_PROPERTIES, "added").set("ok");

        ValidationConfiguration arbitraryDescriptors = new ValidationConfiguration();
        arbitraryDescriptors.registerAttributeArbitraryDescriptor(ROOT_ADDRESS, ROOT_ATTR, "added", null);
        arbitraryDescriptors.registerArbitraryDescriptorForOperation(ROOT_ADDRESS, ROOT_OP, "added", null);
        arbitraryDescriptors.registerAttributeArbitraryDescriptor(CHILD_ADDRESS, CHILD_ATTR, "added", null);
        arbitraryDescriptors.registerArbitraryDescriptorForOperation(CHILD_ADDRESS, CHILD_OP, "added", null);

        List<ValidationFailure> errors = validate(description, arbitraryDescriptors);
        Assert.assertEquals(0, errors.size());
    }

    @Test
    public void testConfiguredOperationParameterArbitraryDescriptors() {
        ModelNode description = createSubsystemSkeleton(ModelType.STRING, (ModelType)null);
        description.get(OPERATIONS, ROOT_OP, REQUEST_PROPERTIES, PARAM, "added").set("ok");
        description.get(OPERATIONS, ROOT_OP, REPLY_PROPERTIES, "added").set("ok");
        description.get(CHILDREN, CHILD_TYPE, MODEL_DESCRIPTION, CHILD_NAME, OPERATIONS, CHILD_OP, REQUEST_PROPERTIES, PARAM, "added").set("ok");
        description.get(CHILDREN, CHILD_TYPE, MODEL_DESCRIPTION, CHILD_NAME, OPERATIONS, CHILD_OP, REPLY_PROPERTIES, "added").set("ok");

        ValidationConfiguration arbitraryDescriptors = new ValidationConfiguration();
        arbitraryDescriptors.registerArbitraryDescriptorForOperationParameter(ROOT_ADDRESS, ROOT_OP, PARAM, "added", null);
        arbitraryDescriptors.registerArbitraryDescriptorForOperationParameter(CHILD_ADDRESS, CHILD_OP, PARAM, "added", null);

        List<ValidationFailure> errors = validate(description, arbitraryDescriptors);
        Assert.assertEquals(2, errors.size());
        assertOperationParameterFailure(errors.get(0), ROOT_ADDRESS, ROOT_OP, REPLY_PROPERTIES);
        assertOperationParameterFailure(errors.get(1), CHILD_ADDRESS, CHILD_OP, REPLY_PROPERTIES);
    }

    @Test
    public void testConfiguredOperationReplyPropertiesArbitraryDescriptors() {
        ModelNode description = createSubsystemSkeleton(ModelType.STRING, (ModelType)null);
        description.get(OPERATIONS, ROOT_OP, REQUEST_PROPERTIES, PARAM, "added").set("ok");
        description.get(OPERATIONS, ROOT_OP, REPLY_PROPERTIES, "added").set("ok");
        description.get(CHILDREN, CHILD_TYPE, MODEL_DESCRIPTION, CHILD_NAME, OPERATIONS, CHILD_OP, REQUEST_PROPERTIES, PARAM, "added").set("ok");
        description.get(CHILDREN, CHILD_TYPE, MODEL_DESCRIPTION, CHILD_NAME, OPERATIONS, CHILD_OP, REPLY_PROPERTIES, "added").set("ok");

        ValidationConfiguration arbitraryDescriptors = new ValidationConfiguration();
        arbitraryDescriptors.registerArbitraryDescriptorForOperationReplyProperties(ROOT_ADDRESS, ROOT_OP, "added", null);
        arbitraryDescriptors.registerArbitraryDescriptorForOperationReplyProperties(CHILD_ADDRESS, CHILD_OP, "added", null);

        List<ValidationFailure> errors = validate(description, arbitraryDescriptors);
        Assert.assertEquals(2, errors.size());
        assertOperationParameterFailure(errors.get(0), ROOT_ADDRESS, ROOT_OP, PARAM);
        assertOperationParameterFailure(errors.get(1), CHILD_ADDRESS, CHILD_OP, PARAM);
    }

    @Test
    public void testConfiguredAllowNullValueTypeForList() {
        ModelNode description = createSubsystemSkeleton(ModelType.LIST, (ModelType)null);

        ValidationConfiguration arbitraryDescriptors = new ValidationConfiguration();
        arbitraryDescriptors.allowNullValueTypeForAttribute(ROOT_ADDRESS, ROOT_ATTR);
        arbitraryDescriptors.allowNullValueTypeForOperation(ROOT_ADDRESS, ROOT_OP);
        arbitraryDescriptors.allowNullValueTypeForAttribute(CHILD_ADDRESS, CHILD_ATTR);
        arbitraryDescriptors.allowNullValueTypeForOperation(CHILD_ADDRESS, CHILD_OP);

        List<ValidationFailure> errors = validate(description, arbitraryDescriptors);
        dumpErrors(errors);
        Assert.assertEquals(0, errors.size());
    }

    @Test
    public void testConfiguredAllowNullValueTypeForObject() {
        ModelNode description = createSubsystemSkeleton(ModelType.OBJECT, (ModelType)null);

        ValidationConfiguration arbitraryDescriptors = new ValidationConfiguration();
        arbitraryDescriptors.allowNullValueTypeForAttribute(ROOT_ADDRESS, ROOT_ATTR);
        arbitraryDescriptors.allowNullValueTypeForOperation(ROOT_ADDRESS, ROOT_OP);
        arbitraryDescriptors.allowNullValueTypeForAttribute(CHILD_ADDRESS, CHILD_ATTR);
        arbitraryDescriptors.allowNullValueTypeForOperation(CHILD_ADDRESS, CHILD_OP);

        List<ValidationFailure> errors = validate(description, arbitraryDescriptors);
        Assert.assertEquals(0, errors.size());
    }

    @Test
    public void testConfiguredAllowNullValueTypeForObjectParameter() {
        ModelNode description = createSubsystemSkeleton(ModelType.OBJECT, (ModelType)null);

        ValidationConfiguration arbitraryDescriptors = new ValidationConfiguration();
        arbitraryDescriptors.allowNullValueTypeForAttribute(ROOT_ADDRESS, ROOT_ATTR);
        arbitraryDescriptors.allowNullValueTypeForOperationParameter(ROOT_ADDRESS, ROOT_OP, PARAM);
        arbitraryDescriptors.allowNullValueTypeForAttribute(CHILD_ADDRESS, CHILD_ATTR);
        arbitraryDescriptors.allowNullValueTypeForOperationParameter(CHILD_ADDRESS, CHILD_OP, PARAM);

        List<ValidationFailure> errors = validate(description, arbitraryDescriptors);
        Assert.assertEquals(2, errors.size());
        assertOperationParameterFailure(errors.get(0), ROOT_ADDRESS, ROOT_OP, REPLY_PROPERTIES);
        assertOperationParameterFailure(errors.get(1), CHILD_ADDRESS, CHILD_OP, REPLY_PROPERTIES);
    }

    @Test
    public void testConfiguredAllowNullValueTypeForObjectReplyProperties() {
        ModelNode description = createSubsystemSkeleton(ModelType.OBJECT, (ModelType)null);

        ValidationConfiguration arbitraryDescriptors = new ValidationConfiguration();
        arbitraryDescriptors.allowNullValueTypeForAttribute(ROOT_ADDRESS, ROOT_ATTR);
        arbitraryDescriptors.allowNullValueTypeForOperationReplyProperties(ROOT_ADDRESS, ROOT_OP);
        arbitraryDescriptors.allowNullValueTypeForAttribute(CHILD_ADDRESS, CHILD_ATTR);
        arbitraryDescriptors.allowNullValueTypeForOperationReplyProperties(CHILD_ADDRESS, CHILD_OP);

        List<ValidationFailure> errors = validate(description, arbitraryDescriptors);
        Assert.assertEquals(2, errors.size());
        assertOperationParameterFailure(errors.get(0), ROOT_ADDRESS, ROOT_OP, PARAM);
        assertOperationParameterFailure(errors.get(1), CHILD_ADDRESS, CHILD_OP, PARAM);
    }

    @Test
    public void testBadKeyAtResourceLevel() {
        ModelNode description = createSubsystemSkeleton(ModelType.STRING, (ModelType)null);
        description.get("bad").set("no");

        List<ValidationFailure> errors = validate(description, null);
        Assert.assertEquals(1, errors.size());
        assertFailure(errors.get(0), ROOT_ADDRESS);
    }

    @Test
    public void testBadKeysAtResourceLevels() {
        ModelNode description = createSubsystemSkeleton(ModelType.STRING, (ModelType)null);
        description.get("bad").set("no");
        description.get(CHILDREN, CHILD_TYPE, MODEL_DESCRIPTION, CHILD_NAME, "bad").set("no");

        List<ValidationFailure> errors = validate(description, null);
        Assert.assertEquals(2, errors.size());
        assertFailure(errors.get(0), ROOT_ADDRESS);
        assertFailure(errors.get(1), CHILD_ADDRESS);
    }

    @Test
    public void testBadKeysAtChildRelationshipLevel() {
        ModelNode description = createSubsystemSkeleton(ModelType.STRING, (ModelType)null);
        description.get(CHILDREN, CHILD_TYPE, "bad").set("no");

        List<ValidationFailure> errors = validate(description, null);
        Assert.assertEquals(1, errors.size());
        assertFailure(errors.get(0), ROOT_ADDRESS);
    }

    @Test
    public void testBadKeysInOperations() {
        ModelNode description = createSubsystemSkeleton(ModelType.STRING, (ModelType)null);
        description.get(OPERATIONS, ROOT_OP, "bad").set("no");
        description.get(CHILDREN, CHILD_TYPE, MODEL_DESCRIPTION, CHILD_NAME, OPERATIONS, CHILD_OP, "bad").set("no");

        List<ValidationFailure> errors = validate(description, null);
        Assert.assertEquals(2, errors.size());
        assertOperationFailure(errors.get(0), ROOT_ADDRESS, ROOT_OP);
        assertOperationFailure(errors.get(1), CHILD_ADDRESS, CHILD_OP);
    }

    @Test
    public void testSuccessfulCoreModelObjectWithBadComplexValueType() {
        ModelNode complex = new ModelNode();
        complex.get("thing", TYPE).set(ModelType.OBJECT);
        complex.get("thing", VALUE_TYPE, "thingb", TYPE).set(ModelType.OBJECT);
        ModelNode description = createSubsystemSkeleton(ModelType.OBJECT, complex);
        List<ValidationFailure> errors = validate(description, null);
        assertAttributeFailure(errors.get(0), ROOT_ADDRESS, ROOT_ATTR);
        assertOperationParameterFailure(errors.get(1), ROOT_ADDRESS, ROOT_OP, PARAM);
        assertOperationParameterFailure(errors.get(2), ROOT_ADDRESS, ROOT_OP, REPLY);
        assertAttributeFailure(errors.get(3), CHILD_ADDRESS, CHILD_ATTR);
        assertOperationParameterFailure(errors.get(4), CHILD_ADDRESS, CHILD_OP, PARAM);
        assertOperationParameterFailure(errors.get(5), CHILD_ADDRESS, CHILD_OP, REPLY);
    }

    private ModelNode createSubsystemSkeleton(ModelType type, ModelType valueType) {
        ModelNode valueTypeNode = valueType == null ? null : new ModelNode().set(valueType);
        return createSubsystemSkeleton(type, valueTypeNode);
    }

    private ModelNode createSubsystemSkeleton(ModelType type, ModelNode valueType) {
        ModelNode description = new ModelNode();
        description.get(DESCRIPTION).set("An example model root");
        description.get(HEAD_COMMENT_ALLOWED).set(true);
        description.get(TAIL_COMMENT_ALLOWED).set(true);
        description.get(NAMESPACE).set(true);

        description.get(ATTRIBUTES, ROOT_ATTR, DESCRIPTION).set("The root attr");
        description.get(ATTRIBUTES, ROOT_ATTR, TYPE).set(type);
        if (valueType != null && valueType.isDefined()) {
            description.get(ATTRIBUTES, ROOT_ATTR, VALUE_TYPE).set(valueType);
        }
        description.get(ATTRIBUTES, ROOT_ATTR, REQUIRED).set(true);
        description.get(ATTRIBUTES, ROOT_ATTR, ACCESS_TYPE).set(AccessType.READ_ONLY.toString());
        description.get(ATTRIBUTES, ROOT_ATTR, STORAGE).set(Storage.CONFIGURATION.toString());

        description.get(OPERATIONS, ROOT_OP, OPERATION_NAME).set(ROOT_OP);
        description.get(OPERATIONS, ROOT_OP, DESCRIPTION).set("The root op");
        description.get(OPERATIONS, ROOT_OP, REQUEST_PROPERTIES, PARAM, DESCRIPTION).set("The param");
        description.get(OPERATIONS, ROOT_OP, REQUEST_PROPERTIES, PARAM, TYPE).set(type);
        if (valueType != null && valueType.isDefined()) {
            description.get(OPERATIONS, ROOT_OP, REQUEST_PROPERTIES, PARAM, VALUE_TYPE).set(valueType);
        }
        description.get(OPERATIONS, ROOT_OP, REQUEST_PROPERTIES, PARAM, REQUIRED).set(true);
        description.get(OPERATIONS, ROOT_OP, REPLY_PROPERTIES, TYPE).set(type);
        if (valueType != null && valueType.isDefined()) {
            description.get(OPERATIONS, ROOT_OP, REPLY_PROPERTIES, VALUE_TYPE).set(valueType);
        }

        description.get(CHILDREN, CHILD_TYPE, DESCRIPTION).set("The children");
        description.get(CHILDREN, CHILD_TYPE, MODEL_DESCRIPTION, CHILD_NAME, DESCRIPTION).set("A child");
        description.get(CHILDREN, CHILD_TYPE, MODEL_DESCRIPTION, CHILD_NAME, HEAD_COMMENT_ALLOWED).set(true);
        description.get(CHILDREN, CHILD_TYPE, MODEL_DESCRIPTION, CHILD_NAME, TAIL_COMMENT_ALLOWED).set(true);

        description.get(CHILDREN, CHILD_TYPE, MODEL_DESCRIPTION, CHILD_NAME, ATTRIBUTES, CHILD_ATTR, DESCRIPTION).set("The root attr");
        description.get(CHILDREN, CHILD_TYPE, MODEL_DESCRIPTION, CHILD_NAME, ATTRIBUTES, CHILD_ATTR, TYPE).set(type);
        if (valueType != null && valueType.isDefined()) {
            description.get(CHILDREN, CHILD_TYPE, MODEL_DESCRIPTION, CHILD_NAME, ATTRIBUTES, CHILD_ATTR, VALUE_TYPE).set(valueType);
        }
        description.get(CHILDREN, CHILD_TYPE, MODEL_DESCRIPTION, CHILD_NAME, ATTRIBUTES, CHILD_ATTR, REQUIRED).set(true);
        description.get(CHILDREN, CHILD_TYPE, MODEL_DESCRIPTION, CHILD_NAME, ATTRIBUTES, CHILD_ATTR, ACCESS_TYPE).set(AccessType.READ_ONLY.toString());
        description.get(CHILDREN, CHILD_TYPE, MODEL_DESCRIPTION, CHILD_NAME, ATTRIBUTES, CHILD_ATTR, STORAGE).set(Storage.CONFIGURATION.toString());

        description.get(CHILDREN, CHILD_TYPE, MODEL_DESCRIPTION, CHILD_NAME, OPERATIONS, CHILD_OP, OPERATION_NAME).set(CHILD_OP);
        description.get(CHILDREN, CHILD_TYPE, MODEL_DESCRIPTION, CHILD_NAME, OPERATIONS, CHILD_OP, DESCRIPTION).set("The child op");
        description.get(CHILDREN, CHILD_TYPE, MODEL_DESCRIPTION, CHILD_NAME, OPERATIONS, CHILD_OP, REQUEST_PROPERTIES, PARAM, DESCRIPTION).set("The param");
        description.get(CHILDREN, CHILD_TYPE, MODEL_DESCRIPTION, CHILD_NAME, OPERATIONS, CHILD_OP, REQUEST_PROPERTIES, PARAM, TYPE).set(type);
        if (valueType != null && valueType.isDefined()) {
            description.get(CHILDREN, CHILD_TYPE, MODEL_DESCRIPTION, CHILD_NAME, OPERATIONS, CHILD_OP, REQUEST_PROPERTIES, PARAM, VALUE_TYPE).set(valueType);
        }
        description.get(CHILDREN, CHILD_TYPE, MODEL_DESCRIPTION, CHILD_NAME, OPERATIONS, CHILD_OP, REQUEST_PROPERTIES, PARAM, REQUIRED).set(true);
        description.get(CHILDREN, CHILD_TYPE, MODEL_DESCRIPTION, CHILD_NAME, OPERATIONS, CHILD_OP, REPLY_PROPERTIES, TYPE).set(type);
        if (valueType != null && valueType.isDefined()) {
            description.get(CHILDREN, CHILD_TYPE, MODEL_DESCRIPTION, CHILD_NAME, OPERATIONS, CHILD_OP, REPLY_PROPERTIES, VALUE_TYPE).set(valueType);
        }

        return description;
    }

    private List<ValidationFailure> validate(ModelNode description, ValidationConfiguration validationConfiguration){
        ModelDescriptionValidator validator = new ModelDescriptionValidator(new ModelNode().setEmptyList(), description, validationConfiguration);
        return validator.validateResource();
    }

    private void assertFailure(ValidationFailure failure, ModelNode address) {
        Assert.assertEquals(address, failure.getAddress());
        Assert.assertNull(failure.getOperationName());
        Assert.assertNull(failure.getAttributeName());
        Assert.assertNull(failure.getOperationParameterName());
    }

    private void assertAttributeFailure(ValidationFailure failure, ModelNode address, String attributeName) {
        Assert.assertEquals(address, failure.getAddress());
        Assert.assertEquals(attributeName, failure.getAttributeName());
        Assert.assertNull(failure.getOperationName());
        Assert.assertNull(failure.getOperationParameterName());
    }

    private void assertOperationFailure(ValidationFailure failure, ModelNode address, String operationName) {
        Assert.assertEquals(address, failure.getAddress());
        Assert.assertEquals(operationName, failure.getOperationName());
        Assert.assertNull(failure.getAttributeName());
        Assert.assertNull(failure.getOperationParameterName());
    }

    private void assertOperationParameterFailure(ValidationFailure failure, ModelNode address, String operationName, String parameterName) {
        Assert.assertEquals(address, failure.getAddress());
        Assert.assertEquals(operationName, failure.getOperationName());
        Assert.assertNull(failure.getAttributeName());
        Assert.assertEquals(parameterName, failure.getOperationParameterName());
    }

    private void dumpErrors(List<ValidationFailure> errors) {
        System.out.println("==== Errors ====");
        for (ValidationFailure failure : errors) {
            System.out.println(failure);
        }
    }
}
