/*
* JBoss, Home of Professional Open Source.
* Copyright 2012, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.config.assembly;

import java.io.File;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class DomainMain {


    public static void main(String[] args) throws Exception {
        File baseDir = new File(args[0]);
        if (!baseDir.exists() || !baseDir.isDirectory()) {
            throw new IllegalArgumentException("Base dir does not exist: " + baseDir);
        }
        File templateFile = new File(args[1]);
        if (!templateFile.exists()) {
            throw new IllegalArgumentException("Template file does not exist: " + templateFile);
        }
        File subsystemsFile = new File(args[2]);
        if (!subsystemsFile.exists()) {
            throw new IllegalArgumentException("Subsystems file does not exist " + subsystemsFile);
        }
        File outputFile = new File(args[3]);


        ConfigurationAssembler assembler = new ConfigurationAssembler(baseDir, templateFile, "domain", subsystemsFile, outputFile);
        assembler.assemble();
    }
}
