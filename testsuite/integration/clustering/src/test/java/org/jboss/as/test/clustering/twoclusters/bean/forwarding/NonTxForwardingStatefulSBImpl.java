package org.jboss.as.test.clustering.twoclusters.bean.forwarding;

import javax.ejb.Stateful;

import org.jboss.as.test.clustering.twoclusters.bean.stateful.RemoteStatefulSB;

@Stateful
public class NonTxForwardingStatefulSBImpl extends AbstractForwardingStatefulSBImpl implements RemoteStatefulSB {
}
