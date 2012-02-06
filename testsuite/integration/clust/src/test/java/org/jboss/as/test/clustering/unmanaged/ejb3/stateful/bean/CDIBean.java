package org.jboss.as.test.clustering.unmanaged.ejb3.stateful.bean;

import java.io.Serializable;

/**
 * @author Stuart Douglas
 */
public class CDIBean implements Serializable, Counter {

    public int getCount() {
        return 10000000;
    }

}
