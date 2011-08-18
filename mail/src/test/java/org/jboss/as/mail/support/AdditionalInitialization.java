package org.jboss.as.mail.support;

import org.jboss.as.controller.ExtensionContext;
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
public interface AdditionalInitialization extends AdditionalParsers {

    /**
     * Adds extra services to the service controller
     *
     * @param target the service controller target
     */
    void addExtraServices(ServiceTarget target);


    /**
     * Allows extra initialization of the model and addition of extra subsystems
     *
     * @param context allows installation of extra subsystem extensions, call {@code extension.initialize(context)} for each extra extension you have
     * @param rootResource the root model resource which allows you to for example add child elements to the model
     * @param rootRegistration the root resource registration which allows you to for example add additional operations to the model
     */
    void initializeExtraSubystemsAndModel(ExtensionContext context, Resource rootResource, ManagementResourceRegistration rootRegistration);

}
