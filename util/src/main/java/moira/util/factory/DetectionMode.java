package moira.util.factory;

public enum DetectionMode {
  TUSCAN_PACKED("tuscan-packed"),
  TUSCAN_CLASS_ONLY("tuscan-class-only"),
  TUSCAN_INTRA_CLASS("tuscan-intra-class"),
  TUSCAN_INTER_CLASS("tuscan-inter-class"),
  TARGET_PAIRS("target-pairs"),
  MOIRA("moira");

  private final String name;

  private DetectionMode(final String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return name;
  }

  public static DetectionMode fromString(final String value) {
    for (final DetectionMode mode : values()) if (mode.name.equals(value)) return mode;

    return null;
  }
}
