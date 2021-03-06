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

package com.liferay.dynamic.data.mapping.data.provider.internal.servlet;

import com.liferay.dynamic.data.mapping.data.provider.DDMDataProvider;
import com.liferay.dynamic.data.mapping.data.provider.DDMDataProviderContext;
import com.liferay.dynamic.data.mapping.data.provider.DDMDataProviderTracker;
import com.liferay.dynamic.data.mapping.io.DDMFormValuesJSONDeserializer;
import com.liferay.dynamic.data.mapping.model.DDMDataProviderInstance;
import com.liferay.dynamic.data.mapping.model.DDMForm;
import com.liferay.dynamic.data.mapping.service.DDMDataProviderInstanceService;
import com.liferay.dynamic.data.mapping.storage.DDMFormValues;
import com.liferay.dynamic.data.mapping.util.DDMFormFactory;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactory;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.servlet.ServletResponseUtil;
import com.liferay.portal.kernel.util.ContentTypes;
import com.liferay.portal.kernel.util.KeyValuePair;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.servlet.filters.authverifier.AuthVerifierFilter;

import java.io.IOException;

import java.util.Hashtable;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

/**
 * @author Bruno Basto
 */
@Component(immediate = true)
public class DDMDataProviderServlet extends HttpServlet {

	@Activate
	protected void activate(BundleContext bundleContext) {
		Hashtable<String, String> properties = new Hashtable<>();

		properties.put(
			HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH,
			"/dynamic-data-mapping-data-provider");
		properties.put(
			HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_INIT_PARAM_PREFIX +
				"auth.verifier.PortalSessionAuthVerifier.urls.includes",
			"/dynamic-data-mapping-data-provider/*");
		properties.put(
			HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME,
			"AuthVerifierFilter");
		properties.put(
			HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN,
			"/dynamic-data-mapping-data-provider/*");

		bundleContext.registerService(
			Filter.class, new AuthVerifierFilter(), properties);

		properties = new Hashtable<>();

		properties.put(
			HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH,
			"/dynamic-data-mapping-data-provider");
		properties.put(
			HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME,
			"DDMDataProviderServlet");
		properties.put(
			HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN,
			"/dynamic-data-mapping-data-provider/*");
		properties.put("servlet.init.httpMethods", "GET,POST,HEAD");

		bundleContext.registerService(Servlet.class, this, properties);
	}

	@Override
	protected void doGet(
			HttpServletRequest request, HttpServletResponse response)
		throws IOException, ServletException {

		long ddmDataProviderInstanceId = ParamUtil.getLong(
			request, "ddmDataProviderInstanceId");

		String data = doGetData(ddmDataProviderInstanceId);

		if (data == null) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST);

			return;
		}

		response.setContentType(ContentTypes.APPLICATION_JSON);
		response.setStatus(HttpServletResponse.SC_OK);

		ServletResponseUtil.write(response, data);
	}

	protected String doGetData(long ddmDataProviderInstanceId) {
		try {
			DDMDataProviderInstance ddmDataProviderInstance =
				_ddmDataProviderInstanceService.getDataProviderInstance(
					ddmDataProviderInstanceId);

			DDMDataProvider ddmDataProvider =
				_ddmDataProviderTracker.getDDMDataProvider(
					ddmDataProviderInstance.getType());

			DDMForm ddmForm = DDMFormFactory.create(
				ddmDataProvider.getSettings());

			DDMFormValues ddmFormValues =
				_ddmFormValuesJSONDeserializer.deserialize(
					ddmForm, ddmDataProviderInstance.getDefinition());

			DDMDataProviderContext ddmDataProviderContext =
				new DDMDataProviderContext(ddmFormValues);

			JSONArray jsonArray = toJSONArray(
				ddmDataProvider.getData(ddmDataProviderContext));

			return jsonArray.toString();
		}
		catch (PortalException pe) {
			if (_log.isDebugEnabled()) {
				_log.debug(pe, pe);
			}
		}

		return null;
	}

	@Reference(unbind = "-")
	protected void setDDMDataProviderInstanceService(
		DDMDataProviderInstanceService ddmDataProviderInstanceService) {

		_ddmDataProviderInstanceService = ddmDataProviderInstanceService;
	}

	@Reference(unbind = "-")
	protected void setDDMDataProviderTracker(
		DDMDataProviderTracker ddmDataProviderTracker) {

		_ddmDataProviderTracker = ddmDataProviderTracker;
	}

	@Reference(unbind = "-")
	protected void setDDMFormValuesJSONDeserializer(
		DDMFormValuesJSONDeserializer ddmFormValuesJSONDeserializer) {

		_ddmFormValuesJSONDeserializer = ddmFormValuesJSONDeserializer;
	}

	@Reference(unbind = "-")
	protected void setJSONFactory(JSONFactory jsonFactory) {
		_jsonFactory = jsonFactory;
	}

	protected JSONArray toJSONArray(List<KeyValuePair> keyValuePairs) {
		JSONArray jsonArray = _jsonFactory.createJSONArray();

		for (KeyValuePair keyValuePair : keyValuePairs) {
			JSONObject jsonObject = _jsonFactory.createJSONObject();

			JSONObject labelJSONObject = _jsonFactory.createJSONObject();

			labelJSONObject.put(
				LanguageUtil.getLanguageId(LocaleUtil.getDefault()),
				keyValuePair.getKey());

			jsonObject.put("label", labelJSONObject);
			jsonObject.put("value", keyValuePair.getValue());

			jsonArray.put(jsonObject);
		}

		return jsonArray;
	}

	private static final Log _log = LogFactoryUtil.getLog(
		DDMDataProviderServlet.class);

	private static final long serialVersionUID = 1L;

	private DDMDataProviderInstanceService _ddmDataProviderInstanceService;
	private DDMDataProviderTracker _ddmDataProviderTracker;
	private DDMFormValuesJSONDeserializer _ddmFormValuesJSONDeserializer;
	private JSONFactory _jsonFactory;

}