package org.jboss.as.test.manualmode.ejb.shutdown;

/**
 * @author Stuart Douglas
 */
public interface RemoteLatch {

    void testDone();

    void setEchoMessage(String echoMessage);

    String getEchoMessage();

}
