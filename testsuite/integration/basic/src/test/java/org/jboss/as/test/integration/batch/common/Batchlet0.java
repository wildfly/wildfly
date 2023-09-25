/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.batch.common;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Named;

@Named
public class Batchlet0 extends BatchletNoNamed {
    @PostConstruct
    void ps() {
        addToJobExitStatus("Batchlet0.ps");
    }

    @PreDestroy
    void pd() {
        addToJobExitStatus("Batchlet0.pd");
    }
}
