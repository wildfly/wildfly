/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.domain.controller;

import java.util.List;

import org.jboss.dmr.ModelNode;

/**
 * @author Emanuel Muckenhuber
 */
public interface HostConnectionInfo {

    String ADDRESS = "address";
    String CONNECTED = "connected";
    String EVENTS = "events";
    String TIMESTAMP = "timestamp";
    String TYPE = "type";

    /**
     * Get the host name.
     *
     * @return the host name
     */
    String getHostName();

    /**
     * Whether the host is connected or not.
     *
      * @return {@code true} if the host is connected, {@code false} otherwise
     */
    boolean isConnected();

    /**
     * Get a list of connection events.
     *
     * @return the events
     */
    List<Event> getEvents();

    public interface Event {

        /**
         * Get the type of event.
         *
         * @return the event type
         */
        EventType getEventType();

        /**
         * Get the remote address
         *
         * @return the remote address
         */
        String getPeerAddress();

        /**
         * The timestamp the even occurred.
         *
         * @return the timestamp of the event
         */
        long getTimestamp();

        /**
         * Fill DMR.
         *
         * @param target the target model
         */
        void toModelNode(final ModelNode target);

    }

    public class Events {

        private Events() { }

        public static Event create(EventType type, final String address) {
            return new BasicEventImpl(type, address);
        }

    }

    public class BasicEventImpl implements Event {

        private final EventType type;
        private final String address;
        private final long timeStamp;

        public BasicEventImpl(EventType type, String address) {
            this.type = type;
            this.address = address;
            this.timeStamp = System.currentTimeMillis();
        }

        @Override
        public EventType getEventType() {
            return type;
        }

        @Override
        public String getPeerAddress() {
            return address;
        }

        @Override
        public long getTimestamp() {
            return timeStamp;
        }

        @Override
        public void toModelNode(ModelNode target) {
            target.get(TYPE);
            target.get(ADDRESS);
            if (type != null) {
                target.get(TYPE).set(type.getName());
            }
            if (address != null) {
                target.get(ADDRESS).set(address);
            }
            target.get(TIMESTAMP).set(timeStamp);
        }
    }


    public enum EventType {

        REGISTERED("registered"),
        REGISTRATION_EXISTING("duplicate-registration"),
        REGISTRATION_REJECTED("registration-rejected"),
        REGISTRATION_FAILED("registration-failed"),
        UNCLEAN_UNREGISTRATION("unclean-unregistration"),
        UNREGISTERED("unregistered"),
        UNKNOWN("unknown"),
        ;

        private final String name;
        private EventType(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

}
