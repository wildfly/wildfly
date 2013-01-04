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
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.dmr.ModelNode;
import org.junit.Test;

/**
 * Unit tests of {@link StringListAttributeDefinition}.
 *
 * @author Brian Stansberry (c) 2012 Red Hat Inc.
 */
public class StringListAttributeDefinitionUnitTestCase {

    @Test
    public void testExpressions() throws OperationFailedException {
        ListAttributeDefinition ld = new StringListAttributeDefinition.Builder("test")
                .setAllowExpression(true)
                .setValidator(new StringLengthValidator(1, false, true))
                .build();

        ModelNode op = new ModelNode();
        op.get("test").add("abc").add("${test:1}");

        ModelNode validated = ld.validateOperation(op);
        Assert.assertEquals(op.get("test").get(0), validated.get(0));
        Assert.assertEquals(new ModelNode().setExpression(op.get("test").get(1).asString()), validated.get(1));

        ModelNode model = new ModelNode();
        ld.validateAndSet(op, model);
        Assert.assertEquals(op.get("test").get(0), model.get("test").get(0));
        Assert.assertEquals(new ModelNode().setExpression(op.get("test").get(1).asString()), model.get("test").get(1));

    }
}
