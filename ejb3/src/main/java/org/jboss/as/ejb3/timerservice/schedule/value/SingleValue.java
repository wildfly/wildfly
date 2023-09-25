/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.timerservice.schedule.value;

/**
 * Represents a value for a {@link jakarta.ejb.ScheduleExpression} which is expressed as a single value
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class SingleValue implements ScheduleValue {

    /**
     * The value
     */
    private final String value;

    /**
     * @param val
     */
    public SingleValue(String val) {
        this.value = val.trim();
    }

    public String getValue() {
        return this.value;
    }
}
