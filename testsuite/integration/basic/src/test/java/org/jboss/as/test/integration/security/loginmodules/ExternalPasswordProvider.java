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
public class ExternalPasswordProvider {
    //private File counterFile = new File(System.getProperty("java.io.tmpdir"), "tmp.password");
    private final File counterFile;

    public static void main(String... args) {
        String password = null;
        if (args != null && args.length == 2) {
            //increase counter in external file
            ExternalPasswordProvider provider = new ExternalPasswordProvider(args[0]);
            provider.increaseFileCounter();
            password = args[1];
        } else if (args != null && args.length == 1) {
            ExternalPasswordProvider provider = new ExternalPasswordProvider(args[0]);
            provider.increaseFileCounter();
            password = "secret";
        } else {
            //original value as default
            password = "secret";
        }

        System.out.println(password);
        System.out.flush();

    }

    public ExternalPasswordProvider(String fileName) {
        this.counterFile = new File(fileName);
    }

    /**
     * Read and increase the number in File that counts how many times was this class called
     *
     * @return new increased number
     */
    public int increaseFileCounter() {
        int callsCounter = -1;
        try {
            FileReader reader = new FileReader(counterFile);
            callsCounter = reader.read();
            reader.close();
            callsCounter++;
            FileWriter writer = new FileWriter(counterFile);
            writer.write(callsCounter);
            writer.close();
        } catch (IOException ex) {
            throw new RuntimeException("File for counting IO exception", ex);
        }

        return callsCounter;
    }

    /**
     * Set number in File to 0
     */
    public void resetFileCounter() {
        try {
            System.out.println("reseting file writer");
            System.out.println("counterFile = " + counterFile);
            FileWriter writer = new FileWriter(counterFile);
            writer.write(0);
            writer.close();
        } catch (IOException ex) {
            throw new RuntimeException("File for counting IO exception", ex);
        }


    }

    /**
     * Read number in File that counts how many times was this class called
     */
    public int readFileCounter() {
        int callsCounter = -1;

        try {
            System.out.println("callsCounter = " + callsCounter);
            System.out.println("reading counter file: " + counterFile);
            FileReader reader = new FileReader(counterFile);
            callsCounter = reader.read();
            reader.close();
        } catch (IOException ex) {
            throw new RuntimeException("File for counting IO exception", ex);
        }

        return callsCounter;

    }

    public String getCounterFile() {
        return counterFile.getAbsolutePath();
    }

    public void cleanup() {
        if (counterFile.exists()) {
            counterFile.delete();
        }
    }

}