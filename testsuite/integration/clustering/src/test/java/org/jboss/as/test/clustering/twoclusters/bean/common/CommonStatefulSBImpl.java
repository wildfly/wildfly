package org.jboss.as.test.clustering.twoclusters.bean.common;

import org.jboss.as.test.clustering.twoclusters.bean.SerialBean;

import javax.annotation.PostConstruct;
import javax.ejb.Remove;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import org.jboss.logging.Logger;

@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class CommonStatefulSBImpl implements CommonStatefulSB {

    private SerialBean bean;
    private static final Logger log = Logger.getLogger(CommonStatefulSBImpl.class.getName());

    @PostConstruct
    private void init() {
        bean = new SerialBean();
        log.tracef("New SFSB created: %s.", this);
    }

    @Override
    public int getSerial() {
        log.trace("getSerial() called on non-forwarding node " + getCurrentNode());
        return bean.getSerial();
    }

    @Override
    public int getSerialAndIncrement() {
        log.trace("getSerialAndIncrement() called on non-forwarding node " + getCurrentNode());
        return bean.getSerialAndIncrement();
    }

    @Override
    public byte[] getCargo() {
        log.trace("getCargo() called on non-forwarding node " + getCurrentNode());
        return bean.getCargo();
    }

    @Remove
    private void destroy() {
        // Let the container do the work.
    }

    private String getCurrentNode() {
        return System.getProperty("jboss.node.name", "unknown");
    }
}
