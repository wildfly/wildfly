/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.restclient.deployment.resource;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.wildfly.test.integration.microprofile.restclient.deployment.model.Message;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@Path("/msg")
@ApplicationScoped
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class MessageResource {

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final AtomicInteger messageId = new AtomicInteger();
    private final Map<Integer, Message> messages = new HashMap<>();

    @Inject
    @ConfigProperty(name = "org.wildfly.expansion.test.deployment.name", defaultValue = "unknown")
    private String source;

    @Inject
    private UriInfo uriInfo;

    @GET
    public List<Message> messages() {
        lock.readLock().lock();
        try {
            return List.copyOf(messages.values());
        } finally {
            lock.readLock().unlock();
        }
    }

    @GET
    @Path("{id}")
    public Message get(@PathParam("id") final int id) {
        lock.readLock().lock();
        try {
            return messages.get(id);
        } finally {
            lock.readLock().unlock();
        }
    }

    @POST
    public Response push(final Message message) {
        lock.writeLock().lock();
        try {
            if (message.getId() == 0L) {
                message.setId(messageId.incrementAndGet());
                message.setTarget(source);
                messages.put(message.getId(), message);
                return Response.created(createGetUri(message.getId()))
                        .build();
            }
            if (messages.containsKey(message.getId())) {
                messages.put(message.getId(), message);
                return Response.status(Response.Status.NOT_MODIFIED)
                        .location(createGetUri(message.getId()))
                        .build();
            }
            return Response.created(createGetUri(message.getId())).build();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @DELETE
    @Path("{id}")
    public Response delete(@PathParam("id") final int id) {
        lock.writeLock().lock();
        try {
            final Message user = messages.get(id);
            if (user == null) {
                return Response.notModified().build();
            }
            messages.remove(id);
            return Response.ok(user).build();
        } finally {
            lock.writeLock().unlock();
        }
    }

    private URI createGetUri(final int id) {
        return uriInfo.getBaseUriBuilder().path("/msg/" + id).build();
    }
}
