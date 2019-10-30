package org.jboss.as.test.clustering.cluster.ejb.remote.bean;

import javax.ejb.Remote;
import javax.ejb.Stateful;

/**
 * SFSB with passivation disabled, behaves as a singleton bean even if deployed on a clustered node
 *
 * @author Paul Ferarro
 */
@Stateful(passivationCapable = false)
@Remote(Incrementor.class)
public class PassivationDisabledStatefulIncrementorBean extends IncrementorBean {
}
