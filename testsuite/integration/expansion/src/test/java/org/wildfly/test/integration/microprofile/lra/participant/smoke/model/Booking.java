/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.lra.participant.smoke.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

public class Booking {
    @JsonProperty("id")
    private String id;
    @JsonProperty("name")
    private String name;
    @JsonProperty("status")
    private BookingStatus status;
    @JsonProperty("type")
    private String type;
    @JsonProperty("details")
    private Booking[] details;

    private IOException decodingException;

    public Booking(String id, String type, Booking... bookings) {
        this(id, "Aggregate Booking", type, BookingStatus.PROVISIONAL, bookings);
    }

    public Booking(String id, String name, String type) {
        this(id, name, type, BookingStatus.PROVISIONAL, null);
    }

    @JsonCreator
    public Booking(@JsonProperty("id") String id,
                   @JsonProperty("name") String name,
                   @JsonProperty("type") String type,
                   @JsonProperty("status") BookingStatus status,
                   @JsonProperty("details") Booking[] details) {

        init(id, name, type, status, details);
    }

    public Booking(IOException decodingException) {
        this.decodingException = decodingException;
    }

    public Booking(Booking booking) {
        this.init(booking.getId(), booking.getName(), booking.getType(), booking.getStatus(), null);

        details = new Booking[booking.getDetails().length];

        IntStream.range(0, details.length).forEach(i -> details[i] = new Booking(booking.getDetails()[i]));
    }

    public static Booking fromJson(String json) {
        try {
            return new ObjectMapper().readValue(json, Booking.class);
        } catch (IOException e) {
            return new Booking(e);
        }
    }

    public static List<Booking> listFromJson(String json) throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        return Arrays.asList(mapper.readValue(json, Booking[].class));
    }

    private void init(String id, String name, String type, BookingStatus status, Booking[] details) {
        this.id = id;
        this.name = name == null ? "" : name;
        this.type = type == null ? "" : type;
        this.status = status;
        this.details = details == null ? new Booking[0] : removeNullEnElements(details);
    }

    @SuppressWarnings("unchecked")
    private <T> T[] removeNullEnElements(T[] a) {
        List<T> list = new ArrayList<T>(Arrays.asList(a));
        list.removeAll(Collections.singleton(null));
        return list.toArray((T[]) Array.newInstance(a.getClass().getComponentType(), list.size()));
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public Booking[] getDetails() {
        return details;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }

    public void requestCancel() {
        status = BookingStatus.CANCEL_REQUESTED;
    }

    @JsonIgnore
    public boolean isCancelPending() {
        return status == BookingStatus.CANCEL_REQUESTED;
    }

    public String toString() {
        return String.format("{\"id\":\"%s\",\"name\":\"%s\",\"type\":\"%s\",\"status\":\"%s\"}",
            id, name, type, status);
    }

    public boolean merge(Booking booking) {
        if (!id.equals(booking.getId()))
            return false; // or throw an exception

        name = booking.getName();
        status = booking.getStatus();

        return true;
    }

    @JsonIgnore
    public String getEncodedId() {
        try {
            return URLEncoder.encode(id, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return id; // TODD do it in the constructor
        }
    }

    public String toJson() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();

        return objectMapper.writeValueAsString(this);
    }

    @JsonIgnore
    public IOException getDecodingException() {
        return decodingException;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Booking booking = (Booking) o;

        if (!getId().equals(booking.getId())) return false;
        if (!getName().equals(booking.getName())) return false;
        if (!getType().equals(booking.getType())) return false;
        return true;
    }

    @Override
    public int hashCode() {
        int result = getId().hashCode();
        result = 31 * result + getName().hashCode();
        result = 31 * result + getType().hashCode();
        return result;
    }

    public enum BookingStatus {
        CONFIRMED, CANCELLED, PROVISIONAL, CONFIRMING, CANCEL_REQUESTED
    }
}