/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.patching.validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.jboss.as.patching.validation.Artifact.State;


/**
 * @author Alexey Loubyansky
 *
 */
public class PatchingHistoryTree {

    public static <P extends Artifact.State, S extends Artifact.State> java.util.Iterator<S> stateIterator(final Artifact<P,S> a, final Context ctx) {
        return new java.util.Iterator<S>(){

            private S current;
            final PatchingHistoryTree tree = Builder.getInstance().addHandler(a, new ArtifactStateHandler<S>(){
                @Override
                public void handle(Context ctx, S state) {
                    current = state;
                }}).build();
            final TreeIterator i = tree.treeIterator(ctx);

            @Override
            public boolean hasNext() {
                return i.hasNext();
            }

            @Override
            public S next() {
                i.handleNext();
                return current;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }};
    }

    public static class Builder {

        static {
            // to initialize the artifact tree
            PatchingHistoryRoot.getInstance();
        }

        public static Builder getInstance() {
            return new Builder();
        }

        private Builder() {}

        private ArtifactTreeNodeBuilder<PatchingHistoryRoot.State, PatchingHistoryRoot.State> root;

        public <P extends Artifact.State, S extends Artifact.State> Builder addHandler(Artifact<P, S> artifact,
                ArtifactStateHandler<S> handler) {
            final ArtifactTreeNodeBuilder<P, S> nodeBuilder = getNodeBuilder(artifact);
            if(nodeBuilder.handler != null) {
                // for now limit to one handler
                throw new IllegalStateException("handler has already been specified for artifact " + artifact);
            }
            nodeBuilder.handler = handler;
            return this;
        }

        <P extends Artifact.State, S extends Artifact.State> ArtifactTreeNodeBuilder<P, S> getNodeBuilder(Artifact<P, S> artifact) {
            final Artifact<? extends State, P> parent = artifact.getParent();
            if(parent == null) {
                if(root == null) {
                    root = (ArtifactTreeNodeBuilder<PatchingHistoryRoot.State, PatchingHistoryRoot.State>) new ArtifactTreeNodeBuilder<P, S>(artifact);
                }
                return (ArtifactTreeNodeBuilder<P, S>) root;
            }
            final ArtifactTreeNodeBuilder<? extends Artifact.State, P> parentBuilder = getNodeBuilder(parent);
            return parentBuilder.getChildNodeBuilder(artifact);
        }

        public PatchingHistoryTree build() {
            if(root == null) {
                throw new IllegalStateException("No instructions to build off.");
            }
            return new PatchingHistoryTree(root.build());
        }

        class ArtifactTreeNodeBuilder<P extends Artifact.State, S extends Artifact.State> {

            private Artifact<P, S> artifact;
            private ArtifactStateHandler<S> handler;
            private Map<Artifact<S, ? extends Artifact.State>, ArtifactTreeNodeBuilder<S, ? extends Artifact.State>> children = Collections.emptyMap();

            ArtifactTreeNodeBuilder(Artifact<P, S> artifact) {
                if(artifact == null) {
                    throw new IllegalArgumentException("artifact is null");
                }
                this.artifact = artifact;
            }

            @SuppressWarnings("unchecked")
            <C extends Artifact.State> ArtifactTreeNodeBuilder<S, C> getChildNodeBuilder(Artifact<S, C> artifact) {
                ArtifactTreeNodeBuilder<S, C> childBuilder = (ArtifactTreeNodeBuilder<S, C>) children.get(artifact);
                if(childBuilder != null) {
                    return childBuilder;
                }

                childBuilder = new ArtifactTreeNodeBuilder<S, C>(artifact);
                switch(children.size()) {
                    case 0:
                        children = Collections.<Artifact<S, ? extends Artifact.State>, ArtifactTreeNodeBuilder<S, ? extends Artifact.State>>singletonMap(artifact, childBuilder);
                        break;
                    case 1:
                        final Map<Artifact<S, ? extends Artifact.State>, ArtifactTreeNodeBuilder<S, ? extends Artifact.State>> tmp = children;
                        children = new HashMap<Artifact<S, ? extends Artifact.State>, ArtifactTreeNodeBuilder<S, ? extends Artifact.State>>();
                        children.putAll(tmp);
                    default:
                        children.put(artifact, childBuilder);
                }
                return childBuilder;
            }

            ArtifactTreeNode<P, S> build() {
                final List<ArtifactTreeNode<S, ? extends Artifact.State>> nodes;
                switch(children.size()) {
                    case 0:
                        nodes = Collections.emptyList();
                        break;
                    case 1:
                        nodes = Collections.<ArtifactTreeNode<S, ? extends Artifact.State>>singletonList(children.values().iterator().next().build());
                        break;
                    default:
                        nodes = new ArrayList<ArtifactTreeNode<S, ? extends Artifact.State>>(children.size());
                        for(ArtifactTreeNodeBuilder<S, ? extends Artifact.State> nodeBuilder : children.values()) {
                            nodes.add(nodeBuilder.build());
                        }
                }
                return new ArtifactTreeNode<P, S>(artifact, handler, nodes);
            }
        }

    }

    static class ArtifactTreeNode<P extends Artifact.State, S extends Artifact.State> {
        private final Artifact<P, S> artifact;
        private final ArtifactStateHandler<S> handler;
        private final List<ArtifactTreeNode<S, ? extends Artifact.State>> children;

        ArtifactTreeNode(Artifact<P, S> artifact, ArtifactStateHandler<S> handler, List<ArtifactTreeNode<S, ? extends Artifact.State>> children) {
            if(artifact == null) {
                throw new IllegalArgumentException("artifact is null");
            }
//            if(handler == null) {
//                throw new IllegalArgumentException("Handler is null");
//            }
            if(children == null) {
                throw new IllegalArgumentException("Children are null");
            }
            this.artifact = artifact;
            this.handler = handler;
            this.children = children;
        }

        @Override
        public String toString() {
            return "<" + artifact.getClass().getSimpleName() + " " + handler + " " + children + ">";
        }
    }

    private ArtifactTreeNode<PatchingHistoryRoot.State, PatchingHistoryRoot.State> root;

    PatchingHistoryTree(ArtifactTreeNode<PatchingHistoryRoot.State, PatchingHistoryRoot.State> root) {
        if(root == null) {
            throw new IllegalArgumentException("root is null");
        }
        this.root = root;
    }

    public void handleAll(Context ctx) {
        if(root == null) {
            throw new IllegalStateException("Tree root has not been initialized.");
        }
        handleNode(ctx, root, null);
    }

    private <P extends Artifact.State, S extends Artifact.State> void handleNode(Context ctx, ArtifactTreeNode<P, S> node, P parentState) {
        final S state = node.artifact.getState(parentState, ctx);
        if(state == null) {
            return;
        }
        if(node.handler != null) {
            node.handler.handle(ctx, state);
        }

        if(state instanceof ArtifactCollectionState) {
            final ArtifactCollectionState<?> col = (ArtifactCollectionState<?>) state;
            col.resetIndex();
            while(col.hasNext(ctx)) {
                col.next(ctx);
                for(ArtifactTreeNode<S, ? extends Artifact.State> child : node.children) {
                    handleNode(ctx, child, state);
                }
            }
        } else {
            for (ArtifactTreeNode<S, ? extends Artifact.State> child : node.children) {
                handleNode(ctx, child, state);
            }
        }
    }

    private class StateNode<P extends Artifact.State, S extends Artifact.State> {
        ArtifactTreeNode<P, S> node;
        S state;
        int childIndex;

        StateNode<?, P> previous;

        boolean collection;

        StateNode(ArtifactTreeNode<P, S> node, S state, P parentState, Context ctx) {
            if(node == null) {
                throw new IllegalArgumentException("node is null");
            }
            if(state == null) {
                throw new IllegalArgumentException("state is null");
            }
            this.node = node;
            this.state = state;
            childIndex = 0;

            collection = state instanceof ArtifactCollectionState;
            if(collection) {
                ArtifactCollectionState<?> col = (ArtifactCollectionState<?>)state;
                col.resetIndex();
                if(col.hasNext(ctx)) {
                    col.next(ctx);
                }
            }
        }

        void handle(Context ctx) {
            if(node.handler == null) {
                throw new IllegalStateException("the handler is null");
            }
            node.handler.handle(ctx, state);
        }

        StateNode<? extends Artifact.State, ? extends Artifact.State> next(Context ctx) {
            while (childIndex < node.children.size()) {
                final ArtifactTreeNode<S, ? extends Artifact.State> child = node.children.get(childIndex++);
                final Artifact.State childState = child.artifact.getState(state, ctx);
                if (childState != null) {
                    if (child.handler == null) {
                        // process its children
                        final StateNode<S, ? extends Artifact.State> next = new StateNode(child, childState, state, ctx);
                        next.previous = this;
                        return next.next(ctx);
                    }
                    final StateNode<S, ? extends Artifact.State> next = new StateNode(child, childState, state, ctx);
                    next.previous = this;
                    return next;
                }
            }

            if (collection) {
                final ArtifactCollectionState<?> col = (ArtifactCollectionState<?>) state;
                if (col.hasNext(ctx)) {
                    col.next(ctx);
                    childIndex = 0;
                    return next(ctx);
                }
            }

            if (previous == null) {
                return null;
            }
            return previous.next(ctx);
        }
    }

    public interface TreeIterator {
        boolean hasNext();
        void handleNext();
    }

    public TreeIterator treeIterator(final Context context) {
        return new TreeIterator() {

            private final Context ctx = context;
            private PatchingHistoryRoot.State rootState = root.artifact.getState(null, ctx);
            private StateNode<?,?> currentState;

            @Override
            public boolean hasNext() {
                if(currentState == null) {
                    if(rootState == null) {
                        return false;
                    }
                    currentState = new StateNode<PatchingHistoryRoot.State, PatchingHistoryRoot.State>(root, rootState, null, ctx);
                    rootState = null;
                    if(currentState.node.handler == null) {
                        currentState = currentState.next(ctx);
                        if(currentState == null) {
                            return false;
                        }
                    }
                }
                return true;
            }

            @Override
            public void handleNext() {
                if(currentState == null) {
                    if(rootState == null) {
                        throw new NoSuchElementException();
                    }
                    currentState = new StateNode<PatchingHistoryRoot.State, PatchingHistoryRoot.State>(root, rootState, null, ctx);
                    rootState = null;
                    if(currentState.node.handler == null) {
                        currentState = currentState.next(ctx);
                        if(currentState == null) {
                            throw new NoSuchElementException();
                        }
                    }
                }
                currentState.handle(ctx);
                currentState = currentState.next(ctx);
            }};
    }

    @Override
    public String toString() {
        return root == null ? "<null>" : root.toString();
    }
}
