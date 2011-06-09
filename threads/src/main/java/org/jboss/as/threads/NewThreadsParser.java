package org.jboss.as.threads;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.parsing.ParseUtils.invalidAttributeValue;
import static org.jboss.as.controller.parsing.ParseUtils.missingRequired;
import static org.jboss.as.controller.parsing.ParseUtils.missingRequiredElement;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoNamespaceAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;
import static org.jboss.as.threads.CommonAttributes.ALLOW_CORE_TIMEOUT;
import static org.jboss.as.threads.CommonAttributes.BLOCKING;
import static org.jboss.as.threads.CommonAttributes.BOUNDED_QUEUE_THREAD_POOL;
import static org.jboss.as.threads.CommonAttributes.CORE_THREADS;
import static org.jboss.as.threads.CommonAttributes.COUNT;
import static org.jboss.as.threads.CommonAttributes.GROUP_NAME;
import static org.jboss.as.threads.CommonAttributes.HANDOFF_EXECUTOR;
import static org.jboss.as.threads.CommonAttributes.KEEPALIVE_TIME;
import static org.jboss.as.threads.CommonAttributes.MAX_THREADS;
import static org.jboss.as.threads.CommonAttributes.NAME;
import static org.jboss.as.threads.CommonAttributes.PER_CPU;
import static org.jboss.as.threads.CommonAttributes.PRIORITY;
import static org.jboss.as.threads.CommonAttributes.PROPERTIES;
import static org.jboss.as.threads.CommonAttributes.QUEUELESS_THREAD_POOL;
import static org.jboss.as.threads.CommonAttributes.QUEUE_LENGTH;
import static org.jboss.as.threads.CommonAttributes.SCHEDULED_THREAD_POOL;
import static org.jboss.as.threads.CommonAttributes.THREAD_FACTORY;
import static org.jboss.as.threads.CommonAttributes.THREAD_NAME_PATTERN;
import static org.jboss.as.threads.CommonAttributes.TIME;
import static org.jboss.as.threads.CommonAttributes.UNBOUNDED_QUEUE_THREAD_POOL;
import static org.jboss.as.threads.CommonAttributes.UNIT;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

public final class NewThreadsParser implements XMLStreamConstants, XMLElementReader<List<ModelNode>>,
        XMLElementWriter<SubsystemMarshallingContext> {

    static final NewThreadsParser INSTANCE = new NewThreadsParser();

    private static String SUBSYSTEM_NAME = "threads";

    @Override
    public void readElement(final XMLExtendedStreamReader reader, final List<ModelNode> list) throws XMLStreamException {

        final ModelNode address = new ModelNode();
        address.add(SUBSYSTEM, SUBSYSTEM_NAME);
        address.protect();

        final ModelNode subsystem = new ModelNode();
        subsystem.get(OP).set(ADD);
        subsystem.get(OP_ADDR).set(address);
        list.add(subsystem);

        readXmlElements(reader, list, address);
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case THREADS_1_0: {
                    readSingleElement(reader, list, address);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    public String readXmlElements(final XMLExtendedStreamReader reader, final List<ModelNode> list, final ModelNode address)
            throws XMLStreamException {
        // Handle elements
        String name = null;

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            name = readSingleElement(reader, list, address);
        }

        if (name == null) {
            throw missingRequired(reader, Collections.singleton(Attribute.NAME));
        } else {
            return name;
        }
    }

    private String readSingleElement(final XMLExtendedStreamReader reader, final List<ModelNode> list, final ModelNode address)
            throws XMLStreamException {
        String name = null;
        final Element element = Element.forName(reader.getLocalName());
        switch (element) {
            case BOUNDED_QUEUE_THREAD_POOL: {
                name = parseBoundedQueueThreadPool(reader, address, list);
                break;
            }
            case THREAD_FACTORY: {
                // Add connector updates
                name = parseThreadFactory(reader, address, list);
                break;
            }
            case QUEUELESS_THREAD_POOL: {
                name = parseQueuelessThreadPool(reader, address, list);
                break;
            }
            case SCHEDULED_THREAD_POOL: {
                name = parseScheduledThreadPool(reader, address, list);
                break;
            }
            case UNBOUNDED_QUEUE_THREAD_POOL: {
                name = parseUnboundedQueueThreadPool(reader, address, list);
                break;
            }
            default: {
                throw unexpectedElement(reader);
            }
        }
        return name;
    }

    private String parseThreadFactory(final XMLExtendedStreamReader reader, final ModelNode parentAddress,
            final List<ModelNode> list) throws XMLStreamException {
        final ModelNode op = new ModelNode();
        list.add(op);

        op.get(OP).set(ADD);

        String name = null;
        int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case NAME: {
                    name = value;
                    break;
                }
                case GROUP_NAME: {
                    op.get(GROUP_NAME).set(value);
                    break;
                }
                case THREAD_NAME_PATTERN: {
                    op.get(THREAD_NAME_PATTERN).set(value);
                    break;
                }
                case PRIORITY: {
                    try {
                        int priority = Integer.valueOf(value);
                        op.get(PRIORITY).set(priority);
                    } catch (NumberFormatException e) {
                        invalidAttributeValue(reader, i);
                    }
                }
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }

        if (name == null) {
            throw missingRequired(reader, Collections.singleton(Attribute.NAME));
        }

        final ModelNode address = parentAddress.clone();
        address.add(THREAD_FACTORY, name);
        address.protect();
        op.get(OP_ADDR).set(address);

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Element.forName(reader.getLocalName())) {
                case PROPERTIES: {
                    ModelNode props = parseProperties(reader);
                    if (props.isDefined()) {
                        op.get(PROPERTIES).set(props);
                    }
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
            break;
        }
        return name;
    }

    String parseBoundedQueueThreadPool(final XMLExtendedStreamReader reader, final ModelNode parentAddress,
            final List<ModelNode> list) throws XMLStreamException {
        final ModelNode op = new ModelNode();
        list.add(op);
        op.get(OP).set(ADD);

        String name = null;
        int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case NAME: {
                    name = value;
                    break;
                }
                case BLOCKING: {
                    op.get(BLOCKING).set(Boolean.valueOf(value));
                    break;
                }
                case ALLOW_CORE_TIMEOUT: {
                    op.get(ALLOW_CORE_TIMEOUT).set(Boolean.valueOf(value));
                    break;
                }
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }

        if (name == null) {
            throw missingRequired(reader, Collections.singleton(Attribute.NAME));
        }

        final ModelNode address = parentAddress.clone();
        address.add(BOUNDED_QUEUE_THREAD_POOL, name);
        address.protect();
        op.get(OP_ADDR).set(address);

        boolean foundQueueLength = false;
        boolean foundMaxThreads = false;
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Element.forName(reader.getLocalName())) {
                case CORE_THREADS: {
                    op.get(CORE_THREADS).set(parseScaledCount(reader));
                    break;
                }
                case HANDOFF_EXECUTOR: {
                    op.get(HANDOFF_EXECUTOR).set(parseRef(reader));
                    break;
                }
                case MAX_THREADS: {
                    op.get(MAX_THREADS).set(parseScaledCount(reader));
                    foundMaxThreads = true;
                    break;
                }
                case KEEPALIVE_TIME: {
                    op.get(KEEPALIVE_TIME).set(parseTimeSpec(reader));
                    break;
                }
                case THREAD_FACTORY: {
                    op.get(CommonAttributes.THREAD_FACTORY).set(parseRef(reader));
                    break;
                }
                case PROPERTIES: {
                    ModelNode props = parseProperties(reader);
                    if (props.isDefined()) {
                        op.get(PROPERTIES).set(props);
                    }
                    break;
                }
                case QUEUE_LENGTH: {
                    op.get(QUEUE_LENGTH).set(parseScaledCount(reader));
                    foundQueueLength = true;
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        if (!foundMaxThreads || !foundQueueLength) {
            Set<Element> missing = new HashSet<Element>();
            if (!foundMaxThreads) {
                missing.add(Element.MAX_THREADS);
            }
            if (!foundQueueLength) {
                missing.add(Element.QUEUE_LENGTH);
            }
            throw missingRequiredElement(reader, missing);
        }
        return name;
    }

    String parseUnboundedQueueThreadPool(final XMLExtendedStreamReader reader, final ModelNode parentAddress,
            final List<ModelNode> list) throws XMLStreamException {
        final ModelNode op = new ModelNode();
        list.add(op);
        op.get(OP).set(ADD);

        String name = null;
        int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case NAME: {
                    name = value;
                    break;
                }
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }

        if (name == null) {
            throw missingRequired(reader, Collections.singleton(Attribute.NAME));
        }

        // FIXME Make relative and use this scheme to add the addresses
        // address.add("profile", "test).add("subsystem", "threads")
        final ModelNode address = parentAddress.clone();
        address.add(UNBOUNDED_QUEUE_THREAD_POOL, name);
        address.protect();
        op.get(OP_ADDR).set(address);

        boolean foundMaxThreads = false;
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Element.forName(reader.getLocalName())) {
                case MAX_THREADS: {
                    op.get(MAX_THREADS).set(parseScaledCount(reader));
                    foundMaxThreads = true;
                    break;
                }
                case KEEPALIVE_TIME: {
                    op.get(KEEPALIVE_TIME).set(parseTimeSpec(reader));
                    break;
                }
                case THREAD_FACTORY: {
                    op.get(CommonAttributes.THREAD_FACTORY).set(parseRef(reader));
                    break;
                }
                case PROPERTIES: {
                    ModelNode props = parseProperties(reader);
                    if (props.isDefined()) {
                        op.get(PROPERTIES).set(props);
                    }
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        if (!foundMaxThreads) {
            throw missingRequiredElement(reader, Collections.singleton(Element.MAX_THREADS));
        }
        return name;
    }

    String parseScheduledThreadPool(final XMLExtendedStreamReader reader, final ModelNode parentAddress,
            final List<ModelNode> list) throws XMLStreamException {
        final ModelNode op = new ModelNode();
        list.add(op);
        op.get(OP).set(ADD);

        String name = null;
        int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case NAME: {
                    name = value;
                    break;
                }
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }

        if (name == null) {
            throw missingRequired(reader, Collections.singleton(Attribute.NAME));
        }

        // FIXME Make relative and use this scheme to add the addresses
        // address.add("profile", "test).add("subsystem", "threads")
        final ModelNode address = parentAddress.clone();
        address.add(SCHEDULED_THREAD_POOL, name);
        address.protect();
        op.get(OP_ADDR).set(address);

        boolean foundMaxThreads = false;
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Element.forName(reader.getLocalName())) {
                case MAX_THREADS: {
                    op.get(MAX_THREADS).set(parseScaledCount(reader));
                    foundMaxThreads = true;
                    break;
                }
                case KEEPALIVE_TIME: {
                    op.get(KEEPALIVE_TIME).set(parseTimeSpec(reader));
                    break;
                }
                case THREAD_FACTORY: {
                    op.get(CommonAttributes.THREAD_FACTORY).set(parseRef(reader));
                    break;
                }
                case PROPERTIES: {
                    ModelNode props = parseProperties(reader);
                    if (props.isDefined()) {
                        op.get(PROPERTIES).set(props);
                    }
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        if (!foundMaxThreads) {
            throw missingRequiredElement(reader, Collections.singleton(Element.MAX_THREADS));
        }
        return name;
    }

    String parseQueuelessThreadPool(final XMLExtendedStreamReader reader, final ModelNode parentAddress,
            final List<ModelNode> list) throws XMLStreamException {
        final ModelNode op = new ModelNode();
        list.add(op);
        op.get(OP).set(ADD);

        String name = null;
        int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case NAME: {
                    name = value;
                    break;
                }
                case BLOCKING: {
                    op.get(BLOCKING).set(Boolean.valueOf(value));
                    break;
                }
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }

        if (name == null) {
            throw missingRequired(reader, Collections.singleton(Attribute.NAME));
        }

        // FIXME Make relative and use this scheme to add the addresses
        // address.add("profile", "test).add("subsystem", "threads")
        final ModelNode address = parentAddress.clone();
        address.add(QUEUELESS_THREAD_POOL, name);
        address.protect();
        op.get(OP_ADDR).set(address);

        boolean foundMaxThreads = false;
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Element.forName(reader.getLocalName())) {
                case HANDOFF_EXECUTOR: {
                    op.get(HANDOFF_EXECUTOR).set(parseRef(reader));
                    break;
                }
                case MAX_THREADS: {
                    op.get(MAX_THREADS).set(parseScaledCount(reader));
                    foundMaxThreads = true;
                    break;
                }
                case KEEPALIVE_TIME: {
                    op.get(KEEPALIVE_TIME).set(parseTimeSpec(reader));
                    break;
                }
                case THREAD_FACTORY: {
                    op.get(CommonAttributes.THREAD_FACTORY).set(parseRef(reader));
                    break;
                }
                case PROPERTIES: {
                    ModelNode props = parseProperties(reader);
                    if (props.isDefined()) {
                        op.get(PROPERTIES).set(props);
                    }
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        if (!foundMaxThreads) {
            throw missingRequiredElement(reader, Collections.singleton(Element.MAX_THREADS));
        }
        return name;
    }

    private ModelNode parseScaledCount(final XMLExtendedStreamReader reader) throws XMLStreamException {
        final int attrCount = reader.getAttributeCount();
        BigDecimal count = null;
        BigDecimal perCpu = null;
        for (int i = 0; i < attrCount; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case COUNT: {
                    try {
                        count = new BigDecimal(value);
                    } catch (NumberFormatException e) {
                        throw invalidAttributeValue(reader, i);
                    }
                    break;
                }
                case PER_CPU: {
                    try {
                        perCpu = new BigDecimal(value);
                    } catch (NumberFormatException e) {
                        throw invalidAttributeValue(reader, i);
                    }
                    break;
                }
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }

        if (count == null || perCpu == null) {
            Set<Attribute> missing = new HashSet<Attribute>();
            if (count == null) {
                missing.add(Attribute.COUNT);
            }
            if (perCpu == null) {
                missing.add(Attribute.PER_CPU);
            }
            throw missingRequired(reader, missing);
        }

        if (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            throw unexpectedElement(reader);
        }

        ModelNode node = new ModelNode();
        node.get(COUNT).set(count);
        node.get(PER_CPU).set(perCpu);

        return node;
    }

    private ModelNode parseTimeSpec(final XMLExtendedStreamReader reader) throws XMLStreamException {
        final int attrCount = reader.getAttributeCount();
        TimeUnit unit = null;
        Long duration = null;
        for (int i = 0; i < attrCount; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case TIME: {
                    duration = reader.getLongAttributeValue(i);
                    break;
                }
                case UNIT: {
                    unit = Enum.valueOf(TimeUnit.class, value.toUpperCase());
                    break;
                }
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }

        if (duration == null || unit == null) {
            Set<Attribute> missing = new HashSet<Attribute>();
            if (duration == null) {
                missing.add(Attribute.TIME);
            }
            if (unit == null) {
                missing.add(Attribute.UNIT);
            }
        }

        if (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            throw unexpectedElement(reader);
        }

        ModelNode node = new ModelNode();
        node.get(TIME).set(duration);
        node.get(UNIT).set(unit.toString());
        return node;
    }

    private String parseRef(XMLExtendedStreamReader reader) throws XMLStreamException {
        final int attrCount = reader.getAttributeCount();
        String refName = null;
        for (int i = 0; i < attrCount; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case NAME: {
                    refName = value;
                    break;
                }
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }

        if (refName == null) {
            throw missingRequired(reader, Collections.singleton(NAME));
        }
        if (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            throw unexpectedElement(reader);
        }

        return refName;
    }

    private ModelNode parseProperties(final XMLExtendedStreamReader reader) throws XMLStreamException {
        ModelNode node = new ModelNode();
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Element.forName(reader.getLocalName())) {
                case PROPERTY: {
                    final int attrCount = reader.getAttributeCount();
                    String propName = null;
                    String propValue = null;
                    for (int i = 0; i < attrCount; i++) {
                        requireNoNamespaceAttribute(reader, i);
                        final String value = reader.getAttributeValue(i);
                        final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                        switch (attribute) {
                            case NAME: {
                                propName = value;
                                break;
                            }
                            case VALUE: {
                                propValue = value;
                            }
                                break;
                            default:
                                throw unexpectedAttribute(reader, i);
                        }
                    }
                    if (propName == null || propValue == null) {
                        Set<Attribute> missing = new HashSet<Attribute>();
                        if (propName == null) {
                            missing.add(Attribute.NAME);
                        }
                        if (propValue == null) {
                            missing.add(Attribute.VALUE);
                        }
                        throw missingRequired(reader, missing);
                    }
                    node.add(propName, propValue);

                    if (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                        throw unexpectedElement(reader);
                    }
                }
            }
        }
        return node;
    }

    /** {@inheritDoc} */
    @Override
    public void writeContent(final XMLExtendedStreamWriter writer, final SubsystemMarshallingContext context)
            throws XMLStreamException {

        context.startSubsystemElement(Namespace.CURRENT.getUriString(), false);

        ModelNode node = context.getModelNode();

        writeThreadsElement(writer, node);

        writer.writeEndElement();
    }

    public void writeThreadsElement(final XMLExtendedStreamWriter writer, ModelNode node) throws XMLStreamException {
        if (node.hasDefined(THREAD_FACTORY)) {
            for (String name : node.get(THREAD_FACTORY).keys()) {
                final ModelNode child = node.get(THREAD_FACTORY, name);
                if (child.isDefined()) {
                    writeThreadFactory(writer, child);
                }
            }
        }
        if (node.hasDefined(BOUNDED_QUEUE_THREAD_POOL)) {
            for (String name : node.get(BOUNDED_QUEUE_THREAD_POOL).keys()) {
                final ModelNode child = node.get(BOUNDED_QUEUE_THREAD_POOL, name);
                if (child.isDefined()) {
                    writeBoundedQueueThreadPool(writer, child);
                }
            }
        }
        if (node.hasDefined(QUEUELESS_THREAD_POOL)) {
            for (String name : node.get(QUEUELESS_THREAD_POOL).keys()) {
                final ModelNode child = node.get(QUEUELESS_THREAD_POOL, name);
                if (child.isDefined()) {
                    writeQueuelessThreadPool(writer, child);
                }
            }
        }
        if (node.hasDefined(SCHEDULED_THREAD_POOL)) {
            for (String name : node.get(SCHEDULED_THREAD_POOL).keys()) {
                final ModelNode child = node.get(SCHEDULED_THREAD_POOL, name);
                if (child.isDefined()) {
                    writeScheduledQueueThreadPool(writer, child);
                }
            }
        }
        if (node.hasDefined(UNBOUNDED_QUEUE_THREAD_POOL)) {
            for (String name : node.get(UNBOUNDED_QUEUE_THREAD_POOL).keys()) {
                final ModelNode child = node.get(UNBOUNDED_QUEUE_THREAD_POOL, name);
                if (child.isDefined()) {
                    writeUnboundedQueueThreadPool(writer, child);
                }
            }
        }
    }

    private void writeThreadFactory(final XMLExtendedStreamWriter writer, final ModelNode node) throws XMLStreamException {
        writer.writeStartElement(Element.THREAD_FACTORY.getLocalName());
        if (node.hasDefined(NAME)) {
            writeAttribute(writer, Attribute.NAME, node.get(NAME));
        }
        if (node.hasDefined(GROUP_NAME)) {
            writeAttribute(writer, Attribute.GROUP_NAME, node.get(GROUP_NAME));
        }
        if (node.hasDefined(THREAD_NAME_PATTERN)) {
            writeAttribute(writer, Attribute.THREAD_NAME_PATTERN, node.get(THREAD_NAME_PATTERN));
        }
        if (node.hasDefined(PRIORITY)) {
            writeAttribute(writer, Attribute.PRIORITY, node.get(PRIORITY));
        }
        if (node.hasDefined(PROPERTIES)) {
            writeProperties(writer, node.get(PROPERTIES));
        }

        writer.writeEndElement();
    }

    private void writeBoundedQueueThreadPool(final XMLExtendedStreamWriter writer, final ModelNode node)
            throws XMLStreamException {
        writer.writeStartElement(Element.BOUNDED_QUEUE_THREAD_POOL.getLocalName());

        if (node.hasDefined(NAME)) {
            writeAttribute(writer, Attribute.NAME, node.get(NAME));
        }
        if (node.hasDefined(BLOCKING)) {
            writeAttribute(writer, Attribute.BLOCKING, node.get(BLOCKING));
        }
        if (node.hasDefined(ALLOW_CORE_TIMEOUT)) {
            writeAttribute(writer, Attribute.ALLOW_CORE_TIMEOUT, node.get(ALLOW_CORE_TIMEOUT));
        }
        writeRef(writer, node, Element.HANDOFF_EXECUTOR, HANDOFF_EXECUTOR);
        writeRef(writer, node, Element.THREAD_FACTORY, THREAD_FACTORY);
        writeThreads(writer, node, Element.CORE_THREADS);
        writeThreads(writer, node, Element.QUEUE_LENGTH);
        writeThreads(writer, node, Element.MAX_THREADS);
        writeTime(writer, node, Element.KEEPALIVE_TIME);

        if (node.hasDefined(PROPERTIES)) {
            writeProperties(writer, node.get(PROPERTIES));
        }

        writer.writeEndElement();
    }

    private void writeQueuelessThreadPool(final XMLExtendedStreamWriter writer, final ModelNode node) throws XMLStreamException {
        writer.writeStartElement(Element.QUEUELESS_THREAD_POOL.getLocalName());

        if (node.hasDefined(NAME)) {
            writeAttribute(writer, Attribute.NAME, node.get(NAME));
        }
        if (node.hasDefined(BLOCKING)) {
            writeAttribute(writer, Attribute.BLOCKING, node.get(BLOCKING));
        }
        writeRef(writer, node, Element.HANDOFF_EXECUTOR, HANDOFF_EXECUTOR);
        writeRef(writer, node, Element.THREAD_FACTORY, THREAD_FACTORY);
        writeThreads(writer, node, Element.MAX_THREADS);
        writeTime(writer, node, Element.KEEPALIVE_TIME);

        if (node.hasDefined(PROPERTIES)) {
            writeProperties(writer, node.get(PROPERTIES));
        }

        writer.writeEndElement();
    }

    private void writeScheduledQueueThreadPool(final XMLExtendedStreamWriter writer, final ModelNode node)
            throws XMLStreamException {
        writer.writeStartElement(Element.SCHEDULED_THREAD_POOL.getLocalName());

        if (node.hasDefined(NAME)) {
            writeAttribute(writer, Attribute.NAME, node.get(NAME));
        }

        writeRef(writer, node, Element.THREAD_FACTORY, THREAD_FACTORY);
        writeThreads(writer, node, Element.MAX_THREADS);
        writeTime(writer, node, Element.KEEPALIVE_TIME);

        if (node.hasDefined(PROPERTIES)) {
            writeProperties(writer, node.get(PROPERTIES));
        }

        writer.writeEndElement();
    }

    private void writeUnboundedQueueThreadPool(final XMLExtendedStreamWriter writer, final ModelNode node)
            throws XMLStreamException {
        writer.writeStartElement(Element.UNBOUNDED_QUEUE_THREAD_POOL.getLocalName());

        if (node.hasDefined(NAME)) {
            writeAttribute(writer, Attribute.BLOCKING, node.get(NAME));
        }

        writeRef(writer, node, Element.THREAD_FACTORY, THREAD_FACTORY);
        writeThreads(writer, node, Element.MAX_THREADS);
        writeTime(writer, node, Element.KEEPALIVE_TIME);

        if (node.hasDefined(PROPERTIES)) {
            writeProperties(writer, node.get(PROPERTIES));
        }

        writer.writeEndElement();
    }

    private void writeRef(final XMLExtendedStreamWriter writer, final ModelNode node, Element element, String name)
            throws XMLStreamException {
        if (node.hasDefined(name)) {
            writer.writeStartElement(element.getLocalName());
            writeAttribute(writer, Attribute.NAME, node.get(name));
            writer.writeEndElement();
        }
    }

    private void writeThreads(final XMLExtendedStreamWriter writer, final ModelNode node, Element element)
            throws XMLStreamException {
        if (node.hasDefined(element.getLocalName())) {
            writer.writeStartElement(element.getLocalName());
            ModelNode threads = node.get(element.getLocalName());
            if (threads.hasDefined(COUNT)) {
                writeAttribute(writer, Attribute.COUNT, threads.get(COUNT));
            }
            if (threads.hasDefined(PER_CPU)) {
                writeAttribute(writer, Attribute.PER_CPU, threads.get(PER_CPU));
            }
            writer.writeEndElement();
        }
    }

    private void writeTime(final XMLExtendedStreamWriter writer, final ModelNode node, Element element)
            throws XMLStreamException {
        if (node.hasDefined(element.getLocalName())) {
            writer.writeStartElement(element.getLocalName());
            ModelNode keepalive = node.get(element.getLocalName());
            if (keepalive.hasDefined(TIME)) {
                writeAttribute(writer, Attribute.TIME, keepalive.get(TIME));
            }
            if (keepalive.hasDefined(UNIT)) {
                writeAttribute(writer, Attribute.UNIT, keepalive.get(UNIT));
            }
            writer.writeEndElement();
        }
    }

    private void writeProperties(final XMLExtendedStreamWriter writer, final ModelNode node) throws XMLStreamException {
        writer.writeStartElement(Element.PROPERTIES.getLocalName());

        if (node.getType() == ModelType.LIST) {
            for (ModelNode prop : node.asList()) {
                if (prop.getType() == ModelType.PROPERTY) {
                    writer.writeStartElement(Element.PROPERTY.getLocalName());

                    final Property property = prop.asProperty();
                    writer.writeAttribute(Attribute.NAME.getLocalName(), property.getName());
                    writeAttribute(writer, Attribute.VALUE, property.getValue());

                    writer.writeEndElement();
                }
            }
        }
        writer.writeEndElement();
    }

    private void writeAttribute(final XMLExtendedStreamWriter writer, final Attribute attr, final ModelNode value)
            throws XMLStreamException {
        writer.writeAttribute(attr.getLocalName(), value.asString());
    }
}
