/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.messaging.activemq;

import static org.jboss.as.controller.PathAddress.EMPTY_ADDRESS;
import static org.jboss.as.controller.RunningMode.ADMIN_ONLY;
import static org.wildfly.extension.messaging.activemq.MessagingExtension.BINDINGS_DIRECTORY_PATH;
import static org.wildfly.extension.messaging.activemq.MessagingExtension.JOURNAL_DIRECTORY_PATH;
import static org.wildfly.extension.messaging.activemq.MessagingExtension.LARGE_MESSAGES_DIRECTORY_PATH;
import static org.wildfly.extension.messaging.activemq.MessagingExtension.PAGING_DIRECTORY_PATH;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

import org.apache.activemq.artemis.cli.commands.tools.XmlDataExporter;
import org.jboss.as.controller.AbstractRuntimeOnlyHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.services.path.PathManager;
import org.jboss.as.controller.services.path.PathManagerService;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceController;
import org.wildfly.extension.messaging.activemq.logging.MessagingLogger;

/**
 * Export a dump of Artemis journal. WildFly must be running in ADMIN-ONLY mode to perform this operation.
 *
 * The dump is stored on WildFly host and is not sent to the client invoking the operation.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2015 Red Hat inc.
 */
public class ExportJournalOperation extends AbstractRuntimeOnlyHandler {

    static final ExportJournalOperation INSTANCE = new ExportJournalOperation();

    // name file of the dump follows the format journal-yyyyMMdd-HHmmssSSSTZ-dump.xml
    private static final String FILE_NAME_FORMAT = "journal-%1$tY%<tm%<td-%<tH%<tM%<tS%<TL%<tz-dump.xml";

    private ExportJournalOperation() {

    }

    static void registerOperation(final ManagementResourceRegistration registry, final ResourceDescriptionResolver resourceDescriptionResolver) {
        registry.registerOperationHandler(new SimpleOperationDefinitionBuilder("export-journal", resourceDescriptionResolver)
                        .setRuntimeOnly()
                        .setReplyValueType(ModelType.STRING)
                        .build(),
                INSTANCE);
    }

    @Override
    protected void executeRuntimeStep(OperationContext context, ModelNode operation) throws OperationFailedException {
        if (context.getRunningMode() != ADMIN_ONLY) {
            throw MessagingLogger.ROOT_LOGGER.managementOperationAllowedOnlyInRunningMode("export-journal", ADMIN_ONLY);
        }

        final Resource serverResource = context.readResource(EMPTY_ADDRESS, false);
        final ServiceController<PathManager> service = (ServiceController<PathManager>) context.getServiceRegistry(false).getService(PathManagerService.SERVICE_NAME);
        final PathManager pathManager = service.getService().getValue();

        final String journal = resolvePath(context, pathManager, JOURNAL_DIRECTORY_PATH, serverResource);
        final String bindings = resolvePath(context, pathManager, BINDINGS_DIRECTORY_PATH, serverResource);
        final String paging = resolvePath(context, pathManager, PAGING_DIRECTORY_PATH, serverResource);
        final String largeMessages = resolvePath(context, pathManager, LARGE_MESSAGES_DIRECTORY_PATH, serverResource);

        final XmlDataExporter exporter = new XmlDataExporter();

        String name = String.format(FILE_NAME_FORMAT, new Date());
        // write the exported dump at the same level than the journal directory
        File dump = new File(new File(journal).getParent(), name);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(dump);
            exporter.process(fos, bindings, journal, paging, largeMessages);
            context.getResult().set(dump.getAbsolutePath());
        } catch (Exception e) {
            throw new OperationFailedException(e);
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                }
            }
        }
    }

    private static String resolvePath(OperationContext context, PathManager pathManager, PathElement pathElement, Resource serverResource) throws OperationFailedException {
        final ModelNode pathModel = serverResource.hasChild(pathElement) ? serverResource.getChild(pathElement).getModel() : new ModelNode();
        final String relativeTo = PathDefinition.RELATIVE_TO.resolveModelAttribute(context, pathModel).asString();
        final String path = PathDefinition.PATHS.get(pathElement.getValue()).resolveModelAttribute(context, pathModel).asString();
        return pathManager.resolveRelativePathEntry(path, relativeTo);
    }
}
