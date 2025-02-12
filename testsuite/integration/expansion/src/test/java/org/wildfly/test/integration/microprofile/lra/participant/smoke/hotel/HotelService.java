/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.lra.participant.smoke.hotel;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import org.wildfly.test.integration.microprofile.lra.participant.smoke.model.Booking;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class HotelService {
    private Map<String, Booking> bookings = new HashMap<>();

    public Booking book(String bid, String hotel) {
        Booking booking = new Booking(bid, hotel, "Hotel");
        Booking earlierBooking = bookings.putIfAbsent(booking.getId(), booking);
        return earlierBooking == null ? booking : earlierBooking;
    }

    public Booking get(String bookingId) throws NotFoundException {
        if (!bookings.containsKey(bookingId))
            throw new NotFoundException(Response.status(404).entity("Invalid bookingId id: " + bookingId).build());

        return bookings.get(bookingId);
    }

    public Collection<Booking> getAll() {
        return bookings.values();
    }
}