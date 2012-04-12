package org.jboss.as.server.deploymentoverlay;

import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.server.deploymentoverlay.service.ContentService;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

/**
 * @author Stuart Douglas
 */
public class ContentRemove extends AbstractRemoveStepHandler {

    public static final ContentRemove INSTANCE = new ContentRemove();

    @Override
    protected void performRuntime(final OperationContext context, final ModelNode operation, final ModelNode model) throws OperationFailedException {
        final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        final String path = address.getLastElement().getValue();
        final String name = address.getElement(address.size() - 2).getValue();
        final ServiceName serviceName = ContentService.SERVICE_NAME.append(name).append(path);

        context.removeService(serviceName);
    }

    @Override
    protected void recoverServices(final OperationContext context, final ModelNode operation, final ModelNode model) throws OperationFailedException {
        final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        final String path = address.getLastElement().getValue();
        final String name = address.getElement(address.size() - 2).getValue();
        final byte[] content = model.get(ModelDescriptionConstants.CONTENT).asBytes();

        ContentAdd.installServices(context, null, null, name, path, content);
    }
}
