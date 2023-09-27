/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.security.lifecycle;

import jakarta.ejb.AfterBegin;
import jakarta.ejb.Stateful;

import org.jboss.ejb3.annotation.SecurityDomain;

/**
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
@Stateful
@SecurityDomain("ejb3-tests")
public class StatefulBean extends SessionBean {

    @AfterBegin
    public void afterBegin() {
        performTests(AFTER_BEGIN);
    }

}
