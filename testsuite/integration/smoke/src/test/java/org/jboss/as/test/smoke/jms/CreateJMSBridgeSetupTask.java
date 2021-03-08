/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
