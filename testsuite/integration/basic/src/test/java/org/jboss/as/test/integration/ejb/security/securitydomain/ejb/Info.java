/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.ejb.security.securitydomain.ejb;

import javax.ejb.EJBContext;
import java.io.Serializable;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

/**
 * @author bmaxwell
 *
 */
public class Info implements Serializable {

    private List<String> path = new ArrayList<String>();
    private String testName = null;

    /**
     *
     */
    public Info(String testName) {
        this.testName = testName;
    }

    public void add(String step) {
        path.add(step);
    }

    public List<String> getPath() {
        return path;
    }

    public String getTestName() {
        return this.testName;
    }

    public Info update(String ejbName, EJBContext ejbContext, String expectedPrinciaplClassName) {
        Principal principal = ejbContext.getCallerPrincipal();
        String caller = principal == null ? "null" : principal.getName();
        String principalClass = principal == null ? "null" : principal.getClass().getName();
        if(expectedPrinciaplClassName.compareTo(principalClass) != 0)
            add(String.format("InCorrect: Principal unexected %s != %s", principalClass, expectedPrinciaplClassName));
        else
            add(String.format("Correct: %s == %s", principalClass, expectedPrinciaplClassName));
        return this;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Test: %s\n", testName));
        sb.append(String.format("Resulting path:\n"));
        for(String step : path)
            sb.append(String.format("- %s\n", step));
        return sb.toString();
    }
}