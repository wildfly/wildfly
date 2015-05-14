package org.jboss.as.test.clustering.twoclusters.bean.stateful;

import org.jboss.as.test.clustering.twoclusters.bean.common.CommonStatefulSBImpl;
import org.jboss.ejb3.annotation.Clustered;

import javax.ejb.Stateful;

/**
 * @author Radoslav Husar
 * @version Dec 2011
 */
@Stateful
@Clustered
public class RemoteStatefulSBImpl extends CommonStatefulSBImpl implements RemoteStatefulSB {
    // Inherit.
}
