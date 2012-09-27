package org.jboss.as.subsystem.test;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_TRANSFORMED_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RECURSIVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.operations.validation.OperationValidator;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationTransformerRegistry;
import org.jboss.as.controller.services.path.PathManagerService;
import org.jboss.as.controller.transform.OperationResultTransformer;
import org.jboss.as.controller.transform.OperationTransformer;
import org.jboss.as.controller.transform.OperationTransformer.TransformedOperation;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.TransformerRegistry;
import org.jboss.as.model.test.ModelTestKernelServices;
import org.jboss.as.model.test.ModelTestModelControllerService;
import org.jboss.as.model.test.ModelTestParser;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.as.model.test.StringConfigurationPersister;
import org.jboss.as.server.Services;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceTarget;


/**
 * Allows access to the service container and the model controller
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class KernelServices extends ModelTestKernelServices {

    private final String mainSubsystemName;
    private final ExtensionRegistry extensionRegistry;


    private static final AtomicInteger counter = new AtomicInteger();


    private KernelServices(ServiceContainer container, ModelController controller, StringConfigurationPersister persister, ManagementResourceRegistration rootRegistration,
            OperationValidator operationValidator, String mainSubsystemName, ExtensionRegistry extensionRegistry, ModelVersion legacyModelVersion, boolean successfulBoot, Throwable bootError) {
        super(container, controller, persister, rootRegistration, operationValidator, legacyModelVersion, successfulBoot, bootError);

        this.mainSubsystemName = mainSubsystemName;
        this.extensionRegistry = extensionRegistry;
    }

    static KernelServices create(String mainSubsystemName, AdditionalInitialization additionalInit,
            ExtensionRegistry controllerExtensionRegistry, List<ModelNode> bootOperations, ModelTestParser testParser, Extension mainExtension, ModelVersion legacyModelVersion) throws Exception {
        ControllerInitializer controllerInitializer = additionalInit.createControllerInitializer();

        PathManagerService pathManager = new PathManagerService() {
        };

        controllerInitializer.setPathManger(pathManager);

        additionalInit.setupController(controllerInitializer);

        //Initialize the controller
        ServiceContainer container = ServiceContainer.Factory.create("test" + counter.incrementAndGet());
        ServiceTarget target = container.subTarget();
        List<ModelNode> extraOps = controllerInitializer.initializeBootOperations();
        List<ModelNode> allOps = new ArrayList<ModelNode>();
        if (extraOps != null) {
            allOps.addAll(extraOps);
        }
        allOps.addAll(bootOperations);
        StringConfigurationPersister persister = new StringConfigurationPersister(allOps, testParser);
        controllerExtensionRegistry.setWriterRegistry(persister);
        controllerExtensionRegistry.setPathManager(pathManager);
        ModelTestModelControllerService svc = TestModelControllerService.create(mainExtension, controllerInitializer, additionalInit, controllerExtensionRegistry,
                persister, additionalInit.isValidateOperations());
        ServiceBuilder<ModelController> builder = target.addService(Services.JBOSS_SERVER_CONTROLLER, svc);
        builder.addDependency(PathManagerService.SERVICE_NAME); // ensure this is up before the ModelControllerService, as it would be in a real server
        builder.install();
        target.addService(PathManagerService.SERVICE_NAME, pathManager).install();

        additionalInit.addExtraServices(target);

        //sharedState = svc.state;
        svc.waitForSetup();
        ModelController controller = svc.getValue();
        //processState.setRunning();

        KernelServices kernelServices = new KernelServices(container, controller, persister, svc.getRootRegistration(),
                new OperationValidator(svc.getRootRegistration()), mainSubsystemName, controllerExtensionRegistry, legacyModelVersion, svc.isSuccessfulBoot(), svc.getBootError());

        return kernelServices;
    }

    @Override
    public KernelServices getLegacyServices(ModelVersion modelVersion) {
        return (KernelServices)super.getLegacyServices(modelVersion);
    }


    /**
     * Transforms an operation in the main controller to the format expected by the model controller containing
     * the legacy subsystem
     *
     * @param modelVersion the subsystem model version of the legacy subsystem model controller
     * @param operation the operation to transform
     * @return the transformed operation
     * @throws IllegalStateException if this is not the test's main model controller
     */
    public TransformedOperation transformOperation(ModelVersion modelVersion, ModelNode operation) throws OperationFailedException {
        checkIsMainController();
        PathElement pathElement = PathElement.pathElement(SUBSYSTEM, mainSubsystemName);
        PathAddress opAddr = PathAddress.pathAddress(operation.get(OP_ADDR));
        if (opAddr.size() > 0 && opAddr.getElement(0).equals(pathElement)) {
            TransformerRegistry transformerRegistry = extensionRegistry.getTransformerRegistry();

            PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
            OperationTransformerRegistry registry = transformerRegistry.resolveServer(modelVersion, createSubsystemVersionRegistry(modelVersion));

            //TODO Initialise this
            TransformationContext transformationContext = null;

            OperationTransformer operationTransformer = registry.resolveOperationTransformer(address, operation.get(OP).asString()).getTransformer();
            if (operationTransformer != null) {
                return operationTransformer.transformOperation(transformationContext, address, operation);
            }
        }
        return new OperationTransformer.TransformedOperation(operation, OperationResultTransformer.ORIGINAL_RESULT);
    }

    /**
     * Transforms the model to the legacy subsystem model version
     * @param modelVersion the target legacy subsystem model version
     * @return the transformed model
     * @throws IllegalStateException if this is not the test's main model controller
     */
    public ModelNode readTransformedModel(ModelVersion modelVersion) {
        getLegacyServices(modelVersion);//Checks we are the main controller
        ModelNode op = new ModelNode();
        op.get(OP).set(READ_TRANSFORMED_RESOURCE_OPERATION);
        op.get(OP_ADDR).set(PathAddress.EMPTY_ADDRESS.toModelNode());
        op.get(RECURSIVE).set(true);
        op.get(SUBSYSTEM).set(mainSubsystemName);
        op.get(ModelDescriptionConstants.MANAGEMENT_MAJOR_VERSION).set(modelVersion.getMajor());
        op.get(ModelDescriptionConstants.MANAGEMENT_MINOR_VERSION).set(modelVersion.getMinor());
        op.get(ModelDescriptionConstants.MANAGEMENT_MICRO_VERSION).set(modelVersion.getMicro());
        ModelNode result = executeOperation(op);
        return ModelTestUtils.checkResultAndGetContents(result);
    }

    /**
     * Execute an operation in the  controller containg the passed in version of the subsystem.
     * The operation and results will be translated from the format for the main controller to the
     * legacy controller's format.
     *
     * @param modelVersion the subsystem model version of the legacy subsystem model controller
     * @param op the operation for the main controller
     * @throws IllegalStateException if this is not the test's main model controller
     * @throws IllegalStateException if there is no legacy controller containing the version of the subsystem
     */
    public ModelNode executeOperation(final ModelVersion modelVersion, final TransformedOperation op) {
        ModelTestKernelServices legacy = getLegacyServices(modelVersion);
        ModelNode result = new ModelNode();
        if (op.getTransformedOperation() != null) {
            result = legacy.executeOperation(op.getTransformedOperation(), new ModelController.OperationTransactionControl() {
                    @Override
                    public void operationPrepared(ModelController.OperationTransaction transaction, ModelNode result) {
                        if(op.rejectOperation(result)) {
                            transaction.rollback();
                        } else {
                            transaction.commit();
                        }
                    }
                });
        }
        OperationResultTransformer resultTransformer = op.getResultTransformer();
        if (resultTransformer != null) {
            result = resultTransformer.transformResult(result);
        }
        return result;
    }


    ExtensionRegistry getExtensionRegistry() {
        return extensionRegistry;
    }

    protected void addLegacyKernelService(ModelVersion modelVersion, KernelServices legacyServices) {
        super.addLegacyKernelService(modelVersion, legacyServices);
    }

    private ModelNode createSubsystemVersionRegistry(ModelVersion modelVersion) {
        ModelNode subsystems = new ModelNode();
        subsystems.get(mainSubsystemName).set(modelVersion.toString());
        return subsystems;
    }
}
