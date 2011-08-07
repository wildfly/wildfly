package org.jboss.as.subsystem.test;

import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationContext.Type;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.msc.service.ServiceTarget;

/**
 * Allows you to additionally initialize the service container and the model controller
 *
 * beyond the subsystem being tested
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
