/***************************************************************************

Copyright © 2016, EPAM SYSTEMS INC

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

****************************************************************************/

package com.epam.dlab.backendapi.core.response.warmup;

import com.epam.dlab.backendapi.ProvisioningServiceApplicationConfiguration;
import com.epam.dlab.backendapi.core.CommandExecutor;
import com.epam.dlab.backendapi.core.response.folderlistener.FolderListenerExecutor;
import com.epam.dlab.dto.imagemetadata.*;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import io.dropwizard.util.Duration;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;

import static junit.framework.TestCase.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DockerWarmuperTest {
    @Inject
    private DockerWarmuper warmuper;
    private ExploratoryMetadataDTO exploratoryMetadata = new ExploratoryMetadataDTO(
            "executeResult");
    private ComputationalMetadataDTO computationalMetadata = new
            ComputationalMetadataDTO("executeResult");
    private static final String EXPLORATORY_TEST_JSON = "{\"exploratory_environment_shapes\" : [ {\"Type\":\"cg1.4xlarge\",\"Ram\": \"22.5 GB\",\"Cpu\": \"16\"}]}";
    private static final String COMPUTATIONAL_TEST_JSON = "{\"template_name\":\"DLab AWS EMR\"}";

    @Before
    public void setup() {
        createInjector().injectMembers(this);
        ComputationalResourceShapeDto computationalResourceShapeDto = new ComputationalResourceShapeDto();
        computationalResourceShapeDto.setType("cg1.4xlarge");
        computationalResourceShapeDto.setRam("22.5 GB");
        computationalResourceShapeDto.setCpu(16);
        ArrayList<ComputationalResourceShapeDto> metadataList = new ArrayList<>();
        metadataList.add(computationalResourceShapeDto);
        exploratoryMetadata.setExploratoryEnvironmentShapes(metadataList);
        computationalMetadata.setTemplateName("DLab AWS EMR");
    }

    @Test
    public void warmupSuccess() throws Exception {
        warmuper.start();
        warmuper.getFileHandlerCallback()
                .handle(getFileName(), EXPLORATORY_TEST_JSON.getBytes());
        warmuper.getFileHandlerCallback()
                .handle(getFileName(), COMPUTATIONAL_TEST_JSON.getBytes());
        assertEquals(exploratoryMetadata, warmuper.getMetadatas(ImageType.EXPLORATORY)
                .toArray(new ImageMetadataDTO[1])[0]);
        assertEquals(computationalMetadata, warmuper.getMetadatas(ImageType.COMPUTATIONAL)
                .toArray(new ImageMetadataDTO[1])[0]);
    }

    private String getFileName() {
        return warmuper.getUuids().keySet().toArray(new String[1])[0] + ".json";
    }

    private Injector createInjector() {
        return Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(FolderListenerExecutor.class).toInstance(mock(FolderListenerExecutor.class));
                bind(ProvisioningServiceApplicationConfiguration.class).toInstance(createConfiguration());
                bind(CommandExecutor.class).toInstance(createCommandExecuter());
            }
        });
    }

    private ProvisioningServiceApplicationConfiguration createConfiguration() {
        ProvisioningServiceApplicationConfiguration result = mock(ProvisioningServiceApplicationConfiguration.class);
        when(result.getWarmupDirectory()).thenReturn("/tmp");
        when(result.getWarmupPollTimeout()).thenReturn(Duration.seconds(3));
        return result;
    }

    private CommandExecutor createCommandExecuter() {
        CommandExecutor result = mock(CommandExecutor.class);
        try {
            when(result.executeSync(anyString())).thenReturn(Collections.singletonList("executeResult"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
}
