package org.jboss.as.ee.component.interceptors;

/**
 * Marker enum that can be used to identify special types of invocations
 *
 * @author Stuart Douglas
 */
public enum InvocationType {
    TIMER,
    REMOTE,
    ASYNC;
}
