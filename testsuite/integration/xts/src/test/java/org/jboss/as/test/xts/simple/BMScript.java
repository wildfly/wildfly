/*
 * JBoss, Home of Professional Open Source
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags.
 * See the copyright.txt in the distribution for a full listing
 * of individual contributors.
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 *
 * (C) 2005-2006,
 * @author JBoss Inc.
 */
package org.jboss.as.test.xts.simple;

import org.jboss.byteman.agent.submit.Submit;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author paul.robinson@redhat.com 21/11/2012
 */
public class BMScript {

    private static final Submit submit = new Submit();

    public static void submit(String script) {

        try {
            List<String> files = getScriptFiles(script);
            submit.addRulesFromFiles(files);
        } catch (Exception e) {
            throw new RuntimeException("Failed to submit Byteman script", e);
        }
    }

    public static void remove(String script) {

        try {
            List<String> files = getScriptFiles(script);
            submit.deleteRulesFromFiles(files);
        } catch (Exception e) {
            throw new RuntimeException("Failed to remove Byteman script", e);
        }
    }

    private static List<String> getScriptFiles(String scriptResourcePath) {
        URL resource=Thread.currentThread().getContextClassLoader().getResource(scriptResourcePath);
        if (resource == null) {
            throw new RuntimeException("'" + scriptResourcePath + "' can't be found on the classpath");
        }
        File file=new File(resource.getFile());

        if(file.exists() && file.isFile()) {
            List<String> files = new ArrayList<String>();
            files.add(resource.getFile());
            return files;
        } else {
            throw new RuntimeException("'" + scriptResourcePath + "' either doesn't exist or isn't a file");
        }

    }
}
