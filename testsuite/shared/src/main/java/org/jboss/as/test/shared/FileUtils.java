package org.jboss.as.test.shared;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URL;

/**
 * Shared fs utils for the test suite
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

}
