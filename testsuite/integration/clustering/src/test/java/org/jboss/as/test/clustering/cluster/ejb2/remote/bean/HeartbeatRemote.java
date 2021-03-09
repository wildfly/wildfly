package org.jboss.as.test.clustering.cluster.ejb2.remote.bean;

import javax.ejb.EJBObject;
import java.util.Date;

/**
 * The remote / business interface for the Heartbeat Enterprise Beans 2.x bean
 *
 * @author Richard Achmatowicz
 */
public interface HeartbeatRemote extends EJBObject {
    Result<Date> pulse();
}
