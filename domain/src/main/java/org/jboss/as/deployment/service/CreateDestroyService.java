package org.jboss.as.deployment.service;

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

    /** {@inheritDoc */
    public void start(final StartContext context) throws StartException {
        final T service = getValue();
        // Handle create
        log.info("Creating Service: " + context.getController().getName());
        try {
            Method startMethod = service.getClass().getMethod("create");
            startMethod.invoke(service);
        } catch(NoSuchMethodException e) {
        } catch(Exception e) {
            throw new StartException("Failed to execute legacy service create", e);
        }
    }

    /** {@inheritDoc */
    public void stop(StopContext context) {
        final T service = getValue();
        // Handle destroy
        log.info("Destroying Service: " + context.getController().getName());
        try {
            Method startMethod = service.getClass().getMethod("destroy");
            startMethod.invoke(service);
        } catch(NoSuchMethodException e) {
        } catch(Exception e) {
            throw new IllegalStateException("Failed to execute legacy service destroy", e);
        }
    }

    /** {@inheritDoc */
    public T getValue() throws IllegalStateException {
        return serviceValue.getValue();
    }
}
