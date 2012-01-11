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
    private static final int SEPARATOR_PROPERTY_VALUE = 3;
    private static final int SEPARATOR_PROPERTY = 4;

    private String groupName;
    private Map<String,String> props;

    private int lastSeparatorIndex;
    private int separator;
    private int lastChunkIndex;

    private String lastPropertyName;
    private String lastPropertyValue;

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

    public void setGroupName(String groupName, int index) {
        this.groupName = groupName;
        this.lastChunkIndex = index;
    }

    public int getLastChunkIndex() {
        return lastChunkIndex;
    }

    // TODO perhaps add a list of allowed properties and their values
    public void addProperty(String name, String value, int valueIndex) {
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
        this.lastPropertyName = name;
        this.lastPropertyValue = value;
        this.lastChunkIndex = valueIndex;
        separator = -1;
    }

    public void addProperty(String name, int index) {
        this.lastPropertyName = name;
        this.lastChunkIndex = index;
        separator = -1;
    }

    public void propertyValueSeparator(int index) {
        separator = SEPARATOR_PROPERTY_VALUE;
        this.lastSeparatorIndex = index;
    }

    public void propertySeparator(int index) {
        separator = SEPARATOR_PROPERTY;
        this.lastSeparatorIndex = index;
        this.lastPropertyName = null;
        this.lastPropertyValue = null;
    }

    public boolean hasProperties() {
        return lastPropertyName != null || props != null;
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
        this.lastPropertyName = null;
        this.lastPropertyValue = null;
    }

    public boolean endsOnPropertyListEnd() {
        return separator == SEPARATOR_PROPERTY_LIST_END;
    }

    public boolean endsOnPropertyValueSeparator() {
        return separator == SEPARATOR_PROPERTY_VALUE;
    }

    public boolean endsOnPropertySeparator() {
        return separator == SEPARATOR_PROPERTY;
    }

    public int getLastSeparatorIndex() {
        return lastSeparatorIndex;
    }

    public String getLastPropertyName() {
        return lastPropertyName;
    }

    public String getLastPropertyValue() {
        return lastPropertyValue;
    }

    public boolean hasProperty(String name) {
        return props == null ? false : props.containsKey(name);
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
