package org.jboss.as.test.clustering.clusteringmonitor;

import org.jboss.ejb3.annotation.Clustered;

/**
 * A stateful session bean.
 *
 * @author Richard Achmatowicz (c) 2013 Red Hat Inc.
 */

@javax.ejb.Stateful
@Clustered
public class StatefulBean implements Stateful {

    private int count = 0 ;

    @Override
    public int increment() {
        return count++;
    }
}
