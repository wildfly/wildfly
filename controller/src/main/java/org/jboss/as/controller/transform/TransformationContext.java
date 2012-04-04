package org.jboss.as.controller.transform;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;

/**
* @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
*/
public interface TransformationContext {

    /**
     * Get the type of process in which this operation is executing.
     *
     * @return the process type. Will not be {@code null}
     */
    ProcessType getProcessType();

    /**
     * Gets the running mode of the process.
     *
     * @return   the running mode. Will not be {@code null}
     */
    RunningMode getRunningMode();

    /**
     * Get the management resource registration.
     *
     * @param address the path address
     * @return the registration
     */
    ImmutableManagementResourceRegistration getResourceRegistration(PathAddress address);

    /**
     * Get the management resource registration.
     *
     * @param address the path address
     * @return the registration
     */
    ImmutableManagementResourceRegistration getResourceRegistrationFromRoot(PathAddress address);

    /**
     * Read a model resource.
     *
     * @param address the path address
     * @return a read-only resource
     */
    Resource readResource(PathAddress address);

    /**
     * Read a model resource from the root.
     *
     * @param address the path address
     * @return the read-only resource
     */
    Resource readResourceFromRoot(PathAddress address);

}
