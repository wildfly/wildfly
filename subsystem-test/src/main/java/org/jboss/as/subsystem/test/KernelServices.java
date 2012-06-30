package org.jboss.as.subsystem.test;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_TRANSFORMED_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RECURSIVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.AssertionFailedError;

import org.jboss.as.controller.ControlledProcessState;
import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.ModelController.OperationTransactionControl;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.operations.validation.OperationValidator;
import org.jboss.as.controller.registry.GlobalTransformerRegistry;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationTransformerRegistry;
import org.jboss.as.controller.services.path.PathManagerService;
import org.jboss.as.controller.transform.DomainModelTransformers;
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
     * @return the result of the operation
     */
    public ModelNode executeOperation(ModelNode operation) {
        return controller.execute(operation, null, OperationTransactionControl.COMMIT, null);
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
    public ModelNode transformOperation(ModelVersion modelVersion, ModelNode operation) {
        if (legacyServices == null) {
            throw new IllegalStateException("Can only be called for the main controller");
        }
        PathElement pathElement = PathElement.pathElement(SUBSYSTEM, mainSubsystemName);
        PathAddress opAddr = PathAddress.pathAddress(operation.get(OP_ADDR));
        if (opAddr.size() > 0 && opAddr.getElement(0).equals(pathElement)) {
            TransformerRegistry transformerRegistry = extensionRegistry.getTransformerRegistry();
            DomainModelTransformers transformers = transformerRegistry.getDomainTransformers();

            PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
            OperationTransformerRegistry registry = transformers.resolveServer(modelVersion, createSubsystemVersionRegistry(modelVersion));

            //TODO Initialise this
            TransformationContext transformationContext = null;
            return registry.resolveOperationTransformer(address, operation.get(OP).asString()).getTransformer().transformOperation(transformationContext, address, operation).getTransformedOperation();
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
        ModelNode op = new ModelNode();
        op.get(OP).set(READ_RESOURCE_OPERATION);
        op.get(OP_ADDR).set(PathAddress.EMPTY_ADDRESS.toModelNode());
        op.get(RECURSIVE).set(true);
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
