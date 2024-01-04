/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.ejb2.reference.global;

import jakarta.ejb.Remote;
import jakarta.ejb.Stateless;
import javax.naming.InitialContext;

import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:bdecoste@jboss.com">William DeCoste</a>
 */
@Stateless(name = "GlobalSession30")
@Remote(Session30RemoteBusiness.class)
public class GlobalSession30Bean implements Session30RemoteBusiness {
    private static final Logger log = Logger.getLogger(GlobalSession30Bean.class);

    public String access() {
        return "Session30";
    }

    public String access21() {
        return null;
    }

    public String accessLocalStateful() {
        return "not supported";
    }

    public String accessLocalStateful(String value) {
        return "not supported";
    }

    public String accessLocalStateful(String value, Integer suffix) {
        return "not supported";
    }

    public String globalAccess21() {
        try {
            InitialContext jndiContext = new InitialContext();
            Session21Home home = (Session21Home) jndiContext.lookup("java:global/global-reference-ejb2/Session21!" + Session21Home.class.getName());
            Session21 session = home.create();
            return session.access();
        } catch (Exception e) {
            log.error("GlobalSession30Bean.globalAccess21()", e);
            return null;
        }
    }

}
