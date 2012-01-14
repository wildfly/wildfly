/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.threads;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.threads.CommonAttributes.ALLOW_CORE_TIMEOUT;
import static org.jboss.as.threads.CommonAttributes.CORE_THREADS;
import static org.jboss.as.threads.CommonAttributes.GROUP_NAME;
import static org.jboss.as.threads.CommonAttributes.HANDOFF_EXECUTOR;
import static org.jboss.as.threads.CommonAttributes.KEEPALIVE_TIME;
import static org.jboss.as.threads.CommonAttributes.MAX_THREADS;
import static org.jboss.as.threads.CommonAttributes.NAME;
import static org.jboss.as.threads.CommonAttributes.PRIORITY;
import static org.jboss.as.threads.CommonAttributes.QUEUE_LENGTH;
import static org.jboss.as.threads.CommonAttributes.THREAD_FACTORY;
import static org.jboss.as.threads.CommonAttributes.THREAD_NAME_PATTERN;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;

/**
 * @author John Bailey
 */
public class ThreadsDescriptionUtil {

    public static void addBoundedQueueThreadPool(final ModelNode result, final ModelNode pool, final PathElement... addressParts) {
        final ModelNode operation = Util.getEmptyOperation(ADD, pathAddress(addressParts));

        operation.get(NAME).set(pool.require(NAME));
        if (pool.hasDefined(THREAD_FACTORY)) {
            operation.get(THREAD_FACTORY).set(pool.get(THREAD_FACTORY));
        }
        if (pool.hasDefined(MAX_THREADS)) {
            operation.get(MAX_THREADS).set(pool.get(MAX_THREADS));
        }
        if (pool.hasDefined(KEEPALIVE_TIME)) {
            operation.get(KEEPALIVE_TIME).set(pool.get(KEEPALIVE_TIME));
        }
        if (pool.hasDefined(HANDOFF_EXECUTOR)) {
            operation.get(HANDOFF_EXECUTOR).set(pool.get(HANDOFF_EXECUTOR));
        }
        if (pool.hasDefined(ALLOW_CORE_TIMEOUT)) {
            operation.get(ALLOW_CORE_TIMEOUT).set(pool.get(ALLOW_CORE_TIMEOUT));
        }
        if (pool.hasDefined(QUEUE_LENGTH)) {
            operation.get(QUEUE_LENGTH).set(pool.get(QUEUE_LENGTH));
        }
        if (pool.hasDefined(CORE_THREADS)) {
            operation.get(CORE_THREADS).set(pool.get(CORE_THREADS));
        }
        result.add(operation);
    }

    public static void addQueuelessThreadPool(final ModelNode result, final ModelNode pool,  final PathElement... addressParts) {
        final ModelNode operation = Util.getEmptyOperation(ADD, pathAddress(addressParts));

        operation.get(NAME).set(pool.require(NAME));
        if (pool.hasDefined(THREAD_FACTORY)) {
            operation.get(THREAD_FACTORY).set(pool.get(THREAD_FACTORY));
        }
        if (pool.hasDefined(MAX_THREADS)) {
            operation.get(MAX_THREADS).set(pool.get(MAX_THREADS));
        }
        if (pool.hasDefined(KEEPALIVE_TIME)) {
            operation.get(KEEPALIVE_TIME).set(pool.get(KEEPALIVE_TIME));
        }
        if (pool.hasDefined(HANDOFF_EXECUTOR)) {
            operation.get(HANDOFF_EXECUTOR).set(pool.get(HANDOFF_EXECUTOR));
        }
        result.add(operation);
    }

    public static void addThreadFactory(final ModelNode result, final ModelNode pool,  final PathElement... addressParts) {
        final ModelNode operation = Util.getEmptyOperation(ADD, pathAddress(addressParts));

        operation.get(NAME).set(pool.require(NAME));
        if (pool.hasDefined(GROUP_NAME)) {
            operation.get(GROUP_NAME).set(pool.get(GROUP_NAME));
        }
        if (pool.hasDefined(THREAD_NAME_PATTERN)) {
            operation.get(THREAD_NAME_PATTERN).set(pool.get(THREAD_NAME_PATTERN));
        }
        if (pool.hasDefined(PRIORITY)) {
            operation.get(PRIORITY).set(pool.get(PRIORITY));
        }
        result.add(operation);
    }

    public static void addScheduledThreadPool(final ModelNode result, final ModelNode pool,  final PathElement... addressParts) {
        final ModelNode operation = Util.getEmptyOperation(ADD, pathAddress(addressParts));

        operation.get(NAME).set(pool.require(NAME));
        if (pool.hasDefined(THREAD_FACTORY)) {
            operation.get(THREAD_FACTORY).set(pool.get(THREAD_FACTORY));
        }
        if (pool.hasDefined(MAX_THREADS)) {
            operation.get(MAX_THREADS).set(pool.get(MAX_THREADS));
        }
        if (pool.hasDefined(KEEPALIVE_TIME)) {
            operation.get(KEEPALIVE_TIME).set(pool.get(KEEPALIVE_TIME));
        }
        result.add(operation);
    }

    public static void addUnboundedQueueThreadPool(final ModelNode result, final ModelNode pool,  final PathElement... addressParts) {
        final ModelNode operation = Util.getEmptyOperation(ADD, pathAddress(addressParts));

        operation.get(NAME).set(pool.require(NAME));
        if (pool.hasDefined(THREAD_FACTORY)) {
            operation.get(THREAD_FACTORY).set(pool.get(THREAD_FACTORY));
        }
        if (pool.hasDefined(MAX_THREADS)) {
            operation.get(MAX_THREADS).set(pool.get(MAX_THREADS));
        }
        if (pool.hasDefined(KEEPALIVE_TIME)) {
            operation.get(KEEPALIVE_TIME).set(pool.get(KEEPALIVE_TIME));
        }
        result.add(operation);
    }

    public static ModelNode pathAddress(PathElement... elements) {
        return PathAddress.pathAddress(elements).toModelNode();
    }
}
