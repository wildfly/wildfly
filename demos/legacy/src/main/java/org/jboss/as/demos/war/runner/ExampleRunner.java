/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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
package org.jboss.as.demos.war.runner;

import static org.jboss.as.protocol.StreamUtils.safeClose;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import org.jboss.as.demos.DeploymentUtils;
import org.jboss.as.demos.war.archive.SimpleServlet;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class ExampleRunner {

   public static void main(String[] args) throws Exception {
      DeploymentUtils utils = null;
      try {
         utils = new DeploymentUtils();
         utils.addWarDeployment("war-example.war", true, SimpleServlet.class.getPackage());
         utils.deploy();

         performCall("simple");
         performCall("legacy");
      } finally {
         utils.undeploy();
         safeClose(utils);
      }
   }

   private static void performCall(String urlPattern) throws Exception {
      URLConnection conn = null;
      InputStream in = null;
      try {
         URL url = new URL("http://localhost:8080/war-example/" + urlPattern + "?input=Hello");
         System.out.println("Reading response from " + url + ":");
         conn = url.openConnection();
         conn.setDoInput(true);
         in = new BufferedInputStream(conn.getInputStream());
         int i = in.read();
         while (i != -1) {
            System.out.print((char) i);
            i = in.read();
         }
         System.out.println("");
      } finally {
         safeClose(in);
      }
   }
}
