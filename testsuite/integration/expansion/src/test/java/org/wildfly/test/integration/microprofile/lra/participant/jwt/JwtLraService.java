/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.lra.participant.jwt;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.NotFoundException;
import org.wildfly.test.integration.microprofile.lra.participant.jwt.model.JwtBooking;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for managing JWT-aware LRA bookings.
 *
 */
@ApplicationScoped
public class JwtLraService {
    private final Map<String, JwtBooking> bookings = new HashMap<>();

    public JwtBooking book(String lraId, String name, String principal) {
        JwtBooking booking = new JwtBooking(lraId, name, principal);
        bookings.put(lraId, booking);
        return booking;
    }

    public JwtBooking get(String lraId) {
        JwtBooking booking = bookings.get(lraId);
        if (booking == null) {
            throw new NotFoundException("Booking not found for LRA: " + lraId);
        }
        return booking;
    }

    public Collection<JwtBooking> getAll() {
        return bookings.values();
    }
}
