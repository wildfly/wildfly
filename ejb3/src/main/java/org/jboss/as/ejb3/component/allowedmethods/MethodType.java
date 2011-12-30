package org.jboss.as.ejb3.component.allowedmethods;

/**
 * Enum of ejb methods controlled by {@link AllowedMethodsInformation}
 *
 * @author Stuart Douglas
 */
public enum MethodType {

    TIMER_SERVICE_METHOD("timer service method"),
    GET_EJB_LOCAL_OBJECT("getEJBLocalObject()"),
    GET_EJB_OBJECT("getEJBObject()"),
    GET_ROLLBACK_ONLY("getRollbackOnly()"),
    SET_ROLLBACK_ONLY("setRollbackOnly()"),
    GET_PRIMARY_KEY("getPrimaryKey()"),
    GET_TIMER_SERVICE("getTimerService()"),
    IS_CALLER_IN_ROLE("isCallerInRole()"),
    GET_CALLER_PRINCIPLE("getCallerPrinciple()"),
    GET_USER_TRANSACTION("getUserTransaction()"),
    ;

    private final String label;

    MethodType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
