package ch.admin.bit.jeap.messaging.registry.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DescriptorDto {
    List<VersionDto> versions;
}
