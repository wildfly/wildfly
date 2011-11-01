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
package org.jboss.as.cli.operation.impl;

import java.util.ArrayList;
import java.util.List;

import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.Util;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author Alexey Loubyansky
 */
public class ConcurrentRolloutPlanGroup implements RolloutPlanGroup {

    private final List<SingleRolloutPlanGroup> groups = new ArrayList<SingleRolloutPlanGroup>();

    public void addGroup(RolloutPlanGroup group) {
        if(group == null) {
            throw new IllegalArgumentException("group is null");
        }
        if(!(group instanceof SingleRolloutPlanGroup)) {
            throw new IllegalArgumentException("Expected a single group but got " + group);
        }
        groups.add((SingleRolloutPlanGroup) group);
    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.operation.impl.RolloutPlanGroup#toModelNode()
     */
    @Override
    public ModelNode toModelNode() throws CommandFormatException {
        ModelNode node = new ModelNode();
        ModelNode groupsNode = node.get(Util.CONCURRENT_GROUPS);
        for(SingleRolloutPlanGroup group : groups) {
            groupsNode.get(group.getGroupName()).set(group.toModelNode());
        }
        return node;
    }

    @Override
    public void addTo(ModelNode inSeries) throws CommandFormatException {
        inSeries.add().set(toModelNode());
    }

/*    public static void main(String[] args) throws Exception {
        ConcurrentRolloutPlanGroup concurrent = new ConcurrentRolloutPlanGroup();

        SingleRolloutPlanGroup group = new SingleRolloutPlanGroup("groupA");
        group.addProperty("rolling-to-servers", "true");
        group.addProperty("max-failure-percentage", "20");
        concurrent.addGroup(group);

        concurrent.addGroup(new SingleRolloutPlanGroup("groupB"));

        System.out.println(concurrent.toModelNode());
    }
*/}
