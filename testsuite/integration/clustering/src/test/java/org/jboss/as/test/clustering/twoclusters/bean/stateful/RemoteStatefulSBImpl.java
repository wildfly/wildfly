package org.jboss.as.test.clustering.twoclusters.bean.stateful;

import javax.ejb.Stateful;

import org.jboss.as.test.clustering.twoclusters.bean.common.CommonStatefulSBImpl;

/**
 * @author Radoslav Husar
 */
@Stateful
public class RemoteStatefulSBImpl extends CommonStatefulSBImpl implements RemoteStatefulSB {
    // Inherit.
}
