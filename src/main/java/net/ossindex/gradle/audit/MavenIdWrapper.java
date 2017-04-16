package net.ossindex.gradle.audit;

import net.ossindex.common.PackageDescriptor;

public class MavenIdWrapper {

    protected String groupId;
    protected String artifactId;
    protected String version;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MavenIdWrapper that = (MavenIdWrapper) o;

        return getMavenVersionId() != null ? getMavenVersionId().equals(that.getMavenVersionId()) : that.getMavenVersionId() == null;
    }

    @Override
    public int hashCode() {
        return getMavenVersionId() != null ? getMavenVersionId().hashCode() : 0;
    }

    /**
     * Required for serialization
     */
    public MavenIdWrapper() {

    }

    public MavenIdWrapper(PackageDescriptor pkg) {
        this.setGroupId(pkg.getGroup());
        this.setArtifactId(pkg.getName());
        this.setVersion(pkg.getVersion());
    }

    /**
     * @return the groupId
     */
    public String getGroupId() {
        return groupId;
    }

    /**
     * @param groupId the groupId to set
     */
    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    /**
     * @return the artifactId
     */
    public String getArtifactId() {
        return artifactId;
    }

    /**
     * @param artifactId the artifactId to set
     */
    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    /**
     * @return the version
     */
    public String getVersion() {
        return version;
    }

    /**
     * @param version the version to set
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * Get the Maven ID excluding the version
     *
     * @return the Maven ID
     */
    public String getMavenPackageId() {
        StringBuilder sb = new StringBuilder();
        if (groupId != null) {
            sb.append(groupId);
        }
        sb.append(":");
        if (artifactId != null) {
            sb.append(artifactId);
        }
        return sb.toString();
    }

    /**
     * Get the maven ID including the version
     *
     * @return the maven ID
     */
    public String getMavenVersionId() {
        StringBuilder sb = new StringBuilder();
        if (groupId != null) {
            sb.append(groupId);
        }
        sb.append(":");
        if (artifactId != null) {
            sb.append(artifactId);
        }
        sb.append(":");
        if (version != null) {
            sb.append(version);
        }
        return sb.toString();
    }
}
