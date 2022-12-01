package io.gingersnapproject.hotrod;

import java.util.Arrays;

/**
 * The various Hot Rod versions Gingersnap supports
 *
 * @since 0.1
 */

public enum HotRodVersion {
   UNKNOWN(0, 0),
   HOTROD_30(3, 0), // since ISPN 10.0
   HOTROD_31(3, 1), // since ISPN 12.0
   HOTROD_40(4, 0), // since ISPN 14.0
   ;

   private final int major;
   private final int minor;
   private final byte version;
   private final String text;

   HotRodVersion(int major, int minor) {
      this.major = major;
      this.minor = minor;
      this.version = (byte) (major * 10 + minor);
      this.text = version > 0 ? String.format("HOTROD/%d.%d", major, minor) : "UNKNOWN";
   }

   public byte getVersion() {
      return version;
   }

   /**
    * Checks whether the supplied version is older than the version represented by this object
    * @param version a Hot Rod version in its wire representation
    * @return true if version is older than this
    */
   public boolean isOlder(byte version) {
      return this.version > version;
   }

   /**
    * Checks whether the supplied version is equal or greater than the version represented by this object
    * @param version a Hot Rod version in its wire representation
    * @return true if version is equal or greater than this
    */
   public boolean isAtLeast(byte version) {
      return this.version <= version;
   }

   public String toString() {
      return text;
   }

   public static final HotRodVersion LATEST;
   private static final HotRodVersion[] VERSIONS = new HotRodVersion[256];

   static {
      LATEST = values()[values().length - 1];
      Arrays.fill(VERSIONS, UNKNOWN);
      for(HotRodVersion version : values()) {
         VERSIONS[version.version] = version;
      }
   }

   public static HotRodVersion forVersion(byte version) {
      return VERSIONS[version];
   }
}
