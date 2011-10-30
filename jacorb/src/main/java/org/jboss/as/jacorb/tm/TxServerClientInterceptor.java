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
package org.jboss.as.jacorb.tm;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.jboss.as.jacorb.service.CorbaORBService;
import org.jboss.logging.Logger;
import org.jboss.tm.TxUtils;
import org.jboss.util.NestedRuntimeException;
import org.omg.CORBA.Any;
import org.omg.CORBA.LocalObject;
import org.omg.CORBA_2_3.ORB;
import org.omg.CosTransactions.PropagationContext;
import org.omg.CosTransactions.PropagationContextHelper;
import org.omg.CosTransactions.TransIdentity;
import org.omg.CosTransactions.otid_t;
import org.omg.IOP.Codec;
import org.omg.IOP.CodecPackage.InvalidTypeForEncoding;
import org.omg.IOP.ServiceContext;
import org.omg.IOP.TransactionService;
import org.omg.PortableInterceptor.ClientRequestInfo;
import org.omg.PortableInterceptor.ClientRequestInterceptor;

/**
 * This implementation of
 * <code>org.omg.PortableInterceptor.ClientRequestInterceptor</code>
 * inserts the transactional context into outgoing requests
 * from JBoss's transaction manager.
 *
 * @author <a href="mailto:adrian@jboss.com">Adrian Brock</a>
 */
public class TxServerClientInterceptor extends LocalObject implements ClientRequestInterceptor {
    /**
     * @since 4.0.1
     */
    static final long serialVersionUID = 4716203472714459196L;


    private static final Logger log = Logger.getLogger(TxServerClientInterceptor.class);
    private static final boolean traceEnabled = log.isTraceEnabled();

    private static final int txContextId = TransactionService.value;
    private static Codec codec;
    private static TransactionManager tm;
    private static final PropagationContext emptyPC;

    static {
        // According to the spec, this should all be ignored
        // But we get NPEs if it doesn't contain some content
        emptyPC = new PropagationContext();
        emptyPC.parents = new TransIdentity[0];
        emptyPC.current = new TransIdentity();
        emptyPC.current.otid = new otid_t();
        emptyPC.current.otid.formatID = 666;
        emptyPC.current.otid.bqual_length = 1;
        emptyPC.current.otid.tid = new byte[]{(byte) 1};
        emptyPC.implementation_specific_data = ORB.init().create_any();
        emptyPC.implementation_specific_data.insert_boolean(false);
    }

    // Static methods ------------------------------------------------

    static void init(Codec codec) {
        TxServerClientInterceptor.codec = codec;
    }

    static TransactionManager getTransactionManager() {
        if (tm == null) {
            try {
                Context ctx = new InitialContext();
                tm = (TransactionManager) ctx.lookup("java:jboss/TransactionManager");
            } catch (NamingException e) {
                throw new NestedRuntimeException("java:jboss/TransactionManager lookup failed", e);
            }
        }
        return tm;
    }

    static PropagationContext getEmptyPropagationContext() {
        return emptyPC;
    }

    // Constructor ---------------------------------------------------

    public TxServerClientInterceptor() {
        // do nothing
    }

    // org.omg.PortableInterceptor.Interceptor operations ------------

    public String name() {
        return "TxServerClientInterceptor";
    }

    public void destroy() {
        // do nothing
    }

    // ClientRequestInterceptor operations ---------------------------

    public void send_request(ClientRequestInfo ri) {
        if (traceEnabled)
            log.trace("Intercepting send_request, operation: " + ri.operation());
        try {
            final Any any = getTransactionPropagationContextAny();
            if (any != null) {
                final ServiceContext sc = new ServiceContext(txContextId, codec.encode_value(any));
                ri.add_request_service_context(sc, true);
            }
        } catch (InvalidTypeForEncoding e) {
            throw new NestedRuntimeException(e);
        }
    }

    public void send_poll(ClientRequestInfo ri) {
        // do nothing
    }

    public void receive_reply(ClientRequestInfo ri) {
        // do nothing
    }

    public void receive_exception(ClientRequestInfo ri) {
        // do nothing
    }

    public void receive_other(ClientRequestInfo ri) {
        // do nothing
    }

    protected Any getTransactionPropagationContextAny() {
        try {
            PropagationContext pc = null;
            final TransactionManager tm = getTransactionManager();
            final Transaction tx = tm.getTransaction();
            if (!TxUtils.isUncommitted(tx)) {
                if (traceEnabled)
                    log.trace("No transaction context");
                return null;
            }

            if (traceEnabled)
                log.trace("Propagating empty OTS context");
            pc = getEmptyPropagationContext();

            final Any any = CorbaORBService.getCurrent().create_any();
            PropagationContextHelper.insert(any, pc);
            return any;
        } catch (Exception e) {
            throw new NestedRuntimeException("Error getting tpc", e);
        }
    }
}
