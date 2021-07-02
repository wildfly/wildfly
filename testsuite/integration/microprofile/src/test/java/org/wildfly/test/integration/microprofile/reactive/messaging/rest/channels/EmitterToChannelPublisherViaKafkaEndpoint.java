/*
 * Copyright 2020 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.test.integration.microprofile.reactive.messaging.rest.channels;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

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
