package org.jboss.as.test.shared.time;

import org.apache.commons.lang.math.NumberUtils;


/**
 *  Transforms default timeouts given by tests,
 *  according to these system properties:
 * 
    ts.tr.gen      General ratio - can be used to adjust all timeouts.
                   When this and specific are defined, both apply.
 
    ts.tr.fs       Filesystem IO
    ts.tr.net      Network IO
    ts.tr.mem      Memory IO
    ts.tr.cpu      Processor
    ts.tr.db       Database

 * 
 * @author ozizka@redhat.com
 */
public class TimeRatio {
    
    private static int gen;
    private static int fs;
    private static int net;
    private static int mem;
    private static int cpu;
    private static int db;

    static {
        gen  = Integer.getInteger("ts.tr.gen",   100 );
        fs   = Integer.getInteger("ts.tr.fs",    100 );
        net  = Integer.getInteger("ts.tr.net",   100 );
        mem  = Integer.getInteger("ts.tr.mem",   100 );
        cpu =  Integer.getInteger("ts.tr.cpu",   100 );
        db   = Integer.getInteger("ts.tr.db",    100 );
    }

    
    /**
     *   Recomputes timeout for operations heavily using file systems.
     *   @returns  Given timeout adjusted by ratio from sys prop "ts.tr.fs". 
     */
    public static int forFilesystem( int amount ){
        return (amount * gen * fs) / (100 * 100);
    }
    
    /**
     *   Recomputes timeout for operations heavily using a network.
     *   @returns  Given timeout adjusted by ratio from sys prop "ts.tr.net". 
     */
    public static int forNetwork( int amount ){
        return (amount * gen * net) / (100 * 100);
    }
    
    /**
     *   Recomputes timeout for operations heavily using the memory.
     *   @returns  Given timeout adjusted by ratio from sys prop "ts.tr.mem". 
     */
    public static int forMemory( int amount ){
        return (amount * gen * mem) / (100 * 100);
    }
    
    /**
     *   Recomputes timeout for computationally expensive tasks.
     *   @returns  Given timeout adjusted by ratio from sys prop "ts.tr.cpu". 
     */
    public static int forCPU( int amount ){
        return (amount * gen * cpu) / (100 * 100);
    }
    
    /**
     *   Recomputes timeout for database operations.
     *   @returns  Given timeout adjusted by ratio from sys prop "ts.tr.db". 
     */
    public static int forDatabase( int amount ){
        return (amount * gen * db) / (100 * 100);
    }
    
    
}// class
