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

package com.liferay.portal.setup;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Account;
import com.liferay.portal.kernel.model.Company;
import com.liferay.portal.kernel.model.Layout;
import com.liferay.portal.kernel.model.LayoutConstants;
import com.liferay.portal.kernel.model.ListTypeConstants;
import com.liferay.portal.kernel.model.Organization;
import com.liferay.portal.kernel.model.OrganizationConstants;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.service.AccountLocalServiceUtil;
import com.liferay.portal.kernel.service.CompanyLocalServiceUtil;
import com.liferay.portal.kernel.service.GroupLocalServiceUtil;
import com.liferay.portal.kernel.service.LayoutLocalServiceUtil;
import com.liferay.portal.kernel.service.OrganizationLocalServiceUtil;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.UserLocalServiceUtil;
import com.liferay.portal.kernel.util.FriendlyURLNormalizerUtil;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.portal.kernel.util.StringPool;

import java.util.Calendar;

import org.apache.commons.lang.time.StopWatch;

/**
 * @author Shinn Lok
 */
public class SetupWizardSampleDataUtil {

	public static void addSampleData(long companyId) throws Exception {
		StopWatch stopWatch = new StopWatch();

		stopWatch.start();

		if (_log.isInfoEnabled()) {
			_log.info("Adding sample data");
		}

		Company company = CompanyLocalServiceUtil.getCompanyById(companyId);

		Account account = company.getAccount();

		account.setName("Liferay");
		account.setLegalName("Liferay, Inc");

		AccountLocalServiceUtil.updateAccount(account);

		User defaultUser = company.getDefaultUser();

		Organization organization =
			OrganizationLocalServiceUtil.addOrganization(
				defaultUser.getUserId(),
				OrganizationConstants.DEFAULT_PARENT_ORGANIZATION_ID,
				"Liferay, Inc.", true);

		GroupLocalServiceUtil.updateFriendlyURL(
			organization.getGroupId(), "/liferay");

		Layout extranetLayout = LayoutLocalServiceUtil.addLayout(
			defaultUser.getUserId(), organization.getGroupId(), false,
			LayoutConstants.DEFAULT_PARENT_LAYOUT_ID, "Liferay, Inc. Extranet",
			null, null, LayoutConstants.TYPE_PORTLET, false, "/extranet",
			new ServiceContext());

		LayoutLocalServiceUtil.updateLayout(
			extranetLayout.getGroupId(), false, extranetLayout.getLayoutId(),
			extranetLayout.getTypeSettings());

		Layout intranetLayout = LayoutLocalServiceUtil.addLayout(
			defaultUser.getUserId(), organization.getGroupId(), true,
			LayoutConstants.DEFAULT_PARENT_LAYOUT_ID, "Liferay, Inc. Intranet",
			null, null, LayoutConstants.TYPE_PORTLET, false, "/intranet",
			new ServiceContext());

		LayoutLocalServiceUtil.updateLayout(
			intranetLayout.getGroupId(), true, intranetLayout.getLayoutId(),
			intranetLayout.getTypeSettings());

		User user = UserLocalServiceUtil.fetchUserByEmailAddress(
			company.getCompanyId(), "test@liferay.com");

		if (user == null) {
			user = UserLocalServiceUtil.addDefaultAdminUser(
				companyId, "joebloggs", "test@liferay.com",
				LocaleUtil.getDefault(), "Joe", StringPool.BLANK, "Bloggs");
		}
		else {
			user.setScreenName("joebloggs");
			user.setGreeting("Welcome Joe Bloggs!");
			user.setFirstName("Joe");
			user.setLastName("Bloggs");
		}

		user.setPasswordReset(false);

		UserLocalServiceUtil.updateUser(user);

		OrganizationLocalServiceUtil.addUserOrganization(
			user.getUserId(), organization);

		addOrganizations(defaultUser, organization);

		if (_log.isInfoEnabled()) {
			_log.info("Finished adding data in " + stopWatch.getTime() + " ms");
		}
	}

	protected static void addOrganizations(
			User defaultUser, Organization parentOrganization)
		throws Exception {

		for (Object[] organizationArray : _ORGANIZATION_ARRAYS) {
			String name = "Liferay " + organizationArray[0];
			long regionId = (Long)organizationArray[1];
			long countryId = (Long)organizationArray[2];
			String type = (String)organizationArray[3];

			Organization organization =
				OrganizationLocalServiceUtil.addOrganization(
					defaultUser.getUserId(),
					parentOrganization.getOrganizationId(), name, type,
					regionId, countryId,
					ListTypeConstants.ORGANIZATION_STATUS_DEFAULT,
					StringPool.BLANK, true, null);

			GroupLocalServiceUtil.updateFriendlyURL(
				organization.getGroupId(),
				FriendlyURLNormalizerUtil.normalize(
					StringPool.SLASH + organizationArray[0]));

			if (organizationArray.length <= 4) {
				continue;
			}

			String organizationPrefix = (String)organizationArray[4];

			long[] groupIds = {organization.getGroupId()};
			long[] organizationIds = {
				parentOrganization.getOrganizationId(),
				organization.getOrganizationId()
			};

			for (int i = 1; i <= 10; i++) {
				String screenName = organizationPrefix + i;

				StringBundler sb = new StringBundler(5);

				sb.append("test.");
				sb.append(organizationPrefix);
				sb.append(StringPool.PERIOD);
				sb.append(i);
				sb.append("@liferay.com");

				String emailAddress = sb.toString();

				String lastName = organizationPrefix + StringPool.SPACE + i;

				User user = UserLocalServiceUtil.addUser(
					0, defaultUser.getCompanyId(), false, "test", "test", false,
					screenName, emailAddress, 0, null, LocaleUtil.getDefault(),
					"Test", null, lastName, 0, 0, true, Calendar.JANUARY, 1,
					1970, null, groupIds, organizationIds, null, null, false,
					new ServiceContext());

				user.setPasswordReset(false);
				user.setAgreedToTermsOfUse(true);

				UserLocalServiceUtil.updateUser(user);
			}
		}
	}

	private static final Object[][] _ORGANIZATION_ARRAYS = {
		{
			"Chicago", 19014L, 19L, OrganizationConstants.TYPE_ORGANIZATION,
			"ORD"
		},
		{
			"Consulting", 19005L, 19L, OrganizationConstants.TYPE_ORGANIZATION
		},
		{
			"Dalian", 0L, 2L, OrganizationConstants.TYPE_ORGANIZATION, "DLC"
		},
		{
			"Engineering", 19005L, 19L, OrganizationConstants.TYPE_ORGANIZATION
		},
		{
			"Frankfurt", 0L, 4L, OrganizationConstants.TYPE_ORGANIZATION, "FRA"
		},
		{
			"Hong Kong", 0L, 2L, OrganizationConstants.TYPE_ORGANIZATION, "HKG"
		},
		{
			"Kuala Lumpur", 0L, 135L, OrganizationConstants.TYPE_ORGANIZATION,
			"KUL"
		},
		{
			"Los Angeles", 19005L, 19L, OrganizationConstants.TYPE_ORGANIZATION,
			"LAX"
		},
		{
			"Madrid", 0L, 15L, OrganizationConstants.TYPE_ORGANIZATION, "MAD"
		},
		{
			"Marketing", 19005L, 19L, OrganizationConstants.TYPE_ORGANIZATION
		},
		{
			"New York", 19033L, 19L, OrganizationConstants.TYPE_ORGANIZATION,
			"NYC"
		},
		{
			"Saint Paulo", 0L, 48L, OrganizationConstants.TYPE_ORGANIZATION,
			"GRU"
		},
		{
			"Sales", 19005L, 19L, OrganizationConstants.TYPE_ORGANIZATION
		},
		{
			"San Francisco", 19005L, 19L,
			OrganizationConstants.TYPE_ORGANIZATION, "SFO"
		},
		{
			"Support", 19005L, 19L, OrganizationConstants.TYPE_ORGANIZATION
		}
	};

	private static final Log _log = LogFactoryUtil.getLog(
		SetupWizardSampleDataUtil.class);

}