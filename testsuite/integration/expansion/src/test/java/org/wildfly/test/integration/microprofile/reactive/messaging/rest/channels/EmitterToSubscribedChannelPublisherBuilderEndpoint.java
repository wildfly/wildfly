/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.reactive.messaging.rest.channels;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;

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
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.jboss.resteasy.annotations.Stream;
import org.reactivestreams.Publisher;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
@Path("/emitter-to-subscribed-channel-publisher-builder")
@Produces(MediaType.TEXT_PLAIN)
@ApplicationScoped
public class EmitterToSubscribedChannelPublisherBuilderEndpoint {

    List<String> list = new ArrayList<>();

    @Inject
    @Channel("emitter-2")
    Emitter<String> emitter;

    CompletionStage<List<String>> completionStage;

    public EmitterToSubscribedChannelPublisherBuilderEndpoint() {
        //Needed for weld proxy
    }

    @Inject
    public EmitterToSubscribedChannelPublisherBuilderEndpoint(@Channel("emitter-2") PublisherBuilder<String> publisher) {
        // We need a subscription or the emitter.send() will fail
        completionStage = publisher.toList().run();
    }

    @POST
    @Path("/publish")
    @Produces("text/plain")
    public Response publish(@FormParam("value") String value) {
        if (value.equals("-end-")) {
            // We have to complete the emitter to end the 'stream' so the completion stage finishes
            emitter.complete();
        } else {
            emitter.send(value);
        }
        return Response.ok().build();
    }

    @GET
    @Path("/poll")
    @Produces("text/plain")
    @Stream
    public Publisher<String> poll() {
        PublisherBuilder<List<String>> listBuilder = ReactiveStreams.fromCompletionStage(completionStage);
        return listBuilder
                //Flat map to extract the list entries, and convert to
                // PublishBuilder<String> with the individual list entries
                .flatMap(list -> ReactiveStreams.of(list.toArray(new String[0])))
                .buildRs();
    }

}
