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

package com.liferay.marketplace.service.impl;

import com.liferay.document.library.kernel.exception.NoSuchFileException;
import com.liferay.document.library.kernel.store.DLStoreUtil;
import com.liferay.marketplace.bundle.BundleManagerUtil;
import com.liferay.marketplace.exception.AppPropertiesException;
import com.liferay.marketplace.exception.AppTitleException;
import com.liferay.marketplace.exception.AppVersionException;
import com.liferay.marketplace.model.App;
import com.liferay.marketplace.model.Module;
import com.liferay.marketplace.service.base.AppLocalServiceBaseImpl;
import com.liferay.marketplace.util.ContextUtil;
import com.liferay.marketplace.util.comparator.AppTitleComparator;
import com.liferay.portal.kernel.deploy.DeployManagerUtil;
import com.liferay.portal.kernel.deploy.auto.context.AutoDeploymentContext;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.CompanyConstants;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.plugin.PluginPackage;
import com.liferay.portal.kernel.util.FileUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.ListUtil;
import com.liferay.portal.kernel.util.PortalUtil;
import com.liferay.portal.kernel.util.PropertiesUtil;
import com.liferay.portal.kernel.util.ReleaseInfo;
import com.liferay.portal.kernel.util.StreamUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.SystemProperties;
import com.liferay.portal.kernel.util.Time;
import com.liferay.portal.kernel.util.Validator;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import java.util.ArrayList;
import java.util.Date;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.osgi.framework.Bundle;

/**
 * @author Ryan Park
 * @author Joan Kim
 */
public class AppLocalServiceImpl extends AppLocalServiceBaseImpl {

	@Override
	public void clearInstalledAppsCache() {
		_installedApps = null;
		_prepackagedApps = null;
	}

	@Override
	public App deleteApp(App app) {

		// App

		clearInstalledAppsCache();

		appPersistence.remove(app);

		// Module

		List<Module> modules = modulePersistence.findByAppId(app.getAppId());

		for (Module module : modules) {
			moduleLocalService.deleteModule(module);
		}

		// File

		try {
			DLStoreUtil.deleteFile(
				app.getCompanyId(), CompanyConstants.SYSTEM, app.getFilePath());
		}
		catch (Exception e) {
			if (_log.isWarnEnabled()) {
				_log.warn(e, e);
			}
		}

		return app;
	}

	@Override
	public App deleteApp(long appId) throws PortalException {
		App app = appPersistence.findByPrimaryKey(appId);

		return deleteApp(app);
	}

	@Override
	public App fetchRemoteApp(long remoteAppId) {
		return appPersistence.fetchByRemoteAppId(remoteAppId);
	}

	@Override
	public List<App> getApps(String category) {
		return appPersistence.findByCategory(category);
	}

	@Override
	public List<App> getInstalledApps() {
		if (_installedApps != null) {
			return _installedApps;
		}

		List<App> installedApps = new ArrayList<>();

		// Core app

		App coreApp = appPersistence.create(0);

		coreApp.setTitle("Liferay Core");
		coreApp.setDescription("Plugins bundled with Liferay Portal.");
		coreApp.setVersion(ReleaseInfo.getVersion());

		coreApp.addContextName(PortalUtil.getServletContextName());

		installedApps.add(coreApp);

		// Deployed apps

		List<PluginPackage> pluginPackages =
			DeployManagerUtil.getInstalledPluginPackages();

		for (PluginPackage pluginPackage : pluginPackages) {
			List<Module> modules = modulePersistence.findByContextName(
				pluginPackage.getContext());

			boolean installedApp = false;

			for (Module module : modules) {
				App app = appPersistence.fetchByPrimaryKey(module.getAppId());

				if ((app != null) && app.isInstalled()) {
					installedApp = true;

					break;
				}
			}

			if (installedApp) {
				continue;
			}

			App app = appPersistence.create(0);

			app.setTitle(pluginPackage.getName());
			app.setDescription(pluginPackage.getLongDescription());
			app.setVersion(pluginPackage.getVersion());

			app.addContextName(pluginPackage.getContext());

			installedApps.add(app);
		}

		// Marketplace apps

		List<App> apps = appPersistence.findAll();

		for (App app : apps) {
			if (app.isInstalled()) {
				installedApps.add(app);
			}
		}

		installedApps = ListUtil.sort(installedApps, new AppTitleComparator());

		_installedApps = installedApps;

		return _installedApps;
	}

	@Override
	public List<App> getInstalledApps(String category) {
		List<App> apps = appPersistence.findByCategory(category);

		List<App> installedApps = new ArrayList<>(apps.size());

		for (App app : apps) {
			if (app.isInstalled()) {
				installedApps.add(app);
			}
		}

		return installedApps;
	}

	@Override
	public Map<String, String> getPrepackagedApps() {
		if (_prepackagedApps != null) {
			return _prepackagedApps;
		}

		Map<String, String> prepackagedApps = new HashMap<>();

		List<Bundle> bundles = BundleManagerUtil.getInstalledBundles();

		for (Bundle bundle : bundles) {
			Dictionary<String, String> headers = bundle.getHeaders();

			boolean liferayRelengBundle = GetterUtil.getBoolean(
				headers.get("Liferay-Releng-Bundle"));

			if (!liferayRelengBundle) {
				continue;
			}

			prepackagedApps.put(
				bundle.getSymbolicName(), String.valueOf(bundle.getVersion()));
		}

		_prepackagedApps = prepackagedApps;

		return _prepackagedApps;
	}

	@Override
	public void installApp(long remoteAppId) throws PortalException {
		App app = appPersistence.findByRemoteAppId(remoteAppId);

		if (!DLStoreUtil.hasFile(
				app.getCompanyId(), CompanyConstants.SYSTEM,
				app.getFilePath())) {

			throw new NoSuchFileException();
		}

		String tmpDir =
			SystemProperties.get(SystemProperties.TMP_DIR) + StringPool.SLASH +
				Time.getTimestamp();

		InputStream inputStream = null;

		ZipFile zipFile = null;

		try {
			inputStream = DLStoreUtil.getFileAsStream(
				app.getCompanyId(), CompanyConstants.SYSTEM, app.getFilePath());

			if (inputStream == null) {
				throw new IOException(
					"Unable to open file at " + app.getFilePath());
			}

			File liferayPackageFile = FileUtil.createTempFile(inputStream);

			zipFile = new ZipFile(liferayPackageFile);

			Enumeration<ZipEntry> enu =
				(Enumeration<ZipEntry>)zipFile.entries();

			while (enu.hasMoreElements()) {
				ZipEntry zipEntry = enu.nextElement();

				String fileName = zipEntry.getName();

				if (!fileName.endsWith(".jar") && !fileName.endsWith(".war") &&
					!fileName.endsWith(".xml") && !fileName.endsWith(".zip") &&
					!fileName.equals("liferay-marketplace.properties")) {

					continue;
				}

				if (_log.isInfoEnabled()) {
					_log.info(
						"Extracting " + fileName + " from app " +
							app.getAppId());
				}

				InputStream zipInputStream = null;

				try {
					zipInputStream = zipFile.getInputStream(zipEntry);

					if (fileName.equals("liferay-marketplace.properties")) {
						String propertiesString = StringUtil.read(
							zipInputStream);

						Properties properties = PropertiesUtil.load(
							propertiesString);

						processMarketplaceProperties(properties);
					}
					else {
						File pluginPackageFile = new File(
							tmpDir + StringPool.SLASH + fileName);

						FileUtil.write(pluginPackageFile, zipInputStream);

						String bundleSymbolicName = StringPool.BLANK;
						String bundleVersion = StringPool.BLANK;
						String contextName = StringPool.BLANK;

						AutoDeploymentContext autoDeploymentContext =
							new AutoDeploymentContext();

						if (fileName.endsWith(".jar")) {
							Manifest manifest = BundleManagerUtil.getManifest(
								pluginPackageFile);

							Attributes attributes =
								manifest.getMainAttributes();

							bundleSymbolicName = GetterUtil.getString(
								attributes.getValue("Bundle-SymbolicName"));
							bundleVersion = GetterUtil.getString(
								attributes.getValue("Bundle-Version"));

							String contextPath = GetterUtil.getString(
								attributes.getValue("Web-ContextPath"));

							contextName = ContextUtil.getContextName(
								contextPath);
						}
						else {
							contextName = ContextUtil.getContextName(fileName);

							autoDeploymentContext.setContext(contextName);
						}

						autoDeploymentContext.setFile(pluginPackageFile);

						DeployManagerUtil.deploy(autoDeploymentContext);

						if (Validator.isNotNull(bundleSymbolicName) ||
							Validator.isNotNull(contextName)) {

							moduleLocalService.addModule(
								app.getUserId(), app.getAppId(),
								bundleSymbolicName, bundleVersion, contextName);
						}
					}
				}
				finally {
					StreamUtil.cleanUp(zipInputStream);
				}
			}
		}
		catch (ZipException ze) {
			if (_log.isInfoEnabled()) {
				_log.info(
					"Deleting corrupt package from app " + app.getAppId(), ze);
			}

			deleteApp(app);
		}
		catch (IOException ioe) {
			throw new PortalException(ioe.getMessage());
		}
		catch (Exception e) {
			_log.error(e, e);
		}
		finally {
			FileUtil.deltree(tmpDir);

			if (zipFile != null) {
				try {
					zipFile.close();
				}
				catch (IOException ioe) {
				}
			}

			StreamUtil.cleanUp(inputStream);

			clearInstalledAppsCache();
		}
	}

	@Override
	public void processMarketplaceProperties(Properties properties)
		throws PortalException {

		long[] supersedesRemoteAppIds = StringUtil.split(
			properties.getProperty("supersedes-remote-app-ids"), 0L);

		for (long supersedesRemoteAppId : supersedesRemoteAppIds) {
			App supersedesApp = appPersistence.fetchByRemoteAppId(
				supersedesRemoteAppId);

			if ((supersedesApp != null) && supersedesApp.isInstalled()) {
				uninstallApp(supersedesRemoteAppId);
			}
		}
	}

	@Override
	public void uninstallApp(long remoteAppId) throws PortalException {
		clearInstalledAppsCache();

		App app = appPersistence.findByRemoteAppId(remoteAppId);

		List<Module> modules = modulePersistence.findByAppId(app.getAppId());

		for (Module module : modules) {
			moduleLocalService.deleteModule(module.getModuleId());

			if (module.isBundle()) {
				BundleManagerUtil.uninstallBundle(
					module.getBundleSymbolicName(), module.getBundleVersion());

				continue;
			}

			if (hasDependentApp(module)) {
				continue;
			}

			try {
				DeployManagerUtil.undeploy(module.getContextName());
			}
			catch (Exception e) {
				_log.error(e, e);
			}
		}
	}

	@Override
	public App updateApp(long userId, File file) throws PortalException {
		Properties properties = getMarketplaceProperties(file);

		if (properties == null) {
			throw new AppPropertiesException(
				"Unable to read liferay-marketplace.properties");
		}

		long remoteAppId = GetterUtil.getLong(
			properties.getProperty("remote-app-id"));
		String title = properties.getProperty("title");
		String description = properties.getProperty("description");
		String category = properties.getProperty("category");
		String iconURL = properties.getProperty("icon-url");
		String version = properties.getProperty("version");

		return updateApp(
			userId, remoteAppId, title, description, category, iconURL, version,
			file);
	}

	@Override
	public App updateApp(
			long userId, long remoteAppId, String title, String description,
			String category, String iconURL, String version, File file)
		throws PortalException {

		// App

		User user = userPersistence.fetchByPrimaryKey(userId);
		Date now = new Date();

		validate(title, version);

		App app = appPersistence.fetchByRemoteAppId(remoteAppId);

		if (app == null) {
			long appId = counterLocalService.increment();

			app = appPersistence.create(appId);
		}

		if (user != null) {
			app.setCompanyId(user.getCompanyId());
			app.setUserId(user.getUserId());
			app.setUserName(user.getFullName());
		}

		app.setCreateDate(now);
		app.setModifiedDate(now);
		app.setRemoteAppId(remoteAppId);
		app.setTitle(title);
		app.setDescription(description);
		app.setCategory(category);
		app.setIconURL(iconURL);
		app.setVersion(version);

		appPersistence.update(app);

		// File

		if (file != null) {
			try {
				DLStoreUtil.deleteFile(
					app.getCompanyId(), CompanyConstants.SYSTEM,
					app.getFilePath());
			}
			catch (Exception e) {
			}

			DLStoreUtil.addFile(
				app.getCompanyId(), CompanyConstants.SYSTEM, app.getFilePath(),
				false, file);
		}

		clearInstalledAppsCache();

		return app;
	}

	protected Properties getMarketplaceProperties(File liferayPackageFile) {
		InputStream inputStream = null;
		ZipFile zipFile = null;

		try {
			zipFile = new ZipFile(liferayPackageFile);

			ZipEntry zipEntry = zipFile.getEntry(
				"liferay-marketplace.properties");

			inputStream = zipFile.getInputStream(zipEntry);

			String propertiesString = StringUtil.read(inputStream);

			return PropertiesUtil.load(propertiesString);
		}
		catch (IOException ioe) {
			return null;
		}
		finally {
			if (zipFile != null) {
				try {
					zipFile.close();
				}
				catch (IOException ioe) {
				}
			}

			StreamUtil.cleanUp(inputStream);
		}
	}

	protected boolean hasDependentApp(Module module) throws PortalException {
		List<Module> modules = modulePersistence.findByContextName(
			module.getContextName());

		for (Module curModule : modules) {
			App app = appPersistence.findByPrimaryKey(curModule.getAppId());

			if (curModule.getAppId() == module.getAppId()) {
				continue;
			}

			if (app.isInstalled()) {
				return true;
			}
		}

		return false;
	}

	protected void validate(String title, String version)
		throws PortalException {

		if (Validator.isNull(title)) {
			throw new AppTitleException();
		}

		if (Validator.isNull(version)) {
			throw new AppVersionException();
		}
	}

	private static final Log _log = LogFactoryUtil.getLog(
		AppLocalServiceImpl.class);

	private List<App> _installedApps;
	private Map<String, String> _prepackagedApps;

}