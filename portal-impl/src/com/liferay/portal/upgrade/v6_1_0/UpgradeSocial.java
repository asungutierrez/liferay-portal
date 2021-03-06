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

package com.liferay.portal.upgrade.v6_1_0;

import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.security.permission.ActionKeys;
import com.liferay.portal.kernel.upgrade.UpgradeProcess;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.LoggingTimer;
import com.liferay.portal.kernel.util.PortalUtil;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Tuple;
import com.liferay.social.kernel.model.SocialActivityConstants;
import com.liferay.social.kernel.model.SocialActivityCounterConstants;
import com.liferay.social.kernel.model.SocialActivityCounterDefinition;
import com.liferay.social.kernel.util.SocialCounterPeriodUtil;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Zsolt Berentey
 */
public class UpgradeSocial extends UpgradeProcess {

	public UpgradeSocial() {
		putEquityToActivityMap(
			"com.liferay.portlet.blogs.model.BlogsEntry",
			ActionKeys.ADD_DISCUSSION,
			SocialActivityConstants.TYPE_ADD_COMMENT);
		putEquityToActivityMap(
			"com.liferay.portlet.blogs.model.BlogsEntry", ActionKeys.ADD_ENTRY,
			2);
		putEquityToActivityMap(
			"com.liferay.portlet.blogs.model.BlogsEntry", ActionKeys.ADD_VOTE,
			SocialActivityConstants.TYPE_ADD_VOTE);
		putEquityToActivityMap(
			"com.liferay.portlet.blogs.model.BlogsEntry", ActionKeys.SUBSCRIBE,
			SocialActivityConstants.TYPE_SUBSCRIBE);
		putEquityToActivityMap(
			"com.liferay.portlet.blogs.model.BlogsEntry", ActionKeys.UPDATE, 3);
		putEquityToActivityMap(
			"com.liferay.portlet.blogs.model.BlogsEntry", ActionKeys.VIEW,
			SocialActivityConstants.TYPE_VIEW);

		putEquityToActivityMap(
			"com.liferay.portlet.messageboards.model.MBCategory",
			ActionKeys.SUBSCRIBE, SocialActivityConstants.TYPE_SUBSCRIBE);
		putEquityToActivityMap(
			"com.liferay.portlet.messageboards.model.MBMessage",
			ActionKeys.ADD_MESSAGE, 1);
		putEquityToActivityMap(
			"com.liferay.portlet.messageboards.model.MBMessage",
			ActionKeys.ADD_VOTE, SocialActivityConstants.TYPE_ADD_VOTE);
		putEquityToActivityMap(
			"com.liferay.portlet.messageboards.model.MBMessage",
			ActionKeys.REPLY_TO_MESSAGE, 2);
		putEquityToActivityMap(
			"com.liferay.portlet.messageboards.model.MBMessage",
			ActionKeys.VIEW, SocialActivityConstants.TYPE_VIEW);
		putEquityToActivityMap(
			"com.liferay.portlet.messageboards.model.MBThread",
			ActionKeys.SUBSCRIBE,
			"com.liferay.portlet.messageboards.model.MBMessage",
			SocialActivityConstants.TYPE_SUBSCRIBE);

		putEquityToActivityMap(
			"com.liferay.portlet.journal.model.JournalArticle",
			ActionKeys.ADD_ARTICLE, 1);
		putEquityToActivityMap(
			"com.liferay.portlet.journal.model.JournalArticle",
			ActionKeys.ADD_DISCUSSION,
			SocialActivityConstants.TYPE_ADD_COMMENT);
		putEquityToActivityMap(
			"com.liferay.portlet.journal.model.JournalArticle",
			ActionKeys.UPDATE, 2);
		putEquityToActivityMap(
			"com.liferay.portlet.journal.model.JournalArticle", ActionKeys.VIEW,
			SocialActivityConstants.TYPE_VIEW);

		putEquityToActivityMap(
			"com.liferay.portlet.wiki.model.WikiNode", ActionKeys.SUBSCRIBE,
			SocialActivityConstants.TYPE_SUBSCRIBE);
		putEquityToActivityMap(
			"com.liferay.portlet.wiki.model.WikiPage",
			ActionKeys.ADD_ATTACHMENT,
			SocialActivityConstants.TYPE_ADD_ATTACHMENT);
		putEquityToActivityMap(
			"com.liferay.portlet.wiki.model.WikiPage",
			ActionKeys.ADD_DISCUSSION,
			SocialActivityConstants.TYPE_ADD_COMMENT);
		putEquityToActivityMap(
			"com.liferay.portlet.wiki.model.WikiPage", ActionKeys.ADD_PAGE, 1);
		putEquityToActivityMap(
			"com.liferay.portlet.wiki.model.WikiPage", ActionKeys.SUBSCRIBE,
			SocialActivityConstants.TYPE_SUBSCRIBE);
		putEquityToActivityMap(
			"com.liferay.portlet.wiki.model.WikiPage", ActionKeys.UPDATE, 2);
		putEquityToActivityMap(
			"com.liferay.portlet.wiki.model.WikiPage", ActionKeys.VIEW,
			SocialActivityConstants.TYPE_VIEW);
	}

	protected void addActivityCounter(
			long activityCounterId, long groupId, long companyId,
			long classNameId, long classPK, String name, int ownerType,
			int currentValue, int totalValue, int graceValue, int startPeriod,
			int endPeriod)
		throws Exception {

		StringBundler sb = new StringBundler(5);

		sb.append("insert into SocialActivityCounter (activityCounterId, ");
		sb.append("groupId, companyId, classNameId, classPK, name, ");
		sb.append("ownerType, currentValue, totalValue, graceValue, ");
		sb.append("startPeriod, endPeriod) values (?, ?, ?, ?, ?, ?, ?, ");
		sb.append("?, ?, ?, ?, ?)");

		try (PreparedStatement ps = connection.prepareStatement(
				sb.toString())) {

			ps.setLong(1, activityCounterId);
			ps.setLong(2, groupId);
			ps.setLong(3, companyId);
			ps.setLong(4, classNameId);
			ps.setLong(5, classPK);
			ps.setString(6, name);
			ps.setInt(7, ownerType);
			ps.setInt(8, currentValue);
			ps.setInt(9, totalValue);
			ps.setInt(10, graceValue);
			ps.setInt(11, startPeriod);
			ps.setInt(12, endPeriod);

			ps.executeUpdate();
		}
	}

	protected void addActivitySetting(
			long groupId, long companyId, long classNameId, int activityType,
			String name, int ownerType, int limitValue, int value)
		throws Exception {

		JSONObject valueJSONObject = JSONFactoryUtil.createJSONObject();

		valueJSONObject.put("enabled", true);
		valueJSONObject.put(
			"limitPeriod", SocialActivityCounterDefinition.LIMIT_PERIOD_DAY);
		valueJSONObject.put("limitValue", limitValue);
		valueJSONObject.put("ownerType", ownerType);
		valueJSONObject.put("value", value);

		addActivitySetting(
			increment(), groupId, companyId, classNameId, activityType, name,
			valueJSONObject.toString());
	}

	protected void addActivitySetting(
			long activitySettingId, long groupId, long companyId,
			long classNameId, int activityType, String name, String value)
		throws Exception {

		try (PreparedStatement ps = connection.prepareStatement(
				"insert into SocialActivitySetting (activitySettingId, " +
					"groupId, companyId, classNameId, activityType, name, " +
						"value) values (?, ?, ?, ?, ?, ?, ?)")) {

			ps.setLong(1, activitySettingId);
			ps.setLong(2, groupId);
			ps.setLong(3, companyId);
			ps.setLong(4, classNameId);
			ps.setInt(5, activityType);
			ps.setString(6, name);
			ps.setString(7, value);

			ps.executeUpdate();
		}
	}

	@Override
	protected void doUpgrade() throws Exception {
		migrateEquityGroupSettings();
		migrateEquitySettings();
		migrateEquityLogs();

		dropEquityTables();
	}

	protected void dropEquityTables() throws Exception {
		try (LoggingTimer loggingTimer = new LoggingTimer()) {
			runSQL("drop table SocialEquityAssetEntry");
			runSQL("drop table SocialEquityGroupSetting");
			runSQL("drop table SocialEquityHistory");
			runSQL("drop table SocialEquityLog");
			runSQL("drop table SocialEquitySetting");
			runSQL("drop table SocialEquityUser");
		}
	}

	protected String encodeEquityToActivityKey(
		long classNameId, String actionId) {

		StringBundler sb = new StringBundler(3);

		sb.append(classNameId);
		sb.append(StringPool.POUND);
		sb.append(actionId);

		return sb.toString();
	}

	protected Object[] getActivityCounter(
			long groupId, long classNameId, long classPK, String name,
			int ownerType, int startPeriod, int endPeriod)
		throws Exception {

		StringBundler sb = new StringBundler(4);

		sb.append("select activityCounterId, totalValue from ");
		sb.append("SocialActivityCounter where groupId = ? and ");
		sb.append("classNameId = ? and classPK = ? and name = ? and ");
		sb.append("ownerType = ? and startPeriod = ? and endPeriod = ?");

		try (PreparedStatement ps = connection.prepareStatement(
				sb.toString())) {

			ps.setLong(1, groupId);
			ps.setLong(2, classNameId);
			ps.setLong(3, classPK);
			ps.setString(4, name);
			ps.setInt(5, ownerType);
			ps.setInt(6, startPeriod);
			ps.setInt(7, endPeriod);

			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					long activityCounterId = rs.getLong("activityCounterId");
					int totalValue = rs.getInt("totalValue");

					return new Object[] {activityCounterId, totalValue};
				}

				return null;
			}
		}
	}

	protected long[] getAssetEntryArray(long assetEntryId) throws Exception {
		try (PreparedStatement ps = connection.prepareStatement(
				"select groupId, companyId, userId, classNameId, classPK " +
					"from AssetEntry where entryId = " + assetEntryId);
			ResultSet rs = ps.executeQuery()) {

			if (rs.next()) {
				long groupId = rs.getLong("groupId");
				long companyId = rs.getLong("companyId");
				long userId = rs.getLong("userId");
				long classNameId = rs.getLong("classNameId");
				long classPK = rs.getLong("classPK");

				return new long[] {
					groupId, companyId, userId, classNameId, classPK
				};
			}

			return null;
		}
	}

	protected long[] getAssetEntryArray(String className, long classPK)
		throws Exception {

		long classNameId = PortalUtil.getClassNameId(className);

		StringBundler sb = new StringBundler(5);

		sb.append("select groupId, companyId, userId from AssetEntry ");
		sb.append("where classNameId = ");
		sb.append(classNameId);
		sb.append(" and classPK = ");
		sb.append(classPK);

		try (PreparedStatement ps = connection.prepareStatement(sb.toString());
			ResultSet rs = ps.executeQuery()) {

			if (rs.next()) {
				long groupId = rs.getLong("groupId");
				long companyId = rs.getLong("companyId");
				long userId = rs.getLong("userId");

				return new long[] {
					groupId, companyId, userId, classNameId, classPK
				};
			}

			return null;
		}
	}

	protected long getMBThreadRootMessageId(long mbThreadId) throws Exception {
		try (PreparedStatement ps = connection.prepareStatement(
				"select rootMessageId from MBThread where threadId = " +
					mbThreadId);
			ResultSet rs = ps.executeQuery()) {

			if (rs.next()) {
				return rs.getLong("rootMessageId");
			}

			return -1;
		}
	}

	protected int getTotalValue(
			long groupId, long classNameId, long classPK, String name,
			int ownerType, int startPeriod)
		throws Exception {

		StringBundler sb = new StringBundler(4);

		sb.append("select max(totalValue) as totalValue from ");
		sb.append("SocialActivityCounter where groupId = ? and ");
		sb.append("classNameId = ? and classPK = ? and name = ? and ");
		sb.append("ownerType = ? and startPeriod < ?");

		try (PreparedStatement ps = connection.prepareStatement(
				sb.toString())) {

			ps.setLong(1, groupId);
			ps.setLong(2, classNameId);
			ps.setLong(3, classPK);
			ps.setString(4, name);
			ps.setInt(5, ownerType);
			ps.setInt(6, startPeriod);

			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					return rs.getInt("totalValue");
				}

				return 0;
			}
		}
	}

	protected void migrateEquityGroupSettings() throws Exception {
		try (LoggingTimer loggingTimer = new LoggingTimer()) {
			try (PreparedStatement ps = connection.prepareStatement(
					"select groupId, companyId, classNameId, enabled from " +
						"SocialEquityGroupSetting where type_ = 1");
				ResultSet rs = ps.executeQuery()) {

				while (rs.next()) {
					long groupId = rs.getLong("groupId");
					long companyId = rs.getLong("companyId");
					long classNameId = rs.getLong("classNameId");
					boolean enabled = rs.getBoolean("enabled");

					addActivitySetting(
						increment(), groupId, companyId, classNameId, 0,
						"enabled", String.valueOf(enabled));
				}
			}

			StringBundler sb = new StringBundler(7);

			sb.append("select groupId from SocialActivitySetting where ");
			sb.append("activityType = 0 and name = 'enabled' and ");
			sb.append("value = 'true' and classNameId in (");

			long mbMessageClassNameId = PortalUtil.getClassNameId(
				"com.liferay.portlet.messageboards.model.MBMessage");

			sb.append(mbMessageClassNameId);
			sb.append(", ");

			long mbThreadClassNameId = PortalUtil.getClassNameId(
				"com.liferay.portlet.messageboards.model.MBThread");

			sb.append(mbThreadClassNameId);
			sb.append(StringPool.CLOSE_PARENTHESIS);

			try (PreparedStatement ps = connection.prepareStatement(
					sb.toString());
				ResultSet rs = ps.executeQuery()) {

				while (rs.next()) {
					long groupId = rs.getLong("groupId");

					sb = new StringBundler(6);

					sb.append(
						"update SocialActivitySetting set value = 'true' ");
					sb.append("where groupId = ");
					sb.append(groupId);
					sb.append(
						" and activityType = 0 and value = 'enabled' and ");
					sb.append("classNameId = ");
					sb.append(mbThreadClassNameId);

					runSQL(sb.toString());
				}

				runSQL(
					"delete from SocialActivitySetting where classNameId = " +
						mbThreadClassNameId);
			}
		}
	}

	protected void migrateEquityLog(ResultSet rs) throws Exception {
		long assetEntryId = rs.getLong("assetEntryId");

		long[] assetEntryArray = getAssetEntryArray(assetEntryId);

		if (assetEntryArray == null) {
			return;
		}

		String actionId = rs.getString("actionId");

		if (actionId.equals(ActionKeys.SUBSCRIBE)) {
			long classNameId = assetEntryArray[3];

			String className = PortalUtil.getClassName(classNameId);

			if (className.equals(
					"com.liferay.portlet.messageboards.model.MBThread")) {

				long classPK = assetEntryArray[4];

				long mbThreadRootMessageId = getMBThreadRootMessageId(classPK);

				if (mbThreadRootMessageId == -1) {
					return;
				}

				assetEntryArray = getAssetEntryArray(
					"com.liferay.portlet.messageboards.model.MBMessage",
					mbThreadRootMessageId);

				if (assetEntryArray == null) {
					return;
				}
			}
		}

		long classNameId = PortalUtil.getClassNameId(
			"com.liferay.portal.model.User");
		long classPK = rs.getLong("userId");
		String name = SocialActivityCounterConstants.NAME_PARTICIPATION;
		int ownerType = SocialActivityCounterConstants.TYPE_ACTOR;

		int actionDate = rs.getInt("actionDate");

		Date actionDateDate = SocialCounterPeriodUtil.getDate(actionDate - 365);

		int startPeriod = SocialCounterPeriodUtil.getStartPeriod(
			actionDateDate.getTime());
		int endPeriod = SocialCounterPeriodUtil.getEndPeriod(
			actionDateDate.getTime());

		if (endPeriod == SocialCounterPeriodUtil.getEndPeriod()) {
			endPeriod = SocialActivityCounterConstants.END_PERIOD_UNDEFINED;
		}

		int type = rs.getInt("type_");
		int value = rs.getInt("value");

		long groupId = assetEntryArray[0];
		long companyId = assetEntryArray[1];

		if (type == 1) {
			long userId = assetEntryArray[2];

			name = SocialActivityCounterConstants.NAME_CONTRIBUTION;
			ownerType = SocialActivityCounterConstants.TYPE_CREATOR;

			updateActivityCounter(
				increment(), groupId, companyId, classNameId, userId, name,
				ownerType, startPeriod, endPeriod, value);

			classNameId = assetEntryArray[3];
			classPK = assetEntryArray[4];
			name = SocialActivityCounterConstants.NAME_POPULARITY;
			ownerType = SocialActivityCounterConstants.TYPE_ASSET;
		}

		long equityLogId = rs.getLong("equityLogId");

		updateActivityCounter(
			equityLogId, groupId, companyId, classNameId, classPK, name,
			ownerType, startPeriod, endPeriod, value);
	}

	protected void migrateEquityLogs() throws Exception {
		try (LoggingTimer loggingTimer = new LoggingTimer()) {
			StringBundler sb = new StringBundler(4);

			sb.append("select AssetEntry.classNameId, AssetEntry.classPK, ");
			sb.append("SocialEquityLog.* from SocialEquityLog, AssetEntry ");
			sb.append("where SocialEquityLog.assetEntryId = ");
			sb.append("AssetEntry.entryId order by actionDate");

			try (PreparedStatement ps = connection.prepareStatement(
					sb.toString());
				ResultSet rs = ps.executeQuery()) {

				while (rs.next()) {
					migrateEquityLog(rs);
				}
			}

			sb = new StringBundler(4);

			sb.append("select groupId, classNameId, classPK, name, ");
			sb.append("max(startPeriod) as startPeriod ");
			sb.append("from SocialActivityCounter group by groupId, ");
			sb.append("classNameId, classPK, name");

			try (PreparedStatement ps = connection.prepareStatement(
					sb.toString());
				ResultSet rs = ps.executeQuery()) {

				while (rs.next()) {
					long groupId = rs.getLong("groupId");
					long classNameId = rs.getLong("classNameId");
					long classPK = rs.getLong("classPK");
					String name = rs.getString("name");
					int startPeriod = rs.getInt("startPeriod");

					sb = new StringBundler(12);

					sb.append("update SocialActivityCounter set endPeriod = ");
					sb.append(
						SocialActivityCounterConstants.END_PERIOD_UNDEFINED);
					sb.append(" where groupId = ");
					sb.append(groupId);
					sb.append(" and classNameId = ");
					sb.append(classNameId);
					sb.append(" and classPK = ");
					sb.append(classPK);
					sb.append(" and name = '");
					sb.append(name);
					sb.append("' and startPeriod = ");
					sb.append(startPeriod);

					runSQL(sb.toString());
				}
			}
		}
	}

	protected void migrateEquitySettings() throws Exception {
		try (LoggingTimer loggingTimer = new LoggingTimer();
			PreparedStatement ps = connection.prepareStatement(
				"select groupId, companyId, classNameId, actionId, " +
					"dailyLimit, type_, value from SocialEquitySetting");
			ResultSet rs = ps.executeQuery()) {

			while (rs.next()) {
				long groupId = rs.getLong("groupId");
				long companyId = rs.getLong("companyId");
				long classNameId = rs.getLong("classNameId");
				String actionId = rs.getString("actionId");
				int dailyLimit = rs.getInt("dailyLimit");
				int type = rs.getInt("type_");
				int value = rs.getInt("value");

				Tuple tuple = _equityToActivityMap.get(
					encodeEquityToActivityKey(classNameId, actionId));

				if (tuple == null) {
					if (_log.isWarnEnabled()) {
						StringBundler sb = new StringBundler(6);

						sb.append("Unknown Social Equity setting with action ");
						sb.append(actionId);
						sb.append("for ");

						String className = PortalUtil.getClassName(classNameId);

						sb.append(className);

						sb.append(". Please configure this action using the ");
						sb.append(
							"Social Activity portlet in the Control Panel.");

						_log.warn(sb.toString());
					}

					continue;
				}

				long activityClassNameId = GetterUtil.getLong(
					tuple.getObject(0));
				int activityType = GetterUtil.getInteger(tuple.getObject(1));

				if (type == 1) {
					addActivitySetting(
						groupId, companyId, activityClassNameId, activityType,
						SocialActivityCounterConstants.NAME_CONTRIBUTION,
						SocialActivityCounterConstants.TYPE_CREATOR, dailyLimit,
						value);

					addActivitySetting(
						groupId, companyId, activityClassNameId, activityType,
						SocialActivityCounterConstants.NAME_POPULARITY,
						SocialActivityCounterConstants.TYPE_ASSET, dailyLimit,
						value);
				}
				else if (type == 2) {
					addActivitySetting(
						groupId, companyId, activityClassNameId, activityType,
						SocialActivityCounterConstants.NAME_PARTICIPATION,
						SocialActivityCounterConstants.TYPE_ACTOR, dailyLimit,
						value);
				}
			}
		}
	}

	protected void putEquityToActivityMap(
		String equityClassName, String equityActionId, int activityType) {

		putEquityToActivityMap(
			equityClassName, equityActionId, equityClassName, activityType);
	}

	protected void putEquityToActivityMap(
		String equityClassName, String equityActionId, String activityClassName,
		int activityType) {

		long equityClassNameId = PortalUtil.getClassNameId(equityClassName);
		long activityClassNameId = PortalUtil.getClassNameId(activityClassName);

		_equityToActivityMap.put(
			encodeEquityToActivityKey(equityClassNameId, equityActionId),
			new Tuple(activityClassNameId, activityType));
	}

	protected void updateActivityCounter(
			long activityCounterId, long groupId, long companyId,
			long classNameId, long classPK, String name, int ownerType,
			int startPeriod, int endPeriod, int increment)
		throws Exception {

		Object[] activityCounter = getActivityCounter(
			groupId, classNameId, classPK, name, ownerType, startPeriod,
			endPeriod);

		if (activityCounter == null) {
			int totalValue = getTotalValue(
				groupId, classNameId, classPK, name, ownerType, startPeriod);

			addActivityCounter(
				activityCounterId, groupId, companyId, classNameId, classPK,
				name, ownerType, increment, totalValue + increment, 0,
				startPeriod, endPeriod);

			return;
		}

		StringBundler sb = new StringBundler(7);

		sb.append("update SocialActivityCounter set currentValue = ");
		sb.append("currentValue + ");
		sb.append(increment);
		sb.append(", totalValue = totalValue + ");
		sb.append(increment);
		sb.append(" where activityCounterId = ");

		activityCounterId = GetterUtil.getLong(activityCounter[0]);

		sb.append(activityCounterId);

		runSQL(sb.toString());
	}

	private static final Log _log = LogFactoryUtil.getLog(UpgradeSocial.class);

	private final Map<String, Tuple> _equityToActivityMap = new HashMap<>();

}