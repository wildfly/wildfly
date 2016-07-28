package org.jboss.as.test.clustering.twoclusters.bean.common;

import org.jboss.as.test.clustering.twoclusters.bean.SerialBean;

import javax.annotation.PostConstruct;
import javax.ejb.Remove;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.logging.Level;
import java.util.logging.Logger;

@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class CommonStatefulSBImpl implements CommonStatefulSB {

    private SerialBean bean;
    private static final Logger log = Logger.getLogger(CommonStatefulSBImpl.class.getName());

    @PostConstruct
    private void init() {
        bean = new SerialBean();
        log.log(Level.INFO, "New SFSB created: {0}.", this);
    }

    @Override
    public int getSerial() {
        log.log(Level.INFO, "getSerial() called on non-forwarding node " + getCurrentNode());
        return bean.getSerial();
    }

    @Override
    public int getSerialAndIncrement() {
        log.log(Level.INFO, "getSerialAndIncrement() called on non-forwarding node " + getCurrentNode());
        return bean.getSerialAndIncrement();
    }

    @Override
    public byte[] getCargo() {
        log.log(Level.INFO, "getCargo() called on non-forwarding node " + getCurrentNode());
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
