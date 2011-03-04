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
package org.jboss.as.cli.completion.mock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
*
* @author Alexey Loubyansky
*/
public class MockNode {
    private final String name;
    private Map<String, MockNode> children;
    private Map<String, MockOperation> operations;

    public MockNode(String name) {
        this.name = name;
    }

    public MockNode remove(String name) {
        return children == null ? null : children.remove(name);
    }

    public MockNode getChild(String name) {
        return children == null ? null : children.get(name);
    }

    public MockNode addChild(String name) {
        MockNode child = new MockNode(name);
        if(children == null) {
            children = new HashMap<String, MockNode>();
        }
        children.put(name, child);
        return child;
    }

    public void addChild(MockNode child) {
        if(children == null) {
            children = new HashMap<String, MockNode>();
        }
        children.put(child.name, child);
    }

    public List<String> getChildNames() {
        return children == null ? Collections.<String>emptyList() : new ArrayList<String>(children.keySet());
    }

    public Set<String> getOperationNames() {
        return operations == null ? Collections.<String>emptySet() : operations.keySet();
    }

    public MockOperation getOperation(String name) {
        return operations == null ? null : operations.get(name);
    }

    public void addOperation(MockOperation operation) {
        if(operations == null) {
            operations = new HashMap<String, MockOperation>();
        }
        operations.put(operation.getName(), operation);
    }
}