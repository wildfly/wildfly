/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.batch.jberet;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.wildfly.extension.batch.jberet.job.repository.CommonAttributes;
import org.wildfly.extension.batch.jberet.job.repository.InMemoryJobRepositoryDefinition;
import org.wildfly.extension.batch.jberet.job.repository.JdbcJobRepositoryDefinition;
import org.wildfly.extension.batch.jberet.job.repository.JpaJobRepositoryDefinition;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:pote@outlook.it">Francesco Potenziani</a>
 */
class BatchSubsystemParser_4_0 extends BatchSubsystemParser_3_0 implements XMLStreamConstants, XMLElementReader<List<ModelNode>> {

    public BatchSubsystemParser_4_0() {
        super();
    }

    protected void parseJobRepository(final XMLExtendedStreamReader reader, final PathAddress subsystemAddress, final List<ModelNode> ops) throws XMLStreamException {
        Map<Attribute, String> topLevelAttributes = AttributeParsers.readAttributes(reader,
                EnumSet.of(Attribute.NAME, Attribute.EXECUTION_RECORDS_LIMIT));
        String name = topLevelAttributes.get(Attribute.NAME);
        String executionRecordsLimit = topLevelAttributes.get(Attribute.EXECUTION_RECORDS_LIMIT);

        if (name == null) {
            throw ParseUtils.missingRequired(reader, Attribute.NAME.getLocalName());
        }

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final String localName = reader.getLocalName();
            final Element element = Element.forName(localName);
            if (element == Element.IN_MEMORY) {
                ModelNode op = Util.createAddOperation(subsystemAddress.append(InMemoryJobRepositoryDefinition.NAME, name));
                if (executionRecordsLimit != null) {
                    CommonAttributes.EXECUTION_RECORDS_LIMIT.parseAndSetParameter(executionRecordsLimit, op, reader);
                }
                ops.add(op);
                ParseUtils.requireNoContent(reader);
            } else if (element == Element.JDBC) {
                final Map<Attribute, String> attributes = AttributeParsers.readRequiredAttributes(reader, EnumSet.of(Attribute.DATA_SOURCE));
                final ModelNode op = Util.createAddOperation(subsystemAddress.append(JdbcJobRepositoryDefinition.NAME, name));
                JdbcJobRepositoryDefinition.DATA_SOURCE.parseAndSetParameter(attributes.get(Attribute.DATA_SOURCE), op, reader);
                if (executionRecordsLimit != null) {
                    CommonAttributes.EXECUTION_RECORDS_LIMIT.parseAndSetParameter(executionRecordsLimit, op, reader);
                }
                ops.add(op);
                ParseUtils.requireNoContent(reader);
            } else if (element == Element.JPA) {
                final Map<Attribute, String> attributes = AttributeParsers.readRequiredAttributes(reader, EnumSet.of(Attribute.DATA_SOURCE));
                final ModelNode op = Util.createAddOperation(subsystemAddress.append(JpaJobRepositoryDefinition.NAME, name));
                JpaJobRepositoryDefinition.DATA_SOURCE.parseAndSetParameter(attributes.get(Attribute.DATA_SOURCE), op, reader);
                if (executionRecordsLimit != null) {
                    CommonAttributes.EXECUTION_RECORDS_LIMIT.parseAndSetParameter(executionRecordsLimit, op, reader);
                }
                ops.add(op);
                ParseUtils.requireNoContent(reader);
            } else {
                throw ParseUtils.unexpectedElement(reader);
            }
        }
    }
}