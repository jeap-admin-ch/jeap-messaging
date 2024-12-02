package ch.admin.bit.jeap.messaging.avro.plugin.helper;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RegistryHelperTest {

    @Test
    void retrieveVersionFromCommonDefinition_matches_returnsVersion(){
        final String version = RegistryHelper.retrieveVersionFromCommonDefinition("test.v12345.test");
        assertEquals("12345", version);
    }

    @Test
    void convertFileNameOfAVDLToFilePathOfJava_fileName_returnsFilePath() {
        String filepath = RegistryHelper.convertFileNameOfAVDLToFilePathOfJava("test.abc.def.file.avdl");
        assertEquals("test/abc/def/file.java", filepath);
    }


}
