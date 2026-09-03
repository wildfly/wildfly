/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.lra.participant.jwt;

import static org.eclipse.microprofile.lra.annotation.ws.rs.LRA.LRA_HTTP_CONTEXT_HEADER;

import java.util.Collection;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.lra.annotation.Compensate;
import org.eclipse.microprofile.lra.annotation.Complete;
import org.eclipse.microprofile.lra.annotation.ws.rs.LRA;
import org.wildfly.test.integration.microprofile.lra.participant.jwt.model.JwtBooking;

import com.fasterxml.jackson.core.JsonProcessingException;

import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * LRA Participant that demonstrates JWT propagation through LRA lifecycle methods.
 * This participant injects JsonWebToken to verify that JWT claims are accessible
 * during LRA operations including @Complete and @Compensate callbacks.
 *
 */
@Singleton
@Path(JwtLraParticipant.JWT_LRA_PARTICIPANT_PATH)
@LRA(LRA.Type.SUPPORTS)
@DenyAll
public class JwtLraParticipant {
    public static final String JWT_LRA_PARTICIPANT_PATH = "/jwt-lra-participant";
    public static final String TRANSACTION_COMPLETE = "/complete";
    public static final String TRANSACTION_COMPENSATE = "/compensate";

    @Inject
    private JwtLraService service;

    @Inject
    private JsonWebToken jwt;

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @LRA(value = LRA.Type.REQUIRED, end = false)
    @RolesAllowed({"Subscriber"})
    public JwtBooking bookRoom(@HeaderParam(LRA_HTTP_CONTEXT_HEADER) String lraId,
                               @QueryParam("hotelName") @DefaultValue("Default") String hotelName) {
        // Capture JWT principal during booking - guaranteed non-null by container authentication
        String principal = jwt.getName();
        return service.book(lraId, hotelName, principal);
    }

    @PUT
    @Path(TRANSACTION_COMPLETE)
    @Produces(MediaType.APPLICATION_JSON)
    @Complete
    @RolesAllowed({"Subscriber", "ServiceAccount"})
    public Response completeWork(@HeaderParam(LRA_HTTP_CONTEXT_HEADER) String lraId,
                                  @HeaderParam("Authorization") String authHeader) throws NotFoundException, JsonProcessingException {
        // Authentication enforced by container via @RolesAllowed
        // Container guarantees jwt is non-null and valid due to @RolesAllowed
        String principal = jwt.getName();

        JwtBooking booking = service.get(lraId);
        booking.setStatus(JwtBooking.BookingStatus.CONFIRMED);
        booking.setCompletePrincipal(principal);
        return Response.ok(booking.toJson()).build();
    }

    @PUT
    @Path(TRANSACTION_COMPENSATE)
    @Produces(MediaType.APPLICATION_JSON)
    @Compensate
    @RolesAllowed({"Subscriber", "ServiceAccount"})
    public Response compensateWork(@HeaderParam(LRA_HTTP_CONTEXT_HEADER) String lraId,
                                   @HeaderParam("Authorization") String authHeader) throws NotFoundException, JsonProcessingException {
        // Authentication enforced by container via @RolesAllowed
        // Container guarantees jwt is non-null and valid due to @RolesAllowed
        String principal = jwt.getName();

        JwtBooking booking = service.get(lraId);
        booking.setStatus(JwtBooking.BookingStatus.CANCELLED);
        booking.setCompensatePrincipal(principal);
        return Response.ok(booking.toJson()).build();
    }

    @GET
    @Path("/{bookingId}")
    @Produces(MediaType.APPLICATION_JSON)
    @LRA(LRA.Type.NOT_SUPPORTED)
    @RolesAllowed({"Subscriber", "ServiceAccount"})
    public JwtBooking getBooking(@PathParam("bookingId") String bookingId) throws JsonProcessingException {
        return service.get(bookingId);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"Subscriber", "ServiceAccount"})
    public Collection<JwtBooking> getAll() {
        return service.getAll();
    }
}
