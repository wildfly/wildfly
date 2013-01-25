package org.jboss.as.subsystem.test;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_TRANSFORMED_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RECURSIVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.operations.validation.OperationValidator;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationTransformerRegistry;
import org.jboss.as.controller.services.path.PathManagerService;
import org.jboss.as.controller.transform.OperationResultTransformer;
import org.jboss.as.controller.transform.OperationTransformer;
import org.jboss.as.controller.transform.OperationTransformer.TransformedOperation;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.TransformationTarget;
import org.jboss.as.controller.transform.TransformationTargetImpl;
import org.jboss.as.controller.transform.TransformerRegistry;
import org.jboss.as.controller.transform.Transformers;
import org.jboss.as.model.test.ModelTestKernelServicesImpl;
import org.jboss.as.model.test.ModelTestModelControllerService;
import org.jboss.as.model.test.ModelTestParser;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.as.model.test.StringConfigurationPersister;
import org.jboss.as.server.Services;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceTarget;
import org.xnio.IoUtils;


/**
 * Allows access to the service container and the model controller
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class KernelServicesImpl extends ModelTestKernelServicesImpl<KernelServices> implements KernelServices {

    private final String mainSubsystemName;
    private final ExtensionRegistry extensionRegistry;
    private final boolean registerTransformers;


    private static final AtomicInteger counter = new AtomicInteger();


    private KernelServicesImpl(ServiceContainer container, ModelTestModelControllerService controllerService, StringConfigurationPersister persister, ManagementResourceRegistration rootRegistration,
            OperationValidator operationValidator, String mainSubsystemName, ExtensionRegistry extensionRegistry, ModelVersion legacyModelVersion, boolean successfulBoot, Throwable bootError, boolean registerTransformers) {
        super(container, controllerService, persister, rootRegistration, operationValidator, legacyModelVersion, successfulBoot, bootError);

        this.mainSubsystemName = mainSubsystemName;
        this.extensionRegistry = extensionRegistry;
        this.registerTransformers = registerTransformers;
    }

    static KernelServicesImpl create(String mainSubsystemName, AdditionalInitialization additionalInit,
            ExtensionRegistry controllerExtensionRegistry, List<ModelNode> bootOperations, ModelTestParser testParser, Extension mainExtension, ModelVersion legacyModelVersion, boolean registerTransformers) throws Exception {
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
                persister, additionalInit.isValidateOperations(), registerTransformers);
        ServiceBuilder<ModelController> builder = target.addService(Services.JBOSS_SERVER_CONTROLLER, svc);
        builder.addDependency(PathManagerService.SERVICE_NAME); // ensure this is up before the ModelControllerService, as it would be in a real server
        builder.install();
        target.addService(PathManagerService.SERVICE_NAME, pathManager).install();

        additionalInit.addExtraServices(target);

        //sharedState = svc.state;
        svc.waitForSetup();
        //processState.setRunning();

        KernelServicesImpl kernelServices = new KernelServicesImpl(container, svc, persister, svc.getRootRegistration(),
                new OperationValidator(svc.getRootRegistration()), mainSubsystemName, controllerExtensionRegistry, legacyModelVersion, svc.isSuccessfulBoot(), svc.getBootError(), registerTransformers);

        return kernelServices;
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

            final Map<PathAddress, ModelVersion> subsystem = Collections.singletonMap(PathAddress.EMPTY_ADDRESS.append(pathElement), modelVersion);
            final TransformationTarget transformationTarget = TransformationTargetImpl.create(extensionRegistry.getTransformerRegistry(), getCoreModelVersionByLegacyModelVersion(modelVersion),
                    subsystem, MOCK_IGNORED_DOMAIN_RESOURCE_REGISTRY, TransformationTarget.TransformationTargetType.SERVER);

            final Transformers transformers = Transformers.Factory.create(transformationTarget);
            final TransformationContext transformationContext = createTransformationContext(transformationTarget);
            return transformers.transformOperation(transformationContext, operation);
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
        ModelNode result = internalExecute(op, new ReadTransformedResourceOperation(getTransformersRegistry(), getCoreModelVersionByLegacyModelVersion(modelVersion), modelVersion));
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
        KernelServices legacy = getLegacyServices(modelVersion);
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
            // TODO this still does not really model the way rejection is handled in the domain
            if(op.rejectOperation(result)) {
                final ModelNode newResponse = new ModelNode();
                newResponse.get(OUTCOME).set(FAILED);
                newResponse.get(FAILURE_DESCRIPTION).set(op.getFailureDescription());
                return newResponse;
            }
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

    protected void addLegacyKernelService(ModelVersion modelVersion, KernelServicesImpl legacyServices) {
        super.addLegacyKernelService(modelVersion, legacyServices);
    }

    private ModelNode createSubsystemVersionRegistry(ModelVersion modelVersion) {
        ModelNode subsystems = new ModelNode();
        subsystems.get(mainSubsystemName).set(modelVersion.toString());
        return subsystems;
    }

    private ModelVersion getCoreModelVersionByLegacyModelVersion(ModelVersion legacyModelVersion) {
        //The reason the core model version is important is that is used to know if the ignored slave resources are known on the host or not
        //e.g 7.2.x uses core model version >= 1.4.0 and so we know which resources are ignored
        //7.1.x uses core model version <= 1.4.0 and so we have no idea which resources are ignored
        //This is important for example in RejectExpressionValuesTransformer

        ModelVersion coreModeVersion = KnownVersions.getCoreModelVersionForSubsystemVersion(mainSubsystemName, legacyModelVersion);
        if (coreModeVersion != null) {
            return coreModeVersion;
        }

        String fileName = mainSubsystemName + "-versions-to-as-versions.properties";

        InputStream in = this.getClass().getResourceAsStream("/" + fileName);
        if (in == null) {
            throw new IllegalArgumentException("Version " + legacyModelVersion + " of " + mainSubsystemName + " is not a known version. Please add it to " +
                    KnownVersions.class.getName() + ". Or if that is not possible, " +
                    "include a src/test/resources/" + fileName +
                    " file, which maps AS versions to model versions. E.g.:\n1.1.0=7.1.2\n1.2.0=7.1.3");
        }
        Properties props = new Properties();
        try {
            props.load(in);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            IoUtils.safeClose(in);
        }

        String asVersion = (String)props.get(legacyModelVersion.toString());
        if (asVersion == null) {
            throw new IllegalArgumentException("src/test/resources/" + fileName +
                    " does not contain an AS mapping for modelversion + " +
                    legacyModelVersion + "'. It needs to map AS versions to model versions. E.g.:\n1.1.0=7.1.2\n1.2.0=7.1.3");
        }

        ModelVersion coreModelVersion = KnownVersions.AS_CORE_MODEL_VERSION_BY_AS_VERSION.get(asVersion);
        if (coreModelVersion == null) {
            throw new IllegalArgumentException("Unknown AS version '" + asVersion + "' determined from src/test/resources/" + fileName +
                    ". Known AS versions are " + KnownVersions.AS_CORE_MODEL_VERSION_BY_AS_VERSION.keySet());
        }
        return coreModelVersion;
    }

    private static TransformationTarget.IgnoredTransformationRegistry MOCK_IGNORED_DOMAIN_RESOURCE_REGISTRY = new  TransformationTarget.IgnoredTransformationRegistry() {

        @Override
        public boolean isResourceTransformationIgnored(PathAddress address) {
            return false;
        }

        @Override
        public boolean isOperationTransformationIgnored(PathAddress address) {
            return false;
        }

        @Override
        public String getHostName() {
            return null;
        }
    };
}
