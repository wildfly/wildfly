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

package org.jboss.as.test.integration.ejb.security;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import com.sun.xml.internal.xsom.impl.Const;
import javassist.CtNewConstructor;
import org.jboss.as.test.integration.ejb.security.authorization.AnnOnlyCheckSFSBForInjection;
import org.jboss.as.test.integration.ejb.security.authorization.AnnOnlyCheckSLSBForInjection;
import org.jboss.as.test.integration.ejb.security.authorization.ParentAnnOnlyCheck;
import org.jboss.as.test.integration.ejb.security.authorization.SimpleAuthorizationRemote;
import org.jboss.as.test.shared.integration.ejb.security.Util;
import org.jboss.ejb.client.ConstantContextSelector;
import org.jboss.ejb.client.ContextSelector;
import org.jboss.ejb.client.EJBClientConfiguration;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.ejb.client.EJBReceiver;
import org.jboss.ejb.client.PropertiesBasedEJBClientConfiguration;
import org.jboss.ejb.client.remoting.ConfigBasedEJBClientContextSelector;
import org.jboss.ejb.client.remoting.IoFutureHelper;
import org.jboss.ejb.client.remoting.RemotingConnectionEJBReceiver;
import org.jboss.logging.Logger;
import org.jboss.remoting3.Connection;
import org.jboss.remoting3.Endpoint;
import org.jboss.remoting3.Remoting;
import org.jboss.remoting3.remote.RemoteConnectionProviderFactory;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.xnio.IoFuture;
import org.xnio.Option;
import org.xnio.OptionMap;
import org.xnio.Options;
import org.xnio.Sequence;

import javax.ejb.EJBAccessException;
import javax.naming.Context;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.RealmCallback;

/**
 * This is a common parent for test cases to check whether basic EJB authorization works from an EJB client to a remote EJB.
 *
 * @author <a href="mailto:jan.lanik@redhat.com">Jan Lanik</a>
 */
public abstract class AnnSBTest extends SecurityTest {

    public static Archive<JavaArchive> testAppDeployment(final Logger LOG, final String MODULE, final Class SB_TO_TEST) {
        // using JavaArchive doesn't work, because of a bug in Arquillian, it only deploys wars properly
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, MODULE + ".jar")
                .addClass(SB_TO_TEST)
                .addClass(SimpleAuthorizationRemote.class)
                .addClass(ParentAnnOnlyCheck.class)
                .addClass(AnnOnlyCheckSLSBForInjection.class)
                .addClass(AnnOnlyCheckSFSBForInjection.class)
                        //.addClass(Util.class)
                        //.addClass(SecurityTest.class)
                .addAsManifestResource("ejb3/security/EMPTY_MANIFEST.MF", "MANIFEST.MF");
        LOG.info(jar.toString(true));
        return jar;
    }


    /**
     * Test objective:
     * Check if default, @RolesAllowed, @PermitAll, @DenyAll and @RolesAllowed with multiple roles
     * works on method level without user logged in as described in EJB 3.1 spec.
     * The target session bean is given as parameter
     * Expected results:
     * Test has to finish without any exception or error.
     *
     * @throws Exception
     */
    public void testSingleMethodAnnotationsNoUserTemplate(final String MODULE, final Logger log, final Class SB_CLASS) throws Exception {
        final Context ctx = Util.createNamingContext();
        ContextSelector<EJBClientContext> old = setupEJBClientContextSelector(null, null);
        try {

            String myContext = Util.createRemoteEjbJndiContext(
                    "",
                    MODULE,
                    "",
                    SB_CLASS.getSimpleName(),
                    SimpleAuthorizationRemote.class.getName(),
                    isBeanClassStatefull(SB_CLASS));

            log.info("JNDI name=" + myContext);

            final SimpleAuthorizationRemote singleMethodsAnnOnlyBean = (SimpleAuthorizationRemote) ctx.lookup(myContext);

            String echoValue = singleMethodsAnnOnlyBean.defaultAccess("alohomora");
            Assert.assertEquals(echoValue, "alohomora");

            try {
                echoValue = singleMethodsAnnOnlyBean.roleBasedAccessOne("alohomora");
                Assert.fail("Method cannot be successfully called without logged in user");
            } catch (Exception e) {
                // expected
                Assert.assertTrue("Thrown exception must be EJBAccessException, but was " + e.getClass().getSimpleName(), e instanceof EJBAccessException);
            }

            try {
                echoValue = singleMethodsAnnOnlyBean.roleBasedAccessMore("alohomora");
                Assert.fail("Method cannot be successfully called without logged in user");
            } catch (Exception e) {
                // expected
                Assert.assertTrue("Thrown exception must be EJBAccessException, but was " + e.getClass().getSimpleName(), e instanceof EJBAccessException);
            }

            try {
                echoValue = singleMethodsAnnOnlyBean.permitAll("alohomora");
                Assert.assertEquals(echoValue, "alohomora");
            } catch (Exception e) {
                Assert.fail("@PermitAll annotation must allow all users and no users to call the method");
            }

            try {
                echoValue = singleMethodsAnnOnlyBean.denyAll("alohomora");
                Assert.fail("@DenyAll annotation must allow all users and no users to call the method");
            } catch (Exception e) {
                // expected
                Assert.assertTrue("Thrown exception must be EJBAccessException, but was " + e.getClass().getSimpleName(), e instanceof EJBAccessException);
            }

        } finally {
            safeClose((Closeable) EJBClientContext.setSelector(old));
        }
    }

    /**
     * Test objective:
     * Check if default, @RolesAllowed, @PermitAll, @DenyAll and @RolesAllowed with multiple roles
     * works on method level with user1 logged in as described in EJB 3.1 spec.
     * user1 has "Users,Role1" roles.
     * The target session bean is given as parameter.
     * Expected results:
     * Test has to finish without any exception or error.
     * <p/>
     *
     * @throws Exception
     */
    public void testSingleMethodAnnotationsUser1Template(final String MODULE, final Logger log, final Class SB_CLASS) throws Exception {
        final Context ctx = Util.createNamingContext();
        ContextSelector<EJBClientContext> old = setupEJBClientContextSelector("user1", "password1");
        try {

            String myContext = Util.createRemoteEjbJndiContext(
                    "",
                    MODULE,
                    "",
                    SB_CLASS.getSimpleName(),
                    SimpleAuthorizationRemote.class.getName(),
                    isBeanClassStatefull(SB_CLASS));
            log.info("JNDI name=" + myContext);

            final SimpleAuthorizationRemote singleMethodsAnnOnlyBean = (SimpleAuthorizationRemote)
                    ctx.lookup(myContext);

            try {
                String echoValue = singleMethodsAnnOnlyBean.defaultAccess("alohomora");
                Assert.assertEquals(echoValue, "alohomora");
            } catch (EJBAccessException e) {
                Assert.fail("EJBAccessException not expected");
            }


            try {
                String echoValue = singleMethodsAnnOnlyBean.roleBasedAccessOne("alohomora");
                Assert.assertEquals(echoValue, "alohomora");
            } catch (EJBAccessException e) {
                Assert.fail("EJBAccessException not expected");
            }

            try {
                String echoValue = singleMethodsAnnOnlyBean.roleBasedAccessMore("alohomora");
                Assert.fail("Method cannot be successfully called with logged in principal.");
            } catch (Exception e) {
                // expected
                Assert.assertTrue("Thrown exception must be EJBAccessException, but was different", e instanceof EJBAccessException);
            }

            try {
                String echoValue = singleMethodsAnnOnlyBean.permitAll("alohomora");
                Assert.assertEquals(echoValue, "alohomora");
            } catch (Exception e) {
                Assert.fail("@PermitAll annotation must allow all users and no users to call the method - principal.");
            }

            try {
                String echoValue = singleMethodsAnnOnlyBean.denyAll("alohomora");
                Assert.fail("@DenyAll annotation must allow all users and no users to call the method");
            } catch (Exception e) {
                // expected
                Assert.assertTrue("Thrown exception must be EJBAccessException, but was different", e instanceof EJBAccessException);
            }

        } finally {
            safeClose((Closeable) EJBClientContext.setSelector(old));
        }
    }

    /**
     * Test objective:
     * Check if default, @RolesAllowed, @PermitAll, @DenyAll and @RolesAllowed with multiple roles
     * works on method level with user1 logged in as described in EJB 3.1 spec.
     * user2 has "Users,Role2" roles.
     * The target session bean is given as parameter.
     * Expected results:
     * Test has to finish without any exception or error.
     * <p/>
     * TODO: remove @Ignore after the JIRA is fixed
     *
     * @throws Exception
     */
    public void testSingleMethodAnnotationsUser2Template(final String MODULE, final Logger log, final Class SB_CLASS) throws Exception {
        final Context ctx = Util.createNamingContext();
        ContextSelector<EJBClientContext> old = setupEJBClientContextSelector("user2", "password2");
        try {

            String myContext = Util.createRemoteEjbJndiContext(
                    "",
                    MODULE,
                    "",
                    SB_CLASS.getSimpleName(),
                    SimpleAuthorizationRemote.class.getName(),
                    isBeanClassStatefull(SB_CLASS));
            log.info("JNDI name=" + myContext);


            final SimpleAuthorizationRemote singleMethodsAnnOnlyBean = (SimpleAuthorizationRemote)
                    ctx.lookup(myContext);

            try {
                String echoValue = singleMethodsAnnOnlyBean.defaultAccess("alohomora");
                Assert.assertEquals(echoValue, "alohomora");
            } catch (EJBAccessException e) {
                Assert.fail("EJBAccessException not expected");
            }

            try {
                String echoValue = singleMethodsAnnOnlyBean.roleBasedAccessOne("alohomora");
                Assert.fail("Method cannot be successfully called with logged in user2");
            } catch (Exception e) {
                // expected
                Assert.assertTrue("Thrown exception must be EJBAccessException, but was different", e instanceof EJBAccessException);
            }


            try {
                String echoValue = singleMethodsAnnOnlyBean.roleBasedAccessMore("alohomora");
                Assert.assertEquals(echoValue, "alohomora");
            } catch (EJBAccessException e) {
                Assert.fail("EJBAccessException not expected");
            }

            try {
                String echoValue = singleMethodsAnnOnlyBean.permitAll("alohomora");
                Assert.assertEquals(echoValue, "alohomora");
            } catch (Exception e) {
                Assert.fail("@PermitAll annotation must allow all users and no users to call the method - principal.");
            }

            try {
                String echoValue = singleMethodsAnnOnlyBean.denyAll("alohomora");
                Assert.fail("@DenyAll annotation must allow all users and no users to call the method");
            } catch (Exception e) {
                // expected
                Assert.assertTrue("Thrown exception must be EJBAccessException, but was different", e instanceof EJBAccessException);
            }

        } finally {
            safeClose((Closeable) EJBClientContext.setSelector(old));
        }

    }

    protected ContextSelector<EJBClientContext> setupEJBClientContextSelector(String username, String password) throws IOException {
        // create the endpoint
        final Endpoint endpoint = Remoting.createEndpoint("remoting-test", OptionMap.create(Options.THREAD_DAEMON, true));
        endpoint.addConnectionProvider("remote", new RemoteConnectionProviderFactory(), OptionMap.create(Options.SSL_ENABLED, false));
        final URI connectionURI;
        try {
            connectionURI = new URI("remote://localhost:4447");
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
        OptionMap.Builder builder = OptionMap.builder().set(Options.SASL_POLICY_NOANONYMOUS, true);
        builder.set(Options.SASL_POLICY_NOPLAINTEXT, false);
        if (password != null) {
            builder.set(Options.SASL_DISALLOWED_MECHANISMS, Sequence.of("JBOSS-LOCAL-USER"));
        } else {
            builder.set(Options.SASL_MECHANISMS, Sequence.of("JBOSS-LOCAL-USER"));
        }

        final IoFuture<Connection> futureConnection = endpoint.connect(connectionURI, builder.getMap(), new AuthenticationCallbackHandler(username, password));
        // wait for the connection to be established
        final Connection connection = IoFutureHelper.get(futureConnection, 5000, TimeUnit.MILLISECONDS);
        // create a remoting EJB receiver for this connection
        final EJBReceiver receiver = new RemotingConnectionEJBReceiver(connection);
        // associate it with the client context
        EJBClientContext context = EJBClientContext.create();
        context.registerEJBReceiver(receiver);
        return EJBClientContext.setSelector(new ClosableContextSelector(context, endpoint, connection, receiver));
    }

    private class ClosableContextSelector implements ContextSelector<EJBClientContext>, Closeable {
        private EJBClientContext context;
        private Endpoint endpoint;
        private Connection connection;
        private EJBReceiver reciever;

        private ClosableContextSelector(EJBClientContext context, Endpoint endpoint, Connection connection, EJBReceiver receiver) {
            this.context = context;
            this.endpoint = endpoint;
            this.connection = connection;
            this.reciever = receiver;
        }

        @Override
        public EJBClientContext getCurrent() {
            return context;
        }

        @Override
        public void close() throws IOException {
            context.unregisterEJBReceiver(reciever);
            safeClose(connection);
            safeClose(endpoint);
            this.context = null;
        }
    }

    private void safeClose(Closeable c) {
        try {
            c.close();
        } catch (Throwable t) {
        }
    }

    private class AuthenticationCallbackHandler implements CallbackHandler {

        private final String username;
        private final String password;

        private AuthenticationCallbackHandler(final String username, final String password) {
            this.username = username == null ? "$local" : username;
            this.password = password;
        }

        public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {

            for (Callback current : callbacks) {
                if (current instanceof RealmCallback) {
                    RealmCallback rcb = (RealmCallback) current;
                    String defaultText = rcb.getDefaultText();
                    rcb.setText(defaultText); // For now just use the realm suggested.
                } else if (current instanceof NameCallback) {
                    NameCallback ncb = (NameCallback) current;
                    ncb.setName(username);
                } else if (current instanceof PasswordCallback) {
                    PasswordCallback pcb = (PasswordCallback) current;
                    if (password != null) {
                        pcb.setPassword(password.toCharArray());
                    }
                } else {
                    throw new UnsupportedCallbackException(current);
                }
            }
        }
    }

    protected static boolean isBeanClassStatefull(Class bean) {
        if (bean.getName().contains("toSLSB")) {
            return false;
        } else if (bean.getName().contains("toSFSB")) {
            return true;
        } else if (bean.getName().contains("SFSB")) {
            return true;
        } else if (bean.getName().contains("SLSB")) {
            return false;
        } else {
            throw new AssertionError("Session bean class has a wrong name!:  " + bean.getCanonicalName());
        }
    }

}

