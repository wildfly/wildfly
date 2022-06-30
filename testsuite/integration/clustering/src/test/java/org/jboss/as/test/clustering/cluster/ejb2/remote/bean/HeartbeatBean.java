package org.jboss.as.test.clustering.cluster.ejb2.remote.bean;

import jakarta.ejb.Remote;
import jakarta.ejb.RemoteHome;
import jakarta.ejb.SessionBean;
import jakarta.ejb.Stateless;

@Stateless
@Remote(HeartbeatRemote.class)
@RemoteHome(HeartbeatRemoteHome.class)
public class HeartbeatBean extends HeartbeatBeanBase implements SessionBean {

}
