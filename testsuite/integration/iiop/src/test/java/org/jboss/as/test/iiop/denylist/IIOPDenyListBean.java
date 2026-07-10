/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.iiop.denylist;

import jakarta.ejb.RemoteHome;
import jakarta.ejb.Stateless;

/**
 * EJB bean for testing IIOP deny-list filter.
 */
@RemoteHome(IIOPDenyListHome.class)
@Stateless
public class IIOPDenyListBean {

    public String acceptDangerousClass(DangerousClass dangerous) {
        return "Accepted dangerous class: " + dangerous.getPayload();
    }

    public String acceptSafeClass(String safe) {
        return "Accepted safe class: " + safe;
    }
}
