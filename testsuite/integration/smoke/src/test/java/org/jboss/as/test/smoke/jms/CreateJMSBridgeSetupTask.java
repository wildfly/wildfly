/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.smoke.jms;

import org.jboss.dmr.ModelNode;

/**
 * Setup task to create/remove a Jakarta Messaging bridge.
 *
 * @author Jeff Mesnil (c) 2012 Red Hat Inc.
 */
public class CreateJMSBridgeSetupTask extends AbstractCreateJMSBridgeSetupTask {

    @Override
    protected void configureBridge(ModelNode jmsBridgeAttributes) {
        jmsBridgeAttributes.get("quality-of-service").set("ONCE_AND_ONLY_ONCE");
        jmsBridgeAttributes.get("failure-retry-interval").set(500);
        jmsBridgeAttributes.get("max-retries").set(2);
        jmsBridgeAttributes.get("max-batch-size").set(1024);
        jmsBridgeAttributes.get("max-batch-time").set(100);
        jmsBridgeAttributes.get("add-messageID-in-header").set("true");
    }
}
