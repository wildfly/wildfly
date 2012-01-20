package org.jboss.as.ejb3.component.entity.interceptors;

/**
 *
 * Marker used to signify a CMP internal invocation.
 *
 * @author Stuart Douglas
 *
 */
public class InternalInvocationMarker {

    public static final InternalInvocationMarker INSTANCE = new InternalInvocationMarker();

    private InternalInvocationMarker () {

    }

}
