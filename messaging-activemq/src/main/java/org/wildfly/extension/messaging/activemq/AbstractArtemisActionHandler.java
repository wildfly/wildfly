/*
 * Copyright 2020 JBoss by Red Hat.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.extension.messaging.activemq;

import static org.jboss.as.controller.AbstractControllerService.PATH_MANAGER_CAPABILITY;
import static org.jboss.as.controller.PathAddress.EMPTY_ADDRESS;

import java.io.File;
import java.nio.file.Path;
import org.jboss.as.controller.AbstractRuntimeOnlyHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.services.path.AbsolutePathService;
import org.jboss.as.controller.services.path.PathManager;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.wildfly.extension.messaging.activemq.logging.MessagingLogger;

/**
 *
 * @author Emmanuel Hugonnet (c) 2020 Red Hat, Inc.
 */
public abstract class AbstractArtemisActionHandler extends AbstractRuntimeOnlyHandler {

    protected String resolvePath(OperationContext context, PathElement pathElement) throws OperationFailedException {
        Resource serverResource = context.readResource(EMPTY_ADDRESS);
        // if the path resource does not exist, resolve its attributes against an empty ModelNode to get its default values
        final ModelNode model = serverResource.hasChild(pathElement) ? serverResource.getChild(pathElement).getModel() : new ModelNode();
        final String path = PathDefinition.PATHS.get(pathElement.getValue()).resolveModelAttribute(context, model).asString();
        final String relativeToPath = PathDefinition.RELATIVE_TO.resolveModelAttribute(context, model).asString();
        final String relativeTo = AbsolutePathService.isAbsoluteUnixOrWindowsPath(path) ? null : relativeToPath;
        return getPathManager(context).resolveRelativePathEntry(path, relativeTo);
    }

    protected Path getServerTempDir(OperationContext context) {
        return new File(getPathManager(context).getPathEntry(ServerEnvironment.CONTROLLER_TEMP_DIR).resolvePath()).toPath();
    }

    protected File resolveFile(OperationContext context, PathElement pathElement) throws OperationFailedException {
        return new File(resolvePath(context, pathElement));
    }

    protected void checkAllowedOnJournal(OperationContext context, String operationName) throws OperationFailedException {
        ModelNode journalDatasource = ServerDefinition.JOURNAL_DATASOURCE.resolveModelAttribute(context, context.readResource(EMPTY_ADDRESS).getModel());
        if (journalDatasource.isDefined() && journalDatasource.asString() != null && !"".equals(journalDatasource.asString())) {
            throw MessagingLogger.ROOT_LOGGER.operationNotAllowedOnJdbcStore(operationName);
        }
    }

    @SuppressWarnings("unchecked")
    private PathManager getPathManager(OperationContext context) {
        final ServiceController<PathManager> service = (ServiceController<PathManager>) context.getServiceRegistry(false).getService(PATH_MANAGER_CAPABILITY.getCapabilityServiceName());
        return service.getService().getValue();
    }
}
