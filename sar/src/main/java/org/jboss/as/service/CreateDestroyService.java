package org.jboss.as.service;

import org.jboss.as.server.deployment.reflect.ClassReflectionIndex;
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

    private static final String CREATE_METHOD_NAME = "create";
    private static final String DESTROY_METHOD_NAME = "destroy";

    CreateDestroyService(final Object mBeanInstance, final ClassReflectionIndex<?> mBeanClassIndex) {
        super(mBeanInstance, mBeanClassIndex);
    }

    /** {@inheritDoc} */
    public void start(final StartContext context) throws StartException {
        log.debugf("Creating Service: %s", context.getController().getName());
        try {
            invokeLifecycleMethod(CREATE_METHOD_NAME);
        } catch (final Exception e) {
            throw new StartException("Failed to execute legacy service create() method", e);
        }
    }

    /** {@inheritDoc} */
    public void stop(final StopContext context) {
        log.debugf("Destroying Service: %s", context.getController().getName());
        try {
            invokeLifecycleMethod(DESTROY_METHOD_NAME);
        } catch (final Exception e) {
            log.error("Failed to execute legacy service destroy() method", e);
        }
    }

}
