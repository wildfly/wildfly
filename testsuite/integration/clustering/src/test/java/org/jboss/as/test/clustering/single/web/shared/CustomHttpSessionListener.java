package org.jboss.as.test.clustering.single.web.shared;

import jakarta.ejb.EJB;
import jakarta.servlet.annotation.WebListener;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;

@WebListener
public class CustomHttpSessionListener implements HttpSessionListener {

    @EJB
    private SessionDestroyCounter sessionDestroyCounter;

    public CustomHttpSessionListener() {
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent event) {
        sessionDestroyCounter.incrementSessionDestroyCount();
    }

}
