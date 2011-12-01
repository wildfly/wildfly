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

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import javax.ejb.EJBMetaData;
import javax.ejb.HomeHandle;
import javax.transaction.Status;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.component.ComponentView;
import org.jboss.as.jacorb.csiv2.idl.SASCurrent;
import org.jboss.as.jacorb.rmi.RmiIdlUtil;
import org.jboss.as.jacorb.rmi.marshal.strategy.SkeletonStrategy;
import org.jboss.as.naming.context.NamespaceContextSelector;
import org.jboss.invocation.InterceptorContext;
import org.jboss.logging.Logger;
import org.jboss.security.SecurityContext;
import org.jboss.security.SecurityContextFactory;
import org.jboss.security.SimplePrincipal;
import org.omg.CORBA.BAD_OPERATION;
import org.omg.CORBA.InterfaceDef;
import org.omg.CORBA.ORB;
import org.omg.CORBA.ORBPackage.InvalidName;
import org.omg.CORBA.portable.InputStream;
import org.omg.CORBA.portable.InvokeHandler;
import org.omg.CORBA.portable.OutputStream;
import org.omg.CORBA.portable.ResponseHandler;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.Servant;

/**
 * CORBA servant class for an <code>EJBHome</code>. An instance of this class
 * "implements" a single <code>EJBHome</code> by forwarding to the bean
 * container all IIOP invocations on the bean home. Such invocations are routed
 * through the JBoss <code>MBean</code> server, which delivers them to the
 * target container.
 *
 * @author <a href="mailto:reverbel@ime.usp.br">Francisco Reverbel</a>
 * @version $Revision: 81018 $
 */
public class EjbHomeCorbaServant extends Servant implements InvokeHandler, LocalIIOPInvoker {

    /**
     * This servant's logger.
     */
    private static final Logger logger = Logger.getLogger(EjbHomeCorbaServant.class);

    /**
     * The view this servant represents
     */
    private final ComponentView componentView;

    /**
     * Mapping from home methods to <code>SkeletonStrategy</code> instances.
     */
    private final Map<String, SkeletonStrategy> methodInvokerMap;

    /**
     * CORBA repository ids of the RMI-IDL interfaces implemented by the bean's
     * home (<code>EJBHome</code> instance).
     */
    private final String[] repositoryIds;

    /**
     * CORBA reference to an IR object representing the bean's home interface.
     */
    private final InterfaceDef interfaceDef;

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


    private final TransactionManager transactionManager;


    /**
     * A reference to the InboundTransactionCurrent, or null if OTS interceptors
     * are not installed.
     */
    private final org.jboss.iiop.tm.InboundTransactionCurrent inboundTxCurrent;

    /**
     * The deployment class loader
     */
    private final ClassLoader classLoader;


    /**
     * Constructs an <code>EjbHomeCorbaServant></code>.
     */
    public EjbHomeCorbaServant(final Map<String, SkeletonStrategy> methodInvokerMap, final String[] repositoryIds, final InterfaceDef interfaceDef, final ORB orb, final ComponentView componentView, final TransactionManager transactionManager, final ClassLoader classLoader) {
        this.methodInvokerMap = methodInvokerMap;
        this.repositoryIds = repositoryIds;
        this.interfaceDef = interfaceDef;
        this.componentView = componentView;
        this.transactionManager = transactionManager;
        this.classLoader = classLoader;
        SASCurrent sasCurrent;
        try {
            sasCurrent = (SASCurrent) orb.resolve_initial_references("SASCurrent");
        } catch (InvalidName invalidName) {
            sasCurrent = null;
        }
        this.sasCurrent = sasCurrent;
        org.jboss.iiop.tm.InboundTransactionCurrent inboundTxCurrent;
        try {
            inboundTxCurrent = (org.jboss.iiop.tm.InboundTransactionCurrent) orb.resolve_initial_references(org.jboss.iiop.tm.InboundTransactionCurrent.NAME);
        } catch (InvalidName invalidName) {
            inboundTxCurrent = null;
        }
        this.inboundTxCurrent = inboundTxCurrent;
    }

    public void setHomeHandle(final HomeHandle homeHandle) {
        this.homeHandle = homeHandle;
    }

    /**
     * Returns an IR object describing the bean's home interface.
     */
    public org.omg.CORBA.Object _get_interface_def() {
        if (interfaceDef != null)
            return interfaceDef;
        else
            return super._get_interface_def();
    }

    /**
     * Returns an array with the CORBA repository ids of the RMI-IDL
     * interfaces implemented by the container's <code>EJBHome</code>.
     */
    public String[] _all_interfaces(final POA poa, final byte[] objectId) {
        return repositoryIds.clone();
    }

    /**
     * Receives IIOP requests to an <code>EJBHome</code> and forwards them to the ejb
     */
    public OutputStream _invoke(final String opName, final InputStream in, final ResponseHandler handler) {
        if (logger.isTraceEnabled()) {
            logger.trace("EJBHome IIOP invocation: " + opName);
        }
        final SkeletonStrategy op = methodInvokerMap.get(opName);
        if (op == null) {
            logger.debug("Unable to find opname '" + opName + "' valid operations:" + methodInvokerMap.keySet());
            throw new BAD_OPERATION(opName);
        }


        //we have to run this bit of the invocation inside the EJB's naming context
        //as during the de-serialization process the Handles may look up the ORB and the
        //HandleDelegate

        final NamespaceContextSelector selector = componentView.getComponent().getNamespaceContextSelector();
        final ClassLoader oldCl = SecurityActions.getContextClassLoader();
        NamespaceContextSelector.pushCurrentSelector(selector);
        try {
            SecurityActions.setContextClassLoader(classLoader);


            org.omg.CORBA_2_3.portable.OutputStream out;
            try {
                Object retVal;

                // The EJBHome method getHomeHandle() receives special
                // treatment because the container does not implement it.
                // The remaining EJBObject methods (getEJBMetaData,
                // remove(java.lang.Object), and remove(javax.ejb.Handle))
                // are forwarded to the container.

                if (opName.equals("_get_homeHandle")) {
                    retVal = homeHandle;
                } else if(opName.equals("_get_EJBMetaData")) {
                    retVal = ejbMetaData;
                } else {
                    Transaction tx = null;
                    if (inboundTxCurrent != null) {
                        tx = inboundTxCurrent.getCurrentTransaction();
                    }
                    if (tx != null) {
                        transactionManager.resume(tx);
                    }
                    try {
                        SimplePrincipal principal = null;
                        char[] password = null;
                        if (sasCurrent != null) {
                            final byte[] username = sasCurrent.get_incoming_username();
                            byte[] credential = sasCurrent.get_incoming_password();
                            String name = new String(username, "UTF-8");
                            int domainIndex = name.indexOf('@');
                            if (domainIndex > 0)
                                name = name.substring(0, domainIndex);
                            if (name.length() == 0) {
                                final byte[] incomingName = sasCurrent.get_incoming_principal_name();
                                if (incomingName.length > 0) {
                                    name = new String(incomingName, "UTF-8");
                                    domainIndex = name.indexOf('@');
                                    if (domainIndex > 0)
                                        name = name.substring(0, domainIndex);
                                    principal = new SimplePrincipal(name);
                                    // username==password is a hack until
                                    // we have a real way to establish trust
                                    password = name.toCharArray();
                                }
                            } else {
                                principal = new SimplePrincipal(name);
                                password = new String(credential, "UTF-8").toCharArray();
                            }
                        }
                        final Object[] params = op.readParams((org.omg.CORBA_2_3.portable.InputStream) in);


                        final SecurityContext sc = SecurityContextFactory.createSecurityContext("CORBA_REMOTE");
                        sc.getUtil().createSubjectInfo(principal, password, null);

                        final InterceptorContext interceptorContext = new InterceptorContext();
                        interceptorContext.setContextData(new HashMap<String, Object>());
                        interceptorContext.setParameters(params);
                        interceptorContext.setMethod(op.getMethod());
                        interceptorContext.putPrivateData(ComponentView.class, componentView);
                        interceptorContext.putPrivateData(Component.class, componentView.getComponent());
                        retVal = componentView.invoke(interceptorContext);
                    } finally {
                        if (tx != null) {
                            if (transactionManager.getStatus() != Status.STATUS_NO_TRANSACTION) {
                                transactionManager.suspend();
                            }
                        }
                    }
                }
                out = (org.omg.CORBA_2_3.portable.OutputStream) handler.createReply();
                if (op.isNonVoid()) {
                    op.writeRetval(out, retVal);
                }
            } catch (Exception e) {
                if (logger.isTraceEnabled()) {
                    logger.trace("Exception in EJBHome invocation", e);
                }
                RmiIdlUtil.rethrowIfCorbaSystemException(e);
                out = (org.omg.CORBA_2_3.portable.OutputStream) handler.createExceptionReply();
                op.writeException(out, e);
            }
            return out;
        } finally {
            NamespaceContextSelector.popCurrentSelector();
            SecurityActions.setContextClassLoader(oldCl);
        }
    }

    /**
     * Receives intra-VM requests to an <code>EJBHome</code> and forwards them
     * to the ejb
     */
    public Object invoke(final String opName, final Object[] arguments, final Transaction tx, final Principal identity, final Object credential)
            throws Exception {
        if (logger.isTraceEnabled()) {
            logger.trace("EJBHome local invocation: " + opName);
        }

        SkeletonStrategy op = methodInvokerMap.get(opName);
        if (op == null) {
            throw new BAD_OPERATION(opName);
        }

        if (tx != null) {
            transactionManager.resume(tx);
        }
        try {
            final InterceptorContext interceptorContext = new InterceptorContext();
            interceptorContext.setContextData(new HashMap<String, Object>());
            interceptorContext.setParameters(arguments);
            interceptorContext.setMethod(op.getMethod());
            interceptorContext.putPrivateData(ComponentView.class, componentView);
            interceptorContext.putPrivateData(Component.class, componentView.getComponent());
            return componentView.invoke(interceptorContext);
        } finally {
            if (tx != null) {
                if (transactionManager.getStatus() != Status.STATUS_NO_TRANSACTION) {
                    transactionManager.suspend();
                }
            }
        }
    }

    public EJBMetaData getEjbMetaData() {
        return ejbMetaData;
    }

    public void setEjbMetaData(final EJBMetaData ejbMetaData) {
        this.ejbMetaData = ejbMetaData;
    }
}
