/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.ejb2.reference.annotation;

import jakarta.annotation.PreDestroy;
import jakarta.ejb.Init;
import jakarta.ejb.Local;
import jakarta.ejb.LocalHome;
import jakarta.ejb.Remote;
import jakarta.ejb.RemoteHome;
import jakarta.ejb.Stateful;
import javax.naming.InitialContext;

import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:bdecoste@jboss.com">William DeCoste</a>
 */
@Stateful(name = "StatefulSession30")
@Local(LocalStatefulSession30Business.class)
@Remote(StatefulSession30RemoteBusiness.class)
@RemoteHome(StatefulSession30Home.class)
@LocalHome(StatefulSession30LocalHome.class)
public class StatefulSession30Bean implements java.io.Serializable, StatefulSession30RemoteBusiness {
    private static final long serialVersionUID = -8986168637251530390L;

    private static final Logger log = Logger.getLogger(StatefulSession30Bean.class);

    private String value = null;

    public void setValue(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setLocalValue(String value) {
        this.value = value;
    }

    public String getLocalValue() {
        return value;
    }

    public String accessLocalStateless() {
        try {
            InitialContext jndiContext = new InitialContext();
            Session30LocalHome localHome = (Session30LocalHome) jndiContext.lookup("java:module/Session30!" + Session30LocalHome.class.getName());
            LocalSession30 localSession = localHome.create();
            return localSession.access();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String accessLocalHome() {
        try {
            InitialContext jndiContext = new InitialContext();
            StatefulSession30LocalHome home = (StatefulSession30LocalHome) jndiContext.lookup("java:module/HomedStatefulSession30!" + StatefulSession30LocalHome.class.getName());
            LocalStatefulSession30 session = home.create();
            session.setLocalValue("LocalHome");
            return session.getLocalValue();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @Init
    public void ejbCreate() {
        value = "default";
    }

    @Init
    public void ejbCreate(String value) {
        this.value = value;
    }

    @Init
    public void ejbCreate(String value, Integer suffix) {
        this.value = value + suffix;
    }

    @PreDestroy
    public void preDestroy() {
        log.trace("Invoking PreDestroy");
    }
}
