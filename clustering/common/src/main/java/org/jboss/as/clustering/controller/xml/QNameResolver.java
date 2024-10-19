/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.controller.xml;

import java.util.Comparator;

import javax.xml.namespace.QName;

/**
 * Resolves a local name to a qualified name.
 * @author Paul Ferraro
 */
public interface QNameResolver {
    Comparator<QName> COMPARATOR = Comparator.comparing(QName::getNamespaceURI).thenComparing(Comparator.comparing(QName::getPrefix)).thenComparing(Comparator.comparing(QName::getLocalPart));

    /**
     * Resolves the specified local name to a qualified name.
     * @param localName an attribute/element local name
     * @return a qualified name
     */
    QName resolve(String localName);
}
