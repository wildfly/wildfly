package org.jboss.as.test.clustering.twoclusters.bean.forwarding;

import org.jboss.as.test.clustering.twoclusters.bean.stateful.RemoteStatefulSB;
import org.jboss.ejb3.annotation.Clustered;

import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

@Stateful
@Clustered
@TransactionAttribute(TransactionAttributeType.REQUIRED) // this is the default anyway
public class ForwardingStatefulSBImpl extends AbstractForwardingStatefulSBImpl implements RemoteStatefulSB {
}
