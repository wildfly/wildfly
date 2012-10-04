/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

import org.jboss.as.controller.CaseParameterCorrector;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.ObjectTypeAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleMapAttributeDefinition;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.logging.correctors.FileCorrector;
import org.jboss.as.logging.resolvers.FileResolver;
import org.jboss.as.logging.resolvers.LevelResolver;
import org.jboss.as.logging.resolvers.OverflowActionResolver;
import org.jboss.as.logging.resolvers.SizeResolver;
import org.jboss.as.logging.resolvers.TargetResolver;
import org.jboss.as.logging.validators.FileValidator;
import org.jboss.as.logging.validators.LogLevelValidator;
import org.jboss.as.logging.validators.SizeValidator;
import org.jboss.as.logging.validators.SuffixValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.logmanager.Level;
import org.jboss.logmanager.handlers.AsyncHandler.OverflowAction;


/**
 * @author Emanuel Muckenhuber
 */
public interface CommonAttributes {

    // Attributes
    PropertyAttributeDefinition APPEND = PropertyAttributeDefinition.Builder.of("append", ModelType.BOOLEAN, true).
            setDefaultValue(new ModelNode(true)).
            setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES).
            build();

    String ASYNC_HANDLER = "async-handler";

    PropertyAttributeDefinition AUTOFLUSH = PropertyAttributeDefinition.Builder.of("autoflush", ModelType.BOOLEAN, true).
            setDefaultValue(new ModelNode(true)).
            setPropertyName("autoFlush").
            build();

    SimpleAttributeDefinition CATEGORY = SimpleAttributeDefinitionBuilder.create("category", ModelType.STRING, true).build();

    SimpleAttributeDefinition CLASS = SimpleAttributeDefinitionBuilder.create("class", ModelType.STRING).
            setAllowExpression(false).
            build();

    String CONSOLE_HANDLER = "console-handler";

    String CUSTOM_HANDLER = "custom-handler";

    PropertyAttributeDefinition ENCODING = PropertyAttributeDefinition.Builder.of("encoding", ModelType.STRING, true).build();

    String FILE_HANDLER = "file-handler";

    PropertyAttributeDefinition FILTER_SPEC = PropertyAttributeDefinition.Builder.of("filter-spec", ModelType.STRING, true).
            addAlternatives("filter").
            setAllowExpression(true).
            build();

    PropertyAttributeDefinition FORMATTER = PropertyAttributeDefinition.Builder.of("formatter", ModelType.STRING, true).
            setDefaultValue(new ModelNode("%d{HH:mm:ss,SSS} %-5p [%c] (%t) %s%E%n")).
            build();

    SimpleAttributeDefinition HANDLER = SimpleAttributeDefinitionBuilder.create("handler", ModelType.STRING).build();

    LogHandlerListAttributeDefinition HANDLERS = LogHandlerListAttributeDefinition.Builder.of("handlers", HANDLER).
            setAllowNull(true).
            build();

    SimpleAttributeDefinition HANDLER_NAME = SimpleAttributeDefinitionBuilder.create("name", ModelType.STRING, true).build();

    // JUL doesn't allow for null levels. Use ALL as the default
    PropertyAttributeDefinition LEVEL = PropertyAttributeDefinition.Builder.of("level", ModelType.STRING, true).
            setCorrector(CaseParameterCorrector.TO_UPPER).
            setDefaultValue(new ModelNode(Level.ALL.getName())).
            setResolver(LevelResolver.INSTANCE).
            setValidator(new LogLevelValidator(true)).
            build();

    String LOGGER = "logger";

    String LOGGING_PROFILE = "logging-profile";

    String LOGGING_PROFILES = "logging-profiles";

    PropertyAttributeDefinition MAX_BACKUP_INDEX = PropertyAttributeDefinition.Builder.of("max-backup-index", ModelType.INT, true).
            setDefaultValue(new ModelNode(1)).
            setPropertyName("maxBackupIndex").
            setValidator(new IntRangeValidator(1, true)).
            build();

    SimpleAttributeDefinition MODULE = SimpleAttributeDefinitionBuilder.create("module", ModelType.STRING).
            setAllowExpression(false).
            build();

    SimpleAttributeDefinition NAME = SimpleAttributeDefinitionBuilder.create("name", ModelType.STRING, true).
            setDeprecated(ModelVersion.create(1, 2, 0)).
            build();

    SimpleAttributeDefinition VALUE = SimpleAttributeDefinitionBuilder.create("value", ModelType.STRING).build();

    PropertyAttributeDefinition OVERFLOW_ACTION = PropertyAttributeDefinition.Builder.of("overflow-action", ModelType.STRING).
            setDefaultValue(new ModelNode(OverflowAction.BLOCK.name())).
            setPropertyName("overflowAction").
            setResolver(OverflowActionResolver.INSTANCE).
            setValidator(EnumValidator.create(OverflowAction.class, false, false)).
            build();

    SimpleAttributeDefinition PATH = SimpleAttributeDefinitionBuilder.create("path", ModelType.STRING).build();

    String PATTERN_FORMATTER = "pattern-formatter";

    String PERIODIC_ROTATING_FILE_HANDLER = "periodic-rotating-file-handler";

    SimpleAttributeDefinition PROPERTY = SimpleAttributeDefinitionBuilder.create("property", ModelType.PROPERTY).build();

    SimpleMapAttributeDefinition PROPERTIES = new SimpleMapAttributeDefinition.Builder("properties", true)
            .setAllowExpression(true)
            .build();

    PropertyAttributeDefinition QUEUE_LENGTH = PropertyAttributeDefinition.Builder.of("queue-length", ModelType.INT).
            setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES).
            setPropertyName("queueLength").
            setValidator(new IntRangeValidator(1, false)).
            build();

    SimpleAttributeDefinition RELATIVE_TO = SimpleAttributeDefinitionBuilder.create("relative-to", ModelType.STRING, true).build();

    String ROOT_LOGGER = "root-logger";

    String ROOT_LOGGER_ATTRIBUTE_NAME = "ROOT";

    PropertyAttributeDefinition ROTATE_SIZE = PropertyAttributeDefinition.Builder.of("rotate-size", ModelType.STRING).
            setDefaultValue(new ModelNode("2m")).
            setPropertyName("rotateSize").
            setResolver(SizeResolver.INSTANCE).
            setValidator(new SizeValidator()).
            build();

    String SIZE_ROTATING_FILE_HANDLER = "size-rotating-file-handler";

    LogHandlerListAttributeDefinition SUBHANDLERS = LogHandlerListAttributeDefinition.Builder.of("subhandlers", HANDLER).
            setAllowNull(true).
            build();

    PropertyAttributeDefinition SUFFIX = PropertyAttributeDefinition.Builder.of("suffix", ModelType.STRING).
            setValidator(new SuffixValidator()).
            build();

    PropertyAttributeDefinition TARGET = PropertyAttributeDefinition.Builder.of("target", ModelType.STRING, true).
            setDefaultValue(new ModelNode(Target.SYSTEM_OUT.toString())).
            setResolver(TargetResolver.INSTANCE).
            setValidator(EnumValidator.create(Target.class, true, false)).
            build();

    PropertyAttributeDefinition USE_PARENT_HANDLERS = PropertyAttributeDefinition.Builder.of("use-parent-handlers", ModelType.BOOLEAN, true).
            setDefaultValue(new ModelNode(true)).
            setPropertyName("useParentHandlers").
            build();

    // Global object types

    PropertyObjectTypeAttributeDefinition FILE = PropertyObjectTypeAttributeDefinition.Builder.of("file", RELATIVE_TO, PATH).
            setCorrector(FileCorrector.INSTANCE).
            setPropertyName("fileName").
            setResolver(FileResolver.INSTANCE).
            setValidator(new FileValidator()).
            build();

    /**
     * The name of the root logger.
     */
    String ROOT_LOGGER_NAME = "";

    // Legacy Filter attributes
    SimpleAttributeDefinition ACCEPT = SimpleAttributeDefinitionBuilder.create("accept", ModelType.BOOLEAN, true).
            setDefaultValue(new ModelNode(true)).
            build();

    SimpleAttributeDefinition CHANGE_LEVEL = SimpleAttributeDefinitionBuilder.create("change-level", ModelType.STRING, true).
            setCorrector(CaseParameterCorrector.TO_UPPER).
            setValidator(new LogLevelValidator(true)).
            build();

    SimpleAttributeDefinition DENY = SimpleAttributeDefinitionBuilder.create("deny", ModelType.BOOLEAN, true).
            setDefaultValue(new ModelNode(true)).
            build();

    SimpleAttributeDefinition MATCH = SimpleAttributeDefinitionBuilder.create("match", ModelType.STRING, true).build();

    SimpleAttributeDefinition MAX_INCLUSIVE = SimpleAttributeDefinitionBuilder.create("max-inclusive", ModelType.BOOLEAN, true).
            setDefaultValue(new ModelNode(true)).
            build();

    SimpleAttributeDefinition MAX_LEVEL = SimpleAttributeDefinitionBuilder.create("max-level", ModelType.STRING, true).
            setCorrector(CaseParameterCorrector.TO_UPPER).
            setValidator(new LogLevelValidator(true)).
            build();

    SimpleAttributeDefinition MIN_INCLUSIVE = SimpleAttributeDefinitionBuilder.create("min-inclusive", ModelType.BOOLEAN, true).
            setDefaultValue(new ModelNode(true)).
            build();

    SimpleAttributeDefinition MIN_LEVEL = SimpleAttributeDefinitionBuilder.create("min-level", ModelType.STRING, true).
            setCorrector(CaseParameterCorrector.TO_UPPER).
            setValidator(new LogLevelValidator(true)).
            build();

    SimpleAttributeDefinition NEW_LEVEL = SimpleAttributeDefinitionBuilder.create("new-level", ModelType.STRING, true).
            setCorrector(CaseParameterCorrector.TO_UPPER).
            setValidator(new LogLevelValidator(true)).
            build();

    SimpleAttributeDefinition PATTERN = SimpleAttributeDefinitionBuilder.create("pattern", ModelType.STRING).build();

    SimpleAttributeDefinition REPLACEMENT = SimpleAttributeDefinitionBuilder.create("replacement", ModelType.STRING).build();

    SimpleAttributeDefinition REPLACE_ALL = SimpleAttributeDefinitionBuilder.create("replace-all", ModelType.BOOLEAN).
            setDefaultValue(new ModelNode(true)).
            build();

    ObjectTypeAttributeDefinition LEVEL_RANGE_LEGACY = ObjectTypeAttributeDefinition.Builder.of("level-range", MIN_LEVEL, MIN_INCLUSIVE, MAX_LEVEL, MAX_INCLUSIVE).
            setAllowNull(true).
            build();

    ObjectTypeAttributeDefinition REPLACE = ObjectTypeAttributeDefinition.Builder.of("replace", PATTERN, REPLACEMENT, REPLACE_ALL).
            setAllowNull(true).
            build();

    ObjectTypeAttributeDefinition NOT = ObjectTypeAttributeDefinition.Builder.of("not", ACCEPT, CHANGE_LEVEL, DENY, LEVEL, LEVEL_RANGE_LEGACY, MATCH, REPLACE).
            setAllowNull(true).
            build();

    ObjectTypeAttributeDefinition ALL = ObjectTypeAttributeDefinition.Builder.of("all", ACCEPT, CHANGE_LEVEL, DENY, LEVEL, LEVEL_RANGE_LEGACY, MATCH, NOT, REPLACE).
            setAllowNull(true).
            build();

    ObjectTypeAttributeDefinition ANY = ObjectTypeAttributeDefinition.Builder.of("any", ACCEPT, CHANGE_LEVEL, DENY, LEVEL, LEVEL_RANGE_LEGACY, MATCH, NOT, REPLACE).
            setAllowNull(true).
            build();

    ObjectTypeAttributeDefinition FILTER = ObjectTypeAttributeDefinition.Builder.of("filter", ALL, ANY, ACCEPT, CHANGE_LEVEL, DENY, LEVEL, LEVEL_RANGE_LEGACY, MATCH, NOT, REPLACE).
            addAlternatives(FILTER_SPEC.getName()).
            setDeprecated(ModelVersion.create(1, 2, 0)).
            setAllowNull(true).
            build();
}
