package org.jboss.as.service;

import java.lang.reflect.Method;

import org.jboss.logging.Logger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.Value;

/**
 * Service wrapper for legacy JBoss services that controls the service create and destroy lifecycle.
 *
 * @author John E. Bailey
 */
public class CreateDestroyService<T> implements Service<T> {
    private static final Logger log = Logger.getLogger("org.jboss.as.service");
    private final Value<T> serviceValue;

    /**
     * Construct new instance.
     *
     * @param serviceValue The service value
     */
    public CreateDestroyService(Value<T> serviceValue) {
        this.serviceValue = serviceValue;
    }

    /** {@inheritDoc} */
    public void start(final StartContext context) throws StartException {
        final T service = getValue();
        // Handle create
        log.debugf("Creating Service: %s", context.getController().getName());
        try {
            Method createMethod = service.getClass().getMethod("create");
            ClassLoader old = SecurityActions.setThreadContextClassLoader(service.getClass().getClassLoader());
            try {
                createMethod.invoke(service);
            } finally {
                SecurityActions.resetThreadContextClassLoader(old);
            }
        } catch(NoSuchMethodException e) {
        } catch(Exception e) {
            throw new StartException("Failed to execute legacy service create", e);
        }
    }

    /** {@inheritDoc} */
    public void stop(StopContext context) {
        final T service = getValue();
        // Handle destroy
        log.debugf("Destroying Service: %s", context.getController().getName());
        try {
            Method destroyMethod = service.getClass().getMethod("destroy");
            ClassLoader old = SecurityActions.setThreadContextClassLoader(service.getClass().getClassLoader());
            try {
                destroyMethod.invoke(service);
            } finally {
                SecurityActions.resetThreadContextClassLoader(old);
            }
            destroyMethod.invoke(service);
        } catch(NoSuchMethodException e) {
        } catch(Exception e) {
            log.error("Failed to execute legacy service destroy", e);
        }
    }

    /** {@inheritDoc} */
    public T getValue() throws IllegalStateException {
        return serviceValue.getValue();
    }
}
