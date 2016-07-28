package org.jboss.as.test.clustering.twoclusters.bean.forwarding;

import org.jboss.as.test.clustering.twoclusters.bean.stateful.RemoteStatefulSB;
import org.jboss.ejb3.annotation.Clustered;

import javax.ejb.Stateful;

@Stateful
@Clustered
public class NonTxForwardingStatefulSBImpl extends AbstractForwardingStatefulSBImpl implements RemoteStatefulSB {
}
