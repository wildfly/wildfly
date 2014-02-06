package org.wildfly.clustering.diagnostics.extension;

import java.util.Map;

import org.jboss.as.controller.AbstractRuntimeOnlyHandler;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.diagnostics.services.cache.CacheState;

/**
 * Base class for handlers reading run-time only attributes from an underlying cache service.
 *
 * @author Richard Achmatowicz (c) 2013 Red Hat Inc.
 */
public abstract class CacheBasedMetricsHandler extends AbstractRuntimeOnlyHandler {

    // JSON string formats for results
    private final String DISTRIBUTION_STATS = "{ \"cache-entries\":\"%s\" }";
    private final String OPERATION_STATS = "{ \"get-hits\":\"%s\", \"get-misses\":\"%s\", \"puts\":\"%s\", \"remove-hits\":\"%s\", \"remove-misses\":\"%s\" }";
    private final String RPC_STATS = "{ \"RPC count\":\"%s\", \"RPC failures\":\"%s\" }";
    private final String TXN_STATS = "{ \"prepares\":\"%s\", \"commits\":\"%s\" , \"rollbacks\":\"%s\" }";

    /*
     * NOTE: for {A,B,C} if a cache is undeployed on B, we do not want the replies displayed
     * on A and C to refer to B. We use rsp.isInView() for this.
     */
    protected ModelNode createCacheView(Map<Node, CacheState> states) {
        ModelNode result = new ModelNode().setEmptyList();
        for (Map.Entry<Node, CacheState> entry: states.entrySet()) {
            Node node = entry.getKey();
            CacheState state = entry.getValue();
            // create a NODE_RESULT object
            ModelNode object = result.addEmptyObject();
            object.get(ModelKeys.NODE_NAME).set(node.getName());
            object.get(ModelKeys.NODE_VALUE).set(state.getView());
        }
        return result;
    }

    protected ModelNode createDistribution(Map<Node, CacheState> states) {
        ModelNode result = new ModelNode().setEmptyList();
        for (Map.Entry<Node, CacheState> entry: states.entrySet()) {
            Node node = entry.getKey();
            CacheState state = entry.getValue();
            String JSONString = String.format(DISTRIBUTION_STATS, state.getEntries());
            ModelNode stats = ModelNode.fromJSONString(JSONString);
            // create a NODE_RESULT object
            ModelNode object = result.addEmptyObject();
            object.get(ModelKeys.NODE_NAME).set(node.getName());
            object.get(ModelKeys.NODE_VALUE).set(stats);
        }
        return result;
    }

    protected ModelNode createOperationStats(Map<Node, CacheState> states) {
        ModelNode result = new ModelNode().setEmptyList();
        for (Map.Entry<Node, CacheState> entry: states.entrySet()) {
            Node node = entry.getKey();
            CacheState state = entry.getValue();
            String JSONString = String.format(OPERATION_STATS, state.getHits(), state.getMisses(), state.getStores(), state.getRemoveHits(), state.getRemoveMisses());
            ModelNode stats = ModelNode.fromJSONString(JSONString);
            // create a NODE_RESULT object
            ModelNode object = result.addEmptyObject();
            object.get(ModelKeys.NODE_NAME).set(node.getName());
            object.get(ModelKeys.NODE_VALUE).set(stats);
        }
        return result;
    }

    protected ModelNode createRpcStats(Map<Node, CacheState> states) {
        ModelNode result = new ModelNode().setEmptyList();
        for (Map.Entry<Node, CacheState> entry: states.entrySet()) {
            Node node = entry.getKey();
            CacheState state = entry.getValue();
            String JSONString = String.format(RPC_STATS, state.getRpcCount(), state.getRpcFailures());
            ModelNode stats = ModelNode.fromJSONString(JSONString);
            // create a NODE_RESULT object
            ModelNode object = result.addEmptyObject();
            object.get(ModelKeys.NODE_NAME).set(node.getName());
            object.get(ModelKeys.NODE_VALUE).set(stats);
        }
        return result;
    }

    protected ModelNode createTxnStats(Map<Node, CacheState> states) {
        ModelNode result = new ModelNode().setEmptyList();
        for (Map.Entry<Node, CacheState> entry: states.entrySet()) {
            Node node = entry.getKey();
            CacheState state = entry.getValue();
            String JSONString = String.format(TXN_STATS, state.getPrepares(), state.getCommits(), state.getRollbacks());
            ModelNode stats = ModelNode.fromJSONString(JSONString);
            // create a NODE_RESULT object
            ModelNode object = result.addEmptyObject();
            object.get(ModelKeys.NODE_NAME).set(node.getName());
            object.get(ModelKeys.NODE_VALUE).set(stats);
        }
        return result;
    }
}
