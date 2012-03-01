package org.jboss.as.test.shared;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

/**
 * Shared fs utils for the test suite.
 * 
 * Keep this class dependent only on JDK classes as it's packed to deployed WAR's.
 *
 * @author Stuart Douglas
 */
public class FileUtils {

    private FileUtils() {

    }

    public static String readFile(Class<?> testClass, String fileName) {
        final URL res = testClass.getResource(fileName);
        return readFile(res);
    }

    public static String readFile(URL url) {
        BufferedInputStream stream = null;
        try {
            stream = new BufferedInputStream(url.openStream());
            byte[] buff = new byte[1024];
            StringBuilder builder = new StringBuilder();
            int read = -1;
            while ((read = stream.read(buff)) != -1) {
                builder.append(new String(buff, 0, read));
            }
            return builder.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    //ignore
                }
            }
        }
    }
    
    
    
    public static File getFileOrCheckParentsIfNotFound( String baseStr, String path ) throws FileNotFoundException {
        //File f = new File( System.getProperty("jbossas.project.dir", "../../..") );
        File base = new File( baseStr );
        if( ! base.exists() ){
            throw new FileNotFoundException( "Base path not found: " + base.getPath() );
        }
        base = base.getAbsoluteFile();
        
        File f = new File( base, path );
        if ( f.exists() )
            return f;
        
        File fLast = f;
        while( ! f.exists() ){
            int slash = path.lastIndexOf( File.separatorChar );
            if( slash <= 0 )  // no slash or "/xxx"
                throw new FileNotFoundException("Path not found: " + f.getPath());
            path = path.substring( 0, slash );
            fLast = f;
            f = new File( base, path );
        }
        // When first existing is found, report the last non-existent.
        throw new FileNotFoundException("Path not found: " + fLast.getPath());
    }


    
    
    public static void copyFile(final File src, final File dest) throws Exception {
        final InputStream in = new BufferedInputStream(new FileInputStream(src));
        try {
            dest.getParentFile().mkdirs();
            final OutputStream out = new BufferedOutputStream(new FileOutputStream(dest));
            try {
                int i = in.read();
                while (i != -1) {
                    out.write(i);
                    i = in.read();
                }
            } finally {
                close(out);
            }
        } finally {
            close(in);
        }
    }
    
    
    public static void close(Closeable closeable) {
        try {
            closeable.close();
        } catch (IOException ignore) {
        }
    }
    

}// class
