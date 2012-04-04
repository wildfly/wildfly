package org.jboss.as.controller.transform;

/**
 * Interface that subsystems have to implement to enable custom transformation of subsystem model
 * Every version of model must have its own implementation of this interface.
 *
 * SubsystemTransformer's must then be registered in extension by calling
 * {@link org.jboss.as.controller.SubsystemRegistration#registerSubsystemTransformer(SubsystemTransformer)}
 *
 *
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 */
public interface SubsystemTransformer extends ResourceTransformer, OperationTransformer {
    int getMajorManagementVersion();

    int getMinorManagementVersion();
}
