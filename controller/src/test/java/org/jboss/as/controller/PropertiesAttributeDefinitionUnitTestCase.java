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
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests of {@link PropertiesAttributeDefinition}.
 *
 * @author Brian Stansberry (c) 2012 Red Hat Inc.
 */
public class PropertiesAttributeDefinitionUnitTestCase {

    @Test
    public void testExpressions() throws OperationFailedException {

        PropertiesAttributeDefinition ld = new PropertiesAttributeDefinition.Builder("test", false)
                .setAllowExpression(true)
                .build();

        ModelNode op = new ModelNode();
        op.get("test", "int").set("int");
        op.get("test", "exp").set("${test:1}");

        ModelNode validated = ld.validateOperation(op);
        Assert.assertEquals(op.get("test", "int"), validated.get("int"));
        Assert.assertEquals(new ModelNode().setExpression(op.get("test", "exp").asString()), validated.get("exp"));

        ModelNode model = new ModelNode();
        ld.validateAndSet(op, model);
        Assert.assertEquals(op.get("test", "int"), model.get("test", "int"));
        Assert.assertEquals(new ModelNode().setExpression(op.get("test", "exp").asString()), model.get("test", "exp"));
    }
}
