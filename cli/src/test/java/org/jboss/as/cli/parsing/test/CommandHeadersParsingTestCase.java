/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.cli.parsing.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.jboss.as.cli.ArgumentValueConverter;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.operation.impl.DefaultCallbackHandler;
import org.jboss.as.cli.parsing.ParserUtil;
import org.jboss.dmr.ModelNode;
import org.junit.Test;

/**
 *
 * @author Alexey Loubyansky
 */
public class CommandHeadersParsingTestCase {

    private final DefaultCallbackHandler handler = new DefaultCallbackHandler();

    @Test
    public void testSingleHeader() throws Exception {
        final ModelNode headers = ArgumentValueConverter.HEADERS.fromString("{rollback-on-runtime-failure=false}");
        final ModelNode expected = new ModelNode();
        expected.get("rollback-on-runtime-failure").set("false");
        assertEquals(expected, headers);
    }

    @Test
    public void testTwoHeaders() throws Exception {
        final ModelNode headers = ArgumentValueConverter.HEADERS.fromString("{rollback-on-runtime-failure=false;allow-resource-service-restart=true}");
        final ModelNode expected = new ModelNode();
        expected.get("rollback-on-runtime-failure").set("false");
        expected.get("allow-resource-service-restart").set("true");
        assertEquals(expected, headers);
    }

    @Test
    public void testArgumentValueConverter() throws Exception {

        final ModelNode node = ArgumentValueConverter.HEADERS.fromString("{ rollout groupA rollback-across-groups; rollback-on-runtime-failure=false}");

        final ModelNode expectedHeaders = new ModelNode();
        final ModelNode rolloutPlan = expectedHeaders.get(Util.ROLLOUT_PLAN);
        final ModelNode inSeries = rolloutPlan.get(Util.IN_SERIES);

        ModelNode sg = new ModelNode();
        ModelNode group = sg.get(Util.SERVER_GROUP);
        group.get("groupA");
        inSeries.add().set(sg);

        rolloutPlan.get("rollback-across-groups").set("true");

        expectedHeaders.get("rollback-on-runtime-failure").set("false");

        assertEquals(expectedHeaders, node);
    }

    @Test
    public void testNonRolloutCompletion() throws Exception {
        parse("read-attribute process-type --headers={rollout main-server-group; rollback-on-runtime-failure=false; allow-resource-service-restart=tr");

        assertFalse(handler.hasAddress());
        assertTrue(handler.hasOperationName());
        assertTrue(handler.hasProperties());
        assertFalse(handler.endsOnAddressOperationNameSeparator());
        assertFalse(handler.endsOnPropertyListStart());
        assertFalse(handler.endsOnPropertySeparator());
        assertFalse(handler.endsOnPropertyValueSeparator());
        assertFalse(handler.endsOnNodeSeparator());
        assertFalse(handler.endsOnNodeTypeNameSeparator());
        assertFalse(handler.isRequestComplete());

        final String headers = handler.getPropertyValue("--headers");
        assertNotNull(headers);
        final ModelNode node = ArgumentValueConverter.HEADERS.fromString(headers);
        assertTrue(node.hasDefined(Util.ROLLOUT_PLAN));
        assertTrue(node.hasDefined(Util.ROLLBACK_ON_RUNTIME_FAILURE));
        assertEquals("tr", node.get(Util.ALLOW_RESOURCE_SERVICE_RESTART).asString());
    }

    @Test
    public void testSimpleHeaderCompletion() throws Exception {
        parse(":do{allow-resource-service-restart =");

        assertFalse(handler.hasAddress());
        assertTrue(handler.hasOperationName());
        assertFalse(handler.hasProperties());
        assertFalse(handler.endsOnAddressOperationNameSeparator());
        assertFalse(handler.endsOnPropertyListStart());
        assertFalse(handler.endsOnPropertySeparator());
        //assertFalse(handler.endsOnPropertyValueSeparator());
        assertFalse(handler.endsOnNodeSeparator());
        assertFalse(handler.endsOnNodeTypeNameSeparator());
        assertFalse(handler.isRequestComplete());
        assertTrue(handler.endsOnSeparator());
    }

    protected void parse(String cmd) throws CommandFormatException {
        handler.reset();
        ParserUtil.parse(cmd, handler);
    }
}
