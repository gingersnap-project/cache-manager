package io.gingersnapproject.infinispan.runtime.graal;

import org.infinispan.commons.configuration.io.ConfigurationReader;
import org.infinispan.configuration.parsing.ConfigurationBuilderHolder;
import org.infinispan.configuration.parsing.Parser;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

public class SubstituteJGroups {
}

@TargetClass(Parser.class)
final class SubstituteParser {

   @Substitute
   private void addJGroupsDefaultStacksIfNeeded(final ConfigurationReader reader, final ConfigurationBuilderHolder holder) {
   }
}
