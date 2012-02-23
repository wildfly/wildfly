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
 * @author Radoslav Husar
 * @version Feb 2012
 */
public class ClusteringTestConstants {

    /**
     * ARQ automatically managed container names.
     */
    public static final String MANAGED_CONTAINER_1 = "clustering-udp-0";
    public static final String MANAGED_CONTAINER_2 = "clustering-udp-1";
    /**
     * Unmanaged (manually managed) container names.
     */
    public static final String CONTAINER_1 = "clustering-udp-0-unmanaged";
    public static final String CONTAINER_2 = "clustering-udp-1-unmanaged";
    /**
     * Deployment names.
     */
    public static final String DEPLOYMENT_1 = "deployment-0-unmanaged";
    public static final String DEPLOYMENT_2 = "deployment-1-unmanaged";
    /**
     * Timeouts.
     */
    public static final long GRACE_TIME_TO_MEMBERSHIP_CHANGE = 5000;
    public static final int GRACE_TIME = 20000;
}
