<%--
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
--%>

<%@ include file="/init.jsp" %>

<%
String className = ParamUtil.getString(request, "className");
long classPK = ParamUtil.getLong(request, "classPK");

int ruleGroupInstancesCount = MDRRuleGroupInstanceServiceUtil.getRuleGroupInstancesCount(className, classPK);

PortletURL portletURL = (PortletURL)request.getAttribute("mobile_device_rules_header.jspf-portletURL");
%>

<c:if test="<%= ruleGroupInstancesCount <= 0 %>">
	<p class="text-muted">
		<%= StringUtil.toLowerCase(LanguageUtil.get(request, "none")) %>
	</p>
</c:if>

<liferay-ui:search-container
	deltaConfigurable="<%= false %>"
	id="rules"
	iteratorURL="<%= portletURL %>"
	total="<%= ruleGroupInstancesCount %>"
>
	<liferay-ui:search-container-results
		results="<%= MDRRuleGroupInstanceServiceUtil.getRuleGroupInstances(className, classPK, searchContainer.getStart(), searchContainer.getEnd(), new RuleGroupInstancePriorityComparator()) %>"
	/>

	<liferay-ui:search-container-row
		className="com.liferay.mobile.device.rules.model.MDRRuleGroupInstance"
		escapedModel="<%= true %>"
		keyProperty="ruleGroupInstanceId"
		modelVar="mdrRuleGroupInstance"
	>
		<liferay-portlet:renderURL portletName="<%= MDRPortletKeys.MOBILE_DEVICE_RULES %>" varImpl="rowURL" windowState="<%= themeDisplay.isStateExclusive() ? LiferayWindowState.POP_UP.toString() : windowState.toString() %>">
			<portlet:param name="mvcRenderCommandName" value="/mobile_device_rules/view_actions" />
			<portlet:param name="redirect" value='<%= currentURL + "#tab=mobileDeviceRules" %>' />
			<portlet:param name="showBackURL" value="<%= themeDisplay.isStateExclusive() ? Boolean.FALSE.toString() : Boolean.TRUE.toString() %>" />
			<portlet:param name="ruleGroupInstanceId" value="<%= String.valueOf(mdrRuleGroupInstance.getRuleGroupInstanceId()) %>" />
		</liferay-portlet:renderURL>

		<%@ include file="/layout/mobile_device_rules_rule_group_instance_columns.jspf" %>
	</liferay-ui:search-container-row>

	<liferay-ui:search-iterator markupView="lexicon" type="more" />
</liferay-ui:search-container>

<%@ include file="/layout/mobile_device_rules_toolbar.jspf" %>

<aui:script use="aui-base">
	A.one('#<portlet:namespace />rules').delegate(
		'click',
		function(event) {
			var currentTarget = event.currentTarget;

			Liferay.Util.openWindow(
				{
					dialog: {
						on: {
							visibleChange: function(event) {
								<portlet:namespace />updateRuleGroupInstances();
							}
						}
					},
					dialogIframe: {
						bodyCssClass: 'dialog-with-footer'
					},
					title: currentTarget.attr('data-title'),
					uri: currentTarget.attr('data-uri')
				}
			);
		},
		'.actions'
	);
</aui:script>

<c:if test="<%= themeDisplay.isStateExclusive() %>">
	<aui:script sandbox="<%= true %>">
		$('#<portlet:namespace />uniqueRuleGroupInstancesContainer').on(
			'click',
			'.mobile-device-rule a',
			function(event) {
				event.preventDefault();

				var href = $(event.target).attr('href');

				<portlet:namespace />mobileDeviceOpenWindow(
					{
						uri: href
					}
				);
			}
		);
	</aui:script>
</c:if>