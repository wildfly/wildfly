/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.as.clustering.jgroups.subsystem;

/**
 * Metric keys for the JGroups subsystem
 *
 * @author Richard Achmatowicz (c) 2013 Red Hat Inc.
 */
public class MetricKeys {
    public static final String ADDRESS = "address";
    public static final String ADDRESS_AS_UUID = "address-as-uuid";
    public static final String CHANNEL = "channel";
    public static final String DISCARD_OWN_MESSAGES  = "discard-own-messages";
    public static final String NUM_TASKS_IN_TIMER = "num-tasks-in-timer";
    public static final String NUM_TIMER_THREADS = "num-timer-threads";
    public static final String RECEIVED_BYTES = "received-bytes";
    public static final String RECEIVED_MESSAGES = "received-messages";
    public static final String SENT_BYTES = "sent-bytes";
    public static final String SENT_MESSAGES = "sent-messages";
    public static final String STATE = "state";
    public static final String STATS_ENABLED = "stats-enabled";
    public static final String VERSION = "version";
    public static final String VIEW = "view";
}
