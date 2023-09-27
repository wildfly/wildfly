/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.elytron.batch;

import jakarta.batch.api.Batchlet;
import jakarta.inject.Named;

import org.jboss.logging.Logger;
import org.wildfly.security.auth.server.SecurityDomain;

/**
 * @author Jan Martiska
 */
@Named
public class IdentityBatchlet implements Batchlet {

    private Logger logger = Logger.getLogger(IdentityBatchlet.class);

    @Override
    public String process() throws Exception {
        final String name = SecurityDomain.getCurrent().getCurrentSecurityIdentity().getPrincipal().getName();
        logger.info("Batchlet running as username: " + name);
        BatchSubsystemSecurityTestCase.identityWithinJob.complete(name);
        return "OK";
    }

    @Override
    public void stop() throws Exception {

    }
}
