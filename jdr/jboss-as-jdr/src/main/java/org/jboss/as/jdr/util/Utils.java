package org.jboss.as.jdr.util;

import org.jboss.vfs.VirtualFile;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;

/**
 * User: csams
 * Date: 11/4/12
 * Time: 3:40 PM
 */
public final class Utils {

    public static void safeClose(JarFile jf){
        try{
            if(jf != null) {
                jf.close();
            }
        }catch(Exception e){

        }
    }

    public static void safelyClose(InputStream is){
        try{
            if(is != null) {
                is.close();
            }
        }catch(Exception e){

        }
    }

    public static List<String> readLines(InputStream input) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(input));
        List<String> result = new ArrayList<String>();
        String line = reader.readLine();

        while(line != null){
            result.add(line);
            line = reader.readLine();
        }
        return result;
    }

    public static String toString(VirtualFile r) throws IOException {
        return new String(toBytes(r));
    }

    public static byte[] toBytes(VirtualFile r) throws IOException {
        byte [] buffer = new byte[1024];
        InputStream is = r.openStream();

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        int bytesRead = is.read(buffer);
        while( bytesRead > -1 ) {
            os.write(buffer, 0, bytesRead);
            bytesRead = is.read(buffer);
        }

        Utils.safelyClose(is);
        return os.toByteArray();
    }

    /**
     * Ensure InputStream actually skips ahead the required number of bytes
     * @param is
     * @param amount
     * @throws IOException
     */
    public static void skip(InputStream is, long amount) throws IOException {
        long leftToSkip = amount;
        long amountSkipped = 0;
        while(leftToSkip > 0 && amountSkipped >= 0){
            amountSkipped = is.skip(leftToSkip);
            leftToSkip -= amountSkipped;
        }
    }

    public static boolean isSymlink(VirtualFile vFile) throws IOException {

        File file = vFile.getPhysicalFile();

        if(Utils.isWindows()){
            return false;
        }

        File fileInCanonicalDir = null;
        if (file.getParent() == null) {
            fileInCanonicalDir = file;
        } else {
            File canonicalDir = file.getParentFile().getCanonicalFile();
            fileInCanonicalDir = new File(canonicalDir, file.getName());
        }

        if (fileInCanonicalDir.getCanonicalFile().equals(fileInCanonicalDir.getAbsoluteFile())) {
            return false;
        } else {
            return true;
        }

    }

    public static final String MANIFEST_NAME = "META-INF/MANIFEST.MF";

    public static final String LINE_SEP = String.format("%n");
    public static final char WIN_SEP = '\\';
    public static final char SYS_SEP = File.separatorChar;

    public static boolean isWindows() {
        return SYS_SEP == WIN_SEP;
    }

    public static final long ONE_KB = 1024;

    public static final long ONE_MB = ONE_KB * ONE_KB;

    public static final long ONE_GB = ONE_KB * ONE_MB;

    public static final long ONE_TB = ONE_KB * ONE_GB;

}
