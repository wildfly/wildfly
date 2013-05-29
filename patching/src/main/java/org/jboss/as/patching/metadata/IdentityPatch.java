package org.jboss.as.patching.metadata;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author Emanuel Muckenhuber
 */
public interface IdentityPatch extends Patch {

    public enum LayerType {

        Layer("layer"),
        AddOn("add-on"),
        ;

        private final String name;
        private LayerType(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    /**
     * Get the patch elements.
     *
     * @return the patch elements
     */
    Collection<PatchElement> getPatchElements();

    public interface PatchElement {

        /**
         * Get the unique patch ID.
         *
         * @return the patch id
         */
        String getPatchId();

        // The applies to @guarded by the identity
        String getLayerName();
        LayerType getLayerType();

        // maybe some requires

        /**
         * Get the content modifications.
         *
         * @return the modifications
         */
        List<ContentModification> getModifications();

    }

    public class Wrapper {


        public static IdentityPatch wrap(final Patch patch) {
            return new IdentityPatch() {
                @Override
                public Collection<PatchElement> getPatchElements() {
                    return Collections.emptyList();
                }

                @Override
                public String getPatchId() {
                    return patch.getPatchId();
                }

                @Override
                public String getDescription() {
                    return patch.getDescription();
                }

                @Override
                public PatchType getPatchType() {
                    return patch.getPatchType();
                }

                @Override
                public String getResultingVersion() {
                    return patch.getResultingVersion();
                }

                @Override
                public List<String> getAppliesTo() {
                    return patch.getAppliesTo();
                }

                @Override
                public List<ContentModification> getModifications() {
                    return patch.getModifications();
                }
            };

        }


    }

}
