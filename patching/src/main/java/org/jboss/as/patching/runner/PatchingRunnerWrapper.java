package org.jboss.as.patching.runner;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jboss.as.patching.DirectoryStructure;
import org.jboss.as.patching.PatchInfo;
import org.jboss.as.patching.installation.AddOn;
import org.jboss.as.patching.installation.Identity;
import org.jboss.as.patching.installation.InstalledIdentity;
import org.jboss.as.patching.installation.Layer;

/**
 * @author Emanuel Muckenhuber
 */
public interface PatchingRunnerWrapper {

    PatchingResult executeDirect(InputStream content) throws PatchingException;
    PatchingResult executeDirect(InputStream content, ContentVerificationPolicy policy) throws PatchingException;
    PatchingResult rollback(String patchId, ContentVerificationPolicy contentPolicy, boolean rollbackTo, boolean restoreConfiguration) throws PatchingException;


    static class Factory {

        private Factory() {
            //
        }

        public static PatchingRunnerWrapper create(final PatchInfo patchInfo, final DirectoryStructure structure) {
            final InstalledIdentity identity = new InstalledIdentity() {
                @Override
                public List<Layer> getLayers() {
                    return Collections.emptyList();
                }

                @Override
                public Identity getIdentity() {
                    return new Identity() {
                        @Override
                        public String getName() {
                            return "test";
                        }

                        @Override
                        public String getVersion() {
                            return patchInfo.getVersion();
                        }

                        @Override
                        public TargetInfo loadTargetInfo() throws IOException {
                            return new TargetInfo() {
                                @Override
                                public String getCumulativeID() {
                                    return patchInfo.getCumulativeID();
                                }

                                @Override
                                public List<String> getPatchIDs() {
                                    return patchInfo.getPatchIDs();
                                }

                                @Override
                                public DirectoryStructure getDirectoryStructure() {
                                    return structure;
                                }
                            };
                        }
                    };
                }

                @Override
                public Collection<AddOn> getAddOns() {
                    return Collections.emptyList();
                }
            };
            return new LegacyPatchRunner(structure.getInstalledImage(), identity);
        }


    }





}
