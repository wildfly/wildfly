package org.jboss.as.test.clustering.twoclusters.bean.stateful;

import org.jboss.as.test.clustering.twoclusters.bean.common.CommonStatefulSB;

import javax.ejb.Remote;
/**
 * The enterprise bean must implement a business interface. That is, remote clients may not access an enterprise bean through a
 * no-interface view.
 *
 * @author Radoslav Husar
 * @version Dec 2011
 */
@Remote
public interface RemoteStatefulSB extends CommonStatefulSB {
    // Inherit.
}