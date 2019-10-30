/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.ejb3.iiop;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.security.AccessController;
import java.security.Principal;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.Map;

import javax.ejb.EJBMetaData;
import javax.ejb.HomeHandle;
import javax.management.MBeanException;
import javax.transaction.Status;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.component.ComponentView;
import org.jboss.as.ee.component.interceptors.InvocationType;
import org.jboss.as.ejb3.component.stateful.StatefulSessionComponent;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.iiop.csiv2.SASCurrent;
import org.jboss.as.naming.context.NamespaceContextSelector;
import org.jboss.ejb.client.SessionID;
import org.jboss.ejb.iiop.HandleImplIIOP;
import org.jboss.invocation.InterceptorContext;
import org.jboss.marshalling.InputStreamByteInput;
import org.jboss.marshalling.MarshallerFactory;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.Unmarshaller;
import org.jboss.security.SecurityContext;
import org.jboss.security.SecurityContextAssociation;
import org.jboss.security.SecurityContextFactory;
import org.omg.CORBA.BAD_OPERATION;
import org.omg.CORBA.InterfaceDef;
import org.omg.CORBA.ORB;
import org.omg.CORBA.ORBPackage.InvalidName;
import org.omg.CORBA.portable.InputStream;
import org.omg.CORBA.portable.InvokeHandler;
import org.omg.CORBA.portable.OutputStream;
import org.omg.CORBA.portable.ResponseHandler;
import org.omg.PortableServer.Current;
import org.omg.PortableServer.CurrentPackage.NoContext;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.Servant;
import org.wildfly.iiop.openjdk.rmi.RmiIdlUtil;
import org.wildfly.iiop.openjdk.rmi.marshal.strategy.SkeletonStrategy;
import org.wildfly.security.auth.client.AuthenticationConfiguration;
import org.wildfly.security.auth.client.AuthenticationContext;
import org.wildfly.security.auth.client.MatchRule;
import org.wildfly.security.auth.principal.NamePrincipal;
import org.wildfly.security.auth.server.RealmUnavailableException;
import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.auth.server.SecurityIdentity;
import org.wildfly.security.auth.server.ServerAuthenticationContext;
import org.wildfly.security.evidence.PasswordGuessEvidence;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * CORBA servant class for the <code>EJBObject</code>s of a given bean. An
 * instance of this class "implements" the bean's set of <code>EJBObject</code>
 * instances by forwarding to the bean container all IIOP invocations on any
 * of the bean's <code>EJBObject</code>s.
 *
 * @author <a href="mailto:reverbel@ime.usp.br">Francisco Reverbel</a>
 * @author Stuart Douglas
 */
public class EjbCorbaServant extends Servant implements InvokeHandler, LocalIIOPInvoker {

    /**
     * The injected component view
     */
    private final ComponentView componentView;

    /**
     * The ORB
     */
    private final ORB orb;

    /**
     * Thread-local <code>Current</code> object from which we get the target oid
     * in an incoming IIOP request.
     */
    private final Current poaCurrent;

    /**
     * Mapping from bean methods to <code>SkeletonStrategy</code> instances.
     */
    private final Map<String, SkeletonStrategy> methodInvokerMap;

    /**
     * CORBA repository ids of the RMI-IDL interfaces implemented by the bean
     * (<code>EJBObject</code> instance).
     */
    private final String[] repositoryIds;

    /**
     * CORBA reference to an IR object representing the bean's remote interface.
     */
    private final InterfaceDef interfaceDef;

    /**
     * The security domain for CORBA invocations
     */
    private final String legacySecurityDomain;

    /**
     * The Elytron security domain for CORBA invocations
     */
    private final SecurityDomain securityDomain;

    /**
     * If true this is the servant for an EJBHome object
     */
    private final boolean home;

    /**
     * <code>HomeHandle</code> for the <code>EJBHome</code>
     * implemented by this servant.
     */
    private volatile HomeHandle homeHandle = null;

    /**
     * The metadata for this
     */
    private volatile EJBMetaData ejbMetaData;

    /**
     * A reference to the SASCurrent, or null if the SAS interceptors are not
     * installed.
     */
    private final SASCurrent sasCurrent;

    /**
     * A reference to the InboundTransactionCurrent, or null if OTS interceptors
     * are not installed.
     */
    private final org.jboss.iiop.tm.InboundTransactionCurrent inboundTxCurrent;

    /**
     * The transaction manager
     */
    private final TransactionManager transactionManager;

    /**
     * Used for serializing EJB id's
     */
    private final MarshallerFactory factory;
    private final MarshallingConfiguration configuration;

    /**
     * The EJB's deployment class loader
     */
    private final ClassLoader classLoader;

    /**
     * Constructs an <code>EjbObjectCorbaServant></code>.
     */
    public EjbCorbaServant(final Current poaCurrent, final Map<String, SkeletonStrategy> methodInvokerMap, final String[] repositoryIds,
                           final InterfaceDef interfaceDef, final ORB orb, final ComponentView componentView, final MarshallerFactory factory,
                           final MarshallingConfiguration configuration, final TransactionManager transactionManager, final ClassLoader classLoader,
                           final boolean home, final String legacySecurityDomain, final SecurityDomain securityDomain) {
        this.poaCurrent = poaCurrent;
        this.methodInvokerMap = methodInvokerMap;
        this.repositoryIds = repositoryIds;
        this.interfaceDef = interfaceDef;
        this.orb = orb;
        this.componentView = componentView;
        this.factory = factory;
        this.configuration = configuration;
        this.transactionManager = transactionManager;
        this.classLoader = classLoader;
        this.home = home;
        this.legacySecurityDomain = legacySecurityDomain;
        this.securityDomain = securityDomain;

        SASCurrent sasCurrent;
        try {
            sasCurrent = (SASCurrent) this.orb.resolve_initial_references("SASCurrent");
        } catch (InvalidName invalidName) {
            sasCurrent = null;
        }
        this.sasCurrent = sasCurrent;
        org.jboss.iiop.tm.InboundTransactionCurrent inboundTxCurrent;
        try {
            inboundTxCurrent = (org.jboss.iiop.tm.InboundTransactionCurrent) this.orb.resolve_initial_references(org.jboss.iiop.tm.InboundTransactionCurrent.NAME);
        } catch (InvalidName invalidName) {
            inboundTxCurrent = null;
        }
        this.inboundTxCurrent = inboundTxCurrent;
    }


    /**
     * Returns an IR object describing the bean's remote interface.
     */
    public org.omg.CORBA.Object _get_interface_def() {
        if (interfaceDef != null)
            return interfaceDef;
        else
            return super._get_interface_def();
    }

    /**
     * Returns an array with the CORBA repository ids of the RMI-IDL interfaces
     * implemented by this servant's <code>EJBObject</code>s.
     */
    public String[] _all_interfaces(POA poa, byte[] objectId) {
        return repositoryIds.clone();
    }

    /**
     * Receives IIOP requests to this servant's <code>EJBObject</code>s
     * and forwards them to the bean container, through the JBoss
     * <code>MBean</code> server.
     */
    public OutputStream _invoke(final String opName, final InputStream in, final ResponseHandler handler) {
        EjbLogger.ROOT_LOGGER.tracef("EJBObject invocation: %s", opName);

        SkeletonStrategy op = methodInvokerMap.get(opName);
        if (op == null) {
            EjbLogger.ROOT_LOGGER.debugf("Unable to find opname '%s' valid operations:%s", opName, methodInvokerMap.keySet());
            throw new BAD_OPERATION(opName);
        }
        final NamespaceContextSelector selector = componentView.getComponent().getNamespaceContextSelector();
        final ClassLoader oldCl = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
        NamespaceContextSelector.pushCurrentSelector(selector);
        try {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(classLoader);
            org.omg.CORBA_2_3.portable.OutputStream out;
            try {
                Object retVal;

                if (!home && opName.equals("_get_handle")) {
                    retVal = new HandleImplIIOP(orb.object_to_string(_this_object()));
                } else if (home && opName.equals("_get_homeHandle")) {
                    retVal = homeHandle;
                } else if (home && opName.equals("_get_EJBMetaData")) {
                    retVal = ejbMetaData;
                } else {
                    Principal identityPrincipal = null;
                    Principal principal = null;
                    Object credential = null;

                    if (this.sasCurrent != null) {
                        final byte[] incomingIdentity = this.sasCurrent.get_incoming_principal_name();

                        //we have an identity token, which is a trust based mechanism
                        if (incomingIdentity != null && incomingIdentity.length > 0) {
                            String name = new String(incomingIdentity, StandardCharsets.UTF_8);
                            int domainIndex = name.indexOf('@');
                            if (domainIndex > 0)
                                name = name.substring(0, domainIndex);
                            identityPrincipal = new NamePrincipal(name);
                        }
                        final byte[] incomingUsername = this.sasCurrent.get_incoming_username();
                        if (incomingUsername != null && incomingUsername.length > 0) {
                            final byte[] incomingPassword = this.sasCurrent.get_incoming_password();
                            String name = new String(incomingUsername, StandardCharsets.UTF_8);
                            int domainIndex = name.indexOf('@');
                            if (domainIndex > 0) {
                                name = name.substring(0, domainIndex);
                            }
                            principal = new NamePrincipal(name);
                            credential = new String(incomingPassword, StandardCharsets.UTF_8).toCharArray();
                        }
                    }
                    final Object[] params = op.readParams((org.omg.CORBA_2_3.portable.InputStream) in);

                    if (!this.home && opName.equals("isIdentical") && params.length == 1) {
                        //handle isIdentical specially
                        Object val = params[0];
                        retVal = val instanceof org.omg.CORBA.Object && handleIsIdentical((org.omg.CORBA.Object) val);
                    } else {
                        if (this.securityDomain != null) {
                            // an elytron security domain is available: authenticate and authorize the client before invoking the component.
                            SecurityIdentity identity = this.securityDomain.getAnonymousSecurityIdentity();
                            AuthenticationConfiguration authenticationConfiguration = AuthenticationConfiguration.EMPTY;

                            if (identityPrincipal != null) {
                                // we have an identity token principal - check if the TLS identity, if available,
                                // has permission to run as the identity token principal.
                                // TODO use the TLS identity when that becomes available to us.

                                // no TLS identity found, check if an initial context token was also sent. If it was,
                                // authenticate the incoming username/password and check if the resulting identity has
                                // permission to run as the identity token principal.
                                if (principal != null) {
                                    char[] password = (char[]) credential;
                                    authenticationConfiguration = authenticationConfiguration.useName(principal.getName())
                                            .usePassword(password);
                                    SecurityIdentity authenticatedIdentity = this.authenticate(principal, password);
                                    identity = authenticatedIdentity.createRunAsIdentity(identityPrincipal.getName(), true);
                                } else {
                                    // no TLS nor initial context token found - check if the anonymous identity has
                                    // permission to run as the identity principal.
                                    identity = this.securityDomain.getAnonymousSecurityIdentity().createRunAsIdentity(identityPrincipal.getName(), true);
                                }
                            } else if (principal != null) {
                                char[] password = (char[]) credential;
                                // we have an initial context token containing a username/password pair.
                                authenticationConfiguration = authenticationConfiguration.useName(principal.getName())
                                        .usePassword(password);
                                identity = this.authenticate(principal, password);
                            }
                            final InterceptorContext interceptorContext = new InterceptorContext();
                            this.prepareInterceptorContext(op, params, interceptorContext);
                            try {
                                final AuthenticationContext context = AuthenticationContext.captureCurrent().with(MatchRule.ALL.matchProtocol("iiop"), authenticationConfiguration);
                                retVal = identity.runAs((PrivilegedExceptionAction<Object>) () -> context.run((PrivilegedExceptionAction<Object>) () -> this.componentView.invoke(interceptorContext)));
                            } catch (PrivilegedActionException e) {
                                throw e.getCause();
                            }
                        } else {
                            // legacy security behavior: setup the security context if a SASCurrent is available and invoke the component.
                            // One of the EJB security interceptors will authenticate and authorize the client.

                            SecurityContext legacyContext = null;
                            if (this.legacySecurityDomain != null && (identityPrincipal != null || principal != null)) {
                                // we don't have any real way to establish trust in identity based auth so we just use
                                // the SASCurrent as a credential, and a custom legacy login module can make a decision for us.
                                final Object finalCredential = identityPrincipal != null ? this.sasCurrent : credential;
                                final Principal finalPrincipal = identityPrincipal != null ? identityPrincipal : principal;
                                if (WildFlySecurityManager.isChecking()) {
                                    legacyContext = AccessController.doPrivileged((PrivilegedExceptionAction<SecurityContext>) () -> {
                                        SecurityContext sc = SecurityContextFactory.createSecurityContext(this.legacySecurityDomain);
                                        sc.getUtil().createSubjectInfo(finalPrincipal, finalCredential, null);
                                        return sc;
                                    });
                                } else {
                                    legacyContext = SecurityContextFactory.createSecurityContext(this.legacySecurityDomain);
                                    legacyContext.getUtil().createSubjectInfo(finalPrincipal, finalCredential, null);
                                }
                            }

                            if (legacyContext != null) {
                                setSecurityContextOnAssociation(legacyContext);
                            }
                            try {
                                final InterceptorContext interceptorContext = new InterceptorContext();
                                if (legacyContext != null) {
                                    interceptorContext.putPrivateData(SecurityContext.class, legacyContext);
                                }
                                prepareInterceptorContext(op, params, interceptorContext);
                                retVal = this.componentView.invoke(interceptorContext);
                            } finally {
                                if (legacyContext != null) {
                                    clearSecurityContextOnAssociation();
                                }
                            }
                        }
                    }
                }
                out = (org.omg.CORBA_2_3.portable.OutputStream)
                        handler.createReply();
                if (op.isNonVoid()) {
                    op.writeRetval(out, retVal);
                }
            } catch (Throwable e) {
                EjbLogger.ROOT_LOGGER.trace("Exception in EJBObject invocation", e);
                if (e instanceof MBeanException) {
                    e = ((MBeanException) e).getTargetException();
                }
                RmiIdlUtil.rethrowIfCorbaSystemException(e);
                out = (org.omg.CORBA_2_3.portable.OutputStream)
                        handler.createExceptionReply();
                op.writeException(out, e);
            }
            return out;
        } finally {
            NamespaceContextSelector.popCurrentSelector();
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(oldCl);
        }
    }

    private void prepareInterceptorContext(final SkeletonStrategy op, final Object[] params, final InterceptorContext interceptorContext) throws IOException, ClassNotFoundException {
        if (!home) {
            if (componentView.getComponent() instanceof StatefulSessionComponent) {
                final SessionID sessionID = (SessionID) unmarshalIdentifier();
                interceptorContext.putPrivateData(SessionID.class, sessionID);
            }
        }
        interceptorContext.setContextData(new HashMap<>());
        interceptorContext.setParameters(params);
        interceptorContext.setMethod(op.getMethod());
        interceptorContext.putPrivateData(ComponentView.class, componentView);
        interceptorContext.putPrivateData(Component.class, componentView.getComponent());
        interceptorContext.putPrivateData(InvocationType.class, InvocationType.REMOTE);
        interceptorContext.setTransaction(inboundTxCurrent == null ? null : inboundTxCurrent.getCurrentTransaction());
    }

    private boolean handleIsIdentical(final org.omg.CORBA.Object val) throws RemoteException {
        //TODO: is this correct?
        return orb.object_to_string(_this_object()).equals(orb.object_to_string(val));
    }

    private Object unmarshalIdentifier() throws IOException, ClassNotFoundException {
        final Object id;
        try {
            final byte[] idData = poaCurrent.get_object_id();
            final Unmarshaller unmarshaller = factory.createUnmarshaller(configuration);
            unmarshaller.start(new InputStreamByteInput(new ByteArrayInputStream(idData)));
            id = unmarshaller.readObject();
            unmarshaller.finish();
        } catch (NoContext noContext) {
            throw new RuntimeException(noContext);
        }
        return id;
    }

    // Implementation of the interface LocalIIOPInvoker ------------------------

    /**
     * Receives intra-VM invocations on this servant's <code>EJBObject</code>s
     * and forwards them to the bean container, through the JBoss
     * <code>MBean</code>
     * server.
     */
    public Object invoke(String opName,
                         Object[] arguments,
                         Transaction tx,
                         Principal identity,
                         Object credential)
            throws Exception {
        EjbLogger.ROOT_LOGGER.tracef("EJBObject local invocation: %s", opName);

        SkeletonStrategy op = methodInvokerMap.get(opName);
        if (op == null) {
            throw new BAD_OPERATION(opName);
        }
        if (tx != null) {
            transactionManager.resume(tx);
        }
        try {
            final InterceptorContext interceptorContext = new InterceptorContext();
            prepareInterceptorContext(op, arguments, interceptorContext);
            return componentView.invoke(interceptorContext);
        } finally {
            if (tx != null) {
                if (transactionManager.getStatus() != Status.STATUS_NO_TRANSACTION) {
                    transactionManager.suspend();
                }
            }
        }

    }

    public void setHomeHandle(final HomeHandle homeHandle) {
        this.homeHandle = homeHandle;
    }

    public void setEjbMetaData(final EJBMetaData ejbMetaData) {
        this.ejbMetaData = ejbMetaData;
    }


    private static void setSecurityContextOnAssociation(final SecurityContext sc) {
        AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
            SecurityContextAssociation.setSecurityContext(sc);
            return null;
        });
    }

    private static void clearSecurityContextOnAssociation() {
        AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
            SecurityContextAssociation.clearSecurityContext();
            return null;
        });
    }

    /**
     * Authenticate the user with the given credential against the configured Elytron security domain.
     *
     * @param principal the principal representing the user being authenticated.
     * @param credential the credential used as evidence to verify the user's identity.
     * @return the authenticated and authorized {@link SecurityIdentity}.
     * @throws Exception if an error occurs while authenticating the user.
     */
    private SecurityIdentity authenticate(final Principal principal, final char[] credential) throws Exception {
        final ServerAuthenticationContext context = this.securityDomain.createNewAuthenticationContext();
        final PasswordGuessEvidence evidence = new PasswordGuessEvidence(credential != null ? credential : null);
        try {
            context.setAuthenticationPrincipal(principal);
            if (context.verifyEvidence(evidence)) {
                if (context.authorize()) {
                    context.succeed();
                    return context.getAuthorizedIdentity();
                } else {
                    context.fail();
                    throw new SecurityException("Authorization failed");
                }
            } else {
                context.fail();
                throw new SecurityException("Authentication failed");
            }
        } catch (IllegalArgumentException | IllegalStateException | RealmUnavailableException e) {
            context.fail();
            throw e;
        } finally {
            evidence.destroy();
        }
    }
}
