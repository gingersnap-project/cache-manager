package io.gingersnapproject.infinispan.runtime.graal;

import org.infinispan.commands.RemoteCommandsFactory;
import org.infinispan.factories.GlobalComponentRegistry;
import org.infinispan.marshall.core.impl.ClassToExternalizerMap;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

public class SubstituteExternalizers {
}

@TargetClass(className = "org.infinispan.marshall.core.InternalExternalizers")
final class Substitute_InternalExternalizers {

   @Substitute
   static ClassToExternalizerMap load(GlobalComponentRegistry gcr, RemoteCommandsFactory cmdFactory) {
      return new ClassToExternalizerMap(1);
   }
}
