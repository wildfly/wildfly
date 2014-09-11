package org.wildfly.extension.undertow.deployment;

import io.undertow.security.api.NotificationReceiver;
import io.undertow.security.api.SecurityNotification;
import io.undertow.security.idm.Account;
import org.jboss.security.CacheableManager;
import org.wildfly.extension.undertow.security.AccountImpl;

import java.security.Principal;

/**
 * Undertow security listener that invalidates the cache on logout
 *
 * @author Stuart Douglas
 */
public class CacheInvalidationNotificationReceiver implements NotificationReceiver {

    private final CacheableManager<?, Principal> cm;

    public CacheInvalidationNotificationReceiver(CacheableManager<?, Principal> cm) {
        this.cm = cm;
    }

    @Override
    public void handleNotification(SecurityNotification notification) {
        if (notification.getEventType() == SecurityNotification.EventType.LOGGED_OUT) {
            Account account = notification.getAccount();
            if(account instanceof AccountImpl) {
                cm.flushCache(((AccountImpl)account).getOriginalPrincipal());
            }
            if(account != null) {
                cm.flushCache(account.getPrincipal());
            }
        }
    }
}
