/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.reactive.messaging.multiple.earmodule.war;

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

import org.wildfly.test.integration.microprofile.reactive.messaging.multiple.earmodule.amqp.AmqpMessagingBean;
import org.wildfly.test.integration.microprofile.reactive.messaging.multiple.earmodule.kafka.KafkaMessagingBean;
import org.wildfly.test.integration.microprofile.reactive.messaging.multiple.earmodule.memory.InVmMessagingBean;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
@Path("/multi")
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class MultiModuleEndpoint {

    @Inject
    InVmMessagingBean inVmMessagingBean;

    @Inject
    KafkaMessagingBean kafkaMessagingBean;

    @Inject
    AmqpMessagingBean amqpMessagingBean;

    @POST
    @Path("/invm")
    public Response publishInVm(@FormParam("value") String value) {
        inVmMessagingBean.send(value);
        return Response.ok().build();
    }

    @GET
    @Path("/invm")
    public List<String> readInVmMessages() {
        return inVmMessagingBean.getReceived();
    }

    @POST
    @Path("/kafka")
    public Response publishKafka(@FormParam("value") String value) {
        kafkaMessagingBean.send(value);
        return Response.ok().build();
    }

    @GET
    @Path("/kafka")
    public List<String> readKafkaMessages() {
        return kafkaMessagingBean.getReceived();
    }

    @POST
    @Path("/amqp")
    public Response publishAmqp(@FormParam("value") String value) {
        amqpMessagingBean.send(value);
        return Response.ok().build();
    }

    @GET
    @Path("/amqp")
    public List<String> readAmqpMessages() {
        return amqpMessagingBean.getReceived();
    }

}
