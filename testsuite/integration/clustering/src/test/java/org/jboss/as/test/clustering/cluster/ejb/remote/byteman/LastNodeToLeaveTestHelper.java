/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.ejb.remote.byteman;

import org.jboss.byteman.rule.helper.Helper;
import org.jboss.byteman.rule.Rule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class LastNodeToLeaveTestHelper extends Helper {

    private static final String NODE_LIST_MAP_NAME = "nodeListMap";
    private static final String STARTED_NODES_MAP_NAME = "startedNodesMap";
    private static final String STARTED_NODES_SET_NAME = "startedNodesSet";

    public LastNodeToLeaveTestHelper(Rule rule) {
        super(rule);
    }

    /*
     * Create a map to store DNR data: one List per thread
     *
     * Sample rule:
     *
     * @BMRule(name = "Set up results linkMap (SETUP)",
     *   targetClass = "org.jboss.ejb.protocol.remote.RemotingEJBDiscoveryProvider",
     *   targetMethod = "<init>",
     *   helper = "org.jboss.as.test.clustering.cluster.ejb.remote.byteman.LastNodeToLeaveTestHelper",
     *   targetLocation = "EXIT",
     *   condition = "debug(\" setting up the map \")",
     *   action = "createNodeListMap();"),
     */

    public void createNodeListMap() {
        createLinkMap(NODE_LIST_MAP_NAME);
        createLinkMap(STARTED_NODES_MAP_NAME);
    }

    /*
     * Create a map to store started nodes
     *
     * Sample rule:
     *
     */

    public void updateStartedNodes(Set<String> startedNodes) {
        link(STARTED_NODES_MAP_NAME,STARTED_NODES_SET_NAME, startedNodes);
    }

    /*
     * Add node data current at the time of invocation to the list
     *
     * Sample rule:
     *
     * @BMRule(name = "Track calls to ClusterNodeSelector (COLLECT)",
     *   isInterface = false,
     *   targetClass = "LastNodeToLeaveRemoteEJBTestCase$CustomClusterNodeSelector",
     *   targetMethod = "selectNode",
     *   helper = "org.jboss.as.test.clustering.cluster.ejb.remote.byteman.LastNodeToLeaveTestHelper",
     *   binding = "clusterName : String = $1;connectedNodes : String[] = $2; totalAvailableNodes : String[] = $3;",
     *   condition = "debug(\"checking the condition\")",
     *   action = "addConnectedNodesEntryForThread(clusterName, connectedNodes, totalAvailableNodes);"),
     */

    @SuppressWarnings("unchecked")
    public void addConnectedNodesEntryForThread(String clusterName, String[] connectedNodes, String[] totalAvailableNodes) {
        String threadName = Thread.currentThread().getName();
        Set<String> connectedNodesSet = Arrays.asList(connectedNodes).stream().collect(Collectors.toSet());
        Set<String> totalNodesSet = Arrays.asList(totalAvailableNodes).stream().collect(Collectors.toSet());
        Set<String> startedNodesSet = (Set<String>) linked(STARTED_NODES_MAP_NAME, STARTED_NODES_SET_NAME);

        List<Set<String>> nodesEntry = new ArrayList<>();
        nodesEntry.add(startedNodesSet);
        nodesEntry.add(connectedNodesSet);
        nodesEntry.add(totalNodesSet);

        List<List<Set<String>>> threadNodesEntryList = (List<List<Set<String>>>) linked(NODE_LIST_MAP_NAME, threadName);
        if (threadNodesEntryList == null) {
            threadNodesEntryList = new ArrayList<>();
        }
        threadNodesEntryList.add(nodesEntry);
        link(NODE_LIST_MAP_NAME, threadName, threadNodesEntryList);
    }

    /*
     * Add node data current at the time of invocation to the list
     *
     * Sample rule:
     *
     * @BMRule(name = "Track current state of nodes used for discovery (COLLECT)",
     *   targetClass = "org.jboss.ejb.protocol.remote.RemotingEJBDiscoveryProvider",
     *   targetMethod = "discover",
     *   helper = "org.jboss.as.test.clustering.cluster.ejb.remote.byteman.LastNodeToLeaveTestHelper",
     *   binding = "provider : RemotingEJBDiscoveryProvider = $0",
     *   condition = "debug(\"checking the condition\")",
     *   action = "addDNREntryForThread(provider.nodes.keySet());"),
     */

    @SuppressWarnings("unchecked")
    public void addDNREntryForThread(Set<String> newNodeList) {
        String threadName = Thread.currentThread().getName();
        List<Set<String>> threadNodeList = (List<Set<String>>) linked(NODE_LIST_MAP_NAME, threadName);
        if (threadNodeList == null) {
            threadNodeList = new ArrayList<>();
        }
        threadNodeList.add(newNodeList);
        link(NODE_LIST_MAP_NAME, threadName, threadNodeList);
    }

    /*
     * Retrieve the node data from the rule and pass to the test case
     */
    @SuppressWarnings("unchecked")
    public Map<String,List<List<Set<String>>>> getNodeListMap() {
        System.out.println("*** Getting results");
        ConcurrentHashMap<String, List<List<Set<String>>>> results = new ConcurrentHashMap<>();
        for (Object threadNameAsObject : linkNames(NODE_LIST_MAP_NAME)) {
            String threadNameAsString = (String) threadNameAsObject;
            List<List<Set<String>>> threadValues = (List<List<Set<String>>>) linked(NODE_LIST_MAP_NAME, threadNameAsString);
            results.put(threadNameAsString, threadValues);
        }
        return results;
    }
}
