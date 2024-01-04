/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.ejb2.reference.annotation;

import jakarta.annotation.PreDestroy;
import jakarta.ejb.Init;
import jakarta.ejb.Local;
import jakarta.ejb.LocalHome;
import jakarta.ejb.RemoteHome;
import jakarta.ejb.Stateful;
import javax.naming.InitialContext;

import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:bdecoste@jboss.com">William DeCoste</a>
 */
@Stateful(name = "HomedStatefulSession30")
@RemoteHome(StatefulSession30Home.class)
@LocalHome(StatefulSession30LocalHome.class)
@Local(LocalStatefulSession30Business.class)
public class HomedStatefulSession30Bean implements java.io.Serializable {

    private static final long serialVersionUID = -1013103935052726415L;

    private static final Logger log = Logger.getLogger(HomedStatefulSession30Bean.class);

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
            Session30LocalHome localHome = (Session30LocalHome) jndiContext.lookup("java:module/StatefulSession30! " + StatefulSession30Home.class.getName());
            LocalSession30 localSession = localHome.create();
            return localSession.access();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String accessLocalHome() {
        return null;
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
