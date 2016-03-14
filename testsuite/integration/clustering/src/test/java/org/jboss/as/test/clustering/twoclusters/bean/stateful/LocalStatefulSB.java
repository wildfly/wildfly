package org.jboss.as.test.clustering.twoclusters.bean.stateful;

import org.jboss.as.test.clustering.twoclusters.bean.common.CommonStatefulSBImpl;
import org.jboss.ejb3.annotation.Clustered;

import javax.ejb.LocalBean;
import javax.ejb.Stateful;
import javax.enterprise.context.SessionScoped;

/**
 * @author Radoslav Husar
 * @version Dec 2011
 */
@Stateful
@LocalBean
@SessionScoped
@Clustered
public class LocalStatefulSB extends CommonStatefulSBImpl {
    // Inherit.
}
