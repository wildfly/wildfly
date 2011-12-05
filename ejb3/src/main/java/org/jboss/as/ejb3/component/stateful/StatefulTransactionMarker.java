package org.jboss.as.ejb3.component.stateful;

/**
 * Marker class that is added to the invocation context to indicate to inner interceptors if this
 * SFSB is participating in a transaction or not.
 *
 * @author Stuart Douglas
 */
public class StatefulTransactionMarker {

    private static final StatefulTransactionMarker FIRST = new StatefulTransactionMarker(true);
    private static final StatefulTransactionMarker SECOND = new StatefulTransactionMarker(false);

    public static StatefulTransactionMarker of(boolean firstInvocation) {
        return firstInvocation ? FIRST : SECOND;
    }

    private final boolean firstInvocation;

    private StatefulTransactionMarker(final boolean firstInvocation) {
        this.firstInvocation = firstInvocation;
    }

    public boolean isFirstInvocation() {
        return firstInvocation;
    }
}
