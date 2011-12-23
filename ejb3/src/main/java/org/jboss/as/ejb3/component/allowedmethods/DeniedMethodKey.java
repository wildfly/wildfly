package org.jboss.as.ejb3.component.allowedmethods;

import org.jboss.as.ee.component.interceptors.InvocationType;

/**
 * @author Stuart Douglas
 */
public final class DeniedMethodKey {

    private final InvocationType invocationType;
    private final MethodType methodType;

    public DeniedMethodKey(InvocationType invocationType, MethodType methodType) {
        this.invocationType = invocationType;
        this.methodType = methodType;
    }

    public InvocationType getInvocationType() {
        return invocationType;
    }

    public MethodType getMethodType() {
        return methodType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DeniedMethodKey deniedMethodKey = (DeniedMethodKey) o;

        if (invocationType != deniedMethodKey.invocationType) return false;
        if (methodType != deniedMethodKey.methodType) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = invocationType != null ? invocationType.hashCode() : 0;
        result = 31 * result + (methodType != null ? methodType.hashCode() : 0);
        return result;
    }
}
