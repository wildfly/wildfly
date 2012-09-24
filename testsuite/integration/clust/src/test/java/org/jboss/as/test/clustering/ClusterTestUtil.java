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
 * Utility class for cluster test.
 *
 * @author Radoslav Husar
 * @version September 2012
 */
public class ClusterTestUtil {

    public static void waitForReplication(int millis) {
        if (ClusteringTestConstants.TEST_CACHE_MODE.equalsIgnoreCase("SYNC")) {
            // In case the replication is sync, we do not need to wait for the replication to happen.
            return;
        }
        // TODO: Instead of dummy waiting, we could attach a listener and notify the test framework the replication has happened. millis value can be used as timeout in that case.
        try {
            Thread.sleep(millis);
        } catch (InterruptedException iex) {
        }
    }

    private ClusterTestUtil() {
    }

}
