package org.jboss.as.service;

import java.lang.reflect.Method;

import org.jboss.as.naming.ManagedReference;
import org.jboss.as.service.component.ServiceComponentInstantiator;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * Service wrapper for legacy JBoss services that controls the service create and destroy lifecycle.
 *
 * @author John E. Bailey
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @author Eduardo Martins
 */
final class CreateDestroyService extends AbstractService {

    private final Method createMethod;
    private final Method destroyMethod;

    private final ServiceComponentInstantiator componentInstantiator;
    private ManagedReference managedReference;

    CreateDestroyService(final Object mBeanInstance, final Method createMethod, final Method destroyMethod, ServiceComponentInstantiator componentInstantiator, final ServiceName duServiceName) {
        super(mBeanInstance, duServiceName);
        this.createMethod = createMethod;
        this.destroyMethod = destroyMethod;
        this.componentInstantiator = componentInstantiator;
    }

    /** {@inheritDoc} */
    public void start(final StartContext context) throws StartException {
        if (SarLogger.ROOT_LOGGER.isTraceEnabled()) {
            SarLogger.ROOT_LOGGER.tracef("Creating Service: %s", context.getController().getName());
        }
        try {
            invokeLifecycleMethod(createMethod, context);
        } catch (final Exception e) {
            throw SarMessages.MESSAGES.failedExecutingLegacyMethod(e, "create()");
        }
        if(componentInstantiator != null) {
            managedReference = componentInstantiator.initializeInstance(getValue());
        }
    }

    /** {@inheritDoc} */
    public void stop(final StopContext context) {
        if (SarLogger.ROOT_LOGGER.isTraceEnabled()) {
            SarLogger.ROOT_LOGGER.tracef("Destroying Service: %s", context.getController().getName());
        }
        if(managedReference != null) {
            managedReference.release();
        }
        try {
            invokeLifecycleMethod(destroyMethod, context);
        } catch (final Exception e) {
            SarLogger.ROOT_LOGGER.failedExecutingLegacyMethod(e, "create()");
        }
    }

}
