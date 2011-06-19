/*
 * JBoss, Home of Professional Open Source
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.clustering;

import java.net.InetAddress;
import java.io.Serializable;

/**
 * Abstract identifier for a member of an @{link HAPartition}.
 *
 * @author Brian Stansberry
 * @version $Revision: 104220 $
 */
public interface ClusterNode extends Comparable<ClusterNode>, Cloneable, Serializable {
    /**
     * Gets a String identifier for this node. The identifier should be unique across all nodes in the current view.
     */
    String getName();

    /**
     * Gets the <code>InetAddress</code> at which the node is receiving intra-cluster communications.
     */
    InetAddress getIpAddress();

    /**
     * Gets the port at which the node is receiving intra-cluster communications.
     */
    int getPort();
}
