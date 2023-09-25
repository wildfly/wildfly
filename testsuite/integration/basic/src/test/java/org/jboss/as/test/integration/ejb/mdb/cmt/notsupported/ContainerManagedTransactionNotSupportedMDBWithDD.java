/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.mdb.cmt.notsupported;

import jakarta.jms.Message;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat inc.
 */
public class ContainerManagedTransactionNotSupportedMDBWithDD extends BaseMDB {

    @Override
    public void onMessage(Message message) {
        super.onMessage(message);
    }
}
