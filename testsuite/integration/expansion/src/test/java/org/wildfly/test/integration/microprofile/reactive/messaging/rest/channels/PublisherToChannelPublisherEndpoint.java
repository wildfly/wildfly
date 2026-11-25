/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.reactive.messaging.rest.channels;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.jboss.resteasy.annotations.Stream;
import org.reactivestreams.Publisher;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
@Path("/publisher-to-channel-publisher")
@Produces(MediaType.TEXT_PLAIN)
@ApplicationScoped
public class PublisherToChannelPublisherEndpoint {

    @Outgoing("generator")
    public PublisherBuilder<String> generate() {
        return ReactiveStreams.of("One", "Zwei", "Tres");
    }

    @Inject
    @Channel("generator")
    Publisher<String> publisher;

    @GET
    @Path("/poll")
    @Produces("text/plain")
    @Stream
    public Publisher<String> poll() {
        return publisher;
    }
}
