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
package org.jboss.as.host.controller;

import org.jboss.as.controller.OperationFailedException;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONCURRENT_GROUPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.IN_SERIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLBACK_ACROSS_GROUPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLING_TO_SERVERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MAX_FAILED_SERVERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MAX_FAILURE_PERCENTAGE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_GROUP;
import org.jboss.as.domain.controller.DomainModelUtil;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Alexey Loubyansky
 */
public class RolloutPlanValidationUnitTestCase {

    @Test
    public void testMissingInSeries() throws Exception {
        try {
            DomainModelUtil.validateRolloutPlanStructure(new ModelNode());
            Assert.fail("Rollout plan is missing in-series");
        } catch(OperationFailedException e) {
            // expected
        }
    }

    @Test
    public void testTooManyChildren() throws Exception {
        final ModelNode rolloutPlan = new ModelNode();
        final ModelNode inSeries = rolloutPlan.get(IN_SERIES);
        inSeries.add().get(SERVER_GROUP).get("group1");
        rolloutPlan.get(ROLLBACK_ACROSS_GROUPS).set(true);
        rolloutPlan.get("unrecognized");
        try {
            DomainModelUtil.validateRolloutPlanStructure(rolloutPlan);
            Assert.fail("Rollout plan has too many children");
        } catch(OperationFailedException e) {
            // expected
        }
    }

    @Test
    public void testUnrecorgnizedChild() throws Exception {
        final ModelNode rolloutPlan = new ModelNode();
        final ModelNode inSeries = rolloutPlan.get(IN_SERIES);
        inSeries.add().get(SERVER_GROUP).get("group1");
        rolloutPlan.get("unrecognized");
        try {
            DomainModelUtil.validateRolloutPlanStructure(rolloutPlan);
            Assert.fail("Rollout plan has unrecognized child.");
        } catch(OperationFailedException e) {
            // expected
        }
    }

    @Test
    public void testInSeriesNotDefined() throws Exception {
        final ModelNode rolloutPlan = new ModelNode();
        rolloutPlan.get(IN_SERIES);
        try {
            DomainModelUtil.validateRolloutPlanStructure(rolloutPlan);
            Assert.fail("in-series undefined");
        } catch(OperationFailedException e) {
            // expected
        }
    }

    @Test
    public void testServerGroupUndefined() throws Exception {
        final ModelNode rolloutPlan = new ModelNode();
        final ModelNode inSeries = rolloutPlan.get(IN_SERIES);
        inSeries.add().get(SERVER_GROUP);
        try {
            DomainModelUtil.validateRolloutPlanStructure(rolloutPlan);
            Assert.fail("server-group undefined");
        } catch(OperationFailedException e) {
            // expected
        }
    }

    @Test
    public void testServerGroupMissingName() throws Exception {
        final ModelNode rolloutPlan = new ModelNode();
        final ModelNode inSeries = rolloutPlan.get(IN_SERIES);
        inSeries.add().get(SERVER_GROUP).setEmptyObject();
        try {
            DomainModelUtil.validateRolloutPlanStructure(rolloutPlan);
            Assert.fail("server-group name missing");
        } catch(OperationFailedException e) {
            // expected
        }
    }

    @Test
    public void testServerGroupOnlyName() throws Exception {
        final ModelNode rolloutPlan = new ModelNode();
        final ModelNode inSeries = rolloutPlan.get(IN_SERIES);
        inSeries.add().get(SERVER_GROUP).get("group1");
        DomainModelUtil.validateRolloutPlanStructure(rolloutPlan);
    }

    @Test
    public void testServerGroupTwoManyNames() throws Exception {
        final ModelNode rolloutPlan = new ModelNode();
        final ModelNode inSeries = rolloutPlan.get(IN_SERIES);
        ModelNode serverGroup = inSeries.add().get(SERVER_GROUP);
        serverGroup.get("group1");
        serverGroup.get("group2");
        try {
            DomainModelUtil.validateRolloutPlanStructure(rolloutPlan);
            Assert.fail("server-group has too many names");
        } catch(OperationFailedException e) {
            // expected
        }
    }

    @Test
    public void testServerGroupWithMaxFailurePercentage() throws Exception {
        final ModelNode rolloutPlan = new ModelNode();
        final ModelNode inSeries = rolloutPlan.get(IN_SERIES);
        final ModelNode group = inSeries.add().get(SERVER_GROUP).get("group1");
        group.get(MAX_FAILURE_PERCENTAGE).set(10);
        DomainModelUtil.validateRolloutPlanStructure(rolloutPlan);
    }

    @Test
    public void testServerGroupWithMaxFailedServers() throws Exception {
        final ModelNode rolloutPlan = new ModelNode();
        final ModelNode inSeries = rolloutPlan.get(IN_SERIES);
        final ModelNode group = inSeries.add().get(SERVER_GROUP).get("group1");
        group.get(MAX_FAILED_SERVERS).set(10);
        DomainModelUtil.validateRolloutPlanStructure(rolloutPlan);
    }

    @Test
    public void testServerGroupWithRollingToServers() throws Exception {
        final ModelNode rolloutPlan = new ModelNode();
        final ModelNode inSeries = rolloutPlan.get(IN_SERIES);
        final ModelNode group = inSeries.add().get(SERVER_GROUP).get("group1");
        group.get(ROLLING_TO_SERVERS).set(true);
        DomainModelUtil.validateRolloutPlanStructure(rolloutPlan);
    }

    @Test
    public void testServerGroupWithUnrecognizedProp() throws Exception {
        final ModelNode rolloutPlan = new ModelNode();
        final ModelNode inSeries = rolloutPlan.get(IN_SERIES);
        final ModelNode group = inSeries.add().get(SERVER_GROUP).get("group1");
        group.get("unrecognized").set(true);
        try {
            DomainModelUtil.validateRolloutPlanStructure(rolloutPlan);
            Assert.fail("unrecognized property");
        } catch(OperationFailedException expected) {
        }
    }

    @Test
    public void testServerGroupWithAllProps() throws Exception {
        final ModelNode rolloutPlan = new ModelNode();
        final ModelNode inSeries = rolloutPlan.get(IN_SERIES);
        final ModelNode group = inSeries.add().get(SERVER_GROUP).get("group1");
        group.get(ROLLING_TO_SERVERS).set(true);
        group.get(MAX_FAILURE_PERCENTAGE).set(1);
        group.get(MAX_FAILED_SERVERS).set(1);
        DomainModelUtil.validateRolloutPlanStructure(rolloutPlan);
    }

    @Test
    public void testEmptyConcurrentGroups() throws Exception {
        final ModelNode rolloutPlan = new ModelNode();
        final ModelNode inSeries = rolloutPlan.get(IN_SERIES);
        inSeries.add().get(CONCURRENT_GROUPS);
        try {
            DomainModelUtil.validateRolloutPlanStructure(rolloutPlan);
            Assert.fail("concurrent groups is empty");
        } catch(OperationFailedException expected) {
        }
    }

    @Test
    public void testConcurrentGroupsWithUndefinedGroup() throws Exception {
        // this doesn't make sense actually
        final ModelNode rolloutPlan = new ModelNode();
        final ModelNode inSeries = rolloutPlan.get(IN_SERIES);
        final ModelNode concurrent = inSeries.add().get(CONCURRENT_GROUPS);
        concurrent.get("group1");
        DomainModelUtil.validateRolloutPlanStructure(rolloutPlan);
    }

    @Test
    public void testConcurrentGroupsWithTwoUndefinedGroups() throws Exception {
        // this doesn't make sense actually
        final ModelNode rolloutPlan = new ModelNode();
        final ModelNode inSeries = rolloutPlan.get(IN_SERIES);
        final ModelNode concurrent = inSeries.add().get(CONCURRENT_GROUPS);
        concurrent.get("group1");
        concurrent.get("group2");
        DomainModelUtil.validateRolloutPlanStructure(rolloutPlan);
    }

    @Test
    public void testConcurrentGroupsWithProps() throws Exception {
        // this doesn't make sense actually
        final ModelNode rolloutPlan = new ModelNode();
        final ModelNode inSeries = rolloutPlan.get(IN_SERIES);
        final ModelNode concurrent = inSeries.add().get(CONCURRENT_GROUPS);
        concurrent.get("group1");
        final ModelNode group = concurrent.get("group2");
        group.get(ROLLING_TO_SERVERS).set(true);
        group.get(MAX_FAILURE_PERCENTAGE).set(1);
        group.get(MAX_FAILED_SERVERS).set(1);
        DomainModelUtil.validateRolloutPlanStructure(rolloutPlan);
    }

    @Test
    public void testConcurrentGroupsWithUnrecognizedProp() throws Exception {
        // this doesn't make sense actually
        final ModelNode rolloutPlan = new ModelNode();
        final ModelNode inSeries = rolloutPlan.get(IN_SERIES);
        final ModelNode concurrent = inSeries.add().get(CONCURRENT_GROUPS);
        concurrent.get("group1");
        final ModelNode group = concurrent.get("group2");
        group.get(ROLLING_TO_SERVERS).set(true);
        group.get(MAX_FAILURE_PERCENTAGE).set(1);
        group.get("unrecognized").set(1);
        try {
            DomainModelUtil.validateRolloutPlanStructure(rolloutPlan);
            Assert.fail("unrecognized prop");
        } catch(OperationFailedException expected) {}
    }

    @Test
    public void testMix() throws Exception {
        // this doesn't make sense actually
        final ModelNode rolloutPlan = new ModelNode();
        final ModelNode inSeries = rolloutPlan.get(IN_SERIES);
        ModelNode concurrent = inSeries.add().get(CONCURRENT_GROUPS);
        ModelNode group = concurrent.get("groupA");
        group.get(ROLLING_TO_SERVERS).set(true);
        group.get(MAX_FAILURE_PERCENTAGE).set(20);
        concurrent.get("groupB");

        group = inSeries.add().get(SERVER_GROUP).get("groupC");
        group.get(ROLLING_TO_SERVERS).set(false);
        group.get(MAX_FAILED_SERVERS).set(1);

        concurrent = inSeries.add().get(CONCURRENT_GROUPS);
        group = concurrent.get("groupD");
        group.get(ROLLING_TO_SERVERS).set(true);
        group.get(MAX_FAILURE_PERCENTAGE).set(20);
        concurrent.get("groupE");

        inSeries.add().get(SERVER_GROUP).get("groupF");

        DomainModelUtil.validateRolloutPlanStructure(rolloutPlan);
    }
}
