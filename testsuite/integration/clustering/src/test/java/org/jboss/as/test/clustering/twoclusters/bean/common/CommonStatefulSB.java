package org.jboss.as.test.clustering.twoclusters.bean.common;


public interface CommonStatefulSB {

    int getSerial();

    int getSerialAndIncrement();

    byte[] getCargo();
}
