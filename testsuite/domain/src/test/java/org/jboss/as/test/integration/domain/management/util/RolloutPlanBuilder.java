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
package org.jboss.as.test.integration.domain.management.util;

import java.util.ArrayList;
import java.util.Map;
import org.jboss.dmr.ModelNode;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.IN_SERIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLING_TO_SERVERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLBACK_ACROSS_GROUPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MAX_FAILURE_PERCENTAGE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MAX_FAILED_SERVERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONCURRENT_GROUPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLOUT_PLAN;


/**
 *
 * Helper class building rollout plan model node.
 * 
 * @author Dominik Pospisil <dpospisi@redhat.com>
 */
public class RolloutPlanBuilder {

    ArrayList series = new ArrayList();
    
    public static class RolloutPolicy {
        private final boolean rollingToServers;
        private final Integer maxFailurePercentage;
        private final Integer maxFailedServers;
        
        public RolloutPolicy(boolean rollingToServers, Integer maxFailurePercentage, Integer maxFailedServers) {
            this.rollingToServers = rollingToServers;
            this.maxFailurePercentage = maxFailurePercentage;
            this.maxFailedServers = maxFailedServers;
        }                
        
        public ModelNode toModelNode() {
            ModelNode node = new ModelNode();
            
            node.get(ROLLING_TO_SERVERS).set(rollingToServers);
            if (maxFailedServers != null) node.get(MAX_FAILED_SERVERS).set(maxFailedServers);
            if (maxFailurePercentage != null) node.get(MAX_FAILURE_PERCENTAGE).set(maxFailurePercentage);
            
            return node;
        }
        
    }
    
    private boolean rollBackAcrossGroups = false;

    public void addGroup(String groupName, RolloutPolicy policy) {
        series.add(new Object[] {groupName, policy});
    }
    
    public void addConcurrentGroups(Map<String, RolloutPolicy> concurrentGroups) {
        series.add(concurrentGroups);
    }
    
    public String buildAsString() {
        ModelNode plan = build();
        String planString = plan.toString();
        planString = planString.replace(System.getProperty("line.separator"), " ");
        return planString;
        
    }
    public ModelNode build() {
        ModelNode plan = new ModelNode();
        
        for (Object step : series) {
            if (step instanceof Object[]) {
                // single group
                Object[] pair = (Object[]) step;
                String groupName = (String) pair[0];
                RolloutPolicy policy = (RolloutPolicy) pair[1];
                ModelNode group = new ModelNode();
                group.get(groupName).set(policy.toModelNode());
                ModelNode serverGroup = new ModelNode();
                serverGroup.get(SERVER_GROUP).set(group);
                plan.get(IN_SERIES).add(serverGroup);
            } else {
                // concurrent groups
                Map<String, RolloutPolicy> concurrentGroups = (Map<String, RolloutPolicy>) step;
                ModelNode groups = new ModelNode();
                for(String groupName : concurrentGroups.keySet()) {
                    ModelNode group = new ModelNode();
                    group.get(groupName).set(concurrentGroups.get(groupName).toModelNode());
                    groups.get(groupName).set(group);
                }
                ModelNode concurrentGroupsNode = new ModelNode();
                concurrentGroupsNode.get(CONCURRENT_GROUPS).set(groups);
                plan.get(IN_SERIES).add(concurrentGroupsNode);
            }
            
        }
        
        plan.get(ROLLBACK_ACROSS_GROUPS).set(rollBackAcrossGroups);
        
        ModelNode rolloutPlan = new ModelNode();
        rolloutPlan.get(ROLLOUT_PLAN).set(plan);
        
        return rolloutPlan;
    }
    
    /**
     * @param rollBackAcrossGroups the rollBackAcrossGroups to set
     */
    public void setRollBackAcrossGroups(boolean rollBackAcrossGroups) {
        this.rollBackAcrossGroups = rollBackAcrossGroups;
    }
        
}
