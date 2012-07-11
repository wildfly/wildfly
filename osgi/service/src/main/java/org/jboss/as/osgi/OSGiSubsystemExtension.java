package org.jboss.as.osgi;

import java.util.List;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;

/**
 * An extension point for the OSGi subsystem
 *
 * @author thomas.diesler@jboss.com
 * @since 11-Jul-2012
 */
public interface OSGiSubsystemExtension {

    void performBoottime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler,
            List<ServiceController<?>> newControllers);

}
