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
        if (SarLogger.ROOT_LOGGER.isTraceEnabled()) {
            SarLogger.ROOT_LOGGER.tracef("Creating Service: %s", context.getController().getName());
        }
        try {
            invokeLifecycleMethod(createMethod);
        } catch (final Exception e) {
            throw SarMessages.MESSAGES.failedExecutingLegacyMethod(e, "create()");
        }
    }

    /** {@inheritDoc} */
    public void stop(final StopContext context) {
        if (SarLogger.ROOT_LOGGER.isTraceEnabled()) {
            SarLogger.ROOT_LOGGER.tracef("Destroying Service: %s", context.getController().getName());
        }
        try {
            invokeLifecycleMethod(destroyMethod);
        } catch (final Exception e) {
            SarLogger.ROOT_LOGGER.failedExecutingLegacyMethod(e, "create()");
        }
    }

}
