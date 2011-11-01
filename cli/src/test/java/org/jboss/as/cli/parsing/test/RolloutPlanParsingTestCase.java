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


import java.util.List;

import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.operation.CommandLineParser;
import org.jboss.as.cli.operation.OperationRequestHeader;
import org.jboss.as.cli.operation.impl.DefaultCallbackHandler;
import org.jboss.as.cli.operation.impl.DefaultOperationRequestParser;
import org.jboss.as.cli.operation.impl.RolloutPlanHeader;
import org.jboss.dmr.ModelNode;
import org.junit.Test;

import junit.framework.TestCase;

/**
 *
 * @author Alexey Loubyansky
 */
public class RolloutPlanParsingTestCase extends TestCase {

    private CommandLineParser parser = DefaultOperationRequestParser.INSTANCE;
    DefaultCallbackHandler handler = new DefaultCallbackHandler();

    @Test
    public void testHeaderListStart() throws Exception {

        parse("/profile=default/subsystem=threads/thread-factory=mytf:do{");

        assertTrue(handler.hasAddress());
        assertTrue(handler.hasOperationName());
        assertFalse(handler.hasProperties());
        assertFalse(handler.endsOnAddressOperationNameSeparator());
        assertFalse(handler.endsOnPropertyListStart());
        assertFalse(handler.endsOnPropertySeparator());
        assertFalse(handler.endsOnPropertyValueSeparator());
        assertFalse(handler.endsOnNodeSeparator());
        assertFalse(handler.endsOnNodeTypeNameSeparator());
        assertTrue(handler.endsOnSeparator());
        assertTrue(handler.endsOnHeaderListStart());
        assertFalse(handler.isRequestComplete());
        assertFalse(handler.hasHeaders());
    }

    @Test
    public void testEmptyHeaders() throws Exception {

        parse("/profile=default/subsystem=threads/thread-factory=mytf:do{}");

        assertTrue(handler.hasAddress());
        assertTrue(handler.hasOperationName());
        assertFalse(handler.hasProperties());
        assertFalse(handler.endsOnAddressOperationNameSeparator());
        assertFalse(handler.endsOnPropertyListStart());
        assertFalse(handler.endsOnPropertySeparator());
        assertFalse(handler.endsOnPropertyValueSeparator());
        assertFalse(handler.endsOnNodeSeparator());
        assertFalse(handler.endsOnNodeTypeNameSeparator());
        assertFalse(handler.endsOnSeparator());
        assertFalse(handler.endsOnHeaderListStart());
        assertFalse(handler.isRequestComplete());
        assertFalse(handler.hasHeaders());
    }

    @Test
    public void testSingleHeader() throws Exception {

        parse("/profile=default/subsystem=threads/thread-factory=mytf:do{ name = value }");

        assertTrue(handler.hasAddress());
        assertTrue(handler.hasOperationName());
        assertFalse(handler.hasProperties());
        assertFalse(handler.endsOnAddressOperationNameSeparator());
        assertFalse(handler.endsOnPropertyListStart());
        assertFalse(handler.endsOnPropertySeparator());
        assertFalse(handler.endsOnPropertyValueSeparator());
        assertFalse(handler.endsOnNodeSeparator());
        assertFalse(handler.endsOnNodeTypeNameSeparator());
        assertFalse(handler.endsOnSeparator());
        assertFalse(handler.endsOnHeaderListStart());
        assertFalse(handler.isRequestComplete());
        assertTrue(handler.hasHeaders());

        final List<OperationRequestHeader> headers = handler.getHeaders();
        assertEquals(1, headers.size());
        final OperationRequestHeader header = headers.get(0);

        final ModelNode node = new ModelNode();
        node.get("name").set("value");
        assertEquals(node, header.toModelNode());
    }

    @Test
    public void testRolloutWrongInSeries() throws Exception {

        try {
            parse("/profile=default/subsystem=threads/thread-factory=mytf:do{ rollout value");
            fail("shouldn't have passed past 'value'");
        } catch(CommandFormatException e) {
            // expected
        }
    }

    @Test
    public void testRolloutCorrectInSeries() throws Exception {

        parse("/profile=default/subsystem=threads/thread-factory=mytf:do{ rollout in-series");

        assertTrue(handler.hasAddress());
        assertTrue(handler.hasOperationName());
        assertFalse(handler.hasProperties());
        assertFalse(handler.endsOnAddressOperationNameSeparator());
        assertFalse(handler.endsOnPropertyListStart());
        assertFalse(handler.endsOnPropertySeparator());
        assertFalse(handler.endsOnPropertyValueSeparator());
        assertFalse(handler.endsOnNodeSeparator());
        assertFalse(handler.endsOnNodeTypeNameSeparator());
        assertTrue(handler.endsOnSeparator());
        assertTrue(handler.endsOnHeaderListStart()); // TODO this is kind of strange but ok...
        assertFalse(handler.isRequestComplete());
        assertTrue(handler.hasHeaders());
    }

    @Test
    public void testRolloutSingleGroupName() throws Exception {

        parse("/profile=default/subsystem=threads/thread-factory=mytf:do{ rollout in-series groupA}");

        assertTrue(handler.hasAddress());
        assertTrue(handler.hasOperationName());
        assertFalse(handler.hasProperties());
        assertFalse(handler.endsOnAddressOperationNameSeparator());
        assertFalse(handler.endsOnPropertyListStart());
        assertFalse(handler.endsOnPropertySeparator());
        assertFalse(handler.endsOnPropertyValueSeparator());
        assertFalse(handler.endsOnNodeSeparator());
        assertFalse(handler.endsOnNodeTypeNameSeparator());
        assertFalse(handler.endsOnSeparator());
        assertFalse(handler.endsOnHeaderListStart());
        assertFalse(handler.isRequestComplete());
        assertTrue(handler.hasHeaders());

        final List<OperationRequestHeader> headers = handler.getHeaders();
        assertEquals(1, headers.size());
        final OperationRequestHeader header = headers.get(0);
        assertTrue(header instanceof RolloutPlanHeader);

        final ModelNode node = new ModelNode();
        final ModelNode inSeries = node.get(Util.ROLLOUT_PLAN).get(Util.IN_SERIES);
        final ModelNode sg = new ModelNode();
        final ModelNode groupA = new ModelNode();
        groupA.get("groupA");
        sg.get(Util.SERVER_GROUP).set(groupA);
        inSeries.add().set(sg);
        assertEquals(node, header.toModelNode());
    }

    @Test
    public void testRolloutSingleGroupWithProps() throws Exception {

        parse("/profile=default/subsystem=threads/thread-factory=mytf:do{ rollout in-series groupA(rolling-to-servers=true,max-failure-percentage=20)");

        assertTrue(handler.hasAddress());
        assertTrue(handler.hasOperationName());
        assertFalse(handler.hasProperties());
        assertFalse(handler.endsOnAddressOperationNameSeparator());
        assertFalse(handler.endsOnPropertyListStart());
        assertFalse(handler.endsOnPropertySeparator());
        assertFalse(handler.endsOnPropertyValueSeparator());
        assertFalse(handler.endsOnNodeSeparator());
        assertFalse(handler.endsOnNodeTypeNameSeparator());
        assertTrue(handler.endsOnSeparator());
        assertTrue(handler.endsOnHeaderListStart()); // TODO this is kind of strange but ok...
        assertFalse(handler.isRequestComplete());

        assertTrue(handler.hasHeaders());

        final List<OperationRequestHeader> headers = handler.getHeaders();
        assertEquals(1, headers.size());
        final OperationRequestHeader header = headers.get(0);
        assertTrue(header instanceof RolloutPlanHeader);

        final ModelNode node = new ModelNode();
        final ModelNode inSeries = node.get(Util.ROLLOUT_PLAN).get(Util.IN_SERIES);
        final ModelNode sg = new ModelNode();
        final ModelNode groupA = new ModelNode();
        final ModelNode groupProps = groupA.get("groupA");
        groupProps.get("rolling-to-servers").set("true");
        groupProps.get("max-failure-percentage").set("20");
        sg.get(Util.SERVER_GROUP).set(groupA);
        inSeries.add().set(sg);
        assertEquals(node, header.toModelNode());
    }

    @Test
    public void testNonConcurrentGroups() throws Exception {

        parse("/profile=default/subsystem=threads/thread-factory=mytf:do{ rollout in-series " +
        		"groupA(rolling-to-servers=true,max-failure-percentage=20), groupB");

        assertTrue(handler.hasAddress());
        assertTrue(handler.hasOperationName());
        assertFalse(handler.hasProperties());
        assertFalse(handler.endsOnAddressOperationNameSeparator());
        assertFalse(handler.endsOnPropertyListStart());
        assertFalse(handler.endsOnPropertySeparator());
        assertFalse(handler.endsOnPropertyValueSeparator());
        assertFalse(handler.endsOnNodeSeparator());
        assertFalse(handler.endsOnNodeTypeNameSeparator());
        assertTrue(handler.endsOnSeparator());
        assertTrue(handler.endsOnHeaderListStart()); // TODO this is kind of strange but ok...
        assertFalse(handler.isRequestComplete());

        assertTrue(handler.hasHeaders());

        final List<OperationRequestHeader> headers = handler.getHeaders();
        assertEquals(1, headers.size());
        final OperationRequestHeader header = headers.get(0);
        assertTrue(header instanceof RolloutPlanHeader);

        final ModelNode node = new ModelNode();
        final ModelNode inSeries = node.get(Util.ROLLOUT_PLAN).get(Util.IN_SERIES);
        ModelNode sg = new ModelNode();
        ModelNode group = new ModelNode();
        final ModelNode groupProps = group.get("groupA");
        groupProps.get("rolling-to-servers").set("true");
        groupProps.get("max-failure-percentage").set("20");
        sg.get(Util.SERVER_GROUP).set(group);
        inSeries.add().set(sg);

        sg = new ModelNode();
        group = new ModelNode();
        group.get("groupB");
        sg.get(Util.SERVER_GROUP).set(group);
        inSeries.add().set(sg);

        assertEquals(node, header.toModelNode());
    }

    @Test
    public void testTwoConcurrentGroups() throws Exception {

        parse("/profile=default/subsystem=threads/thread-factory=mytf:do{ rollout in-series " +
                "groupA(rolling-to-servers=true,max-failure-percentage=20) ^ groupB");

        assertTrue(handler.hasAddress());
        assertTrue(handler.hasOperationName());
        assertFalse(handler.hasProperties());
        assertFalse(handler.endsOnAddressOperationNameSeparator());
        assertFalse(handler.endsOnPropertyListStart());
        assertFalse(handler.endsOnPropertySeparator());
        assertFalse(handler.endsOnPropertyValueSeparator());
        assertFalse(handler.endsOnNodeSeparator());
        assertFalse(handler.endsOnNodeTypeNameSeparator());
        assertTrue(handler.endsOnSeparator());
        assertTrue(handler.endsOnHeaderListStart()); // TODO this is kind of strange but ok...
        assertFalse(handler.isRequestComplete());

        assertTrue(handler.hasHeaders());

        final List<OperationRequestHeader> headers = handler.getHeaders();
        assertEquals(1, headers.size());
        final OperationRequestHeader header = headers.get(0);
        assertTrue(header instanceof RolloutPlanHeader);

        final ModelNode node = new ModelNode();
        final ModelNode inSeries = node.get(Util.ROLLOUT_PLAN).get(Util.IN_SERIES);

        final ModelNode concurrent = new ModelNode();
        final ModelNode cg = concurrent.get(Util.CONCURRENT_GROUPS);

        ModelNode sg = new ModelNode();
        ModelNode group = new ModelNode();
        final ModelNode groupProps = group.get("groupA");
        groupProps.get("rolling-to-servers").set("true");
        groupProps.get("max-failure-percentage").set("20");
        sg.get(Util.SERVER_GROUP).set(group);
        cg.add().set(sg);

        sg = new ModelNode();
        group = new ModelNode();
        group.get("groupB");
        sg.get(Util.SERVER_GROUP).set(group);
        cg.add().set(sg);

        inSeries.add().set(concurrent);

        assertEquals(node, header.toModelNode());
    }

    @Test
    public void testMix() throws Exception {

        parse("/profile=default/subsystem=threads/thread-factory=mytf:do{ rollout in-series " +
                "groupA(rolling-to-servers=true,max-failure-percentage=20) ^ groupB, groupC," +
                "groupD(rolling-to-servers=true,max-failed-servers=1) ^ groupE");

        assertTrue(handler.hasAddress());
        assertTrue(handler.hasOperationName());
        assertFalse(handler.hasProperties());
        assertFalse(handler.endsOnAddressOperationNameSeparator());
        assertFalse(handler.endsOnPropertyListStart());
        assertFalse(handler.endsOnPropertySeparator());
        assertFalse(handler.endsOnPropertyValueSeparator());
        assertFalse(handler.endsOnNodeSeparator());
        assertFalse(handler.endsOnNodeTypeNameSeparator());
        assertTrue(handler.endsOnSeparator());
        assertTrue(handler.endsOnHeaderListStart()); // TODO this is kind of strange but ok...
        assertFalse(handler.isRequestComplete());

        assertTrue(handler.hasHeaders());

        final List<OperationRequestHeader> headers = handler.getHeaders();
        assertEquals(1, headers.size());
        final OperationRequestHeader header = headers.get(0);
        assertTrue(header instanceof RolloutPlanHeader);

        final ModelNode node = new ModelNode();
        final ModelNode inSeries = node.get(Util.ROLLOUT_PLAN).get(Util.IN_SERIES);

        ModelNode concurrent = new ModelNode();
        ModelNode cg = concurrent.get(Util.CONCURRENT_GROUPS);

        ModelNode sg = new ModelNode();
        ModelNode group = new ModelNode();
        ModelNode groupProps = group.get("groupA");
        groupProps.get("rolling-to-servers").set("true");
        groupProps.get("max-failure-percentage").set("20");
        sg.get(Util.SERVER_GROUP).set(group);
        cg.add().set(sg);

        sg = new ModelNode();
        group = new ModelNode();
        group.get("groupB");
        sg.get(Util.SERVER_GROUP).set(group);
        cg.add().set(sg);

        inSeries.add().set(concurrent);

        sg = new ModelNode();
        group = new ModelNode();
        group.get("groupC");
        sg.get(Util.SERVER_GROUP).set(group);
        inSeries.add().set(sg);

        concurrent = new ModelNode();
        cg = concurrent.get(Util.CONCURRENT_GROUPS);

        sg = new ModelNode();
        group = new ModelNode();
        groupProps = group.get("groupD");
        groupProps.get("rolling-to-servers").set("true");
        groupProps.get("max-failed-servers").set("1");
        sg.get(Util.SERVER_GROUP).set(group);
        cg.add().set(sg);

        sg = new ModelNode();
        group = new ModelNode();
        group.get("groupE");
        sg.get(Util.SERVER_GROUP).set(group);
        cg.add().set(sg);

        inSeries.add().set(concurrent);

        assertEquals(node, header.toModelNode());

    }

    @Test
    public void testMixAgainstWholeRequest() throws Exception {

        parse("/profile=default/subsystem=threads/thread-factory=mytf:do{ rollout in-series " +
                "groupA(rolling-to-servers=true,max-failure-percentage=20) ^ groupB, groupC," +
                "groupD(rolling-to-servers=true,max-failed-servers=1) ^ groupE; rollback-across-groups = true}");

        assertTrue(handler.hasAddress());
        assertTrue(handler.hasOperationName());
        assertFalse(handler.hasProperties());
        assertFalse(handler.endsOnAddressOperationNameSeparator());
        assertFalse(handler.endsOnPropertyListStart());
        assertFalse(handler.endsOnPropertySeparator());
        assertFalse(handler.endsOnPropertyValueSeparator());
        assertFalse(handler.endsOnNodeSeparator());
        assertFalse(handler.endsOnNodeTypeNameSeparator());
        assertFalse(handler.endsOnSeparator());
        assertFalse(handler.endsOnHeaderListStart());
        assertFalse(handler.isRequestComplete());

        assertTrue(handler.hasHeaders());

        final List<OperationRequestHeader> headers = handler.getHeaders();
        assertEquals(2, headers.size());

        final ModelNode op = handler.toOperationRequest();
        assertTrue(op.hasDefined(Util.OPERATION_HEADERS));
        final ModelNode headersNode = op.get(Util.OPERATION_HEADERS);

        final ModelNode rolloutPlan = new ModelNode();
        final ModelNode inSeries = rolloutPlan.get(Util.ROLLOUT_PLAN).get(Util.IN_SERIES);

        ModelNode concurrent = new ModelNode();
        ModelNode cg = concurrent.get(Util.CONCURRENT_GROUPS);

        ModelNode sg = new ModelNode();
        ModelNode group = new ModelNode();
        ModelNode groupProps = group.get("groupA");
        groupProps.get("rolling-to-servers").set("true");
        groupProps.get("max-failure-percentage").set("20");
        sg.get(Util.SERVER_GROUP).set(group);
        cg.add().set(sg);

        sg = new ModelNode();
        group = new ModelNode();
        group.get("groupB");
        sg.get(Util.SERVER_GROUP).set(group);
        cg.add().set(sg);

        inSeries.add().set(concurrent);

        sg = new ModelNode();
        group = new ModelNode();
        group.get("groupC");
        sg.get(Util.SERVER_GROUP).set(group);
        inSeries.add().set(sg);

        concurrent = new ModelNode();
        cg = concurrent.get(Util.CONCURRENT_GROUPS);

        sg = new ModelNode();
        group = new ModelNode();
        groupProps = group.get("groupD");
        groupProps.get("rolling-to-servers").set("true");
        groupProps.get("max-failed-servers").set("1");
        sg.get(Util.SERVER_GROUP).set(group);
        cg.add().set(sg);

        sg = new ModelNode();
        group = new ModelNode();
        group.get("groupE");
        sg.get(Util.SERVER_GROUP).set(group);
        cg.add().set(sg);

        inSeries.add().set(concurrent);

        final ModelNode rollbackAcross = new ModelNode();
        rollbackAcross.get("rollback-across-groups").set("true");

        final ModelNode expectedList = new ModelNode();
        expectedList.add().set(rolloutPlan);
        expectedList.add().set(rollbackAcross);

        assertEquals(expectedList, headersNode);
    }

    protected void parse(String opReq) throws CommandFormatException {
        handler.reset();
        parser.parse(opReq, handler);
    }
}
