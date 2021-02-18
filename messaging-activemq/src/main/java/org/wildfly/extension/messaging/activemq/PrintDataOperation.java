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

import static org.jboss.as.controller.RunningMode.ADMIN_ONLY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ARCHIVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SECRET;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.UUID;
import static org.wildfly.extension.messaging.activemq.MessagingExtension.BINDINGS_DIRECTORY_PATH;
import static org.wildfly.extension.messaging.activemq.MessagingExtension.JOURNAL_DIRECTORY_PATH;
import static org.wildfly.extension.messaging.activemq.MessagingExtension.PAGING_DIRECTORY_PATH;

import java.io.File;
import java.io.PrintStream;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.activemq.artemis.cli.commands.tools.PrintData;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.extension.messaging.activemq.logging.MessagingLogger;

/**
 *
 * @author Emmanuel Hugonnet (c) 2020 Red Hat, Inc.
 */
public class PrintDataOperation extends AbstractArtemisActionHandler {

    public static final String OPERATION_NAME = "print-data";
    static final PrintDataOperation INSTANCE = new PrintDataOperation();
    private static final AttributeDefinition REPLY_UUID = new SimpleAttributeDefinitionBuilder(UUID, ModelType.STRING, false).build();

    private static final AttributeDefinition PARAM_SECRET = new SimpleAttributeDefinitionBuilder(SECRET, ModelType.BOOLEAN, false)
            .setDefaultValue(ModelNode.TRUE)
            .build();
    private static final AttributeDefinition PARAM_ARCHIVE = new SimpleAttributeDefinitionBuilder(ARCHIVE, ModelType.BOOLEAN, false)
            .setDefaultValue(ModelNode.FALSE)
            .build();

    private PrintDataOperation() {

    }

    void registerOperation(final ManagementResourceRegistration registry, final ResourceDescriptionResolver resourceDescriptionResolver) {
        registry.registerOperationHandler(
                new SimpleOperationDefinitionBuilder(OPERATION_NAME, resourceDescriptionResolver)
                        .addParameter(PARAM_SECRET)
                        .addParameter(PARAM_ARCHIVE)
                        .setReplyParameters(REPLY_UUID)
                        .setRuntimeOnly()
                        .build(),
                INSTANCE);
    }

    @Override
    protected void executeRuntimeStep(OperationContext context, ModelNode operation) throws OperationFailedException {
        if (context.getRunningMode() != ADMIN_ONLY) {
            throw MessagingLogger.ROOT_LOGGER.managementOperationAllowedOnlyInRunningMode(OPERATION_NAME, ADMIN_ONLY);
        }
        checkAllowedOnJournal(context, OPERATION_NAME);
        boolean secret = PARAM_SECRET.resolveModelAttribute(context, operation).asBoolean();
        boolean archive = PARAM_ARCHIVE.resolveModelAttribute(context, operation).asBoolean();

        final File bindings = resolveFile(context, BINDINGS_DIRECTORY_PATH);
        final File paging = resolveFile(context, PAGING_DIRECTORY_PATH);
        final File journal = resolveFile(context, JOURNAL_DIRECTORY_PATH);

        try {
            final TemporaryFileInputStream temp = new TemporaryFileInputStream(Files.createTempFile("data-print", ".txt"));
            try ( PrintStream out = new PrintStream(temp.getFile().toFile())) {
                PrintData.printData(bindings, journal, paging, out, secret);
            }
            String uuid;
            if (archive) {
                final TemporaryFileInputStream tempZip = new TemporaryFileInputStream(Files.createTempFile("data-print", ".zip"));
                try ( ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(tempZip.getFile()))) {
                    out.putNextEntry(new ZipEntry("data-print-report.txt"));
                    byte[] bytes = new byte[1024];
                    int length;
                    while ((length = temp.read(bytes)) >= 0) {
                        out.write(bytes, 0, length);
                    }
                    out.finish();
                } finally {
                    temp.close();
                }
                uuid = context.attachResultStream("application/zip", tempZip);
            } else {
                uuid = context.attachResultStream("text/plain", temp);
            }
            context.getResult().get(UUID).set(uuid);
        } catch (Exception e) {
            throw new OperationFailedException(e);
        }
    }
}
