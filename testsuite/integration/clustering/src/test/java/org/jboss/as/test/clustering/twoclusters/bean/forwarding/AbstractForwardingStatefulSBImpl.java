package org.jboss.as.test.clustering.twoclusters.bean.forwarding;

import org.jboss.as.test.clustering.twoclusters.bean.stateful.RemoteStatefulSB;
import org.jboss.as.test.clustering.twoclusters.bean.stateful.RemoteStatefulSBImpl;

import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.naming.Context;
import javax.naming.InitialContext;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class AbstractForwardingStatefulSBImpl {

    private static final Logger log = Logger.getLogger(AbstractForwardingStatefulSBImpl.class.getName());

    private RemoteStatefulSB bean;

    private final String appName = "";
    private final String moduleName = "clusterbench-ee6-ejb";
    private final String distinctName = "" ;
    private final String beanName = RemoteStatefulSBImpl.class.getSimpleName();
    private final String viewClassName = RemoteStatefulSB.class.getName() ;

    private final String EJB_NAME = "ejb:" + appName + "/" + moduleName + "/" + distinctName + "/" + beanName +  "!" + viewClassName + "?stateful";

    @SuppressWarnings("unchecked")
    private RemoteStatefulSB forward() {
        if (bean == null) {
            try {
                Hashtable props = new Hashtable();
                props.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
                Context context = new InitialContext(props);
                bean = (RemoteStatefulSB) context.lookup(EJB_NAME);
            } catch (Exception e) {
                log.log(Level.INFO, "exception occurred looking up name " + EJB_NAME + " on forwarding node " + getCurrentNode());
                throw new RuntimeException(e);
            }
        }
        return bean;
    }

    public int getSerial() {
        log.log(Level.INFO, "getSerial() called on forwarding node " + getCurrentNode());
        return forward().getSerial();
    }

    public int getSerialAndIncrement() {
        log.log(Level.INFO, "getSerialAndIncrement() called on forwarding node " + getCurrentNode());
        return forward().getSerialAndIncrement();
    }

    public byte[] getCargo() {
        log.log(Level.INFO, "getCargo() called on forwarding node " + getCurrentNode());
        return forward().getCargo();
    }

    private String getCurrentNode() {
        return System.getProperty("jboss.node.name", "unknown");
    }
}
