/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.batch.jberet.deployment;

import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.wildfly.extension.batch.jberet.Attribute;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

/**
 * A parser for batch deployment descriptors ({@code batch-jberet:3.0}) in {@code jboss-all.xml}.
 */
public class BatchDeploymentDescriptorParser_3_0 extends BatchDeploymentDescriptorParser_2_0 {

    public static final String NAMESPACE = "urn:jboss:domain:batch-jberet:3.0";
    public static final QName ROOT_ELEMENT = new QName(NAMESPACE, "batch");

    @Override
    Integer parseExecutionRecordsLimit(final XMLExtendedStreamReader reader) throws XMLStreamException {
        Integer executionRecordsLimit = null;
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            if (Attribute.EXECUTION_RECORDS_LIMIT.getLocalName().equals(reader.getAttributeLocalName(0))) {
                try {
                    executionRecordsLimit = Integer.valueOf(reader.getAttributeValue(0));
                } catch (NumberFormatException e) {
                    throw ParseUtils.invalidAttributeValue(reader, 0);
                }
            } else {
                throw ParseUtils.unexpectedAttribute(reader, 0);
            }
        }
        return executionRecordsLimit;
    }
}
