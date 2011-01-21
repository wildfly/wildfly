/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.jboss.as.controller.descriptions.common;


import java.util.Arrays;
import java.util.List;

import org.jboss.dmr.ModelNode;

/**
 * TODO add class javadoc for Examples
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 *
 */
public class Examples {

    public static ModelNode getUpdatePlan() {
        ModelNode op = new ModelNode();

        op.get("op").set("write-core-threads");
        op.get("op-addr").add("profile", "production");
        op.get("op-addr").add("subsystem", "threads");
        op.get("op-addr").add("bounded-queue-thread-pool", "pool1");
        op.get("count").set(0);
        op.get("per-cpu").set(20);
        op.get("rollout-plan", "rollback-across-groups").set(false);
        ModelNode set0 = op.get("rollout-plan", "in-series").add();
        set0.get("concurrent-groups", "groupA", "rolling-to-servers").set(true);
        set0.get("concurrent-groups", "groupA", "max-failure-percentage").set(20);
        set0.get("concurrent-groups", "groupB");
        ModelNode set1 = op.get("rollout-plan", "in-series").add();
        set1.get("server-group", "groupC");
        set0.get("server-group", "groupC", "rolling-to-servers").set(false);
        set0.get("server-group", "groupC", "max-failed-servers").set(1);
        ModelNode set2 = op.get("rollout-plan", "in-series").add();
        set2.get("concurrent-groups", "groupD", "rolling-to-servers").set(true);
        set2.get("concurrent-groups", "groupD", "max-failure-percentage").set(20);
        set2.get("concurrent-groups", "groupE");
        return op;
    }

    public static List<ModelNode> getSimpleResults() {

        ModelNode failed = new ModelNode();
        failed.get("success").set(false);
        failed.get("failure-description").set("[DOM-1234] Some failure message");

        ModelNode void_success = new ModelNode();
        void_success.get("success").set(true);
        void_success.get("result").set(new ModelNode());
        void_success.get("compensating-op").set(new ModelNode());

        ModelNode obj_success = new ModelNode();
        obj_success.get("success").set(true);
        ModelNode result = new ModelNode();
        result.get("name").set("Brian");
        result.get("age").set(22);
        obj_success.get("result").set(result);
        obj_success.get("compensating-op").set(new ModelNode());

        ModelNode comp = new ModelNode();
        comp.get("success").set(true);
        comp.get("result").set(new ModelNode());
        ModelNode op = new ModelNode();
        op.get("op").set("write-core-threads");
        op.get("op-addr").add("profile", "production");
        op.get("op-addr").add("subsystem", "threads");
        op.get("op-addr").add("bounded-queue-thread-pool", "pool1");
        op.get("count").set(5);
        op.get("per-cpu").set(5);
        comp.get("compensating-op").set(op);

        ModelNode cancelled = new ModelNode();
        cancelled.get("success").set(false);
        cancelled.get("cancelled").set(true);

        ModelNode rollback_success = new ModelNode();
        rollback_success.get("success").set(false);
        rollback_success.get("result").set(result);
        rollback_success.get("rolled-back").set(true);

        ModelNode rollback_failure = new ModelNode();
        rollback_failure.get("success").set(false);
        rollback_failure.get("result").set(result);
        rollback_failure.get("rollback-failure-description").set("[DOM-9876] Some failure message");

        ModelNode failed_rolledback = failed.clone();
        failed_rolledback.get("rolled-back").set(true);

        ModelNode multiGood = new ModelNode();
        multiGood.get("success").set(true);
        multiGood.get("result").add(obj_success);
        multiGood.get("result").add(comp);
        multiGood.get("compensating-op", "op").set("composite");
        multiGood.get("compensating-op", "op-addr").setEmptyList();
        multiGood.get("compensating-op", "steps").add(op);



        ModelNode multiBad = new ModelNode();
        multiBad.get("success").set(false);
        multiBad.get("failure-description").set("[DOM-9999] Composite operation failed; see individual operation results for details");
        multiBad.get("result").add(rollback_success);
        multiBad.get("result").add(failed_rolledback);
        multiBad.get("result").add(cancelled);



        return Arrays.asList(failed, void_success, obj_success, comp, cancelled, rollback_success, rollback_failure, multiGood, multiBad);
    }
    public static List<ModelNode> getMultiServerResults() {

        ModelNode success = new ModelNode();
        success.get("outcome").set("success");
        success.get("server-groups", "groupA", "serverA-1", "host").set("host1");
        success.get("server-groups", "groupA", "serverA-1", "response", "outcome").set("success");
        success.get("server-groups", "groupA", "serverA-1", "response", "result").set(new ModelNode());
        success.get("server-groups", "groupA", "serverA-2", "host").set("host2");
        success.get("server-groups", "groupA", "serverA-2", "response", "outcome").set("success");
        success.get("server-groups", "groupA", "serverA-2", "response", "result").set(new ModelNode());
        success.get("server-groups", "groupB", "serverB-1", "host").set("host1");
        success.get("server-groups", "groupB", "serverB-1", "response", "outcome").set("success");
        success.get("server-groups", "groupB", "serverB-1", "response", "result").set(new ModelNode());
        success.get("server-groups", "groupB", "serverB-2", "host").set("host2");
        success.get("server-groups", "groupB", "serverB-2", "response", "outcome").set("success");
        success.get("server-groups", "groupB", "serverB-2", "response", "result").set(new ModelNode());
        ModelNode op = new ModelNode();
        op.get("op").set("write-core-threads");
        op.get("op-addr").add("profile", "production");
        op.get("op-addr").add("subsystem", "threads");
        op.get("op-addr").add("bounded-queue-thread-pool", "pool1");
        op.get("count").set(5);
        op.get("per-cpu").set(5);
        success.get("compensating-op").set(op);

        ModelNode partial = new ModelNode();
        partial.get("outcome").set("success");
        partial.get("server-groups", "groupA", "serverA-1", "host").set("host1");
        partial.get("server-groups", "groupA", "serverA-1", "response", "outcome").set("success");
        partial.get("server-groups", "groupA", "serverA-1", "response", "result").set(new ModelNode());
        partial.get("server-groups", "groupA", "serverA-2", "host").set("host2");
        partial.get("server-groups", "groupA", "serverA-2", "response", "outcome").set("success");
        partial.get("server-groups", "groupA", "serverA-2", "response", "result").set(new ModelNode());
        partial.get("server-groups", "groupB", "serverB-1", "host").set("host1");
        partial.get("server-groups", "groupB", "serverB-1", "response", "outcome").set("failed");
        partial.get("server-groups", "groupB", "serverB-1", "response", "result").set(new ModelNode());
        partial.get("server-groups", "groupB", "serverB-1", "response", "rolled-back").set(true);
        partial.get("server-groups", "groupB", "serverB-2", "host").set("host2");
        partial.get("server-groups", "groupB", "serverB-2", "response", "outcome").set("failed");
        partial.get("server-groups", "groupB", "serverB-2", "response", "result").set(new ModelNode());
        partial.get("server-groups", "groupB", "serverB-2", "response", "rolled-back").set(true);
        partial.get("server-groups", "groupB", "serverB-3", "host").set("host3");
        partial.get("server-groups", "groupB", "serverB-3", "response", "outcome").set("failed");
        partial.get("server-groups", "groupB", "serverB-3", "response", "failure-description").set("[DOM-4556] Something didn't work right");
        partial.get("server-groups", "groupB", "serverB-3", "response", "rolled-back").set(true);
        partial.get("compensating-op").set(op);

        ModelNode failed = new ModelNode();
        failed.get("outcome").set("failed");
        failed.get("server-groups", "groupA", "serverA-1", "host").set("host1");
        failed.get("server-groups", "groupA", "serverA-1", "response", "outcome").set("success");
        failed.get("server-groups", "groupA", "serverA-1", "response", "result").set(new ModelNode());
        failed.get("server-groups", "groupA", "serverA-2", "host").set("host2");
        failed.get("server-groups", "groupA", "serverA-2", "response", "outcome").set("success");
        failed.get("server-groups", "groupA", "serverA-2", "response", "result").set(new ModelNode());
        failed.get("server-groups", "groupB", "serverB-1", "host").set("host1");
        failed.get("server-groups", "groupB", "serverB-1", "response", "outcome").set("failed");
        failed.get("server-groups", "groupB", "serverB-1", "response", "result").set(new ModelNode());
        failed.get("server-groups", "groupB", "serverB-1", "response", "rolled-back").set(true);
        failed.get("server-groups", "groupB", "serverB-2", "host").set("host2");
        failed.get("server-groups", "groupB", "serverB-2", "response", "outcome").set("failed");
        failed.get("server-groups", "groupB", "serverB-2", "response", "result").set(new ModelNode());
        failed.get("server-groups", "groupB", "serverB-2", "response", "rolled-back").set(true);
        failed.get("server-groups", "groupB", "serverB-3", "host").set("host3");
        failed.get("server-groups", "groupB", "serverB-3", "response", "outcome").set("failed");
        failed.get("server-groups", "groupB", "serverB-3", "response", "failure-description").set("[DOM-4556] Something didn't work right");
        failed.get("server-groups", "groupB", "serverB-3", "response", "rolled-back").set(true);
        failed.get("compensating-op").set(op);

        ModelNode domainFail = new ModelNode();
        domainFail.get("outcome").set("failed");
        domainFail.get("domain-failure-description").set("[DOM-3333] Failed to apply to the domain model");

        ModelNode hostFail = new ModelNode();
        hostFail.get("outcome").set("failed");
        hostFail.get("host-failure-descriptions", "hostA").set("[DOM-3333] Failed to apply to the domain model");
        hostFail.get("host-failure-descriptions", "hostB").set("[DOM-3333] Failed to apply to the domain model");


        return Arrays.asList(success, partial, failed, domainFail, hostFail);
    }

    public static void main(String[] args) {
//        ModelNode node = getUpdatePlan();
//        System.out.println(node.get("rollout-plan", "in-series").get(0).get("concurrent-groups").keys());
//        System.out.println(node);

//        List<ModelNode> results = getSimpleResults();
//        for (ModelNode result : results) {
//            System.out.println(result);
//            System.out.println();
//        }

        List<ModelNode> results = getMultiServerResults();
        for (ModelNode result : results) {
            System.out.println(result);
            System.out.println();
        }
    }

}
