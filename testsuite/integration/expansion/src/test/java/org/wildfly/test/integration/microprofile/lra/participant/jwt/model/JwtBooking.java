/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.lra.participant.jwt.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Booking model that tracks JWT principal information during LRA lifecycle.
 *
 */
public class JwtBooking {
    private String id;
    private String name;
    private BookingStatus status;
    private String bookingPrincipal;
    private String completePrincipal;
    private String compensatePrincipal;

    public enum BookingStatus {
        PROVISIONAL, CONFIRMED, CANCELLED
    }

    public JwtBooking() {
    }

    public JwtBooking(String id, String name, String bookingPrincipal) {
        this.id = id;
        this.name = name;
        this.status = BookingStatus.PROVISIONAL;
        this.bookingPrincipal = bookingPrincipal;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }

    public String getBookingPrincipal() {
        return bookingPrincipal;
    }

    public void setBookingPrincipal(String bookingPrincipal) {
        this.bookingPrincipal = bookingPrincipal;
    }

    public String getCompletePrincipal() {
        return completePrincipal;
    }

    public void setCompletePrincipal(String completePrincipal) {
        this.completePrincipal = completePrincipal;
    }

    public String getCompensatePrincipal() {
        return compensatePrincipal;
    }

    public void setCompensatePrincipal(String compensatePrincipal) {
        this.compensatePrincipal = compensatePrincipal;
    }

    public String toJson() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(this);
    }

    @Override
    public String toString() {
        return "JwtBooking{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", status=" + status +
                ", bookingPrincipal='" + bookingPrincipal + '\'' +
                ", completePrincipal='" + completePrincipal + '\'' +
                ", compensatePrincipal='" + compensatePrincipal + '\'' +
                '}';
    }
}
