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

import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests of {@link ObjectTypeAttributeDefinition}.
 *
 * @author Brian Stansberry (c) 2012 Red Hat Inc.
 */
public class ObjectTypeAttributeDefinitionUnitTestCase {

    @Test
    public void testExpressions() throws OperationFailedException {

        AttributeDefinition a = SimpleAttributeDefinitionBuilder.create("a", ModelType.INT).build();
        AttributeDefinition b = SimpleAttributeDefinitionBuilder.create("b", ModelType.BOOLEAN).setAllowExpression(true).build();

        ObjectTypeAttributeDefinition ld = new ObjectTypeAttributeDefinition.Builder("test", a, b)
                .setAllowExpression(true)
                .build();

        ModelNode op = new ModelNode();
        op.get("test", "a").set(2);
        op.get("test", "b").set("${test:1}");

        ModelNode validated = ld.validateOperation(op);
        Assert.assertEquals(op.get("test", "a"), validated.get("a"));
        Assert.assertEquals(new ModelNode().setExpression(op.get("test", "b").asString()), validated.get("b"));

        ModelNode model = new ModelNode();
        ld.validateAndSet(op, model);
        Assert.assertEquals(op.get("test", "a"), model.get("test", "a"));
        Assert.assertEquals(new ModelNode().setExpression(op.get("test", "b").asString()), model.get("test", "b"));

        op = new ModelNode();
        op.get("test", "a").set("${test:1}");
        op.get("test", "b").set(true);

        try {
            ld.validateOperation(op);
            org.junit.Assert.fail("Did not reject " + op);
        } catch (OperationFailedException good) {
            //
        }

        try {
            ld.validateAndSet(op, new ModelNode());
            org.junit.Assert.fail("Did not reject " + op);
        } catch (OperationFailedException good) {
            //
        }
    }
}
