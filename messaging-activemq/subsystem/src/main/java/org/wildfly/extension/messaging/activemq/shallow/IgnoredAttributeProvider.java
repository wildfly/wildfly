/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.messaging.activemq.shallow;

import java.util.Set;
import org.jboss.as.controller.OperationContext;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author Emmanuel Hugonnet (c) 2019 Red Hat, Inc.
 */
public interface IgnoredAttributeProvider {
    Set<String> getIgnoredAttributes(OperationContext context, ModelNode operation);
}
