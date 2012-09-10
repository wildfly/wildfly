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
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.RunningModeControl;
import org.jboss.as.controller.operations.validation.OperationValidator;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.host.controller.HostRunningModeControl;
import org.jboss.as.host.controller.RestartMode;
import org.jboss.as.model.test.ModelTestKernelServices;
import org.jboss.as.model.test.ModelTestParser;
import org.jboss.as.model.test.OperationValidation;
import org.jboss.as.model.test.StringConfigurationPersister;
import org.jboss.as.repository.ContentRepository;
import org.jboss.as.server.Services;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceTarget;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class KernelServices extends ModelTestKernelServices {

    static final AtomicInteger counter = new AtomicInteger();

    protected KernelServices(ServiceContainer container, ModelController controller, StringConfigurationPersister persister,
            ManagementResourceRegistration rootRegistration, OperationValidator operationValidator,
            ModelVersion legacyModelVersion, boolean successfulBoot, Throwable bootError) {
        // FIXME KernelServices constructor
        super(container, controller, persister, rootRegistration, operationValidator, legacyModelVersion, successfulBoot, bootError);
    }

    static KernelServices create(ProcessType processType, RunningMode runningMode, OperationValidation validateOperations,
            List<ModelNode> bootOperations, ModelTestParser testParser, ModelVersion legacyModelVersion, TestModelType type, ModelInitializer modelInitializer) throws Exception {

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
        RunningModeControl runningModeControl = type == TestModelType.HOST ? new HostRunningModeControl(runningMode, RestartMode.HC_ONLY) : new RunningModeControl(runningMode);
        TestModelControllerService svc = TestModelControllerService.create(processType, runningModeControl, persister, validateOperations, type, modelInitializer);
        ServiceBuilder<ModelController> builder = target.addService(Services.JBOSS_SERVER_CONTROLLER, svc);
        builder.addDependency(ContentRepository.SERVICE_NAME, ContentRepository.class, svc.getContentRepositoryInjector());
        builder.install();

        //sharedState = svc.state;
        svc.waitForSetup();
        ModelController controller = svc.getValue();
        //processState.setRunning();

        KernelServices kernelServices = new KernelServices(container, controller, persister, svc.getRootRegistration(),
                new OperationValidator(svc.getRootRegistration()), legacyModelVersion, svc.isSuccessfulBoot(), svc.getBootError());

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
}
