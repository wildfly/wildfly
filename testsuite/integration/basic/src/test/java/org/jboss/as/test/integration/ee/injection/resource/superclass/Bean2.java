/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.test.integration.ee.injection.resource.superclass;

import javax.annotation.ManagedBean;
import javax.annotation.Resource;

/**
 * @author Stuart Douglas
 */
@ManagedBean("bean2")
public class Bean2 extends SuperBean {

    public SimpleManagedBean getBean() {
        return simpleManagedBean;
    }

    int setCount = 0;

    @Resource(lookup = "java:module/string2")
    public void setSimpleString(final String simpleString) {
        super.setSimpleString(simpleString);
        //keep a count to make sure this is not injected twice
        ++setCount;
    }

    public int getSetCount() {
        return setCount;
    }
}
