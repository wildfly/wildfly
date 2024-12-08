/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.controller.persistence.xml;

import java.util.Map;
import java.util.Set;

import org.jboss.as.clustering.controller.xml.XMLChoice;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.dmr.ModelNode;

/**
 * A readable/writable XML choice between one or more resources.
 * @author Paul Ferraro
 */
public interface ResourceXMLChoice extends XMLChoice<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>, ModelNode> {

    /**
     * Returns the set of resource paths for this choice.
     * @return the set of resource paths for this choice.
     */
    Set<PathElement> getPathElements();
}
