package ch.admin.bit.jeap.messaging.avro.plugin.git;

import lombok.Value;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.eclipse.jgit.lib.Ref;

@Value
public class RefComparableVersion implements Comparable<RefComparableVersion> {

    Ref ref;

    ComparableVersion comparableVersion;

    public RefComparableVersion(Ref ref) {
        this.ref = ref;
        this.comparableVersion = new ComparableVersion(ref.getName());
    }

    @Override
    public int compareTo(RefComparableVersion other) {
        return this.getComparableVersion().compareTo(other.getComparableVersion());
    }
}
