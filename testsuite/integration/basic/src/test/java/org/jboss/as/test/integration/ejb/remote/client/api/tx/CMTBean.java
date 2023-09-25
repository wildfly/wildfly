/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.remote.client.api.tx;

import jakarta.ejb.Remote;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

/**
 * User: jpai
 */
@Stateless
@Remote(CMTRemote.class)
public class CMTBean implements CMTRemote {

    @TransactionAttribute(value = TransactionAttributeType.MANDATORY)
    @Override
    public void mandatoryTxOp() {
        // do nothing
    }
}
