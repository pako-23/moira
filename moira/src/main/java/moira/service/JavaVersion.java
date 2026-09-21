package moira.service;

public class JavaVersion {
  private JavaVersion() {}

  public static int version() {
    String version = System.getProperty("java.version");

    if (version.startsWith("1.")) version = version.substring(2, 3);
    else version = version.substring(0, version.indexOf("."));

    return Integer.parseInt(version);
  }
}
