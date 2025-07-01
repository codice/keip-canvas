package org.codice.keip.schema.model.eip;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public record Occurrence(long min, long max) {
  public static final long DEFAULT_MIN = 1L;
  public static final long DEFAULT_MAX = 1L;
  public static final Occurrence DEFAULT = new Occurrence(DEFAULT_MIN, DEFAULT_MAX);

  public static final long UNBOUNDED = Long.MAX_VALUE;

  public boolean isDefault() {
    return DEFAULT.equals(this);
  }

  /** Only includes a bound if it differs from defaults. */
  public Map<String, Long> toMinimalMap() {
    Map<String, Long> map = new HashMap<>();
    if (min != DEFAULT_MIN) {
      map.put("min", min);
    }
    if (max != DEFAULT_MAX) {
      long maxVal = max == UNBOUNDED ? -1L : max;
      map.put("max", maxVal);
    }
    return Collections.unmodifiableMap(map);
  }
}
