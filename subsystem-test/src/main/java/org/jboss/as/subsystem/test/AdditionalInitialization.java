package org.jboss.as.subsystem.test;

import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationContext.Type;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.subsystem.test.ModelDescriptionValidator.ValidationConfiguration;
import org.jboss.msc.service.ServiceTarget;

/**
 * Allows you to additionally initialize the service container and the model controller
 * beyond the subsystem being tested. Override this class to add behaviour.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class AdditionalInitialization extends AdditionalParsers {

    /**
     * Return {@code OperationContext.Type#MANAGEMENT} to only affect the model from your tested operations, and thus avoid installing services into the service controller.
     * Return {@code OperationContext.Type#SERVER} to install services, this is the default.
     *
     * @return the type
     */
    protected OperationContext.Type getType() {
        return Type.SERVER;
    }

    protected ProcessType getProcessType() {
        OperationContext.Type contextType = getType();
        return (contextType == Type.HOST) ? ProcessType.HOST_CONTROLLER : ProcessType.STANDALONE_SERVER;
    }

    protected RunningMode getRunningMode() {
        OperationContext.Type contextType = getType();
        return (contextType == Type.MANAGEMENT) ? RunningMode.ADMIN_ONLY : RunningMode.NORMAL;
    }

    /**
     * Return {@code true} to validate operations against their description provider when executing in the controller. The default is
     * {@code false}
     *
     * @return Whether operations should be validated or not
     */
    protected boolean isValidateOperations() {
        return true;
    }

    /**
     * Create a registry for extra configuration that should be taken into account when validating the description providers for the subsystem
     * in the controller. The default is an empty registry.
     *
     * @return An ArbitraryDescriptors instance containing the arbitrary descriptors, or {@code null} to not validate the description providers.
     */
    protected ModelDescriptionValidator.ValidationConfiguration getModelValidationConfiguration() {
        return new ValidationConfiguration();
    }


    /**
     * Creates the controller initializer.
     * Override this method to do custom initialization.
     *
     * @return the created controller initializer
     */
    protected ControllerInitializer createControllerInitializer() {
        return new ControllerInitializer();
    }

    /**
     * Allows easy initialization of commonly used parts of the model and invocation of associated boottime operations
     *
     * @param controllerInitializer the controller initializer
     */
    protected void setupController(ControllerInitializer controllerInitializer) {
    }

    /**
     * Adds extra services to the service controller
     *
     * @param target the service controller target
     */
    protected void addExtraServices(ServiceTarget target) {
    }

    /**
     * Allows extra initialization of the model and addition of extra subsystems
     *
     * @param extensionContext allows installation of extra subsystem extensions, call {@code Extension.initialize(extensionContext)} for each extra extension you have
     * @param rootResource the root model resource which allows you to for example add child elements to the model
     * @param rootRegistration the root resource registration which allows you to for example add additional operations to the model
     */
    protected void initializeExtraSubystemsAndModel(ExtensionContext extensionContext, Resource rootResource, ManagementResourceRegistration rootRegistration) {
    }

}
