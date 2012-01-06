package org.jboss.as.service;

import java.lang.reflect.Method;

import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * Service wrapper for legacy JBoss services that controls the service create and destroy lifecycle.
 *
 * @author John E. Bailey
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class CreateDestroyService extends AbstractService {

    private final Method createMethod;
    private final Method destroyMethod;

    CreateDestroyService(final Object mBeanInstance, final Method createMethod, final Method destroyMethod) {
        super(mBeanInstance);
        this.createMethod = createMethod;
        this.destroyMethod = destroyMethod;
    }

    /** {@inheritDoc} */
    public void start(final StartContext context) throws StartException {
        if (log.isTraceEnabled()) {
            log.tracef("Creating Service: %s", context.getController().getName());
        }
        try {
            invokeLifecycleMethod(createMethod);
        } catch (final Exception e) {
            throw new StartException("Failed to execute legacy service create() method", e);
        }
    }

    /** {@inheritDoc} */
    public void stop(final StopContext context) {
        if (log.isTraceEnabled()) {
            log.tracef("Destroying Service: %s", context.getController().getName());
        }
        try {
            invokeLifecycleMethod(destroyMethod);
        } catch (final Exception e) {
            log.error("Failed to execute legacy service destroy() method", e);
        }
    }

}
