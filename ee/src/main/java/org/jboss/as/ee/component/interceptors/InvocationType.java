package org.jboss.as.ee.component.interceptors;

/**
 * Marker enum that can be used to identify special types of invocations
 *
 * @author Stuart Douglas
 */
public enum InvocationType {
    TIMER("timer"),
    REMOTE("remote"),
    ASYNC("async"),
    SET_ENTITY_CONTEXT("setEntityContext"),
    UNSET_ENTITY_CONTEXT("unsetEntityContext"),
    POST_CONSTRUCT("postConstruct"),
    PRE_DESTROY("preDestroy"),
    DEPENDENCY_INJECTION("setSessionContext"),
    SFSB_INIT_METHOD("stateful session bean init method"),
    FINDER_METHOD("entity bean finder method"),
    HOME_METHOD("entity bean home method"),
    ENTITY_EJB_CREATE("entity bean ejbCreate method"),

    ;

    private final String label;

    InvocationType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
