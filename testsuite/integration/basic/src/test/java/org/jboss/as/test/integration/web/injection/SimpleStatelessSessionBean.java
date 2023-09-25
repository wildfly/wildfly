/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.web.injection;

import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;

/**
 * A simple stateless session bean.
 *
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
@Stateless
@LocalBean
public class SimpleStatelessSessionBean {
    public String echo(String msg) {
        return "Echo " + msg;
    }
}
