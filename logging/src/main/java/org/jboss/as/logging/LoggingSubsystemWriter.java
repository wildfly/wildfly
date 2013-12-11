/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

import static org.jboss.as.logging.AbstractHandlerDefinition.NAMED_FORMATTER;
import static org.jboss.as.logging.AsyncHandlerResourceDefinition.ASYNC_HANDLER;
import static org.jboss.as.logging.AsyncHandlerResourceDefinition.OVERFLOW_ACTION;
import static org.jboss.as.logging.AsyncHandlerResourceDefinition.QUEUE_LENGTH;
import static org.jboss.as.logging.AsyncHandlerResourceDefinition.SUBHANDLERS;
import static org.jboss.as.logging.CommonAttributes.APPEND;
import static org.jboss.as.logging.CommonAttributes.AUTOFLUSH;
import static org.jboss.as.logging.CommonAttributes.ENABLED;
import static org.jboss.as.logging.CommonAttributes.ENCODING;
import static org.jboss.as.logging.CommonAttributes.FILE;
import static org.jboss.as.logging.CommonAttributes.FILTER_SPEC;
import static org.jboss.as.logging.AbstractHandlerDefinition.FORMATTER;
import static org.jboss.as.logging.CommonAttributes.HANDLERS;
import static org.jboss.as.logging.CommonAttributes.HANDLER_NAME;
import static org.jboss.as.logging.CommonAttributes.LEVEL;
import static org.jboss.as.logging.CommonAttributes.LOGGING_PROFILE;
import static org.jboss.as.logging.CommonAttributes.LOGGING_PROFILES;
import static org.jboss.as.logging.CommonAttributes.NAME;
import static org.jboss.as.logging.ConsoleHandlerResourceDefinition.CONSOLE_HANDLER;
import static org.jboss.as.logging.ConsoleHandlerResourceDefinition.TARGET;
import static org.jboss.as.logging.CustomFormatterResourceDefinition.CUSTOM_FORMATTER;
import static org.jboss.as.logging.CommonAttributes.CLASS;
import static org.jboss.as.logging.CustomHandlerResourceDefinition.CUSTOM_HANDLER;
import static org.jboss.as.logging.CommonAttributes.MODULE;
import static org.jboss.as.logging.CommonAttributes.PROPERTIES;
import static org.jboss.as.logging.FileHandlerResourceDefinition.FILE_HANDLER;
import static org.jboss.as.logging.LoggerResourceDefinition.CATEGORY;
import static org.jboss.as.logging.LoggerResourceDefinition.LOGGER;
import static org.jboss.as.logging.LoggerResourceDefinition.USE_PARENT_HANDLERS;
import static org.jboss.as.logging.PatternFormatterResourceDefinition.PATTERN_FORMATTER;
import static org.jboss.as.logging.PeriodicHandlerResourceDefinition.PERIODIC_ROTATING_FILE_HANDLER;
import static org.jboss.as.logging.PeriodicHandlerResourceDefinition.SUFFIX;
import static org.jboss.as.logging.RootLoggerResourceDefinition.ROOT_LOGGER_ATTRIBUTE_NAME;
import static org.jboss.as.logging.RootLoggerResourceDefinition.ROOT_LOGGER_PATH_NAME;
import static org.jboss.as.logging.SizeRotatingHandlerResourceDefinition.MAX_BACKUP_INDEX;
import static org.jboss.as.logging.SizeRotatingHandlerResourceDefinition.ROTATE_ON_BOOT;
import static org.jboss.as.logging.SizeRotatingHandlerResourceDefinition.ROTATE_SIZE;
import static org.jboss.as.logging.SizeRotatingHandlerResourceDefinition.SIZE_ROTATING_FILE_HANDLER;
import static org.jboss.as.logging.SyslogHandlerResourceDefinition.APP_NAME;
import static org.jboss.as.logging.SyslogHandlerResourceDefinition.FACILITY;
import static org.jboss.as.logging.SyslogHandlerResourceDefinition.HOSTNAME;
import static org.jboss.as.logging.SyslogHandlerResourceDefinition.PORT;
import static org.jboss.as.logging.SyslogHandlerResourceDefinition.SERVER_ADDRESS;
import static org.jboss.as.logging.SyslogHandlerResourceDefinition.SYSLOG_FORMATTER;
import static org.jboss.as.logging.SyslogHandlerResourceDefinition.SYSLOG_HANDLER;

import java.util.List;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class LoggingSubsystemWriter implements XMLStreamConstants, XMLElementWriter<SubsystemMarshallingContext> {

    static final LoggingSubsystemWriter INSTANCE = new LoggingSubsystemWriter();

    @Override
    public void writeContent(final XMLExtendedStreamWriter writer, final SubsystemMarshallingContext context) throws XMLStreamException {
        context.startSubsystemElement(Namespace.CURRENT.getUriString(), false);

        ModelNode model = context.getModelNode();

        // Marshall attributes
        LoggingRootResource.ADD_LOGGING_API_DEPENDENCIES.marshallAsElement(model, false, writer);

        writeContent(writer, model);

        if (model.hasDefined(LOGGING_PROFILE)) {
            final List<Property> profiles = model.get(LOGGING_PROFILE).asPropertyList();
            if (!profiles.isEmpty()) {
                writer.writeStartElement(LOGGING_PROFILES);
                for (Property profile : profiles) {
                    final String name = profile.getName();
                    writer.writeStartElement(LOGGING_PROFILE);
                    writer.writeAttribute(Attribute.NAME.getLocalName(), name);
                    writeContent(writer, profile.getValue());
                    writer.writeEndElement();
                }
                writer.writeEndElement();
            }
        }
        writer.writeEndElement();
    }

    private void writeContent(final XMLExtendedStreamWriter writer, final ModelNode model) throws XMLStreamException {

        if (model.hasDefined(ASYNC_HANDLER)) {
            final ModelNode handlers = model.get(ASYNC_HANDLER);

            for (Property handlerProp : handlers.asPropertyList()) {
                final String name = handlerProp.getName();
                final ModelNode handler = handlerProp.getValue();
                if (handler.isDefined()) {
                    writeAsynchHandler(writer, handler, name);
                }
            }
        }
        if (model.hasDefined(CONSOLE_HANDLER)) {
            final ModelNode handlers = model.get(CONSOLE_HANDLER);

            for (Property handlerProp : handlers.asPropertyList()) {
                final String name = handlerProp.getName();
                final ModelNode handler = handlerProp.getValue();
                if (handler.isDefined()) {
                    writeConsoleHandler(writer, handler, name);
                }
            }
        }
        if (model.hasDefined(FILE_HANDLER)) {
            final ModelNode handlers = model.get(FILE_HANDLER);

            for (Property handlerProp : handlers.asPropertyList()) {
                final String name = handlerProp.getName();
                final ModelNode handler = handlerProp.getValue();
                if (handler.isDefined()) {
                    writeFileHandler(writer, handler, name);
                }
            }
        }
        if (model.hasDefined(CUSTOM_HANDLER)) {
            final ModelNode handlers = model.get(CUSTOM_HANDLER);

            for (Property handlerProp : handlers.asPropertyList()) {
                final String name = handlerProp.getName();
                final ModelNode handler = handlerProp.getValue();
                if (handler.isDefined()) {
                    writeCustomHandler(writer, handler, name);
                }
            }
        }
        if (model.hasDefined(PERIODIC_ROTATING_FILE_HANDLER)) {
            final ModelNode handlers = model.get(PERIODIC_ROTATING_FILE_HANDLER);

            for (Property handlerProp : handlers.asPropertyList()) {
                final String name = handlerProp.getName();
                final ModelNode handler = handlerProp.getValue();
                if (handler.isDefined()) {
                    writePeriodicRotatingFileHandler(writer, handler, name);
                }
            }
        }
        if (model.hasDefined(SIZE_ROTATING_FILE_HANDLER)) {
            final ModelNode handlers = model.get(SIZE_ROTATING_FILE_HANDLER);

            for (Property handlerProp : handlers.asPropertyList()) {
                final String name = handlerProp.getName();
                final ModelNode handler = handlerProp.getValue();
                if (handler.isDefined()) {
                    writeSizeRotatingFileHandler(writer, handler, name);
                }
            }
        }
        if (model.hasDefined(SYSLOG_HANDLER)) {
            final ModelNode handlers = model.get(SYSLOG_HANDLER);

            for (Property handlerProp : handlers.asPropertyList()) {
                final String name = handlerProp.getName();
                final ModelNode handler = handlerProp.getValue();
                if (handler.isDefined()) {
                    writeSyslogHandler(writer, handler, name);
                }
            }
        }
        if (model.hasDefined(LOGGER)) {
            for (String name : model.get(LOGGER).keys()) {
                writeLogger(writer, name, model.get(LOGGER, name));
            }
        }
        if (model.hasDefined(ROOT_LOGGER_PATH_NAME)) {
            writeRootLogger(writer, model.get(ROOT_LOGGER_PATH_NAME, ROOT_LOGGER_ATTRIBUTE_NAME));
        }

        writeFormatters(writer, PATTERN_FORMATTER, model);
        writeFormatters(writer, CUSTOM_FORMATTER, model);
    }

    private void writeCommonLogger(final XMLExtendedStreamWriter writer, final ModelNode model) throws XMLStreamException {
        LEVEL.marshallAsElement(model, writer);
        FILTER_SPEC.marshallAsElement(model, writer);
        HANDLERS.marshallAsElement(model, writer);
    }

    private void writeCommonHandler(final XMLExtendedStreamWriter writer, final ModelNode model) throws XMLStreamException {
        LEVEL.marshallAsElement(model, writer);
        ENCODING.marshallAsElement(model, writer);
        FILTER_SPEC.marshallAsElement(model, writer);
        FORMATTER.marshallAsElement(model, writer);
        NAMED_FORMATTER.marshallAsElement(model, writer);
    }

    private void writeConsoleHandler(final XMLExtendedStreamWriter writer, final ModelNode model, final String name)
            throws XMLStreamException {
        writer.writeStartElement(Element.CONSOLE_HANDLER.getLocalName());
        writer.writeAttribute(HANDLER_NAME.getXmlName(), name);
        AUTOFLUSH.marshallAsAttribute(model, writer);
        ENABLED.marshallAsAttribute(model, false, writer);
        writeCommonHandler(writer, model);
        TARGET.marshallAsElement(model, writer);
        writer.writeEndElement();
    }

    private void writeFileHandler(final XMLExtendedStreamWriter writer, final ModelNode model, final String name) throws XMLStreamException {
        writer.writeStartElement(Element.FILE_HANDLER.getLocalName());
        writer.writeAttribute(Attribute.NAME.getLocalName(), name);
        AUTOFLUSH.marshallAsAttribute(model, writer);
        ENABLED.marshallAsAttribute(model, false, writer);
        writeCommonHandler(writer, model);
        FILE.marshallAsElement(model, writer);
        APPEND.marshallAsElement(model, writer);

        writer.writeEndElement();
    }

    private void writeCustomHandler(final XMLExtendedStreamWriter writer, final ModelNode model, final String name)
            throws XMLStreamException {
        writer.writeStartElement(Element.CUSTOM_HANDLER.getLocalName());
        writer.writeAttribute(HANDLER_NAME.getXmlName(), name);
        CLASS.marshallAsAttribute(model, writer);
        MODULE.marshallAsAttribute(model, writer);
        ENABLED.marshallAsAttribute(model, false, writer);
        writeCommonHandler(writer, model);
        PROPERTIES.marshallAsElement(model, writer);

        writer.writeEndElement();
    }

    private void writePeriodicRotatingFileHandler(final XMLExtendedStreamWriter writer, final ModelNode model, final String name) throws XMLStreamException {
        writer.writeStartElement(Element.PERIODIC_ROTATING_FILE_HANDLER.getLocalName());
        writer.writeAttribute(HANDLER_NAME.getXmlName(), name);
        AUTOFLUSH.marshallAsAttribute(model, writer);
        ENABLED.marshallAsAttribute(model, false, writer);
        writeCommonHandler(writer, model);
        FILE.marshallAsElement(model, writer);
        SUFFIX.marshallAsElement(model, writer);
        APPEND.marshallAsElement(model, writer);

        writer.writeEndElement();
    }

    private void writeSizeRotatingFileHandler(final XMLExtendedStreamWriter writer, final ModelNode model, final String name) throws XMLStreamException {
        writer.writeStartElement(Element.SIZE_ROTATING_FILE_HANDLER.getLocalName());
        writer.writeAttribute(HANDLER_NAME.getXmlName(), name);
        AUTOFLUSH.marshallAsAttribute(model, writer);
        ENABLED.marshallAsAttribute(model, false, writer);
        ROTATE_ON_BOOT.marshallAsAttribute(model, false, writer);
        writeCommonHandler(writer, model);
        FILE.marshallAsElement(model, writer);
        ROTATE_SIZE.marshallAsElement(model, writer);
        MAX_BACKUP_INDEX.marshallAsElement(model, writer);
        APPEND.marshallAsElement(model, writer);

        writer.writeEndElement();
    }

    private void writeSyslogHandler(final XMLExtendedStreamWriter writer, final ModelNode model, final String name) throws XMLStreamException {
        writer.writeStartElement(Element.SYSLOG_HANDLER.getLocalName());
        writer.writeAttribute(HANDLER_NAME.getXmlName(), name);
        ENABLED.marshallAsAttribute(model, false, writer);
        LEVEL.marshallAsElement(model, writer);
        SERVER_ADDRESS.marshallAsElement(model, writer);
        HOSTNAME.marshallAsElement(model, writer);
        PORT.marshallAsElement(model, writer);
        APP_NAME.marshallAsElement(model, writer);
        SYSLOG_FORMATTER.marshallAsElement(model, writer);
        FACILITY.marshallAsElement(model, writer);

        writer.writeEndElement();
    }

    private void writeAsynchHandler(final XMLExtendedStreamWriter writer, final ModelNode model, final String name) throws XMLStreamException {
        writer.writeStartElement(Element.ASYNC_HANDLER.getLocalName());
        writer.writeAttribute(HANDLER_NAME.getXmlName(), name);
        ENABLED.marshallAsAttribute(model, false, writer);
        LEVEL.marshallAsElement(model, writer);
        FILTER_SPEC.marshallAsElement(model, writer);
        FORMATTER.marshallAsElement(model, writer);
        QUEUE_LENGTH.marshallAsElement(model, writer);
        OVERFLOW_ACTION.marshallAsElement(model, writer);
        SUBHANDLERS.marshallAsElement(model, writer);

        writer.writeEndElement();
    }

    private void writeLogger(final XMLExtendedStreamWriter writer, String name, final ModelNode model) throws XMLStreamException {
        writer.writeStartElement(Element.LOGGER.getLocalName());
        writer.writeAttribute(CATEGORY.getXmlName(), name);
        USE_PARENT_HANDLERS.marshallAsAttribute(model, writer);
        writeCommonLogger(writer, model);
        writer.writeEndElement();
    }

    private void writeRootLogger(final XMLExtendedStreamWriter writer, final ModelNode model) throws XMLStreamException {
        writer.writeStartElement(Element.ROOT_LOGGER.getLocalName());
        writeCommonLogger(writer, model);
        writer.writeEndElement();
    }

    private void writeFormatters(final XMLExtendedStreamWriter writer, final AttributeDefinition attribute, final ModelNode model) throws XMLStreamException {
        if (model.hasDefined(attribute.getName())) {
            for (String name : model.get(attribute.getName()).keys()) {
                writer.writeStartElement(Element.FORMATTER.getLocalName());
                writer.writeAttribute(NAME.getXmlName(), name);
                final ModelNode value = model.get(attribute.getName(), name);
                attribute.marshallAsElement(value, writer);
                writer.writeEndElement();
            }
        }
    }
}
