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

import org.jboss.as.cli.ArgumentValueConverter;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.completion.mock.MockCommandContext;
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

    private final CommandLineParser parser = DefaultOperationRequestParser.INSTANCE;
    private final DefaultCallbackHandler handler = new DefaultCallbackHandler();
    private final MockCommandContext ctx = new MockCommandContext();

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

        assertEquals("name", header.getName());
        final ModelNode node = new ModelNode();
        node.get("name").set("value");

        final ModelNode headersNode = new ModelNode();
        header.addTo(ctx, headersNode);
        assertEquals(node, headersNode);
    }

    @Test
    public void testTwoHeaders() throws Exception {

        parse("/profile=default/subsystem=threads/thread-factory=mytf:do{ name1 = value1 ; name2=value2 }");

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


        OperationRequestHeader header = headers.get(0);
        assertEquals("name1", header.getName());
        ModelNode node = new ModelNode();
        node.get("name1").set("value1");

        ModelNode headersNode = new ModelNode();
        header.addTo(ctx, headersNode);
        assertEquals(node, headersNode);

        header = headers.get(1);
        assertEquals("name2", header.getName());
        node = new ModelNode();
        node.get("name2").set("value2");

        headersNode = new ModelNode();
        header.addTo(ctx, headersNode);
        assertEquals(node, headersNode);
    }

/*    @Test
    public void testRolloutWithAProp() throws Exception {

        parse("/profile=default/subsystem=threads/thread-factory=mytf:do{ rollout prop=value");

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
        final RolloutPlanHeader rollout = (RolloutPlanHeader) header;
        assertEquals("value", rollout.getProperty("prop"));
    }

    @Test
    public void testRolloutWithTwoProps() throws Exception {

        parse("/profile=default/subsystem=threads/thread-factory=mytf:do{ rollout prop1=value1 prop2 = value2");

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
        final RolloutPlanHeader rollout = (RolloutPlanHeader) header;
        assertEquals("value1", rollout.getProperty("prop1"));
        assertEquals("value2", rollout.getProperty("prop2"));
    }
*/
    @Test
    public void testRolloutSingleGroupName() throws Exception {

        //parse("/profile=default/subsystem=threads/thread-factory=mytf:do{ rollout in-series = groupA}");
        parse("/profile=default/subsystem=threads/thread-factory=mytf:do{ rollout groupA}");

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
        final ModelNode groupA = new ModelNode();
        groupA.get("groupA");
        inSeries.add().get(Util.SERVER_GROUP).set(groupA);

        final ModelNode headersNode = new ModelNode();
        header.addTo(ctx, headersNode);
        assertEquals(node, headersNode);
    }

    @Test
    public void testRolloutSingleGroupWithProps() throws Exception {

        //parse("/profile=default/subsystem=threads/thread-factory=mytf:do{ rollout in-series=groupA(rolling-to-servers=true,max-failure-percentage=20)");
        parse("/profile=default/subsystem=threads/thread-factory=mytf:do{ rollout groupA(rolling-to-servers=true,max-failure-percentage=20)");

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
        final ModelNode groupA = new ModelNode();
        final ModelNode groupProps = groupA.get("groupA");
        groupProps.get("rolling-to-servers").set("true");
        groupProps.get("max-failure-percentage").set("20");
        inSeries.add().get(Util.SERVER_GROUP).set(groupA);

        final ModelNode headersNode = new ModelNode();
        header.addTo(ctx, headersNode);
        assertEquals(node, headersNode);
    }

    @Test
    public void testNonConcurrentGroups() throws Exception {

        parse("/profile=default/subsystem=threads/thread-factory=mytf:do{ rollout " +
        		"groupA(rolling-to-servers=true,max-failure-percentage=20) , groupB");

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
        ModelNode group = new ModelNode();
        final ModelNode groupProps = group.get("groupA");
        groupProps.get("rolling-to-servers").set("true");
        groupProps.get("max-failure-percentage").set("20");
        inSeries.add().get(Util.SERVER_GROUP).set(group);

        group = new ModelNode();
        group.get("groupB");
        inSeries.add().get(Util.SERVER_GROUP).set(group);

        final ModelNode headersNode = new ModelNode();
        header.addTo(ctx, headersNode);
        assertEquals(node, headersNode);
    }

    @Test
    public void testNonConcurrentGroupNames() throws Exception {

        parse("/profile=default/subsystem=threads/thread-factory=mytf:do{ rollout groupA ,  groupB");

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
        ModelNode group = new ModelNode();
        group.get("groupA");
        inSeries.add().get(Util.SERVER_GROUP).set(group);

        group = new ModelNode();
        group.get("groupB");
        inSeries.add().get(Util.SERVER_GROUP).set(group);

        final ModelNode headersNode = new ModelNode();
        header.addTo(ctx, headersNode);
        assertEquals(node, headersNode);
    }

    @Test
    public void testTwoConcurrentGroups() throws Exception {

        parse("/profile=default/subsystem=threads/thread-factory=mytf:do{ rollout " +
                "groupA(rolling-to-servers=true,max-failure-percentage=20) ^  groupB");

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

        ModelNode group = cg.get("groupA");
        group.get("rolling-to-servers").set("true");
        group.get("max-failure-percentage").set("20");

        group = cg.get("groupB");

        inSeries.add().set(concurrent);

        final ModelNode headersNode = new ModelNode();
        header.addTo(ctx, headersNode);
        assertEquals(node, headersNode);
    }

    @Test
    public void testTwoConcurrentGroupNames() throws Exception {

        parse("/profile=default/subsystem=threads/thread-factory=mytf:do{ rollout groupA ^ groupB");

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
        cg.get("groupA");
        cg.get("groupB");

        inSeries.add().set(concurrent);

        final ModelNode headersNode = new ModelNode();
        header.addTo(ctx, headersNode);
        assertEquals(node, headersNode);
    }

    @Test
    public void testMix() throws Exception {

        parse("/profile=default/subsystem=threads/thread-factory=mytf:do{ rollout " +
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

        ModelNode group = cg.get("groupA");
        group.get("rolling-to-servers").set("true");
        group.get("max-failure-percentage").set("20");

        group = cg.get("groupB");

        inSeries.add().set(concurrent);

        ModelNode sg = new ModelNode();
        group = sg.get(Util.SERVER_GROUP);
        group.get("groupC");
        inSeries.add().set(sg);

        concurrent = new ModelNode();
        cg = concurrent.get(Util.CONCURRENT_GROUPS);

        group = cg.get("groupD");
        group.get("rolling-to-servers").set("true");
        group.get("max-failed-servers").set("1");

        group = cg.get("groupE");

        inSeries.add().set(concurrent);

        final ModelNode headersNode = new ModelNode();
        header.addTo(ctx, headersNode);
        assertEquals(node, headersNode);
    }

    @Test
    public void testMixAgainstWholeRequest() throws Exception {

        parse("/profile=default/subsystem=threads/thread-factory=mytf:do{ rollout " +
                "groupA(rolling-to-servers=true,max-failure-percentage=20) ^ groupB, groupC," +
                "groupD(rolling-to-servers=true,max-failed-servers=1) ^ groupE rollback-across-groups}");

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

        final ModelNode op = handler.toOperationRequest(ctx);
        assertTrue(op.hasDefined(Util.OPERATION_HEADERS));
        final ModelNode headersNode = op.get(Util.OPERATION_HEADERS);

        final ModelNode expectedHeaders = new ModelNode();
        final ModelNode rolloutPlan = expectedHeaders.get(Util.ROLLOUT_PLAN);
        final ModelNode inSeries = rolloutPlan.get(Util.IN_SERIES);

        ModelNode concurrent = new ModelNode();
        ModelNode cg = concurrent.get(Util.CONCURRENT_GROUPS);

        ModelNode group = cg.get("groupA");
        group.get("rolling-to-servers").set("true");
        group.get("max-failure-percentage").set("20");

        group = cg.get("groupB");

        inSeries.add().set(concurrent);

        ModelNode sg = new ModelNode();
        group = sg.get(Util.SERVER_GROUP);
        group.get("groupC");
        inSeries.add().set(sg);

        concurrent = new ModelNode();
        cg = concurrent.get(Util.CONCURRENT_GROUPS);

        group = cg.get("groupD");
        group.get("rolling-to-servers").set("true");
        group.get("max-failed-servers").set("1");

        cg.get("groupE");

        inSeries.add().set(concurrent);

        rolloutPlan.get("rollback-across-groups").set("true");

        assertEquals(expectedHeaders, headersNode);
    }

    @Test
    public void testRolloutId() throws Exception {

/* the plans are not in the config any more, just test the parsing of the reference

        RolloutPlanHeader myPlan = new RolloutPlanHeader("myplan");

        ConcurrentRolloutPlanGroup concurrent = new ConcurrentRolloutPlanGroup();

        SingleRolloutPlanGroup group = new SingleRolloutPlanGroup("groupA");
        group.addProperty("rolling-to-servers", "true");
        group.addProperty("max-failure-percentage", "20");
        concurrent.addGroup(group);

        group = new SingleRolloutPlanGroup("groupB");
        group.addProperty("rolling-to-servers", "false");
        group.addProperty("max-failed-servers", "2");
        concurrent.addGroup(group);

        myPlan.addGroup(concurrent);

        group = new SingleRolloutPlanGroup("groupC");
        myPlan.addGroup(group);

        myPlan.addProperty("rollback-across-groups", "true");

        ((MockCliConfig)ctx.getConfig()).addRolloutPlan(myPlan);
*/
        //parse("/profile=default/subsystem=threads/thread-factory=mytf:do{ rollout in-series = groupA}");
        parse("/profile=default/subsystem=threads/thread-factory=mytf:do{ rollout id = myplan}");

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

        final RolloutPlanHeader rollout = (RolloutPlanHeader) header;
        assertEquals("myplan", rollout.getPlanRef());

/*        final ModelNode op = handler.toOperationRequest(ctx);
        assertTrue(op.hasDefined(Util.OPERATION_HEADERS));
        final ModelNode headersNode = op.get(Util.OPERATION_HEADERS);

        final ModelNode expectedHeaders = new ModelNode();
        final ModelNode rolloutPlan = expectedHeaders.get(Util.ROLLOUT_PLAN);
        final ModelNode inSeries = rolloutPlan.get(Util.IN_SERIES);

        ModelNode concurrentNode = new ModelNode();
        ModelNode cg = concurrentNode.get(Util.CONCURRENT_GROUPS);

        ModelNode groupNode = cg.get("groupA");
        groupNode.get("rolling-to-servers").set("true");
        groupNode.get("max-failure-percentage").set("20");

        groupNode = cg.get("groupB");
        groupNode.get("rolling-to-servers").set("false");
        groupNode.get("max-failed-servers").set("2");

        inSeries.add().set(concurrentNode);

        ModelNode sg = new ModelNode();
        groupNode = sg.get(Util.SERVER_GROUP);
        groupNode.get("groupC");
        inSeries.add().set(sg);

        rolloutPlan.get("rollback-across-groups").set("true");

        assertEquals(expectedHeaders, headersNode);
*/    }

    @Test
    public void testArgumentValueConverter() throws Exception {

        final ModelNode node = ArgumentValueConverter.ROLLOUT_PLAN.fromString("{ rollout " +
                "groupA(rolling-to-servers=true,max-failure-percentage=20) ^ groupB, groupC," +
                "groupD(rolling-to-servers=true,max-failed-servers=1) ^ groupE rollback-across-groups}");

        final ModelNode expectedHeaders = new ModelNode();
        final ModelNode rolloutPlan = expectedHeaders.get(Util.ROLLOUT_PLAN);
        final ModelNode inSeries = rolloutPlan.get(Util.IN_SERIES);

        ModelNode concurrent = new ModelNode();
        ModelNode cg = concurrent.get(Util.CONCURRENT_GROUPS);

        ModelNode group = cg.get("groupA");
        group.get("rolling-to-servers").set("true");
        group.get("max-failure-percentage").set("20");

        group = cg.get("groupB");

        inSeries.add().set(concurrent);

        ModelNode sg = new ModelNode();
        group = sg.get(Util.SERVER_GROUP);
        group.get("groupC");
        inSeries.add().set(sg);

        concurrent = new ModelNode();
        cg = concurrent.get(Util.CONCURRENT_GROUPS);

        group = cg.get("groupD");
        group.get("rolling-to-servers").set("true");
        group.get("max-failed-servers").set("1");

        cg.get("groupE");

        inSeries.add().set(concurrent);

        rolloutPlan.get("rollback-across-groups").set("true");

        assertEquals(expectedHeaders, node);
    }

    protected void parse(String opReq) throws CommandFormatException {
        handler.reset();
        parser.parse(opReq, handler);
    }
}
