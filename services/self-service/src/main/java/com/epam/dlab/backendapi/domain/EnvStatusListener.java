/***************************************************************************

Copyright (c) 2016, EPAM SYSTEMS INC

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


package com.epam.dlab.backendapi.domain;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.dao.SettingsDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epam.dlab.backendapi.SelfServiceApplicationConfiguration;
import com.epam.dlab.backendapi.dao.EnvStatusDAO;
import com.epam.dlab.constants.ServiceConsts;
import com.epam.dlab.dto.status.EnvResourceList;
import com.epam.dlab.rest.client.RESTService;
import com.epam.dlab.rest.contracts.InfrasctructureAPI;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import io.dropwizard.lifecycle.Managed;

/** Send requests to the docker for update the status of environment.
 */
@Singleton
public class EnvStatusListener implements Managed, Runnable {
	private static final Logger LOGGER = LoggerFactory.getLogger(EnvStatusListener.class);
	@Inject
	private static SettingsDAO settingsDAO;

	/** Environment status listener. */
	private static EnvStatusListener listener;

	public static void setListener(EnvStatusListener listener) {
		EnvStatusListener.listener = listener;
	}

	/**
	 * Append the status checker for user to environment status listener.
	 * @param userInfo authentication info of the current user
	 */
	public static synchronized void listen(UserInfo userInfo) {
		if (listener.userMap.containsKey(userInfo.getName())) {
			LOGGER.debug("EnvStatus listener the status checker for user {} already exist", userInfo.getName());
			return;
		}
		LOGGER.debug("EnvStatus listener will be added the status checker for user {}", userInfo.getName());
		EnvStatusListenerUserInfo listenerUserInfo = new EnvStatusListenerUserInfo(userInfo);
		listener.userMap.put(listenerUserInfo.getUsername(), listenerUserInfo);
		if (listener.thread == null) {
			LOGGER.info("EnvStatus listener not running and will be started ...");
			listener.thread = new Thread(listener, listener.getClass().getSimpleName());
			listener.thread.start();
		}
	}

	/** Remove the status checker for user from environment status listener.
	 * @param username the name of user.
	 */
	public static void listenStop(String username) {
		LOGGER.debug("EnvStatus listener will be removed the status checker for user {}", username);
		synchronized (listener.userMap) {
			listener.userMap.remove(username);
			if (listener.userMap.size() == 0) {
				LOGGER.info("EnvStatus listener will be terminated because no have the status checkers anymore");
				try {
					listener.stop();
				} catch (Exception e) {
					LOGGER.warn("EnvStatus listener terminating failed: {}", e.getLocalizedMessage(), e);
				}
			}
		}
	}

	/** Return the user info by user name.
	 * @param username the name of user.
	 * @return the user info.
	 */
	public static EnvStatusListenerUserInfo getUserInfo(String username) {
		return (listener == null ? null : listener.userMap.get(username));
	}

	/** Thread of the folder listener. */
	private Thread thread;

	/** Timeout for check the status of environment in milliseconds. */
	private long checkStatusTimeoutMillis;

	@Inject
	private SelfServiceApplicationConfiguration configuration;

	@Inject
	private EnvStatusDAO dao;

    @Inject
    @Named(ServiceConsts.PROVISIONING_SERVICE_NAME)
    private RESTService provisioningService;

    /** Map of users for the checker. */
	private Map<String, EnvStatusListenerUserInfo> userMap = new HashMap<>();

	@Override
	public void start() throws Exception {
		if (listener == null) {
			setListener(this);
			checkStatusTimeoutMillis = configuration
					.getCheckEnvStatusTimeout()
					.toMilliseconds();
		}
	}

	@Override
	public void stop() throws Exception {
		if (listener.thread != null) {
			LOGGER.debug("EnvStatus listener will be stopped ...");
			synchronized (listener.thread) {
				listener.thread.interrupt();
				listener.thread = null;
				listener.userMap.clear();
			}
			LOGGER.info("EnvStatus listener has been stopped");
		}
	}

	@Override
	public void run() {
		while (true) {
			try {
				long ticks = System.currentTimeMillis();
				for (Entry<String, EnvStatusListenerUserInfo> item : userMap.entrySet()) {
					EnvStatusListenerUserInfo userInfo = item.getValue();
					if (userInfo.getNextCheckTimeMillis() < ticks) {
						userInfo.setNextCheckTimeMillis(ticks + checkStatusTimeoutMillis);
						checkStatus(userInfo);
					}
				}

				Thread.sleep(1000);
			} catch (InterruptedException e) {
				LOGGER.trace("EnvStatus listener has been interrupted");
				break;
			} catch (Exception e) {
				LOGGER.warn("EnvStatus listener unhandled error: {}", e.getLocalizedMessage(), e);
			}
		}
	}

	/** Send request to docker for check the status of user environment.
	 * @param userInfo user info.
	 */
	private void checkStatus(EnvStatusListenerUserInfo userInfo) {
		try {
			EnvResourceList resourceList = dao.findEnvResources(userInfo.getUsername());
			LOGGER.trace("EnvStatus listener check status for user {} with resource list {}", userInfo.getUsername(), resourceList);
			if (resourceList.getHostList() != null || resourceList.getClusterList() != null) {
				userInfo.getDTO().withResourceList(resourceList);
				LOGGER.trace("Ask docker for the status of resources for user {}: {}", userInfo.getUsername(), userInfo.getDTO());
				String uuid = provisioningService.post(InfrasctructureAPI.INFRASTRUCTURE_STATUS, userInfo.getAccessToken(), userInfo.getDTO(), String.class);
				RequestId.put(userInfo.getUsername(), uuid);
			}
		} catch (Exception e) {
			LOGGER.warn("Ask docker for the status of resources for user {} fails: {}", e.getLocalizedMessage(), e);
		}
	}
}
