/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.reactive.messaging.rest.channels;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

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
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.jboss.resteasy.annotations.Stream;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import io.smallrye.reactive.messaging.kafka.api.KafkaMetadataUtil;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
@Path("/emitter-to-subscribed-channel-publisher-via-kafka")
@Produces(MediaType.TEXT_PLAIN)
@ApplicationScoped
public class EmitterToChannelPublisherViaKafkaEndpoint {

    @Inject
    @Channel("to-kafka")
    Emitter<String> emitter;

    List<String> values = new ArrayList<>();
    List<Integer> partitions = new ArrayList();

    public EmitterToChannelPublisherViaKafkaEndpoint() {
    }

    @Inject
    public EmitterToChannelPublisherViaKafkaEndpoint(@Channel("from-kafka") Publisher<Message<String>> publisher) {
        AtomicReference<Subscription> ref = new AtomicReference<>();
        publisher.subscribe(new Subscriber<Message<String>>() {
            @Override
            public void onSubscribe(Subscription subscription) {
                subscription.request(1);
                ref.set(subscription);
            }

            @Override
            public void onNext(Message<String> msg) {
                values.add(msg.getPayload());
                partitions.add(KafkaMetadataUtil.readIncomingKafkaMetadata(msg).get().getPartition());
                ref.get().request(1);
            }

            @Override
            public void onError(Throwable throwable) {
                ref.get().cancel();
            }

            @Override
            public void onComplete() {
                ref.get().cancel();
            }
        });
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
        return ReactiveStreams.of(values.toArray(new String[values.size()])).buildRs();
    }

    @GET
    @Path("/partitions")
    @Produces("text/plain")
    public List<Integer> getPartitions() {
        return partitions;
    }
}
