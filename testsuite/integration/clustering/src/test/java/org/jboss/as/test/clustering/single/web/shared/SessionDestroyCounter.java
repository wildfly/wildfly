package org.jboss.as.test.clustering.single.web.shared;

import jakarta.ejb.Singleton;

@Singleton
public class SessionDestroyCounter {

    private int sessionDestroyCount = 0;

    public int getSessionDestroyCount() {
        return this.sessionDestroyCount;
    }

    public void incrementSessionDestroyCount() {
        this.sessionDestroyCount++;
    }
}
