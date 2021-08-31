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
