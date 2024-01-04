/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.servlet;

import jakarta.ejb.Local;
import jakarta.ejb.Remote;
import jakarta.ejb.Stateful;

import org.jboss.ejb3.annotation.SecurityDomain;

/**
 * @author <a href="mailto:bdecoste@jboss.com">William DeCoste</a>
 */
@Stateful(name = "Stateful")
@Remote(StatefulRemote.class)
@Local(StatefulLocal.class)
@SecurityDomain("other")
public class StatefulBean implements StatefulRemote, StatefulLocal {
    public String access(TestObject o) {
        return "Session30";
    }
}
