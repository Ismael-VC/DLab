/*
 * Copyright (c) 2017, EPAM SYSTEMS INC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.dlab.backendapi.core.response.handlers;

import com.epam.dlab.backendapi.ProvisioningServiceApplicationConfiguration;
import com.epam.dlab.backendapi.core.Directories;
import com.epam.dlab.backendapi.core.FileHandlerCallback;
import com.epam.dlab.backendapi.core.commands.*;
import com.epam.dlab.backendapi.core.response.folderlistener.FolderListenerExecutor;
import com.epam.dlab.dto.aws.computational.ComputationalConfigAws;
import com.epam.dlab.dto.aws.computational.ComputationalCreateAws;
import com.epam.dlab.dto.azure.computational.ComputationalConfigAzure;
import com.epam.dlab.dto.azure.computational.ComputationalCreateAzure;
import com.epam.dlab.dto.base.DataEngineType;
import com.epam.dlab.dto.base.computational.ComputationalBase;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.rest.client.RESTService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import static com.epam.dlab.backendapi.core.commands.DockerAction.CONFIGURE;

@Slf4j
@Singleton
public class ComputationalConfigure implements DockerCommands {
    @Inject
    private ProvisioningServiceApplicationConfiguration configuration;
    @Inject
    private FolderListenerExecutor folderListenerExecutor;
    @Inject
    private ICommandExecutor commandExecutor;
    @Inject
    private CommandBuilder commandBuilder;
    @Inject
    private RESTService selfService;

    private String getComputationalType() {
        switch (configuration.getCloudProvider()) {
            case AWS:
                return DataEngineType.CLOUD_SERVICE.getName();
            case AZURE:
                return DataEngineType.SPARK_STANDALONE.getName();
            default:
                throw new IllegalArgumentException("Unsupported cloud provider");

        }
    }


    public String configure(String uuid, ComputationalBase<?> dto) throws DlabException {
        ComputationalBase<?> dtoConf;


        switch (configuration.getCloudProvider()) {
            case AWS:
                dtoConf = new ComputationalConfigAws();
                configure((ComputationalCreateAws) dto, (ComputationalConfigAws) dtoConf);
                break;
            case AZURE:
                dtoConf = new ComputationalConfigAzure();
                configure((ComputationalCreateAzure) dto, (ComputationalConfigAzure) dtoConf);

        }

        return runConfigure(uuid, dto);
    }

    private void configure(ComputationalCreateAws dto, ComputationalConfigAws config) {
        commonConfigure(dto, config);
        config.withVersion(dto.getVersion());
    }

    private void configure(ComputationalCreateAzure dto, ComputationalConfigAzure config) {
        commonConfigure(dto, config);
        config.withDataEngineInstanceCount(dto.getDataEngineInstanceCount());
    }

    private void commonConfigure(ComputationalBase<?> dto, ComputationalBase<?> dtoConf) {
        dtoConf.withServiceBaseName(dto.getServiceBaseName())
                .withApplicationName(dto.getApplicationName())
                .withExploratoryName(dto.getExploratoryName())
                .withComputationalName(dto.getComputationalName())
                .withNotebookInstanceName(dto.getNotebookInstanceName())
                .withEdgeUserName(dto.getEdgeUserName())
                .withCloudSettings(dto.getCloudSettings());

    }

    private String runConfigure(String uuid, ComputationalBase<?> dto) throws DlabException {
        log.debug("Configure computational resources {} for user {}: {}", dto.getComputationalName(), dto.getEdgeUserName(), dto);
        folderListenerExecutor.start(
                configuration.getImagesDirectory(),
                configuration.getResourceStatusPollTimeout(),
                getFileHandlerCallback(CONFIGURE, uuid, dto));
        try {
            commandExecutor.executeAsync(
                    dto.getEdgeUserName(),
                    uuid,
                    commandBuilder.buildCommand(
                            new RunDockerCommand()
                                    .withInteractive()
                                    .withName(nameContainer(dto.getEdgeUserName(), CONFIGURE, dto.getComputationalName()))
                                    .withVolumeForRootKeys(configuration.getKeyDirectory())
                                    .withVolumeForResponse(configuration.getImagesDirectory())
                                    .withVolumeForLog(configuration.getDockerLogDirectory(), getComputationalType())
                                    .withResource(getComputationalType())
                                    .withRequestId(uuid)
                                    .withConfKeyName(configuration.getAdminKey())
                                    .withActionConfigure(getImageConfigure(dto.getApplicationName())),
                            dto
                    )
            );
        } catch (Throwable t) {
            throw new DlabException("Could not configure computational resource cluster", t);
        }
        return uuid;
    }

    private FileHandlerCallback getFileHandlerCallback(DockerAction action, String originalUuid, ComputationalBase<?> dto) {
        return new ComputationalCallbackHandler(this, selfService, action, originalUuid, dto);
    }

    private String nameContainer(String user, DockerAction action, String name) {
        return nameContainer(user, action.toString(), "computational", name);
    }

    private String getImageConfigure(String application) throws DlabException {
        String imageName = configuration.getDataEngineImage();
        int pos = imageName.indexOf('-');
        if (pos > 0) {
            return imageName.substring(0, pos + 1) + application;
        }
        throw new DlabException("Could not describe the image name for computational resources from image " + imageName + " and application " + application);
    }

    public String getResourceType() {
        return Directories.DATA_ENGINE_SERVICE_LOG_DIRECTORY;
    }
}
