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

import com.liferay.portal.kernel.upgrade.UpgradeProcess;
import com.liferay.portal.kernel.util.LoggingTimer;
import com.liferay.portal.upgrade.v6_1_0.util.BlogsEntryTable;

/**
 * @author Minhchau Dang
 * @author Brian Wing Shun Chan
 */
public class UpgradeBlogs extends UpgradeProcess {

	protected void alterTable() throws Exception {
		try (LoggingTimer loggingTimer = new LoggingTimer()) {
			runSQL("alter table BlogsEntry drop column draft");

			alter(
				BlogsEntryTable.class,
				new AlterColumnType("smallImageURL", "STRING null"));
		}
		catch (Exception e) {
		}
	}

	@Override
	protected void doUpgrade() throws Exception {
		dropIndexes();

		alterTable();
	}

	protected void dropIndexes() {
		try (LoggingTimer loggingTimer = new LoggingTimer()) {
			runSQL("drop index IX_E0D90212 on BlogsEntry");
			runSQL("drop index IX_DA53AFD4 on BlogsEntry");
			runSQL("drop index IX_B88E740E on BlogsEntry");
		}
		catch (Exception e) {
		}
	}

}