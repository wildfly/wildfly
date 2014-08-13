package org.jboss.as.ee.component.interceptors;

/**
 * @author Stuart Douglas
 */
public class ComponentSuspendedException extends IllegalStateException {

    public ComponentSuspendedException() {
    }

    public ComponentSuspendedException(String s) {
        super(s);
    }

    public ComponentSuspendedException(String message, Throwable cause) {
        super(message, cause);
    }

    public ComponentSuspendedException(Throwable cause) {
        super(cause);
    }
}
