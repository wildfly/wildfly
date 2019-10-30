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

import static org.jboss.as.naming.util.NamingUtils.cannotProceedException;
import static org.jboss.as.naming.util.NamingUtils.emptyNameException;
import static org.jboss.as.naming.util.NamingUtils.getLastComponent;
import static org.jboss.as.naming.util.NamingUtils.isEmpty;
import static org.jboss.as.naming.util.NamingUtils.isLastComponentEmpty;
import static org.jboss.as.naming.util.NamingUtils.nameAlreadyBoundException;
import static org.jboss.as.naming.util.NamingUtils.nameNotFoundException;
import static org.jboss.as.naming.util.NamingUtils.notAContextException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.concurrent.locks.ReentrantLock;

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
import org.jboss.as.naming.logging.NamingLogger;

/**
 * In-memory implementation of the NamingStore.  The backing for the entries is a basic tree structure with either context
 * nodes or binding nodes.  The context nodes are allowed to have children and can be represented by a NamingContext.  A
 * binding node is only allowed to have a normal object binding.
 *
 * @author John E. Bailey
 */
public class InMemoryNamingStore implements WritableNamingStore {

    /* The root node of the tree.  Represents a JNDI name of "" */
    private final ContextNode root = new ContextNode(null, null, new CompositeName(), new NamingContext(this, null));

    /* Naming Event Coordinator */
    private final NamingEventCoordinator eventCoordinator;

    private final ReentrantLock writeLock = new ReentrantLock();

    private final Name baseName;

    /**
     * Construct instance with no event support, and an empty base name.
     */
    public InMemoryNamingStore() {
        this(null, new CompositeName());
    }

    /**
     * Construct instance with an event coordinator, and an empty base name.
     *
     * @param eventCoordinator The event coordinator
     */
    public InMemoryNamingStore(final NamingEventCoordinator eventCoordinator) {
        this(eventCoordinator, new CompositeName());
    }

    /**
     * Construct instance with no event support, and the specified base name.
     *
     * @param baseName
     */
    public InMemoryNamingStore(final Name baseName) {
        this(null, baseName);
    }

    /**
     * Construct instance with an event coordinator, and the specified base name.
     *
     * @param eventCoordinator
     * @param baseName
     */
    public InMemoryNamingStore(final NamingEventCoordinator eventCoordinator, final Name baseName) {
        this.eventCoordinator = eventCoordinator;
        if(baseName == null) {
            throw new NullPointerException(NamingLogger.ROOT_LOGGER.cannotBeNull("baseName"));
        }
        this.baseName = baseName;
    }

    /** {@inheritDoc} */
    public Name getBaseName() throws NamingException {
        return baseName;
    }

    /** {@inheritDoc} */
    public void bind(Name name, Object object) throws NamingException {
        bind(name, object, object.getClass());
    }

    /** {@inheritDoc} */
    public void bind(final Name name, final Object object, final Class<?> bindType) throws NamingException {
        if (isLastComponentEmpty(name)) {
            throw emptyNameException();
        }

        writeLock.lock();
        try {
            root.accept(new BindVisitor(true, name, object, bindType.getName()));
        } finally {
            writeLock.unlock();
        }
    }

    /** {@inheritDoc} */
    public void rebind(Name name, Object object) throws NamingException {
        rebind(name, object, object.getClass());
    }

    /** {@inheritDoc} */
    public void rebind(final Name name, final Object object, final Class<?> bindType) throws NamingException {
        if (isLastComponentEmpty(name)) {
            throw emptyNameException();
        }

        writeLock.lock();
        try {
            root.accept(new RebindVisitor(name, object, bindType.getName()));
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Unbind the entry in the provided location.  This will remove the node in the tree and no longer manage it.
     *
     * @param name The entry name
     * @throws NamingException
     */
    public void unbind(final Name name) throws NamingException {
        if (isLastComponentEmpty(name)) {
            throw emptyNameException();
        }

        writeLock.lock();
        try {
            root.accept(new UnbindVisitor(name));
        } finally {
            writeLock.unlock();
        }
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
            return new NamingContext(emptyName, this, new Hashtable<String, Object>());
        }
        return root.accept(new LookupVisitor(name));
    }

    @Override
    public Object lookup(Name name, boolean dereference) throws NamingException {
        // ignoring dereference arg, it's not relevant to this store impl
        return lookup(name);
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
        return root.accept(new ListBindingsVisitor(nodeName));
    }

    public Context createSubcontext(final Name name) throws NamingException {
        if (isLastComponentEmpty(name)) {
            throw emptyNameException();
        }
        return root.accept(new CreateSubContextVisitor(name));
    }

    /**
     * Close the store.  This will clear all children from the root node.
     *
     * @throws NamingException
     */
    public void close() throws NamingException {
        writeLock.lock();
        try {
            root.clear();
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Add a {@code NamingListener} to the naming event coordinator.
     *
     * @param target The target name to add the listener to
     * @param scope The listener scope
     * @param listener The listener
     */
    public void addNamingListener(final Name target, final int scope, final NamingListener listener) {
        final NamingEventCoordinator coordinator = eventCoordinator;
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
        final NamingEventCoordinator coordinator = eventCoordinator;
        if (coordinator != null) {
            coordinator.removeListener(listener);
        }
    }

    private void fireEvent(final ContextNode contextNode, final Name name, final Binding existingBinding, final Binding newBinding, final int type, final String changeInfo) {
        final NamingEventCoordinator coordinator = eventCoordinator;
        if (eventCoordinator != null) {
            final Context context = Context.class.cast(contextNode.binding.getObject());
            if(context instanceof EventContext) {
                coordinator.fireEvent(EventContext.class.cast(context), name, existingBinding, newBinding, type, changeInfo, NamingEventCoordinator.DEFAULT_SCOPES);
            }
        }
    }

    private void checkReferenceForContinuation(final Name name, final Object object) throws CannotProceedException {
        if (object instanceof Reference) {
            if (((Reference) object).get("nns") != null) {
                throw cannotProceedException(object, name);
            }
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

    private static final AtomicMapFieldUpdater<ContextNode, String, TreeNode> childrenUpdater = AtomicMapFieldUpdater.newMapUpdater(AtomicReferenceFieldUpdater.newUpdater(ContextNode.class, Map.class, "children"));

    private class ContextNode extends TreeNode {
        volatile Map<String, TreeNode> children = Collections.emptyMap();
        protected final String name;
        protected final ContextNode parentNode;

        private ContextNode(final ContextNode parentNode, final String name, final Name fullName, final NamingContext context) {
            super(fullName, new Binding(getLastComponent(fullName), Context.class.getName(), context));
            this.name = name;
            this.parentNode = parentNode;
        }

        private void addChild(final String childName, final TreeNode childNode) throws NamingException {
            if (childrenUpdater.putIfAbsent(this, childName, childNode) != null) {
                throw nameAlreadyBoundException(fullName.add(childName));
            }
        }

        private TreeNode replaceChild(final String childName, final TreeNode childNode) throws NamingException {
            return childrenUpdater.put(this, childName, childNode);
        }

        private TreeNode removeChild(final String childName) throws NameNotFoundException {
            TreeNode old = childrenUpdater.remove(this, childName);
            if (old == null) {
                throw nameNotFoundException(childName, fullName);
            }
            if(parentNode != null && children.isEmpty()) {
                childrenUpdater.remove(parentNode, name);
            }
            return old;
        }

        private void clear() {
            childrenUpdater.clear(this);
        }

        protected final <T> T accept(NodeVisitor<T> visitor) throws NamingException {
            return visitor.visit(this);
        }

        public TreeNode addOrGetChild(final String childName, final TreeNode childNode) {
            TreeNode appearing = childrenUpdater.putIfAbsent(this, childName, childNode);
            return appearing == null ? childNode : appearing;
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
        private final boolean createIfMissing;
        private Name currentName;
        private Name traversedName;
        protected final Name targetName;

        protected NodeTraversingVisitor(final boolean createIfMissing, final Name targetName) {
            this.createIfMissing = createIfMissing;
            this.targetName = currentName = targetName;
            this.traversedName = new CompositeName();
        }

        protected NodeTraversingVisitor(final Name targetName) {
            this(false, targetName);
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
            traversedName.add(childName);
            currentName = currentName.getSuffix(1);
            final TreeNode node = contextNode.children.get(childName);
            if (node == null) {
                if (createIfMissing) {
                    final NamingContext subContext = new NamingContext((Name)traversedName.clone(), InMemoryNamingStore.this, new Hashtable<String, Object>());
                    return contextNode.addOrGetChild(childName, new ContextNode(contextNode, childName, (Name)traversedName.clone(), subContext)).accept(this);
                } else {
                    throw nameNotFoundException(childName, contextNode.fullName);
                }
            }
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

        protected BindingContextVisitor(final boolean createIfMissing, final Name targetName) {
            super(createIfMissing, targetName.getPrefix(targetName.size() - 1));
            this.targetName = targetName;
        }

        protected BindingContextVisitor(final Name targetName) {
            this(false, targetName);
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
        private final Object object;
        private final String className;

        private BindVisitor(final boolean createIfMissing, final Name name, final Object object, final String className) {
            super(createIfMissing, name);
            this.object = object;
            this.className = className;
        }

        protected Void foundBindContext(final ContextNode contextNode) throws NamingException {
            final String childName = getLastComponent(targetName);
            final Binding binding = new Binding(childName, className, object, true);
            final BindingNode bindingNode = new BindingNode(targetName, binding);
            contextNode.addChild(childName, bindingNode);
            fireEvent(contextNode, targetName, null, binding, NamingEvent.OBJECT_ADDED, "bind");
            return null;
        }
    }

    private final class RebindVisitor extends BindingContextVisitor<Void> {
        private final Object object;
        private final String className;

        private RebindVisitor(final Name name, final Object object, final String className) {
            super(name);
            this.object = object;
            this.className = className;
        }

        protected Void foundBindContext(final ContextNode contextNode) throws NamingException {
            final String childName = getLastComponent(targetName);
            final Binding binding = new Binding(childName, className, object, true);
            final BindingNode bindingNode = new BindingNode(targetName, binding);
            final TreeNode previous = contextNode.replaceChild(childName, bindingNode);

            final Binding previousBinding = previous != null ? previous.binding : null;
            fireEvent(contextNode, targetName, previousBinding, binding, previousBinding != null ? NamingEvent.OBJECT_CHANGED : NamingEvent.OBJECT_ADDED, "rebind");
            return null;
        }
    }

    private final class UnbindVisitor extends BindingContextVisitor<Void> {

        private UnbindVisitor(final Name targetName) throws NamingException {
            super(targetName);
        }

        protected Void foundBindContext(final ContextNode contextNode) throws NamingException {
            final TreeNode previous = contextNode.removeChild(getLastComponent(targetName));
            fireEvent(contextNode, targetName, previous.binding, null, NamingEvent.OBJECT_REMOVED, "unbind");
            return null;
        }
    }

    private final class LookupVisitor extends NodeTraversingVisitor<Object> {
        private LookupVisitor(final Name targetName) {
            super(targetName);
        }

        protected Object found(final ContextNode contextNode) throws NamingException {
            return contextNode.binding.getObject();
        }

        protected Object found(final BindingNode bindingNode) throws NamingException {
            return bindingNode.binding.getObject();
        }

        protected Object foundReferenceInsteadOfContext(final BindingNode bindingNode) throws NamingException {
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

        protected List<NameClassPair> found(final ContextNode contextNode) throws NamingException {
            final List<NameClassPair> nameClassPairs = new ArrayList<NameClassPair>();
            for (TreeNode childNode : contextNode.children.values()) {
                final Binding binding = childNode.binding;
                nameClassPairs.add(new NameClassPair(binding.getName(), binding.getClassName(), true));
            }
            return nameClassPairs;
        }

        protected List<NameClassPair> found(final BindingNode bindingNode) throws NamingException {
            checkReferenceForContinuation(new CompositeName(), bindingNode.binding.getObject());
            throw notAContextException(targetName);
        }
    }

    private final class ListBindingsVisitor extends NodeTraversingVisitor<List<Binding>> {
        private ListBindingsVisitor(final Name targetName) {
            super(targetName);
        }

        protected List<Binding> found(final ContextNode contextNode) throws NamingException {
            final List<Binding> bindings = new ArrayList<Binding>();
            for (TreeNode childNode : contextNode.children.values()) {
                bindings.add(childNode.binding);
            }
            return bindings;
        }

        protected List<Binding> found(final BindingNode bindingNode) throws NamingException {
            checkReferenceForContinuation(new CompositeName(), bindingNode.binding.getObject());
            throw notAContextException(targetName);
        }
    }

    private final class CreateSubContextVisitor extends BindingContextVisitor<Context> {
        private CreateSubContextVisitor(final Name targetName) throws NamingException {
            super(targetName);
        }

        protected Context foundBindContext(ContextNode contextNode) throws NamingException {
            final NamingContext subContext = new NamingContext(targetName, InMemoryNamingStore.this, new Hashtable<String, Object>());
            final String childName = getLastComponent(targetName);
            final ContextNode subContextNode = new ContextNode(contextNode, childName, targetName, subContext);
            contextNode.addChild(getLastComponent(targetName), subContextNode);
            fireEvent(contextNode, targetName, null, subContextNode.binding, NamingEvent.OBJECT_ADDED, "createSubcontext");
            return subContext;
        }
    }
}
