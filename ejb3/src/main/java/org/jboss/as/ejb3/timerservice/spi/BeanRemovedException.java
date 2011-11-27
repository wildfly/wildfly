package org.jboss.as.ejb3.timerservice.spi;

/**
 * Exception used to signal the timer implementation that the entity bean no longer exists and the timer should be removed.
 *
 * @author Stuart Douglas
 */
public class BeanRemovedException extends Exception {

    public BeanRemovedException(final Throwable cause) {
        super(cause);
    }
}
