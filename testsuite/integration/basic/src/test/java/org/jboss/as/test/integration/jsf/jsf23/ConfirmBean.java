/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jsf.jsf23;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;

/**
 *
 * @author <a href="mailto:fjuma@redhat.com">Farah Juma</a>
 */
@Named
@RequestScoped
public class ConfirmBean {

    private String confirmMsg;

    public ConfirmBean(){
        confirmMsg = "Are you sure you want to register?";
    }

    public String getConfirmMsg() {
        return confirmMsg;
    }

    public void setConfirmMsg(String confirmMsg) {
        this.confirmMsg = confirmMsg;
    }
}
