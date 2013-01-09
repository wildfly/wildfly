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

import junit.framework.Assert;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.junit.Test;

/**
 * Unit tests of {@link ObjectListAttributeDefinition}.
 *
 * @author Brian Stansberry (c) 2012 Red Hat Inc.
 */
public class ObjectListAttributeDefinitionUnitTestCase {

    @Test
    public void testExpressions() throws OperationFailedException {

        AttributeDefinition a = SimpleAttributeDefinitionBuilder.create("a", ModelType.INT).build();
        AttributeDefinition b = SimpleAttributeDefinitionBuilder.create("b", ModelType.BOOLEAN).setAllowExpression(true).build();

        ObjectTypeAttributeDefinition otad = new ObjectTypeAttributeDefinition.Builder("", a, b)
                .setAllowExpression(true)
                .build();

        ObjectListAttributeDefinition ld = ObjectListAttributeDefinition.Builder.of("test", otad).build();

        ModelNode op = new ModelNode();
        ModelNode one = op.get("test").add();
        one.get("a").set(2);
        one.get("b").set(true);
        ModelNode two = op.get("test").add();
        two.get("a").set(2);
        two.get("b").set("${test:1}");

        ModelNode validated = ld.validateOperation(op);
        Assert.assertEquals(op.get("test").get(0), validated.get(0));
        Assert.assertEquals(op.get("test").get(1).get("a"), validated.get(1).get("a"));
        Assert.assertEquals(new ModelNode().setExpression(op.get("test").get(1).get("b").asString()), validated.get(1).get("b"));

        ModelNode model = new ModelNode();
        ld.validateAndSet(op, model);
        Assert.assertEquals(op.get("test").get(0), model.get("test").get(0));
        Assert.assertEquals(op.get("test").get(1).get("a"), model.get("test").get(1).get("a"));
        Assert.assertEquals(new ModelNode().setExpression(op.get("test").get(1).get("b").asString()), model.get("test").get(1).get("b"));

        op = new ModelNode();
        one = op.get("test").add();
        one.get("a").set(2);
        one.get("b").set(true);
        two = op.get("test").add();
        two.get("a").set("${test:1}");
        two.get("b").set(false);

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
