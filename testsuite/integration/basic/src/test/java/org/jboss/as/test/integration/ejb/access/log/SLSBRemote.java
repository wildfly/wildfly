package org.jboss.as.test.integration.ejb.access.log;

public interface SLSBRemote {
    String echo(String msg);
    String echoSecuredRole1(String msg);
    String echoSecuredRole2(String msg);
}
