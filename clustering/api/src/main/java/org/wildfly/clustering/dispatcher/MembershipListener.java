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
package org.wildfly.clustering.dispatcher;

import java.util.List;

import org.wildfly.clustering.Node;

/**
 * Implemented by objects who wish to be notified of cluster membership changes.
 * @author Paul Ferraro
 */
public interface MembershipListener {
    /**
     * Called when the membership of a cluster changes.
     * @param allNodes the list of nodes in the current cluster view
     * @param deadNodes a list of nodes that were members of the previous cluster view, but not the current view
     * @param newNodes a list of nodes that were not members of the previous cluster view, but are members of the current view
     * @param groups if the current view is the result of a merge, this identifies a list of the former membership groups that were merged.
     */
    void membershipChanged(List<Node> allNodes, List<Node> deadNodes, List<Node> newNodes, List<List<Node>> groups);
}
