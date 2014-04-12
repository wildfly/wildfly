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

package org.jboss.as.controller;

import static org.jboss.as.controller.registry.AttributeAccess.Flag.RESTART_ALL_SERVICES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import junit.framework.Assert;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.junit.Test;

/**
 * Unit tests of {@link PrimitiveListAttributeDefinition}.
 *
 * @author Brian Stansberry (c) 2012 Red Hat Inc.
 */
public class PrimitiveListAttributeDefinitionUnitTestCase {

    @Test
    public void testExpressions() throws OperationFailedException {

        PrimitiveListAttributeDefinition ld = PrimitiveListAttributeDefinition.Builder.of("test", ModelType.INT)
                .setAllowExpression(true)
                .setElementValidator(new IntRangeValidator(1, false, true))
                .build();

        ModelNode op = new ModelNode();
        op.get("test").add(2).add("${test:1}");

        ModelNode validated = ld.validateOperation(op);
        assertEquals(op.get("test").get(0), validated.get(0));
        assertEquals(new ModelNode().setExpression(op.get("test").get(1).asString()), validated.get(1));

        ModelNode model = new ModelNode();
        ld.validateAndSet(op, model);
        assertEquals(op.get("test").get(0), model.get("test").get(0));
        assertEquals(new ModelNode().setExpression(op.get("test").get(1).asString()), model.get("test").get(1));

        ld = PrimitiveListAttributeDefinition.Builder.of("test", ModelType.PROPERTY)
                .setAllowExpression(true)
                .setElementValidator(new ModelTypeValidator(ModelType.PROPERTY, false, true))
                .build();

        op = new ModelNode();
        op.get("test").add("foo", 2).add("bar", "${test:1}");

        try {
            ld.validateOperation(op);
            fail("Did not reject " + op);
        } catch (IllegalStateException good) {
            //
        }

        try {
            ld.validateAndSet(op, new ModelNode());
            fail("Did not reject " + op);
        } catch (IllegalStateException good) {
            //
        }

    }

    @Test
    public void testBuilderCopyPreservesElementValidator() throws OperationFailedException {
        PrimitiveListAttributeDefinition original = PrimitiveListAttributeDefinition.Builder.of("test", ModelType.STRING)
                .setElementValidator(new StringLengthValidator(1))
                .build();

        // will use the same validator than original
        PrimitiveListAttributeDefinition copy = new PrimitiveListAttributeDefinition.Builder(original)
                // add a flag to distinguish the copy from the original
                .setFlags(RESTART_ALL_SERVICES)
                .build();

        // use a different validator than original & copy
        PrimitiveListAttributeDefinition copyWithOtherValidator = new PrimitiveListAttributeDefinition.Builder(original)
                // add a flag to distinguish the copy from the original
                .setFlags(RESTART_ALL_SERVICES)
                .setElementValidator(new StringLengthValidator(Integer.MAX_VALUE))
                .build();

        assertFalse(original.getFlags().contains(RESTART_ALL_SERVICES));
        assertTrue(copy.getFlags().contains(RESTART_ALL_SERVICES));
        assertTrue(copyWithOtherValidator.getFlags().contains(RESTART_ALL_SERVICES));

        assertSame(original.getElementValidator(), copy.getElementValidator());
        assertNotSame(original.getElementValidator(), copyWithOtherValidator.getElementValidator());

        ModelNode operation = new ModelNode();
        operation.get("test").add("foo");

        original.validateOperation(operation);
        copy.validateOperation(operation);
        try {
            copyWithOtherValidator.validateOperation(operation);
            fail("the operation must not be validated");
        } catch (OperationFailedException e) {
        }
    }
}
