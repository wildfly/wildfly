/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.stateful.passivation;

import java.io.Serializable;

import jakarta.ejb.EJB;
import jakarta.ejb.Remote;
import jakarta.ejb.Stateful;

import org.jboss.ejb3.annotation.Cache;

/**
 * @author Tomas Hofman (thofman@redhat.com)
 */
@Stateful
@Cache("distributable")
@Remote(TestPassivationRemote.class)
public class BeanWithSerializationIssue extends TestPassivationBean {

    private Object object;

    @EJB
    private NestledBean nestledBean;

    public BeanWithSerializationIssue() {
        object = new Serializable() {
            private static final long serialVersionUID = 1L;

            private void writeObject(java.io.ObjectOutputStream s) throws java.io.IOException {
                throw new RuntimeException("Test");
            }
        };
    }

}
