/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jboss.as.subsystem.test;

import org.jboss.modules.filter.ClassFilter;

/**
 * ClassFilter that accepts all classes except the one sepcified and its inner classes.
 *
 * @author <a href="ehugonne@redhat.com">Emmanuel Hugonnet</a>
 */
public class SingleClassFilter implements ClassFilter {

    private final String filteredClassName;

    SingleClassFilter(String filteredClassName) {
        this.filteredClassName = filteredClassName;
    }

    @Override
    public boolean accept(String className) {
        return className != null && className.startsWith(this.filteredClassName);
    }

    public static ClassFilter createFilter(Class filteredClass) {
        return new SingleClassFilter(filteredClass.getName());
    }
}
