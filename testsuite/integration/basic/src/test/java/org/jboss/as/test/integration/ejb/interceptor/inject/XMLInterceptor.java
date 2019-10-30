/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

package org.jboss.as.test.integration.ejb.interceptor.inject;

import java.util.ArrayList;
import javax.interceptor.InvocationContext;
import javax.sql.DataSource;

/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class XMLInterceptor {
    MySession2 session2;
    DataSource ds;

    MySession2Local session2Method;
    DataSource dsMethod;

    public void setSession2Method(MySession2Local session2Method) {
        this.session2Method = session2Method;
    }

    public void setDsMethod(DataSource dsMethod) {
        this.dsMethod = dsMethod;
    }

    public Object intercept(InvocationContext ctx) throws Exception {
        session2.doit();
        if (ds == null) { throw new RuntimeException("ds was null"); }

        session2Method.doit();
        if (dsMethod == null) { throw new RuntimeException("ds was null"); }
        ArrayList list = new ArrayList();
        list.add("MyInterceptor");
        return list;
    }

}
