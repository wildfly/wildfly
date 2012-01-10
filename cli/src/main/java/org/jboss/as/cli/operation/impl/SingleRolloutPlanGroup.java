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

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.Util;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author Alexey Loubyansky
 */
public class SingleRolloutPlanGroup implements RolloutPlanGroup {

    private static final int SEPARATOR_PROPERTY_LIST_START = 1;
    private static final int SEPARATOR_PROPERTY_LIST_END = 2;
    private static final int SEPARATOR_PROPERTY = 3;

    private String groupName;
    private Map<String,String> props;

    private int lastSeparatorIndex;
    private int separator;

    public SingleRolloutPlanGroup() {
    }

    public SingleRolloutPlanGroup(String groupName) {
        if(groupName == null || groupName.isEmpty()) {
            throw new IllegalArgumentException("Invalid group name: " + groupName);
        }
        this.groupName = groupName;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    // TODO perhaps add a list of allowed properties and their values
    public void addProperty(String name, String value, int nameValueSep) {
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
        lastSeparatorIndex = nameValueSep;
    }

    public boolean hasProperties() {
        return props != null;
    }

    public void propertyListStart(int index) {
        this.lastSeparatorIndex = index;
        separator = SEPARATOR_PROPERTY_LIST_START;
    }

    public boolean endsOnPropertyListStart() {
        return separator == SEPARATOR_PROPERTY_LIST_START;
    }

    public void propertyListEnd(int index) {
        this.lastSeparatorIndex = index;
        separator = SEPARATOR_PROPERTY_LIST_END;
    }

    public boolean endsOnPropertyListEnd() {
        return separator == SEPARATOR_PROPERTY_LIST_END;
    }

    public int getLastSeparatorIndex() {
        return lastSeparatorIndex;
    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.operation.impl.RolloutPlanGroup#toModelNode()
     */
    @Override
    public ModelNode toModelNode() throws CommandFormatException {
        ModelNode node = new ModelNode();
        if(props != null) {
            for(String propName : props.keySet()) {
                node.get(propName).set(props.get(propName));
            }
        }
        return node;
    }

    @Override
    public void addTo(ModelNode inSeries) throws CommandFormatException {
        inSeries.add().get(Util.SERVER_GROUP).get(this.groupName).set(toModelNode());
    }

/*    public static void main(String[] args) throws Exception {

        SingleRolloutPlanGroup group = new SingleRolloutPlanGroup("groupA");
        group.addProperty("rolling-to-servers", "true");
        group.addProperty("max-failed-servers", "1");
        group.addProperty("max-failure-percentage", "20");
        System.out.println(group.toModelNode());
    }
*/}
