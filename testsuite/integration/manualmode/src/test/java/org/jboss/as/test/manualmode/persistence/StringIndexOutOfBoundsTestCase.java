package org.jboss.as.test.manualmode.persistence;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import javax.inject.Inject;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.core.testrunner.Server;
import org.wildfly.core.testrunner.ServerControl;
import org.wildfly.core.testrunner.ServerController;
import org.wildfly.core.testrunner.WildflyTestRunner;
import org.xnio.IoUtils;

/**
 * Test if config file with wrong name can be present in 'standalone_xml_history/snapshot' folder

 *
 * @author Petr Adamec
 */
@ServerControl(manual = true)
@RunWith(WildflyTestRunner.class)
public class StringIndexOutOfBoundsTestCase {

    private static final String SERVER_CONFIG = "standalone.xml";
    private static final String BASE_PATH = TestSuiteEnvironment.getJBossHome()+File.separatorChar+"standalone"+File.separatorChar+"configuration";
    private static final String COPY_PATH = BASE_PATH+"standalone_xml_history"+File.separatorChar+"snapshot";

    @Inject
    private ServerController container;
    private File copyConfigFile;
    private File targetDirectory;


    private static void copyFile(final File src, final File dst) throws IOException{
        FileInputStream in = null;
        FileOutputStream out = null;
        try {
            in = new FileInputStream(src);
            out = new FileOutputStream(dst);
            byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        } finally {
            IoUtils.safeClose(in);
            IoUtils.safeClose(out);
        }
    }

    /**
     * Test if config file with wrong name can be present in 'standalone_xml_history/snapshot' folder.</br>
     * It makes directory and copy config file with wrong name to there. Then try start container.</br>
     * For more information visit <a href="https://issues.jboss.org/browse/JBEAP-14107">https://issues.jboss.org/browse/JBEAP-14107</a>
     * @throws Exception Throw exception if container could not start,
     *                  RuntimeException is captured and its message does not contain String <i>Could not start container<i/>.
     */
    @Test
    public void testStartContainerWithBadFileInSnapshotFolder() throws Exception {
        File serverConfig = new File(BASE_PATH+File.separatorChar+SERVER_CONFIG);
        Assert.assertTrue(serverConfig.exists());
        targetDirectory = new File(COPY_PATH);
        if(!targetDirectory.isDirectory()){
            targetDirectory.mkdirs();
        }
        copyConfigFile = new File(COPY_PATH+File.separatorChar+SERVER_CONFIG);
        copyFile(serverConfig, copyConfigFile);
        try{
            container.start(SERVER_CONFIG, Server.StartMode.ADMIN_ONLY);
            Assert.assertTrue(container.isStarted());

        }catch (RuntimeException e){
            if(e.getMessage().contains("Could not start container")){
                Assert.fail("Server could not start because probably https://issues.jboss.org/browse/JBEAP-14107");
            }else{
                throw e;
            }
        }
    }
    @After
    public void cleanUp() throws Exception {
        if (container.isStarted()) {
            container.stop();
            if(container.isStarted()){
                throw new Exception("Could not stop container");
            }
        }
        if(targetDirectory != null){
            if(copyConfigFile != null && copyConfigFile.exists()){
                copyConfigFile.delete();
            }
            targetDirectory.delete();
        }
    }


}
