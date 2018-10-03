package net.ossindex.gradle.audit;

import net.ossindex.common.OssiPackage;
import net.ossindex.common.OssiVulnerability;
import org.gradle.internal.impldep.com.google.gson.annotations.SerializedName;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import java.util.List;
import java.util.Objects;

public class MavenPackageDescriptor extends MavenIdWrapper {

    private MavenIdWrapper parent;

    @XmlElement(name = "unfiltered-vulnerability-count")
    @SerializedName("unfiltered-vulnerability-count")
    private int unfilteredVulnerabilityCount;

    @XmlElement(name = "vulnerability-matches")
    @SerializedName("vulnerability-matches")
    private int vulnerabilityMatches;

    @XmlElementWrapper(name = "vulnerabilities")
    @XmlElement(name = "vulnerability")
    private List<OssiVulnerability> vulnerabilities;

    /**
     * Constructor required by jaxb
     */
    public MavenPackageDescriptor() {

    }

    public MavenPackageDescriptor(OssiPackage pkg) {
        groupId = pkg.getNamespace();
        artifactId = pkg.getName();
        version = pkg.getVersion();
        vulnerabilities = pkg.getVulnerabilities();
        unfilteredVulnerabilityCount = pkg.getUnfilteredVulnerabilityMatches();
        vulnerabilityMatches = vulnerabilities.size();
    }

    public void setParent(MavenIdWrapper parent) {
        this.parent = parent;
    }

    public MavenIdWrapper getParent() {
        return parent;
    }

    /**
     * Get the number of vulnerabilities matching the supplied version, prior to any exclusions and filtering.
     *
     * @return Total number of vulnerabilities.
     */
    public int getAllVulnerabilityCount() {
        return unfilteredVulnerabilityCount;
    }

    /**
     * Get the total number of vulnerabilities matching the supplied version.
     *
     * @return Number of matching vulnerabilities
     */
    public int getVulnerabilityMatches() {
        return vulnerabilityMatches;
    }

    /**
     * Get vulnerabilities belonging to this package.
     *
     * @return all vulnerabilities
     */
    public List<OssiVulnerability> getVulnerabilities() {
        return vulnerabilities;
    }

    @Override
    public int hashCode() {return 31 * super.hashCode() + Objects.hash(parent);}

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        if (!super.equals(obj)) {
            return false;
        }
        final MavenPackageDescriptor other = (MavenPackageDescriptor) obj;
        return Objects.equals(this.parent, other.parent);
    }
}
