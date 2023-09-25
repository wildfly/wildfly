/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.remote.client.api.tx;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.io.Serializable;

/**
 * User: jpai
 */
@Entity
public class Batch implements Serializable {

    @Id
    private String batchName;

    private String stepNames;

    public String getBatchName() {
        return batchName;
    }

    public void setBatchName(String batchName) {
        this.batchName = batchName;
    }

    public String getStepNames() {
        return stepNames;
    }

    public void addStep(String stepName) {
        if (this.stepNames == null) {
            this.stepNames = stepName;
            return;
        }
        this.stepNames = this.stepNames + "," + stepName;
    }
}
