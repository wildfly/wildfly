/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.mdb.cdi;

import jakarta.ejb.Remote;

/**
 * @author baranowb
 *
 */
@Remote
public interface MDBProxy {

    String REPLY_QUEUE_JNDI_NAME = "java:/mdb-cdi-test/replyQueue";
    String QUEUE_JNDI_NAME = "java:/mdb-cdi-test/mdb-cdi-test";

    void trigger() throws Exception;
}
