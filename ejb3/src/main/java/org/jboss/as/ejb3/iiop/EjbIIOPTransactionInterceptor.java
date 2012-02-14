package org.jboss.as.ejb3.iiop;

import javax.ejb.TransactionAttributeType;
import javax.transaction.Transaction;

import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.component.ComponentView;
import org.jboss.as.ejb3.EjbMessages;
import org.jboss.as.ejb3.component.EJBComponent;
import org.jboss.as.ejb3.component.MethodIntf;
import org.jboss.as.jacorb.tm.ForeignTransaction;
import org.jboss.as.jacorb.tm.TxServerInterceptor;
import org.jboss.invocation.ImmediateInterceptorFactory;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;

/**
 * @author Stuart Douglas
 */
public class EjbIIOPTransactionInterceptor implements Interceptor {

    public static final InterceptorFactory FACTORY = new ImmediateInterceptorFactory(new EjbIIOPTransactionInterceptor());

    @Override
    public Object processInvocation(final InterceptorContext invocation) throws Exception {
        // Do we have a foreign transaction context?
        Transaction tx = TxServerInterceptor.getCurrentTransaction();
        if (tx instanceof ForeignTransaction) {
            final EJBComponent component = (EJBComponent) invocation.getPrivateData(Component.class);

            //for timer invocations there is no view, so the methodInf is attached directly
            //to the context. Otherwise we retrieve it from the invoked view
            MethodIntf methodIntf = invocation.getPrivateData(MethodIntf.class);
            if (methodIntf == null) {
                final ComponentView componentView = invocation.getPrivateData(ComponentView.class);
                if (componentView != null) {
                    methodIntf = componentView.getPrivateData(MethodIntf.class);
                } else {
                    methodIntf = MethodIntf.BEAN;
                }
            }

            final TransactionAttributeType attr = component.getTransactionAttributeType(methodIntf, invocation.getMethod());
            if (attr != TransactionAttributeType.NOT_SUPPORTED && attr != TransactionAttributeType.REQUIRES_NEW) {
                throw EjbMessages.MESSAGES.transactionPropagationNotSupported();
            }
        }
        return invocation.proceed();
    }
}
