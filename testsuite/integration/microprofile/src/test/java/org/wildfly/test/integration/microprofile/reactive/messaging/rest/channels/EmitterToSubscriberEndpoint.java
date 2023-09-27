/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.reactive.messaging.rest.channels;

import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.jboss.resteasy.annotations.Stream;
import org.reactivestreams.Publisher;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
@Path("/emitter-to-subscriber")
@Produces(MediaType.TEXT_PLAIN)
@ApplicationScoped
public class EmitterToSubscriberEndpoint {

    List<String> list = new ArrayList<>();

    @Inject
    @Channel("emitter-1")
    Emitter<String> emitter;

    @Incoming("emitter-1")
    public void dest(String s) {
        list.add(s);
    }

    @POST
    @Path("/publish")
    @Produces("text/plain")
    public Response publish(@FormParam("value") String value) {
        emitter.send(value);
        return Response.ok().build();
    }

    @GET
    @Path("/poll")
    @Produces("text/plain")
    @Stream
    public Publisher<String> poll() {
        return ReactiveStreams.fromIterable(list).buildRs();
    }

}
