/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.transaction.cmt.beforecompletion;

import jakarta.ejb.BeforeCompletion;
import jakarta.ejb.Stateful;

/**
 *
 * @author Stuart Douglas
 */
@Stateful
public class BeforeCompletionSFSB {

    @BeforeCompletion
    public void beforeCompletion() {
        throw new RuntimeException("failed @BeforeCompletion");
    }

    public void enlist() {

    }

}
