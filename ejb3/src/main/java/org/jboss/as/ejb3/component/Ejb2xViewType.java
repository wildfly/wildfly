package org.jboss.as.ejb3.component;

/**
 * An enum that is used as a marker for EJB 2.x views.
 *
 * {@link MethodIntf} is not sufficent for this, as it cannot differentiate
 * between EJB 3 business and EJB 2 component views
 *
 * @author Stuart Douglas
 */
public enum Ejb2xViewType {

    LOCAL,
    LOCAL_HOME,
    REMOTE,
    HOME,
}
