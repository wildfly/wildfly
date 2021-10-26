/*
 * Copyright 2016 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
class BatchSubsystemParser_3_0 extends BatchSubsystemParser_2_0 implements XMLStreamConstants, XMLElementReader<List<ModelNode>> {

    public BatchSubsystemParser_3_0() {
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
            } else {
                throw ParseUtils.unexpectedElement(reader);
            }
        }
    }
}