/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.logging;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationContext.ResultHandler;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleOperationDefinition;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess.Flag;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.services.path.PathManager;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class LoggingResourceDefinition extends TransformerResourceDefinition {

    static final PathElement SUBSYSTEM_PATH = PathElement.pathElement(SUBSYSTEM, LoggingExtension.SUBSYSTEM_NAME);

    static final SimpleAttributeDefinition ADD_LOGGING_API_DEPENDENCIES = SimpleAttributeDefinitionBuilder.create("add-logging-api-dependencies", ModelType.BOOLEAN, true)
            .setAllowExpression(true)
            .setAttributeMarshaller(ElementAttributeMarshaller.VALUE_ATTRIBUTE_MARSHALLER)
            .setDefaultValue(new ModelNode(true))
            .setFlags(Flag.RESTART_ALL_SERVICES)
            .build();

    static final SimpleAttributeDefinition USE_DEPLOYMENT_LOGGING_CONFIG = SimpleAttributeDefinitionBuilder.create("use-deployment-logging-config", ModelType.BOOLEAN, true)
            .setAllowExpression(true)
            .setAttributeMarshaller(ElementAttributeMarshaller.VALUE_ATTRIBUTE_MARSHALLER)
            .setDefaultValue(new ModelNode(true))
            .setFlags(Flag.RESTART_ALL_SERVICES)
            .build();

    static final SimpleAttributeDefinition NAME = SimpleAttributeDefinitionBuilder.create("name", ModelType.STRING, false)
            .setAllowExpression(true)
            .setValidator(new StringLengthValidator(1, false))
            .build();

    static final SimpleAttributeDefinition LINES = SimpleAttributeDefinitionBuilder.create("lines", ModelType.INT, true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(10))
            .setValidator(new IntRangeValidator(-1, true))
            .build();

    static final SimpleAttributeDefinition SKIP = SimpleAttributeDefinitionBuilder.create("skip", ModelType.INT, true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(0))
            .setValidator(new IntRangeValidator(0, true))
            .build();

    static final SimpleAttributeDefinition TAIL = SimpleAttributeDefinitionBuilder.create("tail", ModelType.BOOLEAN, true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(true))
            .build();

    static final SimpleAttributeDefinition FILE_NAME = SimpleAttributeDefinitionBuilder.create("file-name", ModelType.STRING, false)
            .setAllowExpression(false)
            .build();

    static final SimpleAttributeDefinition FILE_SIZE = SimpleAttributeDefinitionBuilder.create("file-size", ModelType.LONG, false)
            .setAllowExpression(false)
            .build();

    static final SimpleAttributeDefinition LAST_MODIFIED_DATE = SimpleAttributeDefinitionBuilder.create("last-modified-date", ModelType.STRING, false)
            .setAllowExpression(false)
            .build();

    static final SimpleOperationDefinition READ_LOG_FILE = new SimpleOperationDefinitionBuilder("read-log-file", LoggingExtension.getStandardResourceDescriptionResolver())
            .addAccessConstraint(LogFileResourceDefinition.VIEW_SERVER_LOGS)
            .setDeprecated(ModelVersion.create(2, 1, 0))
            .setParameters(NAME, CommonAttributes.ENCODING, LINES, SKIP, TAIL)
            .setReplyType(ModelType.LIST)
            .setReplyValueType(ModelType.STRING)
            .setReadOnly()
            .setRuntimeOnly()
            .build();

    static final SimpleOperationDefinition LIST_LOG_FILES = new SimpleOperationDefinitionBuilder("list-log-files", LoggingExtension.getStandardResourceDescriptionResolver())
            .addAccessConstraint(LogFileResourceDefinition.VIEW_SERVER_LOGS)
            .setDeprecated(ModelVersion.create(2, 1, 0))
            .setReplyType(ModelType.LIST)
            .setReplyParameters(FILE_NAME, FILE_SIZE, LAST_MODIFIED_DATE)
            .setReadOnly()
            .setRuntimeOnly()
            .build();

    static final SimpleAttributeDefinition[] ATTRIBUTES = {
            ADD_LOGGING_API_DEPENDENCIES,
            USE_DEPLOYMENT_LOGGING_CONFIG,
    };

    private final PathManager pathManager;

    protected LoggingResourceDefinition(final PathManager pathManager) {
        super(SUBSYSTEM_PATH,
                LoggingExtension.getResourceDescriptionResolver(),
                new LoggingSubsystemAdd(pathManager),
                ReloadRequiredRemoveStepHandler.INSTANCE);
        this.pathManager = pathManager;
    }

    @Override
    public void registerAttributes(final ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);

        final OperationStepHandler writeHandler = new ReloadRequiredWriteAttributeHandler(ATTRIBUTES);
        for (SimpleAttributeDefinition attribute : ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(attribute, null, writeHandler);
        }
    }

    @Override
    public void registerOperations(final ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
        // Only register on server
        if (pathManager != null) {
            resourceRegistration.registerOperationHandler(LIST_LOG_FILES, new ListLogFilesOperation());
            resourceRegistration.registerOperationHandler(READ_LOG_FILE, new ReadLogFileOperation());
        }
    }

    @Override
    public void registerTransformers(final KnownModelVersion modelVersion, final ResourceTransformationDescriptionBuilder rootResourceBuilder, final ResourceTransformationDescriptionBuilder loggingProfileBuilder) {
        switch (modelVersion) {
            case VERSION_1_1_0:
            case VERSION_1_2_0:
            case VERSION_1_3_0: {
                rootResourceBuilder.getAttributeBuilder()
                        .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(false, true, ADD_LOGGING_API_DEPENDENCIES.getDefaultValue()), ADD_LOGGING_API_DEPENDENCIES)
                        .addRejectCheck(RejectAttributeChecker.DEFINED, ADD_LOGGING_API_DEPENDENCIES)
                        .end();
            }
            case VERSION_1_4_0: {
                rootResourceBuilder.getAttributeBuilder()
                        .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(false, true, USE_DEPLOYMENT_LOGGING_CONFIG.getDefaultValue()), USE_DEPLOYMENT_LOGGING_CONFIG)
                        .addRejectCheck(RejectAttributeChecker.DEFINED, USE_DEPLOYMENT_LOGGING_CONFIG)
                        .end();
            }
        }
    }

    private class ListLogFilesOperation implements OperationStepHandler {

        @Override
        public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
            final ModelNode model = Resource.Tools.readModel(context.readResource(PathAddress.EMPTY_ADDRESS));
            final List<File> logFiles = findFiles(pathManager.getPathEntry(ServerEnvironment.SERVER_LOG_DIR).resolvePath(), model);
            final SimpleDateFormat dateFormat = new SimpleDateFormat(LogFileResourceDefinition.ISO_8601_FORMAT);
            final ModelNode result = context.getResult().setEmptyList();
            for (File logFile : logFiles) {
                final ModelNode fileInfo = new ModelNode();
                fileInfo.get(FILE_NAME.getName()).set(logFile.getName());
                fileInfo.get(FILE_SIZE.getName()).set(logFile.length());
                fileInfo.get(LAST_MODIFIED_DATE.getName()).set(dateFormat.format(new Date(logFile.lastModified())));
                result.add(fileInfo);
            }
            context.completeStep(ResultHandler.NOOP_RESULT_HANDLER);
        }
    }


    /**
     * Reads a log file and returns the results.
     * <p/>
     * <i>Note: </i> If this operation ends up being repeatedly invoked, from the web console for instance, there could
     * be a performance impact as the model is read and processed for file names during each invocation
     */
    private class ReadLogFileOperation implements OperationStepHandler {

        @Override
        public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
            // Validate the operation
            for (AttributeDefinition attribute : READ_LOG_FILE.getParameters()) {
                attribute.validateOperation(operation);
            }

            final String fileName = NAME.resolveModelAttribute(context, operation).asString();
            final int numberOfLines = LINES.resolveModelAttribute(context, operation).asInt();
            final int skip = SKIP.resolveModelAttribute(context, operation).asInt();
            final boolean tail = TAIL.resolveModelAttribute(context, operation).asBoolean();
            final ModelNode encodingModel = CommonAttributes.ENCODING.resolveModelAttribute(context, operation);
            final String encoding = (encodingModel.isDefined() ? encodingModel.asString() : null);
            final File path = new File(pathManager.resolveRelativePathEntry(fileName, ServerEnvironment.SERVER_LOG_DIR));

            // The file must exist
            if (!path.exists()) {
                throw LoggingMessages.MESSAGES.logFileNotFound(fileName, ServerEnvironment.SERVER_LOG_DIR);
            }
            final ModelNode model = Resource.Tools.readModel(context.readResource(PathAddress.EMPTY_ADDRESS));
            final List<File> logFiles = findFiles(pathManager.getPathEntry(ServerEnvironment.SERVER_LOG_DIR).resolvePath(), model);
            // User must have permissions to read the file
            if (!path.canRead() || !logFiles.contains(path)) {
                throw LoggingMessages.MESSAGES.readNotAllowed(fileName);
            }

            // Read the contents of the log file
            try {
                final List<String> lines;
                if (numberOfLines == 0) {
                    lines = Collections.emptyList();
                } else {
                    lines = readLines(path, encoding, tail, skip, numberOfLines);
                }
                final ModelNode result = context.getResult().setEmptyList();
                for (String line : lines) {
                    result.add(line);
                }
            } catch (IOException e) {
                throw LoggingMessages.MESSAGES.failedToReadLogFile(e, fileName);
            }
            context.completeStep(ResultHandler.NOOP_RESULT_HANDLER);
        }

        private List<String> readLines(final File file, final String encoding, final boolean tail, final int skip, final int numberOfLines) throws IOException {
            final List<String> lines;
            if (numberOfLines < 0) {
                lines = new ArrayList<String>();
            } else {
                lines = new ArrayList<String>(numberOfLines);
            }
            final InputStream in;
            BufferedReader reader = null;
            try {
                if (tail) {
                    in = new LogFileResourceDefinition.LifoFileInputStream(file);
                } else {
                    in = new FileInputStream(file);
                }
                if (encoding == null) {
                    reader = new BufferedReader(new InputStreamReader(in));
                } else {
                    reader = new BufferedReader(new InputStreamReader(in, encoding));
                }
                int lineCount = 0;
                String line;
                while ((line = reader.readLine()) != null) {
                    if (++lineCount <= skip) continue;
                    if (lines.size() == numberOfLines) break;
                    lines.add(line);
                }
                if (tail) {
                    Collections.reverse(lines);
                }
                return lines;
            } finally {
                safeClose(reader);
            }
        }
    }

    private static void safeClose(final Closeable closeable) {
        if (closeable != null) try {
            closeable.close();
        } catch (Throwable t) {
            LoggingLogger.ROOT_LOGGER.failedToCloseResource(t, closeable);
        }
    }

    private static List<File> findFiles(final String defaultLogDir, final ModelNode model) {
        final List<File> logFiles = new ArrayList<>(LoggingResource.findFiles(defaultLogDir, model));
        // Also need to include logging profile log files
        if (model.hasDefined(CommonAttributes.LOGGING_PROFILE)) {
            for (Property property : model.get(CommonAttributes.LOGGING_PROFILE).asPropertyList()) {
                logFiles.addAll(LoggingResource.findFiles(defaultLogDir, property.getValue()));
            }
        }
        return logFiles;
    }
}
