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
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.event.EventContext;
import javax.naming.event.NamingEvent;
import javax.naming.event.NamingListener;
import javax.naming.spi.ResolveResult;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import static org.jboss.as.naming.util.NamingUtils.asReference;
import static org.jboss.as.naming.util.NamingUtils.cannotProceedException;
import static org.jboss.as.naming.util.NamingUtils.emptyName;
import static org.jboss.as.naming.util.NamingUtils.emptyNameException;
import static org.jboss.as.naming.util.NamingUtils.getLastComponent;
import static org.jboss.as.naming.util.NamingUtils.isEmpty;
import static org.jboss.as.naming.util.NamingUtils.isLastComponentEmpty;
import static org.jboss.as.naming.util.NamingUtils.nameAlreadyBoundException;
import static org.jboss.as.naming.util.NamingUtils.nameNotFoundException;
import static org.jboss.as.naming.util.NamingUtils.notAContextException;

/**
 * In-memory implementation of the NamingStore.  The backing for the entries is a basic tree structure with either context
 * nodes or binding nodes.  The context nodes are allowed to have children and can be represented by a NamingContext.  A
 * binding node is only allowed to have a normal object binding.
 *
 * @author John E. Bailey
 */
public class InMemoryNamingStore implements NamingStore {

    /* The root node of the tree.  Represents a JNDI name of "" */
    private final ContextNode root = new ContextNode(emptyName(), null);

    /* Cached security namanger */
    private transient SecurityManager securityManager;

    /* Naming Event Coordinator */
    private final NamingEventCoordinator eventCoordinator;

    /**
     * Construct instance with no event support.
     */
    public InMemoryNamingStore() {
        this(null);
    }

    /**
     * Construct instance with an event coordinator.
     *
     * @param eventCoordinator The event coordinator
     */
    public InMemoryNamingStore(final NamingEventCoordinator eventCoordinator) {
        this.eventCoordinator = eventCoordinator;
    }

    /**
     * Bind an entry into the tree.  This will create a binding node in the tree for the provided named object.
     *
     * @param callingContext The calling context
     * @param name The entry name
     * @param object The entry object
     * @param className The entry class name
     * @throws NamingException
     */
    public void bind(final Context callingContext, final Name name, final Object object, final String className) throws NamingException {
        if (isLastComponentEmpty(name)) {
            throw emptyNameException();
        }
        checkPermissions(name, JndiPermission.Action.BIND);
        root.accept(new BindVisitor(callingContext, name, object, className));
    }

    /**
     * Replace an existing entry in the tree.  This will create a new binding node in the tree and no longer store
     * the previous value.
     *
     * @param callingContext The calling context
     * @param name The entry name
     * @param object The entry object
     * @param className The entry class name
     * @throws NamingException
     */
    public void rebind(final Context callingContext, final Name name, final Object object, final String className) throws NamingException {
        if (isLastComponentEmpty(name)) {
            throw emptyNameException();
        }
        checkPermissions(name, JndiPermission.Action.REBIND);
        root.accept(new RebindVisitor(callingContext, name, object, className));
    }

    /**
     * Unbind the entry in the provided location.  This will remove the node in the tree and no longer manage it.
     *
     * @param callingContext The calling context
     * @param name The entry name
     * @throws NamingException
     */
    public void unbind(final Context callingContext, final Name name) throws NamingException {
        if (isLastComponentEmpty(name)) {
            throw emptyNameException();
        }
        checkPermissions(name, JndiPermission.Action.UNBIND);
        root.accept(new UnbindVisitor(callingContext, name));
    }

    /**
     * Lookup the object value of a binding node in the tree.
     *
     * @param name The entry name
     * @return The object value of the binding
     * @throws NamingException
     */
    public Object lookup(final Name name) throws NamingException {
        if (isEmpty(name)) {
            final Name emptyName = new CompositeName("");
            checkPermissions(emptyName, JndiPermission.Action.LOOKUP);
            return new NamingContext(emptyName, this, new Hashtable<String, Object>());
        }
        checkPermissions(name, JndiPermission.Action.LOOKUP);
        return root.accept(new LookupVisitor(name));
    }

    /**
     * List all NameClassPair instances at a given location in the tree.
     *
     * @param name The entry name
     * @return The NameClassPair instances
     * @throws NamingException
     */
    public List<NameClassPair> list(final Name name) throws NamingException {
        final Name nodeName = name.isEmpty() ? new CompositeName("") : name;
        checkPermissions(nodeName, JndiPermission.Action.LIST);
        return root.accept(new ListVisitor(nodeName));
    }

    /**
     * List all the Binding instances at a given location in the tree.
     *
     * @param name The entry name
     * @return The Binding instances
     * @throws NamingException
     */
    public List<Binding> listBindings(final Name name) throws NamingException {
        final Name nodeName = name.isEmpty() ? new CompositeName("") : name;
        checkPermissions(nodeName, JndiPermission.Action.LIST_BINDINGS);
        return root.accept(new ListBindingsVisitor(name));
    }

    /**
     * Create a context node at the give location in the tree.
     *
     * @param callingContext The calling context
     * @param name The entry name
     * @return The new context
     * @throws NamingException
     */
    public Context createSubcontext(final Context callingContext, final Name name) throws NamingException {
        if (isLastComponentEmpty(name)) {
            throw emptyNameException();
        }
        checkPermissions(name, JndiPermission.Action.CREATE_SUBCONTEXT);
        return root.accept(new CreateSubContextVisitor(callingContext, name));
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
     * Add a {@code NamingListener} to the naming event coordinator.
     *
     * @param target The target name to add the listener to
     * @param scope The listener scope
     * @param listener The listener
     */
    public void addNamingListener(final Name target, final int scope, final NamingListener listener) {
        final NamingEventCoordinator coordinator = this.eventCoordinator;
        if (coordinator != null) {
            coordinator.addListener(target.toString(), scope, listener);
        }
    }

    /**
     * Remove a {@code NamingListener} from the naming event coordinator.
     *
     * @param listener The listener
     */
    public void removeNamingListener(final NamingListener listener) {
        final NamingEventCoordinator coordinator = this.eventCoordinator;
        if (coordinator != null) {
            coordinator.removeListener(listener);
        }
    }

    private void fireEvent(final Context callingContext, final Name name, final Binding existingBinding, final Binding newBinding, final int type, final String changeInfo) {
        final NamingEventCoordinator coordinator = this.eventCoordinator;
        if (coordinator != null && callingContext instanceof EventContext) {
            coordinator.fireEvent(EventContext.class.cast(callingContext), name, existingBinding, newBinding, type, changeInfo, NamingEventCoordinator.DEFAULT_SCOPES);
        }
    }

    private void checkReferenceForContinuation(final Name name, final Object object) throws CannotProceedException {
        if (object instanceof Reference) {
            if (asReference(object).get("nns") != null) {
                throw cannotProceedException(object, name);
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

    private abstract class TreeNode {
        protected final Name fullName;
        protected final Binding binding;

        private TreeNode(final Name fullName, final Binding binding) {
            this.fullName = fullName;
            this.binding = binding;
        }

        protected abstract <T> T accept(NodeVisitor<T> visitor) throws NamingException;
    }

    private class ContextNode extends TreeNode {
        private volatile Map<String, TreeNode> children = Collections.emptyMap();

        private ContextNode(final Name fullName, final NamingContext context) {
            super(fullName, new Binding(getLastComponent(fullName), Context.class.getName(), context));
        }

        private synchronized void addChild(final String childName, final TreeNode childNode) throws NamingException {
            if (children.containsKey(childName)) {
                throw nameAlreadyBoundException(fullName.add(childName));
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
            if (!children.containsKey(childName)) {
                throw nameNotFoundException(childName, fullName);
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

        protected final <T> T accept(NodeVisitor<T> visitor) throws NamingException {
            return visitor.visit(this);
        }
    }

    private class BindingNode extends TreeNode {
        private BindingNode(final Name fullName, final Binding binding) {
            super(fullName, binding);
        }

        protected final <T> T accept(NodeVisitor<T> visitor) throws NamingException {
            return visitor.visit(this);
        }
    }

    private interface NodeVisitor<T> {
        T visit(BindingNode bindingNode) throws NamingException;

        T visit(ContextNode contextNode) throws NamingException;
    }

    private abstract class NodeTraversingVisitor<T> implements NodeVisitor<T> {
        private Name currentName;
        protected final Name targetName;

        private NodeTraversingVisitor(final Name targetName) {
            this.currentName = targetName;
            this.targetName = targetName;
        }

        public final T visit(final BindingNode bindingNode) throws NamingException {
            if (isEmpty(currentName)) {
                return found(bindingNode);
            }
            return foundReferenceInsteadOfContext(bindingNode);
        }

        public final T visit(final ContextNode contextNode) throws NamingException {
            if (isEmpty(currentName)) {
                return found(contextNode);
            }
            final String childName = currentName.get(0);
            final TreeNode node = contextNode.children.get(childName);
            if (node == null) {
                throw nameNotFoundException(childName, currentName);
            }
            currentName = currentName.getSuffix(1);
            return node.accept(this);
        }

        protected abstract T found(ContextNode contextNode) throws NamingException;

        protected abstract T found(BindingNode bindingNode) throws NamingException;

        protected T foundReferenceInsteadOfContext(BindingNode bindingNode) throws NamingException {
            final Object object = bindingNode.binding.getObject();
            checkReferenceForContinuation(currentName, object);
            throw notAContextException(bindingNode.fullName);
        }
    }

    private abstract class BindingContextVisitor<T> extends NodeTraversingVisitor<T> {
        protected final Name targetName;

        private BindingContextVisitor(final Name targetName) {
            super(targetName.getPrefix(targetName.size() - 1));
            this.targetName = targetName;
        }

        protected final T found(final ContextNode contextNode) throws NamingException {
            return foundBindContext(contextNode);
        }

        protected final T found(final BindingNode bindingNode) throws NamingException {
            checkReferenceForContinuation(targetName.getSuffix(bindingNode.fullName.size()), bindingNode.binding.getObject());
            throw notAContextException(targetName);
        }

        protected abstract T foundBindContext(final ContextNode contextNode) throws NamingException;
    }

    private final class BindVisitor extends BindingContextVisitor<Void> {
        private final Context callingContext;
        private final Object object;
        private final String className;

        private BindVisitor(final Context callingContext, final Name name, final Object object, final String className) {
            super(name);
            this.callingContext = callingContext;
            this.object = object;
            this.className = className;
        }

        protected final Void foundBindContext(ContextNode contextNode) throws NamingException {
            final String childName = getLastComponent(targetName);
            final Binding binding = new Binding(childName, className, object, true);
            final BindingNode bindingNode = new BindingNode(targetName, binding);
            contextNode.addChild(childName, bindingNode);
            fireEvent(callingContext, targetName, null, binding, NamingEvent.OBJECT_ADDED, "bind");
            return null;
        }
    }

    private final class RebindVisitor extends BindingContextVisitor<Void> {
        private final Context callingContext;
        private final Object object;
        private final String className;

        private RebindVisitor(final Context callingContext, final Name name, final Object object, final String className) {
            super(name);
            this.callingContext = callingContext;
            this.object = object;
            this.className = className;
        }

        protected final Void foundBindContext(ContextNode contextNode) throws NamingException {
            final String childName = getLastComponent(targetName);
            final Binding binding = new Binding(childName, className, object, true);
            final BindingNode bindingNode = new BindingNode(targetName, binding);
            final TreeNode previous = contextNode.replaceChild(childName, bindingNode);

            final Binding previousBinding = previous != null ? previous.binding : null;
            fireEvent(callingContext, targetName, previousBinding, binding, previousBinding != null ? NamingEvent.OBJECT_CHANGED : NamingEvent.OBJECT_ADDED, "rebind");
            return null;
        }
    }

    private final class UnbindVisitor extends BindingContextVisitor<Void> {
        private final Context callingContext;

        private UnbindVisitor(final Context callingContext, final Name targetName) throws NamingException {
            super(targetName);
            this.callingContext = callingContext;
        }

        protected final Void foundBindContext(ContextNode contextNode) throws NamingException {
            final TreeNode previous = contextNode.removeChild(getLastComponent(targetName));
            fireEvent(callingContext, targetName, previous.binding, null, NamingEvent.OBJECT_REMOVED, "unbind");
            return null;
        }
    }

    private final class CreateSubContextVisitor extends BindingContextVisitor<Context> {
        private final Context callingContext;
        
        private CreateSubContextVisitor(final Context callingContext, final Name targetName) throws NamingException {
            super(targetName);
            this.callingContext = callingContext;
        }

        protected final Context foundBindContext(ContextNode contextNode) throws NamingException {
            final NamingContext subContext = new NamingContext(targetName, InMemoryNamingStore.this, new Hashtable<String, Object>());
            final ContextNode subContextNode = new ContextNode(targetName, subContext);
            contextNode.addChild(getLastComponent(targetName), subContextNode);
            fireEvent(callingContext, targetName, null, subContextNode.binding, NamingEvent.OBJECT_ADDED, "createSubcontext");
            return subContext;
        }
    }

    private final class LookupVisitor extends NodeTraversingVisitor<Object> {
        private LookupVisitor(final Name targetName) {
            super(targetName);
        }

        protected final Object found(final ContextNode contextNode) throws NamingException {
            return contextNode.binding.getObject();
        }

        protected final Object found(final BindingNode bindingNode) throws NamingException {
            return bindingNode.binding.getObject();
        }

        protected final Object foundReferenceInsteadOfContext(final BindingNode bindingNode) throws NamingException {
            final Name remainingName = targetName.getSuffix(bindingNode.fullName.size());
            final Object boundObject = bindingNode.binding.getObject();
            checkReferenceForContinuation(remainingName, boundObject);
            return new ResolveResult(boundObject, remainingName);
        }
    }

    private final class ListVisitor extends NodeTraversingVisitor<List<NameClassPair>> {
        private ListVisitor(final Name targetName) {
            super(targetName);
        }

        protected final List<NameClassPair> found(final ContextNode contextNode) throws NamingException {
            final List<NameClassPair> nameClassPairs = new ArrayList<NameClassPair>();
            for (TreeNode childNode : contextNode.children.values()) {
                final Binding binding = childNode.binding;
                nameClassPairs.add(new NameClassPair(binding.getName(), binding.getClassName(), true));
            }
            return nameClassPairs;
        }

        protected final List<NameClassPair> found(final BindingNode bindingNode) throws NamingException {
            checkReferenceForContinuation(emptyName(), bindingNode.binding.getObject());
            throw notAContextException(targetName);
        }
    }

    private final class ListBindingsVisitor extends NodeTraversingVisitor<List<Binding>> {
        private ListBindingsVisitor(final Name targetName) {
            super(targetName);
        }

        protected final List<Binding> found(final ContextNode contextNode) throws NamingException {
            final List<Binding> bindings = new ArrayList<Binding>();
            for (TreeNode childNode : contextNode.children.values()) {
                bindings.add(childNode.binding);
            }
            return bindings;
        }

        protected final List<Binding> found(final BindingNode bindingNode) throws NamingException {
            checkReferenceForContinuation(emptyName(), bindingNode.binding.getObject());
            throw notAContextException(targetName);
        }
    }

}
