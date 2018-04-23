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

import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

/**
 * Remote command execution response.
 *
 * @param <R> the response type
 * @author Paul Ferraro
 */
@Deprecated
public interface CommandResponse<R> {

    /**
     * @return response of the remote command execution
     * @throws ExecutionException exception that was raised during execution
     */
    R get() throws ExecutionException;

    static <R> CommandResponse<R> get(CompletionStage<R> stage) {
        return new CommandResponse<R>() {
            @Override
            public R get() throws ExecutionException {
                try {
                    return stage.toCompletableFuture().join();
                } catch (CompletionException e) {
                    throw new ExecutionException(e.getCause());
                }
            }
        };
    }
}