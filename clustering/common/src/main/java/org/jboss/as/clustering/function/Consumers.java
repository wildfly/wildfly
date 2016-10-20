/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.function;

import java.util.function.Consumer;

import org.jboss.as.clustering.logging.ClusteringLogger;

/**
 * {@link Consumer} utility methods.
 * @author Paul Ferraro
 */
public class Consumers {

    /**
     * Returns a consumer that closes its input.
     * @return a consumer that closes its input.
     */
    public static <T extends AutoCloseable> Consumer<T> close() {
        return value -> {
            try {
                value.close();
            } catch (Throwable e) {
                ClusteringLogger.ROOT_LOGGER.failedToClose(e, value);
            }
        };
    }

    private Consumers() {
        // Hide
    }
}
