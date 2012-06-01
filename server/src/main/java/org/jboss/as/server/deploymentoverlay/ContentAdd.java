package org.jboss.as.server.deploymentoverlay;

import java.util.Arrays;
import java.util.List;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.repository.ContentRepository;
import org.jboss.as.repository.DeploymentFileRepository;
import org.jboss.as.server.ServerMessages;
import org.jboss.as.server.deploymentoverlay.service.DeploymentOverlayService;
import org.jboss.as.server.deploymentoverlay.service.ContentService;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

/**
 * @author Stuart Douglas
 */
public class ContentAdd extends AbstractAddStepHandler {

    protected final ContentRepository contentRepository;
    private final DeploymentFileRepository remoteRepository;

    public ContentAdd(final ContentRepository contentRepository, final DeploymentFileRepository remoteRepository) {
        this.contentRepository = contentRepository;
        this.remoteRepository = remoteRepository;
    }

    @Override
    protected void populateModel(final ModelNode operation, final ModelNode model) throws OperationFailedException {
        for(AttributeDefinition attr : ContentDefinition.attributes()) {
            attr.validateAndSet(operation, model);
        }
        final byte[] hash = operation.get(ContentDefinition.CONTENT.getName()).asBytes();
        if(remoteRepository != null) {
            remoteRepository.getDeploymentFiles(hash);
        }
        if(!contentRepository.syncContent(hash)) {
            throw ServerMessages.MESSAGES.noSuchDeploymentContent(Arrays.toString(hash));
        }
    }


    @Override
    protected void performRuntime(final OperationContext context, final ModelNode operation, final ModelNode model, final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> newControllers) throws OperationFailedException {
        final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        final String path = address.getLastElement().getValue();
        final String name = address.getElement(address.size() - 2).getValue();
        final byte[] content = model.get(ModelDescriptionConstants.CONTENT).asBytes();

        installServices(context, verificationHandler, newControllers, name, path, content);

    }

    static void installServices(final OperationContext context, final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> newControllers, final String name, final String path, final byte[] content) {
        final ServiceName serviceName = ContentService.SERVICE_NAME.append(name).append(path);
        final ContentService service = new ContentService(path, content);

        ServiceBuilder<ContentService> builder = context.getServiceTarget().addService(serviceName, service)
                .addDependency(DeploymentOverlayService.SERVICE_NAME.append(name), DeploymentOverlayService.class, service.getDeploymentOverlayServiceInjectedValue())
                .addDependency(ContentRepository.SERVICE_NAME, ContentRepository.class, service.getContentRepositoryInjectedValue());
        if(verificationHandler != null) {
            builder.addListener(verificationHandler);
        }
        final ServiceController<ContentService> controller = builder.install();
        if(newControllers != null) {
            newControllers.add(controller);
        }
    }
}
