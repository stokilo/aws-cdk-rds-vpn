package com.sstec.cdk.rdsvpn;

import com.sstec.cdk.rdsvpn.stacks.*;
import software.amazon.awscdk.core.App;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import software.amazon.awscdk.core.Environment;
import software.amazon.awscdk.core.StackProps;

import static org.assertj.core.api.Assertions.assertThat;

public class RdsVpnTest {
    private final static ObjectMapper JSON =
        new ObjectMapper().configure(SerializationFeature.INDENT_OUTPUT, true);

    @Test
    public void testStack() throws Exception {
        App app = new App();
//        MultiStageStackProps devStackProps = new MultiStageStackProps();

//        S3Stack stack = new S3Stack(app, "test-dev-stack", devStackProps);

//        JsonNode actual = JSON.valueToTree(app.synth().getStackArtifact(stack.getArtifactId()).getTemplate());

//        assertThat(new ObjectMapper().createObjectNode()).isEqualTo(actual);
    }
}
