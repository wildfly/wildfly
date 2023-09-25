/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq;

import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;

/**
 * Write attribute handler for attributes that update a divert resource.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class DivertConfigurationWriteHandler extends ReloadRequiredWriteAttributeHandler {

    public static final DivertConfigurationWriteHandler INSTANCE = new DivertConfigurationWriteHandler();

    private DivertConfigurationWriteHandler() {
        super(DivertDefinition.ATTRIBUTES);
    }
}