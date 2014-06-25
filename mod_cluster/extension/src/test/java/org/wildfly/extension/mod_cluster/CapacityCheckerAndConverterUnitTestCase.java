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

package org.wildfly.extension.mod_cluster;

import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.ValueExpression;
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for {@link CapacityCheckerAndConverter}.
 *
 * @author Brian Stansberry (c) 2012 Red Hat Inc.
 */
public class CapacityCheckerAndConverterUnitTestCase {

    private static final CapacityCheckerAndConverter testee = CapacityCheckerAndConverter.INSTANCE;

    @Test
    public void testSimpleDouble() {
        ModelNode val = new ModelNode(1d);
        Assert.assertFalse(testee.rejectAttribute(PathAddress.EMPTY_ADDRESS, "foo", val, null));
        testee.convertAttribute(PathAddress.EMPTY_ADDRESS, "foo", val, null);
        Assert.assertEquals(ModelType.INT, val.getType());
        Assert.assertEquals(1, val.asInt());
    }

    @Test
    public void testNonIntegerDouble() {
        ModelNode val = new ModelNode(1.1d);
        Assert.assertTrue(testee.rejectAttribute(PathAddress.EMPTY_ADDRESS, "foo", val, null));
        testee.convertAttribute(PathAddress.EMPTY_ADDRESS, "foo", val, null);
        Assert.assertEquals(ModelType.DOUBLE, val.getType());
        Assert.assertEquals(1.1d, val.asDouble(), 0.0d);
    }

    @Test
    public void testUndefined() {
        ModelNode val = new ModelNode();
        Assert.assertFalse(testee.rejectAttribute(PathAddress.EMPTY_ADDRESS, "foo", val, null));
        testee.convertAttribute(PathAddress.EMPTY_ADDRESS, "foo", val, null);
        Assert.assertEquals(ModelType.UNDEFINED, val.getType());
    }

    @Test
    public void testExpression() {
        ModelNode val = new ModelNode().set(new ValueExpression("${foo}"));
        Assert.assertTrue(testee.rejectAttribute(PathAddress.EMPTY_ADDRESS, "foo", val, null));
        testee.convertAttribute(PathAddress.EMPTY_ADDRESS, "foo", val, null);
        Assert.assertEquals(ModelType.EXPRESSION, val.getType());
    }

    @Test
    public void testStringExpression() {
        ModelNode val = new ModelNode().set("${foo}");
        Assert.assertTrue(testee.rejectAttribute(PathAddress.EMPTY_ADDRESS, "foo", val, null));
        testee.convertAttribute(PathAddress.EMPTY_ADDRESS, "foo", val, null);
        Assert.assertEquals(ModelType.STRING, val.getType());
    }
}
