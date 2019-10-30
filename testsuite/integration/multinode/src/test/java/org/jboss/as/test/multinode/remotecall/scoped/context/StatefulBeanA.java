/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.multinode.remotecall.scoped.context;

import org.jboss.ejb3.annotation.Cache;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.CreateException;
import javax.ejb.LocalBean;
import javax.ejb.PostActivate;
import javax.ejb.PrePassivate;
import javax.ejb.Remote;
import javax.ejb.Stateful;
import javax.naming.Context;
import javax.naming.InitialContext;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

/**
 * @author Jaikiran Pai
 */
@Stateful
@Remote(LocalServerStatefulRemote.class)
@LocalBean
@Cache("passivating") // reference to the passivating cache configuration which comes shipped in EJB3 subsystem
public class StatefulBeanA implements LocalServerStatefulRemote {

    @Resource(name = "other-server-remoting-port")
    private String otherServerRemotingPort;

    @Resource(name = "other-server-host-address")
    private String otherServerHostAddress;

    @Resource(name = "other-server-auth-user-name")
    private String otherServerAuthUserName;

    @Resource(name = "other-server-auth-password")
    private String otherServerAuthPassword;

    private StatefulRemoteOnOtherServer statefulBeanOnOtherServer;
    private StatelessRemoteOnOtherServer statelessRemoteOnOtherServer;
    private StatefulRemoteHomeForBeanOnOtherServer statefulRemoteHomeForBeanOnOtherServer;

    private transient CountDownLatch passivationNotificationLatch;
    private boolean postActivateInvoked;

    @PostConstruct
    void onConstruct() throws Exception {
        final Properties jndiProps = this.getInitialContextProperties();
        final Context context = new InitialContext(jndiProps);
        // lookup and set the SFSB from remote server
        this.statefulBeanOnOtherServer = (StatefulRemoteOnOtherServer) context.lookup("ejb:/deployment-on-other-server//StatefulBeanOnOtherServer!" + StatefulRemoteOnOtherServer.class.getName() + "?stateful");
        // lookup and set the SLSB from remote server
        this.statelessRemoteOnOtherServer = (StatelessRemoteOnOtherServer) context.lookup("ejb:/deployment-on-other-server//StatelessBeanOnOtherServer!" + StatelessRemoteOnOtherServer.class.getName());
        // EJB 2.x remote home view of bean on other server
        this.statefulRemoteHomeForBeanOnOtherServer = (StatefulRemoteHomeForBeanOnOtherServer) context.lookup("ejb:/deployment-on-other-server//StatefulBeanOnOtherServer!" + StatefulRemoteHomeForBeanOnOtherServer.class.getName());
    }

    @Override
    public int getCountByInvokingOnRemoteServerBean() {
        // invoke the SFSB which resides on a remote server and was looked up via JNDI
        // using the scoped EJB client context feature
        return this.statefulBeanOnOtherServer.getCount();
    }

    @Override
    public int incrementCountByInvokingOnRemoteServerBean() {
        // invoke the SFSB which resides on a remote server and was looked up via JNDI
        // using the scoped EJB client context feature
        return this.statefulBeanOnOtherServer.incrementCount();
    }

    @Override
    public String getEchoByInvokingOnRemoteServerBean(final String msg) {
        // invoke the SLSB which resides on a remote server and was looked up via JNDI
        // using the scoped EJB client context feature
        return this.statelessRemoteOnOtherServer.echo(msg);
    }

    public int getStatefulBeanCountUsingEJB2xHomeView() {
        final StatefulRemoteOnOtherServer bean;
        try {
            bean = this.statefulRemoteHomeForBeanOnOtherServer.create();
        } catch (CreateException e) {
            throw new RuntimeException(e);
        }
        return bean.getCount();
    }

    public int getStatefulBeanCountUsingEJB2xHomeViewDifferentWay() {
        final StatefulRemoteOnOtherServer bean;
        try {
            bean = this.statefulRemoteHomeForBeanOnOtherServer.createDifferentWay();
        } catch (CreateException e) {
            throw new RuntimeException(e);
        }
        return bean.getCount();
    }

    public int getStatefulBeanCountUsingEJB2xHomeViewYetAnotherWay(int initialCount) {
        final StatefulRemoteOnOtherServer bean;
        try {
            bean = this.statefulRemoteHomeForBeanOnOtherServer.createYetAnotherWay(initialCount);
        } catch (CreateException e) {
            throw new RuntimeException(e);
        }
        return bean.getCount();
    }

    @Override
    public void registerPassivationNotificationLatch(final CountDownLatch latch) {
        this.passivationNotificationLatch = latch;
    }

    @Override
    public boolean wasPostActivateInvoked() {
        return this.postActivateInvoked;
    }

    @Override
    public StatefulRemoteOnOtherServer getSFSBCreatedWithScopedEJBClientContext() {
        return this.statefulBeanOnOtherServer;
    }

    @Override
    public StatelessRemoteOnOtherServer getSLSBCreatedWithScopedEJBClientContext() {
        return this.statelessRemoteOnOtherServer;
    }

    @PrePassivate
    private void prePassivate() {
        this.postActivateInvoked = false;
        if (this.passivationNotificationLatch != null) {
            this.passivationNotificationLatch.countDown();
        }
    }

    @PostActivate
    private void postActivate() {
        this.postActivateInvoked = true;
    }

    private Properties getInitialContextProperties() {
        final Properties jndiProps = new Properties();
        // Property to enable scoped EJB client context which will be tied to the JNDI context
        jndiProps.put("org.jboss.ejb.client.scoped.context", true);
        // Property which will handle the ejb: namespace during JNDI lookup
        jndiProps.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");

        final String connectionName = "foo-bar-connection";
        jndiProps.put("remote.connectionprovider.create.options.org.xnio.Options.SSL_ENABLED", "false");
        // add a property which lists the connections that we are configuring. In
        // this example, we are just configuring a single connection named "foo-bar-connection"
        jndiProps.put("remote.connections", connectionName);
        // add a property which points to the host server of the "foo-bar-connection"
        jndiProps.put("remote.connection." + connectionName + ".host", this.otherServerHostAddress);
        // add a property which points to the port on which the server is listening for EJB invocations
        jndiProps.put("remote.connection." + connectionName + ".port", this.otherServerRemotingPort);
        // add the username and password properties which will be used to establish this connection
        jndiProps.put("remote.connection." + connectionName + ".username", this.otherServerAuthUserName);
        jndiProps.put("remote.connection." + connectionName + ".password", this.otherServerAuthPassword);

        return jndiProps;
    }
}
