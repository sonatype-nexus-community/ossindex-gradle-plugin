package net.ossindex.common;

import java.util.Objects;

import net.ossindex.version.IVersionRange;
import net.ossindex.version.InvalidRangeException;
import net.ossindex.version.VersionFactory;

public class PackageCoordinate
{
  public final String type;

  public final String namespace;

  public final String name;

  public final String version;

  private PackageCoordinate(final Builder builder) {
    type = builder.format;
    namespace = builder.namespace;
    name = builder.name;
    version = builder.version;
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public static Builder newBuilder(final PackageCoordinate copy) {
    Builder builder = new Builder();
    builder.format = copy.type;
    builder.namespace = copy.namespace;
    builder.name = copy.name;
    builder.version = copy.version;
    return builder;
  }

  public String getType() {
    return type;
  }

  public String getNamespace() {
    return namespace;
  }

  public String getName() {
    return name;
  }

  public String getVersion() {
    return version;
  }

  @Override
  public int hashCode() {
    // DIRTY HACK: Don't include version in the hash because we want to be forced to compare coordinates with
    // different versions, cause if one coordinate specifies a version and the other does not then they are equivalent.
    // We also need to use some custom version comparison code, since one will be a range, and the other a specific
    // version and we will consider them equal if they overlap. This is a dirty misuse of the equals method.
    int hashcode = Objects.hash(type, namespace, name);
    return hashcode;
  }

  /**
   * For our purposes we will allow coordinates to be equal if they overlap, even though technically they are different
   * objects. This is a bit of a dirty hack to be able to force set to our will.
   */
  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final PackageCoordinate other = (PackageCoordinate) obj;
    if (Objects.equals(this.type, other.type)
        && Objects.equals(this.namespace, other.namespace)
        && Objects.equals(this.name, other.name)) {
      // Do versions overlap
      if (other.version == null || this.version == null) {
        return true;
      }
      try {
        IVersionRange orange = VersionFactory.getVersionFactory().getRange(other.version);
        IVersionRange range = VersionFactory.getVersionFactory().getRange(this.version);
        if (orange.intersects(range)) {
          return true;
        }
      }
      catch (InvalidRangeException e) {
        e.printStackTrace();
      }
    }

    return false;
  }

  @Override
  public String toString() {
    return "PackageCoordinate{" +
        "type='" + type + '\'' +
        ", namespace='" + namespace + '\'' +
        ", name='" + name + '\'' +
        ", version='" + version + '\'' +
        '}';
  }

  public static final class Builder
  {
    private String format;

    private String namespace;

    private String name;

    private String version;

    private Builder() {}

    public Builder withFormat(final String val) {
      format = val == null || val.trim().isEmpty() ? null : val;
      return this;
    }

    public Builder withNamespace(final String val) {
      namespace = val == null || val.trim().isEmpty() ? null : val;
      return this;
    }

    public Builder withName(final String val) {
      name = val == null || val.trim().isEmpty() ? null : val;
      return this;
    }

    public Builder withVersion(final String val) {
      version = val == null || val.trim().isEmpty() ? null : val;
      return this;
    }

    public PackageCoordinate build() {
      return new PackageCoordinate(this);
    }
  }
}
