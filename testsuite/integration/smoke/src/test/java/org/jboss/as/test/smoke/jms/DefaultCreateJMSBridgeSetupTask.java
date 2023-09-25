/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.smoke.jms;

import org.jboss.dmr.ModelNode;

/**
 * Setup task to create/remove a minimal Jakarta Messaging bridge.
 * @author Emmanuel Hugonnet (c) 2019 Red Hat, Inc.
 */
public class DefaultCreateJMSBridgeSetupTask extends AbstractCreateJMSBridgeSetupTask {

    @Override
    protected void configureBridge(ModelNode jmsBridgeAttributes) {
    }
}
