package be.cytomine.appengine.dto.inputs.task;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import be.cytomine.appengine.exceptions.BundleArchiveException;

@Slf4j
@Setter
@Getter
@NoArgsConstructor
public class UploadTaskArchive {
    private byte[] descriptorFile;

    private byte[] dockerImage;

    private JsonNode descriptorFileAsJson;

    public UploadTaskArchive(
        byte[] discriptorFile,
        byte[] dockerImage
    ) throws BundleArchiveException {
        this.descriptorFile = discriptorFile;
        this.dockerImage = dockerImage;
        descriptorFileAsJson = convertFromYamlToJson();
    }

    private JsonNode convertFromYamlToJson() throws BundleArchiveException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        JsonNode map;
        try {
            map = mapper.readTree(descriptorFile);
        } catch (IOException e) {
            log.info("UploadTask : failed to convert descriptor.yml to json [{}]", e.getMessage());
            throw new BundleArchiveException(e);
        }
        return map;
    }
}
