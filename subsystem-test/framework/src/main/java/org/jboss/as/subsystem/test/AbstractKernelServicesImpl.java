package org.jboss.as.subsystem.test;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.operations.validation.OperationValidator;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.services.path.PathManagerService;
import org.jboss.as.controller.transform.SubsystemDescriptionDump;
import org.jboss.as.model.test.ModelTestKernelServicesImpl;
import org.jboss.as.model.test.ModelTestModelControllerService;
import org.jboss.as.model.test.ModelTestOperationValidatorFilter;
import org.jboss.as.model.test.ModelTestParser;
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
public abstract class AbstractKernelServicesImpl extends ModelTestKernelServicesImpl<KernelServices> implements KernelServices {

    protected final String mainSubsystemName;
    protected final ExtensionRegistry extensionRegistry;
    protected final boolean registerTransformers;


    private static final AtomicInteger counter = new AtomicInteger();


    protected AbstractKernelServicesImpl(ServiceContainer container, ModelTestModelControllerService controllerService, StringConfigurationPersister persister, ManagementResourceRegistration rootRegistration,
            OperationValidator operationValidator, String mainSubsystemName, ExtensionRegistry extensionRegistry, ModelVersion legacyModelVersion, boolean successfulBoot, Throwable bootError, boolean registerTransformers) {
        super(container, controllerService, persister, rootRegistration, operationValidator, legacyModelVersion, successfulBoot, bootError);

        this.mainSubsystemName = mainSubsystemName;
        this.extensionRegistry = extensionRegistry;
        this.registerTransformers = registerTransformers;
    }

    public static AbstractKernelServicesImpl create(String mainSubsystemName, AdditionalInitialization additionalInit, ModelTestOperationValidatorFilter validateOpsFilter,
            ExtensionRegistry controllerExtensionRegistry, List<ModelNode> bootOperations, ModelTestParser testParser, Extension mainExtension, ModelVersion legacyModelVersion,
            boolean registerTransformers, boolean persistXml) throws Exception {
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
        StringConfigurationPersister persister = new StringConfigurationPersister(allOps, testParser, persistXml);
        controllerExtensionRegistry.setWriterRegistry(persister);
        controllerExtensionRegistry.setPathManager(pathManager);

        //Use the default implementation of test controller for the main controller, and for tests that don't have another one set up on the classpath
        TestModelControllerFactory testModelControllerFactory = StandardTestModelControllerServiceFactory.INSTANCE;
        if (legacyModelVersion != null) {
            ServiceLoader<TestModelControllerFactory> factoryLoader = ServiceLoader.load(TestModelControllerFactory.class, AbstractKernelServicesImpl.class.getClassLoader());
            for (TestModelControllerFactory factory : factoryLoader) {
                testModelControllerFactory = factory;
                break;
            }
        }

        ModelTestModelControllerService svc = testModelControllerFactory.create(mainExtension, controllerInitializer, additionalInit, controllerExtensionRegistry, persister, validateOpsFilter, registerTransformers);
        ServiceBuilder<ModelController> builder = target.addService(Services.JBOSS_SERVER_CONTROLLER, svc);
        builder.addDependency(PathManagerService.SERVICE_NAME); // ensure this is up before the ModelControllerService, as it would be in a real server
        builder.install();
        target.addService(PathManagerService.SERVICE_NAME, pathManager).install();

        additionalInit.addExtraServices(target);

        //sharedState = svc.state;
        svc.waitForSetup();
        //processState.setRunning();

        AbstractKernelServicesImpl kernelServices = legacyModelVersion == null ?
                    new MainKernelServicesImpl(container, svc, persister, svc.getRootRegistration(),
                            new OperationValidator(svc.getRootRegistration()), mainSubsystemName, controllerExtensionRegistry, legacyModelVersion, svc.isSuccessfulBoot(), svc.getBootError(), registerTransformers) :
                                new LegacyKernelServicesImpl(container, svc, persister, svc.getRootRegistration(), new OperationValidator(svc.getRootRegistration()), mainSubsystemName, controllerExtensionRegistry, legacyModelVersion, svc.isSuccessfulBoot(), svc.getBootError(), registerTransformers);

        return kernelServices;
    }


    public ModelNode readFullModelDescription(ModelNode pathAddress) {
        return SubsystemDescriptionDump.readFullModelDescription(PathAddress.pathAddress(pathAddress), getRootRegistration().getSubModel(PathAddress.pathAddress(pathAddress)));
    }


    ExtensionRegistry getExtensionRegistry() {
        return extensionRegistry;
    }

    protected void addLegacyKernelService(ModelVersion modelVersion, KernelServices legacyServices) {
        super.addLegacyKernelService(modelVersion, legacyServices);
    }

    private static class StandardTestModelControllerServiceFactory implements TestModelControllerFactory {
        static final TestModelControllerFactory INSTANCE = new StandardTestModelControllerServiceFactory();

        @Override
        public ModelTestModelControllerService create(Extension mainExtension, ControllerInitializer controllerInitializer,
                AdditionalInitialization additionalInit, ExtensionRegistry extensionRegistry, StringConfigurationPersister persister, ModelTestOperationValidatorFilter validateOpsFilter,
                boolean registerTransformers) {
            return TestModelControllerService.create(mainExtension, controllerInitializer, additionalInit, extensionRegistry, persister, validateOpsFilter, registerTransformers);
        }

    }
}
