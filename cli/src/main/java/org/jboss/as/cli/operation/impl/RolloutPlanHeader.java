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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.operation.OperationRequestHeader;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author Alexey Loubyansky
 */
public class RolloutPlanHeader implements OperationRequestHeader {

    private static final String HEADER_NAME = "rollout-plan";

    private final String planId;
    private final List<RolloutPlanGroup> groups = new ArrayList<RolloutPlanGroup>();
    private Map<String,String> props;

    public RolloutPlanHeader() {
        this(null);
    }

    public RolloutPlanHeader(String planId) {
        this.planId = planId;
    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.operation.OperationRequestHeader#getName()
     */
    @Override
    public String getName() {
        return HEADER_NAME;
    }

    public void addGroup(RolloutPlanGroup group) {
        if(group == null) {
            throw new IllegalArgumentException("group is null");
        }
        groups.add(group);
    }

    public void addConcurrentGroup(RolloutPlanGroup group) {
        if(group == null) {
            throw new IllegalArgumentException("group is null");
        }
        int lastIndex = groups.size() - 1;
        if(lastIndex < 0) {
            throw new IllegalStateException("There must be a group before a concurrent group can be added.");
        }
        RolloutPlanGroup lastGroup = groups.get(lastIndex);
        if(lastGroup instanceof ConcurrentRolloutPlanGroup) {
            ((ConcurrentRolloutPlanGroup)lastGroup).addGroup(group);
        } else {
            ConcurrentRolloutPlanGroup concurrent = new ConcurrentRolloutPlanGroup();
            concurrent.addGroup(lastGroup);
            concurrent.addGroup(group);
            groups.set(lastIndex, concurrent);
        }
    }

    // TODO perhaps add a list of allowed properties and their values
    public void addProperty(String name, String value) {
        if(name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Invalid property name: " + name);
        }
        if(value == null || value.isEmpty()) {
            throw new IllegalArgumentException("Invalid property value: " + value);
        }
        if(props == null) {
            props = new HashMap<String,String>();
        }
        props.put(name, value);
    }

    public String getProperty(String name) {
        return props == null ? null : props.get(name);
    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.operation.OperationRequestHeader#toModelNode()
     */
    @Override
    public void addTo(ModelNode headers) throws CommandFormatException {

        ModelNode header = headers.get(HEADER_NAME);
        final ModelNode series = header.get(Util.IN_SERIES);
        for(RolloutPlanGroup group : groups) {
            group.addTo(series);
        }

        if(props != null) {
            for(String propName : props.keySet()) {
                header.get(propName).set(props.get(propName));
            }
        }
    }

/*    public static void main(String[] args) throws Exception {
        RolloutPlanHeader header = new RolloutPlanHeader();

        ConcurrentRolloutPlanGroup concurrent = new ConcurrentRolloutPlanGroup();
        SingleRolloutPlanGroup group = new SingleRolloutPlanGroup("groupA");
        group.addProperty("rolling-to-servers", "true");
        group.addProperty("max-failure-percentage", "20");
        concurrent.addGroup(group);
        concurrent.addGroup(new SingleRolloutPlanGroup("groupB"));
        header.addGroup(concurrent);

        header.addGroup(new SingleRolloutPlanGroup("groupC"));

        concurrent = new ConcurrentRolloutPlanGroup();
        group = new SingleRolloutPlanGroup("groupD");
        group.addProperty("rolling-to-servers", "true");
        group.addProperty("max-failure-percentage", "20");
        concurrent.addGroup(group);
        concurrent.addGroup(new SingleRolloutPlanGroup("groupE"));
        header.addGroup(concurrent);

//        header.addProperty("rollback-across-groups", "true");

    }
*/}
