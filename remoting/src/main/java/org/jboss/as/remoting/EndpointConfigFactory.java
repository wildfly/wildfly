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

package org.jboss.as.remoting;

import org.jboss.as.controller.ExpressionResolver;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.xnio.OptionMap;
import org.xnio.Options;

/**
 * Runtime configuration factory for remoting endpoints.
 *
 * @author Emanuel Muckenhuber
 */
public final class EndpointConfigFactory {

    private EndpointConfigFactory() {
        //
    }

    public static OptionMap create(final ExpressionResolver resolver, final ModelNode model) throws OperationFailedException {
        return create(resolver, model, OptionMap.EMPTY);
    }

    public static OptionMap create(final ExpressionResolver resolver, final ModelNode model, final OptionMap defaults) throws OperationFailedException {
        final OptionMap map = OptionMap.builder()
                .addAll(defaults)
                .set(Options.WORKER_READ_THREADS, RemotingSubsystemRootResource.WORKER_READ_THREADS.resolveModelAttribute(resolver, model).asInt())
                .set(Options.WORKER_TASK_CORE_THREADS, RemotingSubsystemRootResource.WORKER_TASK_CORE_THREADS.resolveModelAttribute(resolver, model).asInt())
                .set(Options.WORKER_TASK_KEEPALIVE, RemotingSubsystemRootResource.WORKER_TASK_KEEPALIVE.resolveModelAttribute(resolver, model).asInt())
                .set(Options.WORKER_TASK_LIMIT, RemotingSubsystemRootResource.WORKER_TASK_LIMIT.resolveModelAttribute(resolver, model).asInt())
                .set(Options.WORKER_TASK_MAX_THREADS, RemotingSubsystemRootResource.WORKER_TASK_MAX_THREADS.resolveModelAttribute(resolver, model).asInt())
                .set(Options.WORKER_WRITE_THREADS, RemotingSubsystemRootResource.WORKER_WRITE_THREADS.resolveModelAttribute(resolver, model).asInt())
                .set(Options.WORKER_READ_THREADS, RemotingSubsystemRootResource.WORKER_READ_THREADS.resolveModelAttribute(resolver, model).asInt())
                .getMap();
        return map;
    }

}
