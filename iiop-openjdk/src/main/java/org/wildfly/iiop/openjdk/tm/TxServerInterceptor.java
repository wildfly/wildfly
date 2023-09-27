/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.iiop.openjdk.tm;

import jakarta.transaction.Transaction;

import org.omg.CORBA.Any;
import org.omg.CORBA.BAD_PARAM;
import org.omg.CORBA.LocalObject;
import org.omg.CORBA.TCKind;
import org.omg.CosTransactions.PropagationContextHelper;
import org.omg.IOP.Codec;
import org.omg.IOP.CodecPackage.FormatMismatch;
import org.omg.IOP.CodecPackage.TypeMismatch;
import org.omg.IOP.ServiceContext;
import org.omg.PortableInterceptor.InvalidSlot;
import org.omg.PortableInterceptor.ServerRequestInfo;
import org.omg.PortableInterceptor.ServerRequestInterceptor;
import org.wildfly.iiop.openjdk.logging.IIOPLogger;

/**
 * This implementation of
 * <code>org.omg.PortableInterceptor.ServerRequestInterceptor</code>
 * retrieves the transactional context from incoming IIOP requests and
 * makes it available to the servant methods that handle the requests,
 * through the static method <code>getCurrentTransaction</code).
 *
 * In practice this is only used to throw an exception when importing transactions from
 * a 3rd party application server, as required by the spec.
 *
 * @author <a href="mailto:reverbel@ime.usp.br">Francisco Reverbel</a>
 */
@SuppressWarnings("unused")
public class TxServerInterceptor extends LocalObject implements ServerRequestInterceptor {

    static final long serialVersionUID = 7474707114565659371L;

    private static final int txContextId = org.omg.IOP.TransactionService.value;

    private static int slotId;

    private static Codec codec;

    private static org.omg.PortableInterceptor.Current piCurrent = null;


    /**
     * Called by <code>TxServerInterceptorInitializer</code> at ORB initialization time.
     */
    static void init(int slotId, Codec codec, org.omg.PortableInterceptor.Current piCurrent) {
        TxServerInterceptor.slotId = slotId;
        TxServerInterceptor.codec = codec;
        TxServerInterceptor.piCurrent = piCurrent;
    }

    /**
     * Returns the transaction associated with the transaction propagation
     * context that arrived in the current IIOP request.
     */
    public static Transaction getCurrentTransaction() {
        Transaction tx = null;
        if (piCurrent != null) {
            // A non-null piCurrent means that a TxServerInterceptor was
            // installed: check if there is a transaction propagation context
            try {
                Any any = piCurrent.get_slot(slotId);
                if (any.type().kind().value() != TCKind._tk_null) {
                    // Yes, there is a TPC: add the foreign transaction marker
                    tx = ForeignTransaction.INSTANCE;
                }
            } catch (InvalidSlot e) {
                throw IIOPLogger.ROOT_LOGGER.errorGettingSlotInTxInterceptor(e);
            }

        }
        return tx;
    }

    public String name() {
        return "TxServerInterceptor";
    }

    public void destroy() {
        // do nothing
    }

    public void receive_request_service_contexts(ServerRequestInfo ri) {
        IIOPLogger.ROOT_LOGGER.tracef("Intercepting receive_request_service_contexts, operation: %s", ri.operation());
        try {
            ServiceContext sc = ri.get_request_service_context(txContextId);
            Any any = codec.decode_value(sc.context_data, PropagationContextHelper.type());
            ri.set_slot(slotId, any);
        } catch (BAD_PARAM e) {
            // no service context with txContextId: do nothing
        } catch (FormatMismatch e) {
            throw IIOPLogger.ROOT_LOGGER.errorDecodingContextData(this.name(), e);
        } catch (TypeMismatch e) {
            throw IIOPLogger.ROOT_LOGGER.errorDecodingContextData(this.name(), e);
        } catch (InvalidSlot e) {
            throw IIOPLogger.ROOT_LOGGER.errorSettingSlotInTxInterceptor(e);
        }
    }

    public void receive_request(ServerRequestInfo ri) {
        // do nothing
    }

    public void send_reply(ServerRequestInfo ri) {
        // do nothing
    }

    public void send_exception(ServerRequestInfo ri) {
        // do nothing
    }

    public void send_other(ServerRequestInfo ri) {
        // do nothing
    }
}
