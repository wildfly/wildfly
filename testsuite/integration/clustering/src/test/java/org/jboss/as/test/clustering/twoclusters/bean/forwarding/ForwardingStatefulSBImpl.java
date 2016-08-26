package org.jboss.as.test.clustering.twoclusters.bean.forwarding;

import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.jboss.as.test.clustering.twoclusters.bean.stateful.RemoteStatefulSB;
import org.jboss.ejb3.annotation.Clustered;

@Stateful
@Clustered
@TransactionAttribute(TransactionAttributeType.REQUIRED) // this is the default anyway
public class ForwardingStatefulSBImpl extends AbstractForwardingStatefulSBImpl implements RemoteStatefulSB {

    // we need to override these methods so that the TransactionAttribute gets processed on this class!

    @Override
    public int getSerial() {
        return super.getSerial();
    }

    @Override
    public int getSerialAndIncrement() {
        return super.getSerialAndIncrement();
    }

    @Override
    public byte[] getCargo() {
        return super.getCargo();
    }
}
