package org.jboss.as.osgi;

import java.util.List;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.osgi.framework.BundleContext;

/**
 * An extension point for the OSGi subsystem
 *
 * @author thomas.diesler@jboss.com
 * @since 11-Jul-2012
 */
public interface SubsystemExtension {

    void performBoottime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler,
            List<ServiceController<?>> newControllers);

    void configureServiceDependencies(ServiceName serviceName, ServiceBuilder<?> builder);

    void startSystemServices(StartContext startContext, BundleContext systemContext);

    void stopSystemServices(StopContext stopContext, BundleContext systemContext);

}
