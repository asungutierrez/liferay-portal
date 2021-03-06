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

package com.liferay.notifications.web.upgrade.v2_0_0;

import com.liferay.portal.kernel.model.UserNotificationEvent;
import com.liferay.portal.kernel.service.UserNotificationEventLocalService;
import com.liferay.portal.kernel.upgrade.UpgradeProcess;
import com.liferay.portal.kernel.util.LoggingTimer;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * @author Sergio González
 */
public class UpgradeUserNotificationEvent extends UpgradeProcess {

	public UpgradeUserNotificationEvent(
		UserNotificationEventLocalService userNotificationEventLocalService) {

		_userNotificationEventLocalService = userNotificationEventLocalService;
	}

	@Override
	protected void doUpgrade() throws Exception {
		if (!hasTable("Notifications_UserNotificationEvent")) {
			return;
		}

		updateUserNotificationEvents();
	}

	protected void updateUserNotificationEvents() throws Exception {
		try (LoggingTimer loggingTimer = new LoggingTimer();
			PreparedStatement ps = connection.prepareStatement(
				"select userNotificationEventId, actionRequired from " +
					"Notifications_UserNotificationEvent");
			ResultSet rs = ps.executeQuery()) {

			while (rs.next()) {
				long userNotificationEventId = rs.getLong(
					"userNotificationEventId");
				boolean actionRequired = rs.getBoolean("actionRequired");

				UserNotificationEvent userNotificationEvent =
					_userNotificationEventLocalService.getUserNotificationEvent(
						userNotificationEventId);

				userNotificationEvent.setActionRequired(actionRequired);

				_userNotificationEventLocalService.updateUserNotificationEvent(
					userNotificationEvent);
			}
		}
	}

	private final UserNotificationEventLocalService
		_userNotificationEventLocalService;

}