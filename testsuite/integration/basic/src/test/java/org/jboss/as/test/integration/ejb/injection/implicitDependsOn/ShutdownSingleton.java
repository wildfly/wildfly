package org.jboss.as.test.integration.ejb.injection.implicitDependsOn;

import javax.annotation.PreDestroy;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.servlet.http.HttpServlet;

/**
 * @author Stuart Douglas
 */
@Singleton
@Startup
public class ShutdownSingleton extends HttpServlet {

    @EJB
    private ServiceEjb serviceEjb;


    @PreDestroy
    public void preDestroy() {
        //we sleep to allow the other EJB to shut down if the service dependencies were not correct
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        StaticDataClass.servletPreDestroyResult = serviceEjb.sayHello();
    }
}
