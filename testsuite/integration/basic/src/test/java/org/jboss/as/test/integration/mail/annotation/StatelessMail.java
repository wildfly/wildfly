/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.mail.annotation;

import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;
import jakarta.mail.Session;

import org.junit.Assert;


/**
 * @author Tomaz Cerar
 */
@Stateless
public class StatelessMail {

    @Resource(name = "java:app/mail/MySession")
    private Session mailSession;
    @Resource(lookup = "java:jboss/mail/Default")
    private Session session;

    public void testMailInjection() {
        Assert.assertNotNull(mailSession);
        Assert.assertNotNull(session);
    }

}
