package io.gingersnapproject.profile;

import static io.gingersnapproject.profile.IntegrationNativeProfile.INTEGRATION_NATIVE;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.Tag;


@Tag(INTEGRATION_NATIVE)
@TestProfile(IntegrationNativeProfile.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface GingersnapIntegrationTest { }
