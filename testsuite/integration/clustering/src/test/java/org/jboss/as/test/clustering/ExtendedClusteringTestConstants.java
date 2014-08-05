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
package org.jboss.as.test.clustering;

/**
 * Test constants statically imported used across the clustering tests.
 *
 * Used for testing against 4 node clusters required for xsite.
 *
 * @author Richard Achmatowicz
 * @version May 2013
 */
public interface ExtendedClusteringTestConstants extends ClusteringTestConstants {

    /**
     * Manual container with unmanaged deployments names.
     */
    String CONTAINER_3 = "container-2";
    String CONTAINER_4 = "container-3";
    String[] XSITE_CONTAINERS = new String[] { CONTAINER_1, CONTAINER_2, CONTAINER_3, CONTAINER_4 };

    /**
     * Deployment names.
     */
    String DEPLOYMENT_3 = "deployment-2";
    String DEPLOYMENT_4 = "deployment-3";
    String[] XSITE_DEPLOYMENTS = new String[] { DEPLOYMENT_1, DEPLOYMENT_2, DEPLOYMENT_3, DEPLOYMENT_4 };

    /**
     * Some helper deployment names.
     */
    String DEPLOYMENT_HELPER_3 = "deployment-helper-2";
    String DEPLOYMENT_HELPER_4 = "deployment-helper-3";
    String[] XSITE_DEPLOYMENT_HELPERS = new String[] { DEPLOYMENT_HELPER_1, DEPLOYMENT_HELPER_2, DEPLOYMENT_HELPER_3, DEPLOYMENT_HELPER_4 };

    /**
     * Node names passed in arquillian.xml via -Djboss.node.name property.
     */
    String NODE_3 = "node-2";
    String NODE_4 = "node-3";
    String[] XSITE_NODES = new String[] { NODE_1, NODE_2 , NODE_3, NODE_4};
}
