package io.gingersnapproject.infinispan.runtime.graal;

import java.util.ArrayList;
import java.util.List;

import org.infinispan.marshall.protostream.impl.SerializationContextRegistry;
import org.infinispan.marshall.protostream.impl.SerializationContextRegistryImpl;
import org.infinispan.protostream.BaseMarshaller;
import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.SerializationContextInitializer;

import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

public class SubstituteSerializationContextRegistry {
}

@TargetClass(SerializationContextRegistryImpl.class)
final class Substitute_SerializationContextRegistryImpl {

   @Substitute
   void addProtoFile(SerializationContextRegistry.MarshallerType type, FileDescriptorSource fileDescriptorSource) {}

   @Substitute
   void removeProtoFile(SerializationContextRegistry.MarshallerType type, String fileName) {}

   @Substitute
   void addMarshaller(SerializationContextRegistry.MarshallerType type, BaseMarshaller marshaller) {}

   @Substitute
   void addContextInitializer(SerializationContextRegistry.MarshallerType type, SerializationContextInitializer sci) {}
}

@Substitute
@TargetClass(className = "org.infinispan.marshall.protostream.impl.SerializationContextRegistryImpl$MarshallerContext")
final class Substitute_SerializationContextRegistryImpl_MarshallerContext {

   @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.Reset)
   private final SerializationContext ctx = null;

   @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.Reset)
   private final List<BaseMarshaller<?>> marshallers = new ArrayList<>();

   @Substitute
   public Substitute_SerializationContextRegistryImpl_MarshallerContext() {
   }

   @Substitute
   Substitute_SerializationContextRegistryImpl_MarshallerContext addContextInitializer(SerializationContextInitializer sci) {
      return this;
   }

   @Substitute
   Substitute_SerializationContextRegistryImpl_MarshallerContext addProtoFile(FileDescriptorSource fileDescriptorSource) {
      return this;
   }

   @Substitute
   Substitute_SerializationContextRegistryImpl_MarshallerContext removeProtoFile(String fileName) {
      return this;
   }
   @Substitute
   Substitute_SerializationContextRegistryImpl_MarshallerContext addMarshaller(BaseMarshaller marshaller) {
      return this;
   }
}
