/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.xts.util;

/**
 * XTS callbacks which could be logged to event log and then checked in tests.
 */
public enum EventLogEvent {

    // XTS AT callbacks events
    BEFORE_PREPARE, // volatile participant
    PREPARE, // durable participant
    ERROR,
    ROLLBACK, // durable participant
    VOLATILE_ROLLBACK, // volatile participant
    COMMIT, // durable participant
    VOLATILE_COMMIT, // volatile participant
    @Deprecated UNKNOWN,

    // XTS BA callbacks events
    COMPLETE,
    CONFIRM_COMPLETED,
    CONFIRM_FAILED, // method confirmCompleted called with false
    CLOSE,
    CANCEL,
    COMPENSATE
}
