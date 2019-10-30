/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.test.integration.ejb.ejb2.reference.annotation;

import javax.annotation.PreDestroy;
import javax.ejb.Init;
import javax.ejb.Local;
import javax.ejb.LocalHome;
import javax.ejb.Remote;
import javax.ejb.RemoteHome;
import javax.ejb.Stateful;
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
