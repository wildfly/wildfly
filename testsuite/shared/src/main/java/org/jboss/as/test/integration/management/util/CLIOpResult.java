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
package org.jboss.as.test.integration.management.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;


/**
 *
 * @author Dominik Pospisil <dpospisi@redhat.com>
 */
public class CLIOpResult {

    private boolean isOutcomeSuccess;
    private Object result;
    private Object serverGroups;

    public CLIOpResult() {}

    public CLIOpResult(ModelNode node) {
        final Map<String, Object> map = toMap(node);
        isOutcomeSuccess = "success".equals(map.get("outcome"));
        result = map.get("result");
        serverGroups = map.get("server-groups");
    }

    protected Map<String, Object> toMap(ModelNode node) {
        final Set<String> keys = node.keys();
        Map<String,Object> map = new HashMap<String,Object>(keys.size());
        for(String key : keys) {
            map.put(key, toObject(node.get(key)));
        }
        return map;
    }

    protected List<Object> toList(ModelNode node) {
        final List<ModelNode> nodeList = node.asList();
        final List<Object> list = new ArrayList<Object>(nodeList.size());
        for(ModelNode item : nodeList) {
            list.add(toObject(item));
        }
        return list;
    }

    protected Object toObject(ModelNode node) {
        final ModelType type = node.getType();
        if(type.equals(ModelType.LIST)) {
            return toList(node);
        } else if(type.equals(ModelType.OBJECT)) {
            return toMap(node);
        } else if(type.equals(ModelType.PROPERTY)) {
            final Property prop = node.asProperty();
            return Collections.singletonMap(prop.getName(), toObject(prop.getValue()));
        } else {
            return node.asString();
        }
    }

    public boolean isIsOutcomeSuccess() {
        return isOutcomeSuccess;
    }

    public void setIsOutcomeSuccess(boolean isOutcomeSuccess) {
        this.isOutcomeSuccess = isOutcomeSuccess;
    }

    public Object getResult() {
        return result;
    }

    /**
     * Get the result as map or null if the result is not a map
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getResultAsMap() {
        return (Map<String, Object>) (result instanceof Map ? result : null);
    }

    /**
     * Get a named entry from the result map or null
     */
    public Object getNamedResult(String key) {
        Map<String, Object> map = getResultAsMap();
        return map != null ? map.get(key) : null;
    }

    /**
     * Get the named result as map or null if the named result value is not a map
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getNamedResultAsMap(String key) {
        Object value = getNamedResult(key);
        return (Map<String, Object>) (value instanceof Map ? value : null);
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public Object getServerGroups() {
        return serverGroups;
    }

    public void setServerGroups(Object serverGroups) {
        this.serverGroups = serverGroups;
    }
}
