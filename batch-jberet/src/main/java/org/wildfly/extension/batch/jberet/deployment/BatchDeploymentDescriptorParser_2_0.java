/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.batch.jberet.deployment;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.wildfly.extension.batch.jberet.Attribute;

/**
 * A parser for batch deployment descriptors ({@code batch-jberet:2.0}) in {@code jboss-all.xml}.
 */
public class BatchDeploymentDescriptorParser_2_0 extends BatchDeploymentDescriptorParser_1_0 {

    public static final String NAMESPACE = "urn:jboss:domain:batch-jberet:2.0";
    public static final QName ROOT_ELEMENT = new QName(NAMESPACE, "batch");

    @Override
    String parseJdbcJobRepository(final XMLExtendedStreamReader reader) throws XMLStreamException {
        final String dataSourceName = readRequiredAttribute(reader, Attribute.DATA_SOURCE);
        ParseUtils.requireNoContent(reader);
        return dataSourceName;
    }
}
