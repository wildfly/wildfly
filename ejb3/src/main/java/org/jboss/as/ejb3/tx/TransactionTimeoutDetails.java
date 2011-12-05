package org.jboss.as.ejb3.tx;

import java.util.concurrent.TimeUnit;

public class TransactionTimeoutDetails {

    private final long value;

    private final TimeUnit timeUnit;

    public TransactionTimeoutDetails(final long value, final TimeUnit timeUnit) {
        this.value = value;
        this.timeUnit = timeUnit;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    public long getValue() {
        return value;
    }

    public int seconds() {
        return (int)(timeUnit==null?value:timeUnit.toSeconds(value));
    }
}
