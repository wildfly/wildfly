/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ee.injection.resource.mail;

import jakarta.annotation.Resource;
import jakarta.ejb.Remote;
import jakarta.ejb.Stateless;
import jakarta.mail.Session;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;


/**
 * @author <a href="mailto:bdecoste@jboss.com">William DeCoste</a>
 */
@Stateless
@Remote(StatelessMail.class)
public class StatelessMailBean
        implements StatelessMail {

    @Resource(name = "MyDefaultMail")
    private Session mailSession;

    @Resource(lookup = "java:jboss/mail/Default")
    private Session session;

    // injected via xml descriptor
    private Session dsSession;

    public void testMail() throws NamingException {
        Context initCtx = new InitialContext();
        Context myEnv = (Context) initCtx.lookup("java:comp/env");

        // JavaMail Session
        Object obj = myEnv.lookup("MyDefaultMail");
        if ((obj instanceof jakarta.mail.Session) == false) { throw new NamingException("DefaultMail is not a jakarta.mail.Session"); }
    }

    public void testMailInjection() {
        mailSession.getProperties();
        session.getProperties();
        dsSession.getProperties();
    }

}
