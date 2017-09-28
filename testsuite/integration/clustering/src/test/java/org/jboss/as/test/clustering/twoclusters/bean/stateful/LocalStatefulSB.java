package org.jboss.as.test.clustering.twoclusters.bean.stateful;

import javax.ejb.LocalBean;
import javax.ejb.Stateful;
import javax.enterprise.context.SessionScoped;

import org.jboss.as.test.clustering.twoclusters.bean.common.CommonStatefulSBImpl;

/**
 * @author Radoslav Husar
 */
@Stateful
@LocalBean
@SessionScoped
public class LocalStatefulSB extends CommonStatefulSBImpl {
    // Inherit.
}
