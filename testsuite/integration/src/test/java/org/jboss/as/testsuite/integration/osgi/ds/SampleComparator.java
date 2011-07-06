package org.jboss.as.testsuite.integration.osgi.ds;

import java.util.Comparator;

public class SampleComparator implements Comparator<Object> {

    public int compare(Object o1, Object o2) {
        return o1.equals(o2) ? 0 : -1;
    }
}