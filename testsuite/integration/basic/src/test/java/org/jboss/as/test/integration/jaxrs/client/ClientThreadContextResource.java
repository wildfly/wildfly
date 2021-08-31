/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2021 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.as.test.integration.jaxrs.client;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.concurrent.ManagedScheduledExecutorService;
import javax.enterprise.concurrent.ManagedThreadFactory;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import org.jboss.as.test.shared.TimeoutUtil;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@Path("/context")
public class ClientThreadContextResource {

    @Resource
    private ManagedExecutorService executor;

    @Resource
    private ManagedScheduledExecutorService scheduledExecutor;

    @Resource
    private ManagedThreadFactory threadFactory;

    @GET
    @Path("/async")
    @Produces(MediaType.TEXT_PLAIN)
    public CompletionStage<String> async(@Context final UriInfo uriInfo) {
        final CompletableFuture<String> cs = new CompletableFuture<>();
        executor.execute(() -> {
            try {
                cs.complete(uriInfo.getPath());
            } catch (Exception e) {
                cs.completeExceptionally(e);
            }
        });
        return cs;
    }

    @GET
    @Path("/async/delayed")
    @Produces(MediaType.TEXT_PLAIN)
    @SuppressWarnings("MagicNumber")
    public CompletionStage<String> delayedAsync(@Context final UriInfo uriInfo) {
        final CompletableFuture<String> cs = new CompletableFuture<>();
        scheduledExecutor.schedule(() -> {
            try {
                cs.complete(uriInfo.getPath());
            } catch (Exception e) {
                cs.completeExceptionally(e);
            }
        }, 200, TimeUnit.MILLISECONDS);
        return cs;
    }

    @GET
    @Path("/async/thread-factory")
    @Produces(MediaType.TEXT_PLAIN)
    public CompletionStage<String> threadFactory(@Context final UriInfo uriInfo) {
        final ExecutorService executor = Executors.newSingleThreadExecutor(threadFactory);
        final CompletableFuture<String> cs = new CompletableFuture<>();
        executor.submit(() -> {
            try {
                cs.complete(uriInfo.getPath());
            } catch (Exception e) {
                cs.completeExceptionally(e);
            } finally {
                executor.shutdownNow();
            }
        });
        return cs;
    }

    @GET
    @Path("/async/scheduled/{count}")
    @Produces(MediaType.APPLICATION_JSON)
    @SuppressWarnings("MagicNumber")
    public Set<String> scheduled(@Context final UriInfo uriInfo, @PathParam("count") final int count) throws Exception {
        final Set<String> collected = new ConcurrentSkipListSet<>(String::compareTo);
        final ScheduledFuture<?> sf = scheduledExecutor.scheduleAtFixedRate(() -> {
            final long current = collected.size();
            if (current <= count) {
                collected.add(uriInfo.getPath() + "-" + current);
            }
        }, 0L, TimeoutUtil.adjust(600), TimeUnit.MILLISECONDS);
        try {
            // Wait no more than 5 seconds
            long timeout = TimeUnit.SECONDS.toMillis(TimeoutUtil.adjust(5));
            while (timeout > 0) {
                if (collected.size() == 3) {
                    break;
                }
                TimeUnit.MILLISECONDS.sleep(100L);
                timeout -= 100L;
            }
            if (timeout < 0) {
                throw new WebApplicationException("Scheduled tasks did not complete within 5 seconds");
            }
        } finally {
            sf.cancel(true);
        }
        return collected;
    }
}
