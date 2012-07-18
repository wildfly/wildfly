package org.jboss.as.subsystem.test;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INCLUDE_ALIASES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_TRANSFORMED_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RECURSIVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.Assert;
import junit.framework.AssertionFailedError;

import org.jboss.as.controller.ControlledProcessState;
import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.ModelController.OperationTransactionControl;
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
public class KernelServices {

    private volatile ServiceContainer container;
    private final ModelController controller;
    private final StringConfigurationPersister persister;
    private final OperationValidator operationValidator;
    private final String mainSubsystemName;
    private final ManagementResourceRegistration rootRegistration;
    private final Map<ModelVersion, KernelServices> legacyServices;
    private final ExtensionRegistry extensionRegistry;
    private final ModelVersion legacyModelVersion;

    private static final AtomicInteger counter = new AtomicInteger();


    private KernelServices(ServiceContainer container, ModelController controller, StringConfigurationPersister persister, ManagementResourceRegistration rootRegistration,
            OperationValidator operationValidator, String mainSubsystemName, ExtensionRegistry extensionRegistry, ModelVersion legacyModelVersion) {
        this.container = container;
        this.controller = controller;
        this.persister = persister;
        this.operationValidator = operationValidator;
        this.mainSubsystemName = mainSubsystemName;
        this.rootRegistration = rootRegistration;
        this.legacyServices = legacyModelVersion != null ? null : new HashMap<ModelVersion, KernelServices>();
        this.extensionRegistry = extensionRegistry;
        this.legacyModelVersion = legacyModelVersion;
    }

    static KernelServices create(String mainSubsystemName, AdditionalInitialization additionalInit,
            ExtensionRegistry controllerExtensionRegistry, List<ModelNode> bootOperations, TestParser testParser, Extension mainExtension, ModelVersion legacyModelVersion) throws Exception {
        ControllerInitializer controllerInitializer = additionalInit.createControllerInitializer();

        PathManagerService pathManager = new PathManagerService() {
        };

        controllerInitializer.setPathManger(pathManager);

        additionalInit.setupController(controllerInitializer);

        //Initialize the controller
        ServiceContainer container = ServiceContainer.Factory.create("test" + counter.incrementAndGet());
        ServiceTarget target = container.subTarget();
        ControlledProcessState processState = new ControlledProcessState(true);
        List<ModelNode> extraOps = controllerInitializer.initializeBootOperations();
        List<ModelNode> allOps = new ArrayList<ModelNode>();
        if (extraOps != null) {
            allOps.addAll(extraOps);
        }
        allOps.addAll(bootOperations);
        StringConfigurationPersister persister = new StringConfigurationPersister(allOps, testParser);
        controllerExtensionRegistry.setWriterRegistry(persister);
        controllerExtensionRegistry.setPathManager(pathManager);
        TestModelControllerService svc = new TestModelControllerService(mainExtension, controllerInitializer, additionalInit, controllerExtensionRegistry,
                processState, persister, additionalInit.isValidateOperations());
        ServiceBuilder<ModelController> builder = target.addService(Services.JBOSS_SERVER_CONTROLLER, svc);
        builder.install();
        target.addService(PathManagerService.SERVICE_NAME, pathManager).install();

        additionalInit.addExtraServices(target);

        //sharedState = svc.state;
        svc.waitForSetup();
        ModelController controller = svc.getValue();
        processState.setRunning();

        KernelServices kernelServices = new KernelServices(container, controller, persister, svc.getRootRegistration(),
                new OperationValidator(svc.getRootRegistration()), mainSubsystemName, controllerExtensionRegistry, legacyModelVersion);

        return kernelServices;
    }

    /**
     * Gets one the legacy subsystem controller services for the controller containing the passed in version of the subsystem
     *
     * @param modelVersion the subsystem model version of the legacy subsystem model controller
     * @throws IllegalStateException if this is not the test's main model controller
     * @throws IllegalStateException if there is no legacy controller containing the version of the subsystem
     */
    public KernelServices getLegacyServices(ModelVersion modelVersion) {
        if (legacyServices == null) {
            throw new IllegalStateException("Can only be called for the main controller");
        }
        KernelServices legacy = legacyServices.get(modelVersion);
        if (legacy == null) {
            throw new IllegalStateException("No legacy subsystem controller was found for model version " + modelVersion);
        }
        return legacy;
    }

    /**
     * Gets the service container
     *
     * @return the service container
     */
    public ServiceContainer getContainer() {
        return container;
    }

    /**
     * Execute an operation in the model controller
     *
     * @param operation the operation to execute
     * @return the whole result of the operation
     */
    public ModelNode executeOperation(ModelNode operation) {
        return controller.execute(operation, null, OperationTransactionControl.COMMIT, null);
    }


    /**
     * Execute an operation in the  controller containg the passed in version of the subsystem.
     * The operation and results will be translated from the format for the main controller to the
     * legacy controller's format.
     *
     * @param modelVersion the subsystem model version of the legacy subsystem model controller
     * @param operation the operation for the main controller
     * @throws IllegalStateException if this is not the test's main model controller
     * @throws IllegalStateException if there is no legacy controller containing the version of the subsystem
     */
    public ModelNode executeOperation(ModelVersion modelVersion, TransformedOperation op) {
        if (legacyServices == null) {
            throw new IllegalStateException("Can only be called for the main controller");
        }
        KernelServices legacy = legacyServices.get(modelVersion);
        if (legacy == null) {
            throw new IllegalStateException("No legacy subsystem controller was found for model version " + modelVersion);
        }

        ModelNode result = new ModelNode();
        if (op.getTransformedOperation() != null) {
            result = legacy.executeOperation(op.getTransformedOperation());
        }
        OperationResultTransformer resultTransformer = op.getResultTransformer();
        if (resultTransformer != null) {
            result = resultTransformer.transformResult(result);
        }
        return result;
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
        if (legacyServices == null) {
            throw new IllegalStateException("Can only be called for the main controller");
        }
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



    /*
     * Execute an operation in the model controller, expecting succes and return the "result" node
     *
     * @param operation the operation to execute
     * @return the result of the operation
     * @throws OperationFailedException if the operation failed
     */
    public ModelNode executeForResult(ModelNode operation) throws OperationFailedException {
        ModelNode rsp = executeOperation(operation);
        if (FAILED.equals(rsp.get(OUTCOME).asString())) {
            throw new OperationFailedException(rsp.get(FAILURE_DESCRIPTION));
        }
        return rsp.get(RESULT);
    }

    /**
     * Execute an operation in the model controller, expecting failure.
     * Gives a junit {@link AssertionFailedError} if the operation did not fail.
     *
     * @param operation the operation to execute
     * @return the result of the operation
     */
    public void executeForFailure(ModelNode operation) {
        try {
            executeForResult(operation);
            Assert.fail("Should have given error");
        } catch (OperationFailedException expected) {
        }
    }

    /**
     * Create an operation
     *
     * @param operationName the name of the operation
     * @param address the address
     * @throws IllegalArgumentException if the address is bad
     */
    public ModelNode createOperation(String operationName, String...address) {
        ModelNode operation = new ModelNode();
        operation.get(OP).set(operationName);
        if (address.length > 0) {
            if (address.length % 2 != 0) {
                throw new IllegalArgumentException("Address must be in pairs");
            }
            for (String addr : address) {
                operation.get(OP_ADDR).add(addr);
            }
        } else {
            operation.get(OP_ADDR).setEmptyList();
        }

        return operation;
    }

    /**
     * Reads the persisted subsystem xml
     *
     * @return the xml
     */
    public String getPersistedSubsystemXml() {
        return persister.getMarshalled();
    }

    /**
     * Reads the whole model from the model controller
     *
     * @return the whole model
     */
    public ModelNode readWholeModel() {
        return readWholeModel(false);
    }

    /**
     * Reads the whole model from the model controller
     *
     * @return the whole model
     */
    public ModelNode readWholeModel(boolean includeAliases) {
        ModelNode op = new ModelNode();
        op.get(OP).set(READ_RESOURCE_OPERATION);
        op.get(OP_ADDR).set(PathAddress.EMPTY_ADDRESS.toModelNode());
        op.get(RECURSIVE).set(true);
        if (includeAliases) {
            op.get(INCLUDE_ALIASES).set(true);
        }
        ModelNode result = executeOperation(op);
        return AbstractSubsystemTest.checkResultAndGetContents(result);
    }

    /**
     * Transforms the model to the legacy subsystem model version
     * @param modelVersion the target legacy subsystem model version
     * @return the transformed model
     * @throws IllegalStateException if this is not the test's main model controller
     */
    public ModelNode readTransformedModel(ModelVersion modelVersion) {
        if (legacyServices == null) {
            throw new IllegalStateException("Can only be called for the main controller");
        }
        ModelNode op = new ModelNode();
        op.get(OP).set(READ_TRANSFORMED_RESOURCE_OPERATION);
        op.get(OP_ADDR).set(PathAddress.EMPTY_ADDRESS.toModelNode());
        op.get(RECURSIVE).set(true);
        op.get(SUBSYSTEM).set(mainSubsystemName);
        op.get(ModelDescriptionConstants.MANAGEMENT_MAJOR_VERSION).set(modelVersion.getMajor());
        op.get(ModelDescriptionConstants.MANAGEMENT_MINOR_VERSION).set(modelVersion.getMinor());
        op.get(ModelDescriptionConstants.MANAGEMENT_MICRO_VERSION).set(modelVersion.getMicro());
        ModelNode result = executeOperation(op);
        return AbstractSubsystemTest.checkResultAndGetContents(result);
    }

    /**
     * Validates the operations against the description providers in the model controller
     *
     * @param operations the operations to validate
     * @throws AssertionFailedError if the operations are not valid
     */
    public void validateOperations(List<ModelNode> operations) {
        operationValidator.validateOperations(operations);
    }

    /**
     * Validates the operation against the description providers in the model controller
     *
     * @param operation the operation to validate
     * @throws AssertionFailedError if the operation is not valid
     */
    public void validateOperation(ModelNode operation) {
        operationValidator.validateOperation(operation);
    }

    public void shutdown() {
        if (container != null) {
            container.shutdown();
            try {
                container.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            container = null;

            for (KernelServices legacyService: legacyServices.values()) {
                legacyService.shutdown();
            }
        }
    }

    ManagementResourceRegistration getRootRegistration() {
        return rootRegistration;
    }

    ExtensionRegistry getExtensionRegistry() {
        return extensionRegistry;
    }

    void addLegacyKernelService(ModelVersion modelVersion, KernelServices legacyServices) {
        this.legacyServices.put(modelVersion, legacyServices);
    }

    private ModelNode createSubsystemVersionRegistry(ModelVersion modelVersion) {
        ModelNode subsystems = new ModelNode();
        subsystems.get(mainSubsystemName).set(modelVersion.toString());
        return subsystems;
    }
}
