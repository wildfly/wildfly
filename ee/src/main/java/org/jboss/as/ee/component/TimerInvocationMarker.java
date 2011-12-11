package org.jboss.as.ee.component;

/**
 * Marker that is added to private data of an invocation to signify that it is a timer invocation
 *
 * @author Stuart Douglas
 */
public class TimerInvocationMarker {

    public static final TimerInvocationMarker INSTANCE = new TimerInvocationMarker();

    private TimerInvocationMarker() {

    }

}