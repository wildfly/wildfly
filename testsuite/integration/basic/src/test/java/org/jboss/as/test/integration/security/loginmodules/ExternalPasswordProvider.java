/*
 * JBoss, Home of Professional Open Source
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.test.integration.security.loginmodules;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;



/**
 * A test class for the command starts with {EXT}/{EXTC[:TIMEOUT]} to obtain password for login modules
 * Prints first argument as a password to stream. Number of calls is saved in external file.
 *
 * @author Filip Bogyai
 */
public class ExternalPasswordProvider
{
   private static File counterFile = new File(System.getProperty("java.io.tmpdir"), "tmp.password");

   public static void main(String[] args)
   {
      String password = null;
      if (args != null && args.length == 1) {
    	 //increase counter in external file
    	 increaseFileCounter();
         password = args[0];
      }
      else {
         //original value as default
         password = "secret";

      }

      System.out.println(password);
      System.out.flush();

   }

   /**
    * Read and increase the number in File that counts how many times was this class called
    *
    * @return new increased number
    */
   public static int increaseFileCounter(){
	   int callsCounter = -1;
	   try{
		   FileReader reader = new FileReader(counterFile);
		   callsCounter = reader.read();
		   reader.close();

		   callsCounter++;

		   FileWriter writer = new FileWriter(counterFile);
		   writer.write(callsCounter);
		   writer.close();
	   }catch(IOException ex){
		   throw new RuntimeException("File for counting IO exception", ex);
	   }

	   return callsCounter;
   }

   /**
    * Set number in File to 0
    *
    */
   public void resetFileCounter(){
	   try{
		   FileWriter writer = new FileWriter(counterFile);
		   writer.write(0);
		   writer.close();
	   }catch(IOException ex){
		   throw new RuntimeException("File for counting IO exception", ex);
	   }


   }

   /**
    * Read number in File that counts how many times was this class called
    *
    */
   public int readFileCounter(){
	   int callsCounter = -1 ;

	   try{
		   FileReader reader = new FileReader(counterFile);
		   callsCounter = reader.read();
		   reader.close();
	   }catch(IOException ex){
		   throw new RuntimeException("File for counting IO exception", ex);
	   }

	   return callsCounter;

   }

    static void cleanup(){
        if (counterFile.exists()){
            counterFile.delete();
        }
    }
}