package org.jboss.as.ejb3.timerservice;

import org.jboss.as.ejb3.EjbMessages;

/**
 * According to the EJB spec there are times when calling timer service methods is not allowed.
 * <p/>
 * This class is used to track these events
 *
 * @author Stuart Douglas
 */
public class TimerServiceDisabledTacker {

    private static final ThreadLocal<String> disabledReason = new ThreadLocal<String>();

    /**
     * Temorarily disables the timer service methods, as according to the EJB spec timer service methods
     * must be disallowed
     * invoked
     *
     * @param method
     */
    public static void setDisabledReason(String method) {
        disabledReason.set(method);
    }

    public static String getDisabledReason() {
        return disabledReason.get();
    }

    public static void assertEnabled() {
        String s = disabledReason.get();
        if (s != null) {
            throw EjbMessages.MESSAGES.cannotCallTimerServiceMethod(s);
        }
    }

}
