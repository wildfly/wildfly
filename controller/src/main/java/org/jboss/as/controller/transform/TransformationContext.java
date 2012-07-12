package org.jboss.as.controller.transform;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;

/**
* @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
*/
public interface TransformationContext {

    /**
     * Get the transformation target.
     *
     * @return the transformation target
     */
    TransformationTarget getTarget();

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

    /**
     * Resolve an expression.
     *
     * In some cases an outdated target might not understand expressions for a given attribute, therefore it needs to be
     * resolve before being sent to the target.
     *
     * @param node the node
     * @return the resolved expression
     * @throws OperationFailedException
     */
    ModelNode resolveExpressions(ModelNode node) throws OperationFailedException;

}
