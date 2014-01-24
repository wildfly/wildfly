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
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.jboss.as.controller.AttributeDefinition;
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
import org.jboss.as.controller.access.constraint.SensitivityClassification;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess.Flag;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.services.path.PathManager;
import org.jboss.as.controller.services.path.PathResourceDefinition;
import org.jboss.as.controller.transform.description.AttributeTransformationDescriptionBuilder;
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
public class LoggingRootResource extends TransformerResourceDefinition {

    private static final SensitiveTargetAccessConstraintDefinition VIEW_SERVER_LOGS = new SensitiveTargetAccessConstraintDefinition(
            new SensitivityClassification(LoggingExtension.SUBSYSTEM_NAME, "view-server-logs", false, false, false));

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

    static final SimpleOperationDefinition READ_LOG_FILE = new SimpleOperationDefinitionBuilder("read-log-file", LoggingExtension.getResourceDescriptionResolver())
            .addAccessConstraint(VIEW_SERVER_LOGS)
            .setParameters(NAME, CommonAttributes.ENCODING, LINES, SKIP, TAIL)
            .setReplyType(ModelType.LIST)
            .setReplyValueType(ModelType.STRING)
            .setReadOnly()
            .setRuntimeOnly()
            .build();

    static final SimpleOperationDefinition LIST_LOG_FILES = new SimpleOperationDefinitionBuilder("list-log-files", LoggingExtension.getResourceDescriptionResolver())
            .addAccessConstraint(VIEW_SERVER_LOGS)
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

    protected LoggingRootResource(final PathManager pathManager) {
        super(SUBSYSTEM_PATH,
                LoggingExtension.getResourceDescriptionResolver(),
                LoggingSubsystemAdd.INSTANCE,
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
            resourceRegistration.registerOperationHandler(LIST_LOG_FILES, new ListLogFilesOperation(pathManager));
            resourceRegistration.registerOperationHandler(READ_LOG_FILE, new ReadLogFileOperation(pathManager));
        }
    }

    @Override
    public void registerTransformers(final KnownModelVersion modelVersion, final ResourceTransformationDescriptionBuilder rootResourceBuilder, final ResourceTransformationDescriptionBuilder loggingProfileBuilder) {
        switch (modelVersion) {
            case VERSION_1_1_0:
            case VERSION_1_2_0:
            case VERSION_1_3_0: {
                AttributeTransformationDescriptionBuilder attributeBuilder = rootResourceBuilder.getAttributeBuilder();
                for (SimpleAttributeDefinition attribute : ATTRIBUTES) {
                    attributeBuilder.setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(false, true, attribute.getDefaultValue()), attribute)
                            .addRejectCheck(RejectAttributeChecker.DEFINED, attribute);
                }
                attributeBuilder.end();
            }
        }
    }

    private static class ListLogFilesOperation implements OperationStepHandler {

        private final PathManager pathManager;

        private ListLogFilesOperation(final PathManager pathManager) {
            this.pathManager = pathManager;
        }

        @Override
        public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
            final List<File> logFiles = findFiles(pathManager, context);
            final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
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
    private static class ReadLogFileOperation implements OperationStepHandler {

        private final PathManager pathManager;

        private ReadLogFileOperation(final PathManager pathManager) {
            this.pathManager = pathManager;
        }

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
            final List<File> logFiles = findFiles(pathManager, context);
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
                    in = new LifoFileInputStream(file);
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

    /**
     * Finds all the files in the {@code jboss.server.log.dir} that are defined on a known file handler.
     *
     * @param pathManager the path manager used to resolve the {@code jboss.server.log.dir}
     * @param context     the context used to read the model from
     *
     * @return a list of files or an empty list if no files were found
     */
    private static List<File> findFiles(final PathManager pathManager, final OperationContext context) {
        final ModelNode model = Resource.Tools.readModel(context.readResource(PathAddress.EMPTY_ADDRESS));
        final String logDir = pathManager.getPathEntry(ServerEnvironment.SERVER_LOG_DIR).resolvePath();
        final File[] logFiles = new File(logDir).listFiles(AllowedFilesFilter.create(model));
        final List<File> result;
        if (logFiles == null) {
            result = Collections.emptyList();
        } else {
            result = Arrays.asList(logFiles);
            Collections.sort(result);
        }
        return result;
    }

    private static class AllowedFilesFilter implements FileFilter {
        private static final List<String> FILE_HANDLER_TYPES = Arrays.asList(FileHandlerResourceDefinition.FILE_HANDLER,
                PeriodicHandlerResourceDefinition.PERIODIC_ROTATING_FILE_HANDLER,
                SizeRotatingHandlerResourceDefinition.SIZE_ROTATING_FILE_HANDLER);

        private final List<String> allowedNames;

        private AllowedFilesFilter(final List<String> allowedNames) {
            this.allowedNames = allowedNames;
        }

        static AllowedFilesFilter create(final ModelNode subsystemModel) {
            final List<String> allowedNames = new ArrayList<String>();
            findFileNames(subsystemModel, allowedNames);
            return new AllowedFilesFilter(allowedNames);
        }

        private static void findFileNames(final ModelNode model, final List<String> names) {
            // Get all the file names from the model
            for (Property resource : model.asPropertyList()) {
                final String name = resource.getName();
                if (CommonAttributes.LOGGING_PROFILE.equals(name)) {
                    for (Property profileResource : resource.getValue().asPropertyList()) {
                        findFileNames(profileResource.getValue(), names);
                    }
                } else if (FILE_HANDLER_TYPES.contains(name)) {
                    for (Property handlerResource : resource.getValue().asPropertyList()) {
                        final ModelNode handlerModel = handlerResource.getValue();
                        // This should always exist, but better to be safe
                        if (handlerModel.hasDefined(CommonAttributes.FILE.getName())) {
                            final ModelNode fileModel = handlerModel.get(CommonAttributes.FILE.getName());
                            if (fileModel.hasDefined(PathResourceDefinition.PATH.getName())) {
                                names.add(fileModel.get(PathResourceDefinition.PATH.getName()).asString());
                            }
                        }
                    }
                }
            }
        }

        @Override
        public boolean accept(final File pathname) {
            if (pathname.canRead()) {
                final String name = pathname.getName();
                // Let's do a best guess on the file
                for (String allowedName : allowedNames) {
                    if (name.equals(allowedName) || name.startsWith(allowedName)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    static final class LifoFileInputStream extends InputStream {
        private final RandomAccessFile raf;
        private final long len;
        private long start;
        private long end;
        private long pos;

        LifoFileInputStream(final File file) throws IOException {
            raf = new RandomAccessFile(file, "r");
            len = raf.length();
            start = len;
            end = len;
            pos = end;
        }

        private void positionFile() throws IOException {
            end = start;
            // If we're at the beginning of the file, nothing more to read
            if (end == 0) {
                end = -1;
                start = -1;
                pos = -1;
                return;
            }

            long filePointer = start - 1;
            while (true) {
                filePointer--;
                // We're at the start of the file
                if (filePointer < 0) {
                    break;
                }
                // Position the file
                raf.seek(filePointer);
                final byte readByte = raf.readByte();
                // If the byte is a line feed we've found the next line ignoring the last line feed in the file
                if (readByte == '\n' && filePointer != (len - 1)) {
                    break;
                }
            }
            start = filePointer + 1;
            pos = start;
        }

        @Override
        public int read() throws IOException {
            if (pos < end) {
                raf.seek(pos++);
                return raf.readByte();
            } else if (pos < 0) {
                return -1;
            } else {
                positionFile();
                return read();
            }
        }

        @Override
        public void close() throws IOException {
            raf.close();
        }
    }
}
