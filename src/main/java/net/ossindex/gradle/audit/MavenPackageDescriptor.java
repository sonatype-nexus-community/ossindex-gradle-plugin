package net.ossindex.gradle.audit;

import net.ossindex.common.PackageDescriptor;
import net.ossindex.common.VulnerabilityDescriptor;
import org.gradle.internal.impldep.com.google.gson.annotations.SerializedName;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import java.util.List;

public class MavenPackageDescriptor extends MavenIdWrapper {

    private MavenIdWrapper parent;

    @XmlElement(name = "vulnerability-total")
    @SerializedName("vulnerability-total")
    private int vulnerabilityTotal;

    @XmlElement(name = "vulnerability-matches")
    @SerializedName("vulnerability-matches")
    private int vulnerabilityMatches;

    @XmlElementWrapper(name = "vulnerabilities")
    @XmlElement(name = "vulnerability")
    private List<VulnerabilityDescriptor> vulnerabilities;

    /**
     * Constructor required by jaxb
     */
    public MavenPackageDescriptor() {

    }

    public MavenPackageDescriptor(PackageDescriptor pkg) {
        groupId = pkg.getGroup();
        artifactId = pkg.getName();
        version = pkg.getVersion();
        vulnerabilityTotal = pkg.getVulnerabilityTotal();
        vulnerabilityMatches = pkg.getVulnerabilityMatches();
        vulnerabilities = pkg.getVulnerabilities();
    }

    public void setParent(MavenIdWrapper parent) {
        this.parent = parent;
    }

    public MavenIdWrapper getParent() {
        return parent;
    }

    /**
     * Get the total number of vulnerabilities for the package identified on the server.
     *
     * @return Total number of vulnerabilities.
     */
    public int getVulnerabilityTotal() {
        return vulnerabilityTotal;
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
     */
    public List<VulnerabilityDescriptor> getVulnerabilities() {
        return vulnerabilities;
    }

}
