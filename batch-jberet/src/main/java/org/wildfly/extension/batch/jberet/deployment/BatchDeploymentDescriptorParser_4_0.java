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
 * A parser for batch deployment descriptors ({@code batch-jberet:4.0}) in {@code jboss-all.xml}.
 */
public class BatchDeploymentDescriptorParser_4_0 extends BatchDeploymentDescriptorParser_3_0 {

    public static final String NAMESPACE = "urn:jboss:domain:batch-jberet:4.0";
    public static final QName ROOT_ELEMENT = new QName(NAMESPACE, "batch");

    @Override
    String parseJpaJobRepository(final XMLExtendedStreamReader reader) throws XMLStreamException {
        final String dataSourceName = readRequiredAttribute(reader, Attribute.DATA_SOURCE);
        ParseUtils.requireNoContent(reader);
        return dataSourceName;
    }

}
