package net.ossindex.common.request;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PackageRequestDto
{
  private String[] coordinates;

  private PackageRequestDto(final Builder builder) {
    setCoordinates(builder.coordinates.toArray(new String[builder.coordinates.size()]));
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public static Builder newBuilder(final PackageRequestDto copy) {
    Builder builder = new Builder();
    builder.coordinates = Arrays.asList(copy.coordinates);
    return builder;
  }

  public String[] getCoordinates() {
    return coordinates;
  }

  private void setCoordinates(final String[] coordinates) {
    this.coordinates = coordinates;
  }

  public static final class Builder
  {
    private List<String> coordinates = new ArrayList<>();

    private Builder() {}

    public Builder withCoordinate(final String val) {
      coordinates.add(val);
      return this;
    }

    public PackageRequestDto build() {
      return new PackageRequestDto(this);
    }

    public int size() {
      return coordinates.size();
    }
  }
}
