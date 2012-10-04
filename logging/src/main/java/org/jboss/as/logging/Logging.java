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

package org.jboss.as.logging;

import java.util.Arrays;
import java.util.Set;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.registry.AttributeAccess.Flag;
import org.jboss.dmr.ModelNode;

/**
 * A set of utilities for the logging subsystem.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 * @since 7.1.0
 */
public final class Logging {

    private Logging() {
    }

    /**
     * Checks to see within the flags if a restart of any kind is required.
     *
     * @param flags the flags to check
     *
     * @return {@code true} if a restart is required, otherwise {@code false}
     */
    public static boolean requiresRestart(final Set<Flag> flags) {
        for (Flag flag : flags) {
            switch (flag) {
                case RESTART_ALL_SERVICES:
                case RESTART_JVM:
                case RESTART_RESOURCE_SERVICES: {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Creates a new {@link OperationFailedException} with the message as a {@link ModelNode model node}.
     *
     * @param message the message to initialize the {@link ModelNode model node} with
     *
     * @return a new {@link OperationFailedException}
     */
    public static OperationFailedException createOperationFailure(final String message) {
        return new OperationFailedException(new ModelNode(message));
    }

    /**
     * Creates a new {@link OperationFailedException} with the message as a {@link ModelNode model node} and the cause.
     *
     * @param cause   the cause of the error
     * @param message the message to initialize the {@link ModelNode model node} with
     *
     * @return a new {@link OperationFailedException}
     */
    public static OperationFailedException createOperationFailure(final Throwable cause, final String message) {
        return new OperationFailedException(cause, new ModelNode(message));
    }

    /**
     * Joins two arrays.
     * <p/>
     * If the base array is null, the {@code add} parameter is returned. If the add array is null, the {@code base}
     * array is returned.
     *
     * @param base the base array to add to
     * @param add  the array to add
     * @param <T>  the type of the array
     *
     * @return the joined array
     */
    static <T> T[] join(final T[] base, final T... add) {
        if (add == null) {
            return base;
        } else if (base == null) {
            return add;
        }
        final T[] result = Arrays.copyOf(base, base.length + add.length);
        System.arraycopy(add, 0, result, base.length, add.length);
        return result;
    }
}
