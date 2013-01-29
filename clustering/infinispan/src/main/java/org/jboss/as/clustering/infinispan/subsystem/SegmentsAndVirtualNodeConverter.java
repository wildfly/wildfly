/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.infinispan.subsystem;


/**
 * Convert the 1.4 SEGMENTS value to VIRTUAL_NODES in model and operations, if defined and not an expression
 * Remove the 1.4 attributes INDEXING_PROPERTIES and SEGMENTS from model and operations
 *
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 * @author Richard Achmatowicz (c) RedHat 2013
 */
public class SegmentsAndVirtualNodeConverter  {

    // ratio of segments to virtual nodes to convert between the two
    public static final int SEGMENTS_PER_VIRTUAL_NODE = 6;

    /*
     * Convert a 1.3 virtual nodes value to a 1.4 segments value
     */
    public static String virtualNodesToSegments(String virtualNodesValue) {
        int segments = 0 ;
        try {
            segments =  Integer.parseInt(virtualNodesValue) * SEGMENTS_PER_VIRTUAL_NODE;
        }
        catch(NumberFormatException nfe) {
            // in case of expression
        }
        return Integer.toString(segments);
    }

    /*
     * Convert a 1.4 segments value to a 1.3 virtual nodes value
     */
    public static String segmentsToVirtualNodes(String segmentsValue) {
        int virtualNodes = 0 ;
        try {
            // divide by zero should not occur as segments is required to be > 0
            virtualNodes =  Integer.parseInt(segmentsValue) / SEGMENTS_PER_VIRTUAL_NODE;
        }
        catch(NumberFormatException nfe) {
            // in case of expression
        }
        return Integer.toString(virtualNodes);
    }

}

