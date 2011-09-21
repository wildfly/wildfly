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
package org.jboss.as.security.vault;

import java.io.Console;
import java.util.Scanner;

import org.jboss.security.vault.SecurityVault;

/**
 * Command Line Tool for the default implementation of the {@link SecurityVault}
 *
 * @author Anil Saldhana
 */
public class VaultTool {

    private VaultInteractiveSession session = null;

    public void setSession(VaultInteractiveSession sess){
        session = sess;
    }
    public VaultInteractiveSession getSession(){
        return session;
    }

    public static void main(String[] args) {
       System.out.println("**********************************");
       System.out.println("****  JBoss Vault ********");
       System.out.println("**********************************");
       VaultTool tool = new VaultTool();

       Console console = System.console();

       if (console == null) {
           System.err.println("No console.");
           System.exit(1);
       }

       Scanner in = new Scanner(System.in);
       while(true){
          String commandStr = "Please enter a Digit::   0: Start Interactive Session " +
                 " 1: Remove Interactive Session " +
                 " 2: Exit";

          System.out.println(commandStr);
          int choice = in.nextInt();
          switch(choice){
              case 0:
                  System.out.println("Starting an interactive session");
                  VaultInteractiveSession vsession = new VaultInteractiveSession();
                  tool.setSession(vsession);
                  vsession.start();
                  break;
              case 1:
                  System.out.println("Removing the current interactive session");
                  tool.setSession(null);
                  break;
              default:
                  System.exit(0);
          }
       }
    }
}