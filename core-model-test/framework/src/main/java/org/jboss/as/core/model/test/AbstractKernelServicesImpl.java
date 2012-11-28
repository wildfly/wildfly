/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
* as indicated by the @author tags. See the copyright.txt file in the
* distribution for a full listing of individual contributors.
*
* This is free software; you can redistribute it and/or modify it
* under the terms of the GNU Lesser General Public License as
* published by the Free Software Foundation; either version 2.1 of
* the License, or (at your option) any later version.
*
* This software is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
* Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with this software; if not, write to the Free
* Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
* 02110-1301 USA, or see the FSF site: http://www.fsf.org.
*/
package org.jboss.as.core.model.test;

import java.io.File;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.RunningModeControl;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.operations.validation.OperationValidator;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.transform.OperationTransformer.TransformedOperation;
import org.jboss.as.model.test.ModelTestKernelServicesImpl;
import org.jboss.as.model.test.ModelTestModelControllerService;
import org.jboss.as.model.test.ModelTestParser;
import org.jboss.as.model.test.StringConfigurationPersister;
import org.jboss.as.repository.ContentRepository;
import org.jboss.as.server.Services;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.InjectedValue;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public abstract class AbstractKernelServicesImpl extends ModelTestKernelServicesImpl<KernelServices> implements KernelServices {

    static final AtomicInteger counter = new AtomicInteger();

    protected AbstractKernelServicesImpl(ServiceContainer container, ModelTestModelControllerService controllerService, StringConfigurationPersister persister,
            ManagementResourceRegistration rootRegistration, OperationValidator operationValidator,
            ModelVersion legacyModelVersion, boolean successfulBoot, Throwable bootError, ExtensionRegistry extensionRegistry) {
        super(container, controllerService, persister, rootRegistration, operationValidator, legacyModelVersion, successfulBoot, bootError);
    }

    public static AbstractKernelServicesImpl create(ProcessType processType, RunningModeControl runningModeControl, boolean validateOperations,
            List<ModelNode> bootOperations, ModelTestParser testParser, ModelVersion legacyModelVersion, TestModelType type, ModelInitializer modelInitializer, ExtensionRegistry extensionRegistry) throws Exception {

        //TODO initialize the path manager service like we do for subsystems?

        //Create the controller
        ServiceContainer container = ServiceContainer.Factory.create("test" + counter.incrementAndGet());
        ServiceTarget target = container.subTarget();

        //Initialize the content repository
        File repositoryFile = new File("target/deployment-repository");
        deleteFile(repositoryFile);
        ContentRepository.Factory.addService(target, repositoryFile);

        //Initialize the controller
        StringConfigurationPersister persister = new StringConfigurationPersister(bootOperations, testParser);

        //Use the default implementation of test controller for the main controller, and for tests that don't have another one set up on the classpath
        TestModelControllerFactory testModelControllerFactory = StandardTestModelControllerServiceFactory.INSTANCE;
        if (legacyModelVersion != null) {
            ServiceLoader<TestModelControllerFactory> factoryLoader = ServiceLoader.load(TestModelControllerFactory.class, AbstractKernelServicesImpl.class.getClassLoader());
            for (TestModelControllerFactory factory : factoryLoader) {
                testModelControllerFactory = factory;
                break;
            }
        }

        ModelTestModelControllerService svc = testModelControllerFactory.create(processType, runningModeControl, persister, validateOperations, type, modelInitializer, extensionRegistry);
        ServiceBuilder<ModelController> builder = target.addService(Services.JBOSS_SERVER_CONTROLLER, svc);
        builder.addDependency(ContentRepository.SERVICE_NAME, ContentRepository.class, testModelControllerFactory.getContentRepositoryInjector(svc));
        builder.install();

        //sharedState = svc.state;
        svc.waitForSetup();
        //processState.setRunning();

        AbstractKernelServicesImpl kernelServices = legacyModelVersion == null ?
                new MainKernelServicesImpl(container, svc, persister, svc.getRootRegistration(),
                        new OperationValidator(svc.getRootRegistration()), legacyModelVersion, svc.isSuccessfulBoot(), svc.getBootError(), extensionRegistry) :
                            new LegacyKernelServicesImpl(container, svc, persister, svc.getRootRegistration(),
                                    new OperationValidator(svc.getRootRegistration()), legacyModelVersion, svc.isSuccessfulBoot(), svc.getBootError(), extensionRegistry);

        return kernelServices;
    }

    private static void deleteFile(File file) {
        if (file.isDirectory()) {
            for (File child : file.listFiles()) {
                deleteFile(child);
            }
        }
        file.delete();
    }


    public abstract TransformedOperation transformOperation(ModelVersion modelVersion, ModelNode operation) throws OperationFailedException;

    public abstract ModelNode readTransformedModel(ModelVersion modelVersion);

    public abstract ModelNode executeOperation(final ModelVersion modelVersion, final TransformedOperation op);

    protected void addLegacyKernelService(ModelVersion modelVersion, KernelServices legacyServices) {
        super.addLegacyKernelService(modelVersion, legacyServices);
    }

    private static class StandardTestModelControllerServiceFactory implements TestModelControllerFactory {
        static final TestModelControllerFactory INSTANCE = new StandardTestModelControllerServiceFactory();
        @Override
        public ModelTestModelControllerService create(ProcessType processType, RunningModeControl runningModeControl,
                StringConfigurationPersister persister, boolean validateOperations, TestModelType type, ModelInitializer modelInitializer, ExtensionRegistry extensionRegistry) {
            return TestModelControllerService.create(processType, runningModeControl, persister, validateOperations, type, modelInitializer, extensionRegistry);
        }

        @Override
        public InjectedValue<ContentRepository> getContentRepositoryInjector(ModelTestModelControllerService service) {
            return ((TestModelControllerService)service).getContentRepositoryInjector();
        }
    }

}
