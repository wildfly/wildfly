/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.smoke.ejb3.dd;

import jakarta.ejb.EJB;
import jakarta.ejb.LocalBean;

/**
 * @author Jaikiran Pai
 */
@LocalBean
public class PartialDDSFSB implements Echo {

    @EJB (beanName = "DDBasedSLSB")
    private Echo otherEchoBean;

    @Override
    public String echo(String msg) {
        return this.otherEchoBean.echo(msg);
    }
}
