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

import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.junit.Assert;
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
                .setValidator(new IntRangeValidator(1, false, true))
                .build();

        ModelNode op = new ModelNode();
        op.get("test").add(2).add("${test:1}");

        ModelNode validated = ld.validateOperation(op);
        Assert.assertEquals(op.get("test").get(0), validated.get(0));
        Assert.assertEquals(new ModelNode().setExpression(op.get("test").get(1).asString()), validated.get(1));

        ModelNode model = new ModelNode();
        ld.validateAndSet(op, model);
        Assert.assertEquals(op.get("test").get(0), model.get("test").get(0));
        Assert.assertEquals(new ModelNode().setExpression(op.get("test").get(1).asString()), model.get("test").get(1));

        ld = PrimitiveListAttributeDefinition.Builder.of("test", ModelType.PROPERTY)
                .setAllowExpression(true)
                .setValidator(new ModelTypeValidator(ModelType.PROPERTY, false, true))
                .build();

        op = new ModelNode();
        op.get("test").add("foo", 2).add("bar", "${test:1}");

        try {
            ld.validateOperation(op);
            org.junit.Assert.fail("Did not reject " + op);
        } catch (IllegalStateException good) {
            //
        }

        try {
            ld.validateAndSet(op, new ModelNode());
            org.junit.Assert.fail("Did not reject " + op);
        } catch (IllegalStateException good) {
            //
        }

    }
}
