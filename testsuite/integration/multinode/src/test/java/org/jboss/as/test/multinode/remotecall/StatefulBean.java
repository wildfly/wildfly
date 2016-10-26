/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.test.multinode.remotecall;

import java.util.Properties;

import javax.ejb.Local;
import javax.ejb.LocalHome;
import javax.ejb.Remote;
import javax.ejb.RemoteHome;
import javax.ejb.Stateful;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.logging.Logger;

/**
 * @author William DeCoste
 */
@Stateful
@Local(StatefulLocal.class)
@LocalHome(StatefulLocalHome.class)
@Remote(StatefulRemote.class)
@RemoteHome(StatefulRemoteHome.class)
public class StatefulBean {
    private static final Logger log = Logger.getLogger(StatefulBean.class);

    private static int methodCount = 0;
    private static int homeMethodCount = 0;

    private InitialContext getInitialContext() throws NamingException {
        final Properties props = new Properties();
        // setup the ejb: namespace URL factory
        props.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
        return new InitialContext(props);
    }

    public void localCall() throws Exception {
        InitialContext jndiContext = getInitialContext();
        log.trace("Calling Local remotely...  " + jndiContext.getEnvironment());
        StatelessLocal stateless = (StatelessLocal) jndiContext.lookup("ejb:/" + RemoteLocalCallTestCase.ARCHIVE_NAME_SERVER
                + "//" + StatelessBean.class.getSimpleName() + "!" + StatelessLocal.class.getName());
        stateless.method();
    }

    public void localHomeCall() throws Exception {
        InitialContext jndiContext = getInitialContext();
        log.trace("Calling LocalHome remotely...  " + jndiContext.getEnvironment());
        StatelessLocalHome statelessHome = (StatelessLocalHome) jndiContext.lookup("ejb:/"
                + RemoteLocalCallTestCase.ARCHIVE_NAME_SERVER + "//" + StatelessBean.class.getSimpleName() + "!"
                + StatelessLocalHome.class.getName());
        StatelessLocal stateless = statelessHome.create();
        stateless.homeMethod();
    }

    public int remoteCall() throws Exception {
        ++methodCount;
        InitialContext jndiContext = getInitialContext();
        log.trace("Calling Remote... " + jndiContext.getEnvironment());
        StatefulRemote stateful = (StatefulRemote) jndiContext.lookup("ejb:/" + RemoteLocalCallTestCase.ARCHIVE_NAME_SERVER
                + "//" + StatefulBean.class.getSimpleName() + "!" + StatefulRemote.class.getName() + "?stateful");
        log.trace("We have got statefulbean...");
        return stateful.method();
    }

    public int remoteHomeCall() throws Exception {
        ++homeMethodCount;
        InitialContext jndiContext = new InitialContext();
        log.trace("Calling RemoteHome... " + jndiContext.getEnvironment());
        StatefulRemoteHome statefulHome = (StatefulRemoteHome) jndiContext.lookup("ejb:/"
                + RemoteLocalCallTestCase.ARCHIVE_NAME_SERVER + "//" + StatefulBean.class.getSimpleName() + "!"
                + StatefulRemoteHome.class.getName());
        StatefulRemote stateful = statefulHome.create();
        return stateful.homeMethod();
    }

    public int method() throws Exception {
        ++methodCount;
        log.trace("Method called " + methodCount);
        return methodCount;
    }

    public int homeMethod() throws Exception {
        log.trace("Before adding ++ is homeMethodCount: " + homeMethodCount);
        ++homeMethodCount;
        log.trace("HomeMethod called " + homeMethodCount);
        return homeMethodCount;
    }

    public void ejbCreate() throws java.rmi.RemoteException, javax.ejb.CreateException {
        log.debug("Creating method for home interface...");
    }
}
