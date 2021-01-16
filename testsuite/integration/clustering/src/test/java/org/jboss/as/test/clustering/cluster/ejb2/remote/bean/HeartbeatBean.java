package org.jboss.as.test.clustering.cluster.ejb2.remote.bean;

import javax.ejb.Remote;
import javax.ejb.RemoteHome;
import javax.ejb.SessionBean;
import javax.ejb.Stateless;

@Stateless
@Remote(HeartbeatRemote.class)
@RemoteHome(HeartbeatRemoteHome.class)
public class HeartbeatBean extends HeartbeatBeanBase implements SessionBean {

}
