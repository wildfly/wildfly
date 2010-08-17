/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.naming;

import org.jboss.as.naming.util.FastCopyHashMap;

import javax.naming.Binding;
import javax.naming.CannotProceedException;
import javax.naming.CompositeName;
import javax.naming.Context;
import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NameClassPair;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.NotContextException;
import javax.naming.Reference;
import javax.naming.spi.ResolveResult;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

/**
 * In-memory implementation of the NamingStore.  The backing for the entries is a basic tree structure with either context
 * nodes or binding nodes.  The context nodes are allowed to have children and can be represented by a NamingContext.  A
 * binding node is only allowed to have a normal object binding.
 *
 * @author John E. Bailey
 */
public class InMemoryNamingStore implements NamingStore {

    /**
     * The root node of the tree.  Represents a JNDI name of ""
     */
    private final ContextNode root = new ContextNode(new CompositeName(), null);

    private transient SecurityManager securityManager;

    /**
     * Bind an entry into the tree.  This will create a binding node in the tree for the provided named object.
     *
     * @param name      The entry name
     * @param object    The entry object
     * @param className The entry class name
     * @throws NamingException
     */
    public void bind(final Name name, final Object object, final String className) throws NamingException {
        if (name.isEmpty() || getLastComponent(name).equals("")) {
            throw emptyName();
        }
        checkPermissions(name, JndiPermission.Action.BIND);

        final ContextNode bindContextNode = getBindingContext(name);

        final Binding binding = new Binding(getLastComponent(name), className, object, true);
        final BindingNode bindingNode = new BindingNode(name, binding);
        bindContextNode.addChild(getLastComponent(name), bindingNode);
    }

    /**
     * Replace an existing entry in the tree.  This will create a new binding node in the tree and no longer store
     * the previous value.
     *
     * @param name      The entry name
     * @param object    The entry object
     * @param className The entry class name
     * @throws NamingException
     */
    public void rebind(final Name name, final Object object, final String className) throws NamingException {
        if (name.isEmpty() || getLastComponent(name).equals("")) {
            throw emptyName();
        }
        checkPermissions(name, JndiPermission.Action.REBIND);

        final ContextNode bindContextNode = getBindingContext(name);
        final Binding binding = new Binding(getLastComponent(name), className, object, true);
        final BindingNode bindingNode = new BindingNode(name, binding);
        bindContextNode.replaceChild(getLastComponent(name), bindingNode);
    }

    /**
     * Unbind the entry in the provided location.  This will remove the node in the tree and no longer manage it.
     *
     * @param name The entry name
     * @throws NamingException
     */
    public void unbind(final Name name) throws NamingException {
        if (name.isEmpty() || getLastComponent(name).equals("")) {
            throw emptyName();
        }
        checkPermissions(name, JndiPermission.Action.UNBIND);
        final ContextNode bindContextNode = getBindingContext(name);
        bindContextNode.removeChild(getLastComponent(name));
    }

    /**
     * Lookup the object value of a binding node in the tree.
     *
     * @param name The entry name
     * @return The object value of the binding
     * @throws NamingException
     */
    public Object lookup(final Name name) throws NamingException {
        if (name.isEmpty() || (name.size() == 1 && "".equals(name.get(0)))) {
            final Name emptyName = new CompositeName("");
            checkPermissions(emptyName, JndiPermission.Action.LOOKUP);
            return new NamingContext(emptyName, this, new Hashtable<String, Object>());
        }
        checkPermissions(name, JndiPermission.Action.LOOKUP);
        return findObject(name);
    }

    /**
     * List all NameClassPair instances at a given location in the tree.
     *
     * @param name The entry name
     * @return The NameClassPair instances
     * @throws NamingException
     */
    public List<NameClassPair> list(final Name name) throws NamingException {
        checkPermissions(name, JndiPermission.Action.LIST);
        final Name nodeName = name.isEmpty() ? new CompositeName("") : name;
        final TreeNode node = findNode(nodeName);
        if (node instanceof BindingNode) {
            checkReferenceForContinuation(new CompositeName(""), node.binding.getObject());
            throw notAContext(name);
        }
        final ContextNode contextNode = ContextNode.class.cast(node);

        final List<NameClassPair> nameClassPairs = new ArrayList<NameClassPair>();
        for (TreeNode childNode : contextNode.children.values()) {
            final Binding binding = childNode.binding;
            NameClassPair nameClassPair = new NameClassPair(binding.getName(), binding.getClassName(), true);
            nameClassPairs.add(nameClassPair);
        }
        return nameClassPairs;
    }

    /**
     * List all the Binding instances at a given location in the tree.
     *
     * @param name The entry name
     * @return The Binding instances
     * @throws NamingException
     */
    public List<Binding> listBindings(final Name name) throws NamingException {
        checkPermissions(name, JndiPermission.Action.LIST_BINDINGS);
        final Name nodeName = name.isEmpty() ? new CompositeName("") : name;
        final TreeNode node = findNode(nodeName);
        if (node instanceof BindingNode) {
            checkReferenceForContinuation(new CompositeName(""), node.binding.getObject());
            throw notAContext(name);
        }
        final ContextNode contextNode = ContextNode.class.cast(node);

        final List<Binding> bindings = new ArrayList<Binding>();
        for (TreeNode childNode : contextNode.children.values()) {
            final Binding binding = childNode.binding;
            bindings.add(binding);
        }
        return bindings;
    }

    /**
     * Close the store.  This will clear all children from the root node.
     *
     * @throws NamingException
     */
    public void close() throws NamingException {
        root.clear();
    }

    /**
     * Create a context node at the give location in the tree.
     *
     * @param name The entry name
     * @return The new context
     * @throws NamingException
     */
    public Context createSubcontext(final Name name) throws NamingException {
        if (name.isEmpty()) {
            throw emptyName();
        }
        checkPermissions(name, JndiPermission.Action.CREATE_SUBCONTEXT);

        final ContextNode bindingContextNode = getBindingContext(name);

        final NamingContext context = new NamingContext(name, this, new Hashtable<String, Object>());
        bindingContextNode.addChild(getLastComponent(name), new ContextNode(name, context));
        return context;
    }

    private TreeNode findNode(final Name name) throws NamingException {
        Name currentName = name;
        ContextNode currentNode = root;
        for(int i = 0; i < name.size(); i++) {
            String childName = currentName.get(0);
            final TreeNode childNode = currentNode.children.get(childName);
            currentName = currentName.getSuffix(1);

            if(childNode == null) {
                throw nameNotFound(childName, currentName);
            } else if(currentName.isEmpty()) {
                return childNode;
            } else if(childNode instanceof ContextNode) {
                currentNode = ContextNode.class.cast(childNode);
            } else {
                checkReferenceForContinuation(currentName, childNode.binding.getObject());
                throw notAContext(name);
            }
        }
        return currentNode;
    }

    private ContextNode getBindingContext(final Name name) throws NamingException {
        final Name bindingContextName = name.getPrefix(name.size() - 1);
        final TreeNode node = findNode(bindingContextName);
        if (node instanceof ContextNode) {
            return ContextNode.class.cast(node);
        } else {
            checkReferenceForContinuation(name.getSuffix(1), node.binding.getObject());
            throw notAContext(bindingContextName);
        }

    }

    private Object findObject(final Name name) throws NamingException {
        Name currentName = name;
        ContextNode currentNode = root;
        for(int i = 0; i < name.size(); i++) {
            String childName = currentName.get(0);
            final TreeNode childNode = currentNode.children.get(childName);
            currentName = currentName.getSuffix(1);
            if(childNode == null) {
                throw nameNotFound(childName, currentName);
            } else if(currentName.isEmpty()) {
                return childNode.binding.getObject();
            } else if(childNode instanceof ContextNode) {
                currentNode = ContextNode.class.cast(childNode);
            } else {
                final Object boundObject = childNode.binding.getObject();
                if(boundObject instanceof Reference) {
                    checkReferenceForContinuation(currentName, boundObject);
                    return new ResolveResult(boundObject, currentName);
                }
                throw notAContext(name);
            }
        }
        return currentNode;
    }

    private void checkReferenceForContinuation(final Name name, final Object object) throws CannotProceedException {
        if (object instanceof Reference) {
            if (Reference.class.cast(object).get("nns") != null) {
                CannotProceedException cpe = new CannotProceedException();
                cpe.setResolvedObj(object);
                cpe.setRemainingName(name);
                throw cpe;
            }
        }
    }

    private void checkPermissions(final Name name, JndiPermission.Action permission) {
        if (securityManager == null)
            securityManager = System.getSecurityManager();

        if (securityManager != null) {
            securityManager.checkPermission(new JndiPermission(name, permission));
        }
    }

    private String getLastComponent(final Name name) {
        if(name.size() > 0)
            return name.get(name.size() - 1);
        return "";
    }

    private NameNotFoundException nameNotFound(final String name, final Name contextName) {
        return new NameNotFoundException(String.format("Name '%s' not found in context '%s'", name, contextName.toString()));
    }

    private NameAlreadyBoundException nameAlreadyBound(final Name name) throws NameAlreadyBoundException {
        throw new NameAlreadyBoundException(name.toString());
    }

    private InvalidNameException emptyName() {
        return new InvalidNameException("An empty name is not allowed");
    }

    private NotContextException notAContext(Name name) {
        return new NotContextException(name.toString());
    }

    private abstract class TreeNode {
        protected final Name fullName;
        protected final Binding binding;

        private TreeNode(final Name fullName, final Binding binding) {
            this.fullName = fullName;
            this.binding = binding;
        }
    }

    private class ContextNode extends TreeNode {
        private volatile Map<String, TreeNode> children = Collections.emptyMap();

        private ContextNode(final Name fullName, final NamingContext context) {
            super(fullName, new Binding(getLastComponent(fullName), Context.class.getName(), context));
        }

        private synchronized void addChild(final String childName, final TreeNode childNode) throws NamingException {
            if(children.containsKey(childName)) {
                throw nameAlreadyBound(fullName.add(childName));
            }
            final Map<String, TreeNode> copy = new FastCopyHashMap<String, TreeNode>(children);
            copy.put(childName, childNode);
            children = copy;
        }

        private synchronized TreeNode replaceChild(final String childName, final TreeNode childNode) throws NamingException {
            final Map<String, TreeNode> copy = new FastCopyHashMap<String, TreeNode>(children);
            final TreeNode existing = copy.put(childName, childNode);
            children = copy;
            return existing;
        }

        private synchronized TreeNode removeChild(final String childName) throws NameNotFoundException {
            if(!children.containsKey(childName)) {
                throw nameNotFound(childName, fullName);
            }
            final Map<String, TreeNode> copy = new FastCopyHashMap<String, TreeNode>(children);
            final TreeNode existing = copy.remove(childName);
            children = copy;
            return existing;
        }

        private synchronized void clear() {
            final Map<String, TreeNode> copy = new FastCopyHashMap<String, TreeNode>(children);
            copy.clear();
            children = copy;
        }
    }

    private class BindingNode extends TreeNode {
        private BindingNode(final Name fullName, final Binding binding) {
            super(fullName, binding);
        }
    }
}
