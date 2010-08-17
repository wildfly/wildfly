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
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

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
        checkPermissions(name, JndiPermission.BIND);

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
        checkPermissions(name, JndiPermission.REBIND);

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
        checkPermissions(name, JndiPermission.UNBIND);
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
            checkPermissions(emptyName, JndiPermission.LOOKUP);
            return new NamingContext(emptyName, this, new Hashtable<String, Object>());
        }
        checkPermissions(name, JndiPermission.LOOKUP);
        final TreeNode boundNode = root.findNode(name);
        return boundNode.binding.getObject();
    }

    /**
     * List all NameClassPair instances at a given location in the tree.
     *
     * @param name The entry name
     * @return The NameClassPair instances
     * @throws NamingException
     */
    public List<NameClassPair> list(final Name name) throws NamingException {
        checkPermissions(name, JndiPermission.LIST);
        final Name nodeName = name.isEmpty() ? new CompositeName("") : name;
        final TreeNode node = root.findNode(nodeName);
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
        checkPermissions(name, JndiPermission.LIST_BINDINGS);
        final Name nodeName = name.isEmpty() ? new CompositeName("") : name;
        final TreeNode node = root.findNode(nodeName);
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
        root.children.clear();
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
        checkPermissions(name, JndiPermission.CREATE_SUBCONTEXT);

        final ContextNode bindingContextNode = getBindingContext(name);

        final NamingContext context = new NamingContext(name, this, new Hashtable<String, Object>());
        bindingContextNode.addChild(getLastComponent(name), new ContextNode(name, context));
        return context;
    }

    private ContextNode getBindingContext(final Name name) throws NamingException {
        final Name bindingContextName = name.getPrefix(name.size() - 1);
        final TreeNode node = root.findNode(bindingContextName);
        if (node instanceof ContextNode) {
            return ContextNode.class.cast(node);
        } else {
            checkReferenceForContinuation(name.getSuffix(1), node.binding.getObject());
            throw notAContext(bindingContextName);
        }

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

    private void checkPermissions(final Name name, int permission) {
        if (securityManager == null)
            securityManager = System.getSecurityManager();

        if (securityManager != null) {
            JndiPermission perm = new JndiPermission(name, permission);
            securityManager.checkPermission(perm);
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
        private final ConcurrentMap<String, TreeNode> children = new ConcurrentHashMap<String, TreeNode>();

        private ContextNode(final Name fullName, final NamingContext context) {
            super(fullName, new Binding(getLastComponent(fullName), Context.class.getName(), context));
        }

        private void addChild(final String childName, final TreeNode childNode) throws NamingException {
            if (children.putIfAbsent(childName, childNode) != null) {
                throw nameAlreadyBound(fullName.add(childName));
            }
        }

        private TreeNode replaceChild(final String childName, final TreeNode childNode) throws NamingException {
            return children.put(childName, childNode);
        }

        private TreeNode removeChild(final String childName) throws NameNotFoundException {
            final TreeNode existing = children.remove(childName);
            if (existing == null) {
                throw nameNotFound(childName, fullName);
            }
            return existing;
        }

        private TreeNode findNode(final Name name) throws NamingException {
            if (name.size() == 0) {
                return this;
            } else {
                final String childName = name.get(0);
                final TreeNode childNode = children.get(childName);
                if (childNode == null) {
                    throw nameNotFound(childName, fullName);
                } else if (childNode instanceof ContextNode) {
                    final ContextNode childContextNode = ContextNode.class.cast(childNode);
                    return childContextNode.findNode(name.getSuffix(1));
                } else {
                    if (name.size() == 1) {
                        return childNode;
                    } else {
                        checkReferenceForContinuation(name.getSuffix(1), childNode.binding.getObject());
                        throw notAContext(name);
                    }
                }
            }
        }
    }

    private class BindingNode extends TreeNode {
        private BindingNode(final Name fullName, final Binding binding) {
            super(fullName, binding);
        }
    }
}
