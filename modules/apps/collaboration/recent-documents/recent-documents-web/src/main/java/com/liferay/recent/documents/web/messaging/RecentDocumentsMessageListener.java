/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.recent.documents.web.messaging;

import com.liferay.bnd.util.ConfigurableUtil;
import com.liferay.document.library.kernel.service.DLFileRankLocalService;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.messaging.BaseSchedulerEntryMessageListener;
import com.liferay.portal.kernel.messaging.DestinationNames;
import com.liferay.portal.kernel.messaging.Message;
import com.liferay.portal.kernel.module.framework.ModuleServiceLifecycle;
import com.liferay.portal.kernel.scheduler.SchedulerEngineHelper;
import com.liferay.portal.kernel.scheduler.TimeUnit;
import com.liferay.portal.kernel.scheduler.TriggerFactory;
import com.liferay.portal.kernel.scheduler.TriggerFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.Props;
import com.liferay.portal.kernel.util.PropsKeys;
import com.liferay.recent.documents.web.configuration.RecentDocumentsConfiguration;

import java.util.Map;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Peter Fellwock
 */
@Component(
	configurationPid = "com.liferay.recent.documents.web.configuration.RecentDocumentsConfiguration",
	configurationPolicy = ConfigurationPolicy.OPTIONAL, immediate = true,
	service = RecentDocumentsMessageListener.class
)
public class RecentDocumentsMessageListener
	extends BaseSchedulerEntryMessageListener {

	@Activate
	protected void activate(Map<String, Object> properties) {
		if (!GetterUtil.getBoolean(
				_props.get(PropsKeys.DL_FILE_RANK_ENABLED))) {

			if (_log.isDebugEnabled()) {
				_log.debug(
					"Skipping because the portal property " +
						"\"dl.file.rank.enabled being\" is set to false");
			}

			return;
		}

		RecentDocumentsConfiguration recentDocumentsConfiguration =
			ConfigurableUtil.createConfigurable(
				RecentDocumentsConfiguration.class, properties);

		schedulerEntryImpl.setTrigger(
			TriggerFactoryUtil.createTrigger(
				getEventListenerClass(), getEventListenerClass(),
				recentDocumentsConfiguration.checkFileRanksInterval(),
				TimeUnit.MINUTE));

		_schedulerEngineHelper.register(
			this, schedulerEntryImpl, DestinationNames.SCHEDULER_DISPATCH);
	}

	@Deactivate
	protected void deactivate() {
		_schedulerEngineHelper.unregister(this);
	}

	@Override
	protected void doReceive(Message message) throws Exception {
		_dLFileRankLocalService.checkFileRanks();
	}

	@Modified
	protected void modified(Map<String, Object> properties) {
		deactivate();

		activate(properties);
	}

	@Reference(target = ModuleServiceLifecycle.PORTAL_INITIALIZED, unbind = "-")
	protected void setModuleServiceLifecycle(
		ModuleServiceLifecycle moduleServiceLifecycle) {
	}

	@Reference(unbind = "-")
	protected void setTriggerFactory(TriggerFactory triggerFactory) {
	}

	private static final Log _log = LogFactoryUtil.getLog(
		RecentDocumentsMessageListener.class);

	@Reference
	private DLFileRankLocalService _dLFileRankLocalService;

	@Reference
	private Props _props;

	@Reference
	private SchedulerEngineHelper _schedulerEngineHelper;

}