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
String backURL = ParamUtil.getString(request, "backURL");

Layout selLayout = layoutsAdminDisplayContext.getSelLayout();

Long groupId = layoutsAdminDisplayContext.getGroupId();
boolean privateLayout = layoutsAdminDisplayContext.isPrivateLayout();
long parentPlid = LayoutConstants.DEFAULT_PLID;
long parentLayoutId = LayoutConstants.DEFAULT_PARENT_LAYOUT_ID;

if (layout.isTypeControlPanel()) {
	if (layoutsAdminDisplayContext.getSelPlid() != 0) {
		selLayout = LayoutLocalServiceUtil.getLayout(layoutsAdminDisplayContext.getSelPlid());

		privateLayout = selLayout.isPrivateLayout();
		parentPlid = selLayout.getPlid();
		parentLayoutId = selLayout.getLayoutId();
	}
}
else {
	selLayout = layout;

	privateLayout = selLayout.isPrivateLayout();
	parentPlid = layout.getParentPlid();
	parentLayoutId = layout.getParentLayoutId();
}

String[] types = LayoutTypeControllerTracker.getTypes();

if (Validator.isNotNull(backURL)) {
	portletDisplay.setShowBackIcon(true);
	portletDisplay.setURLBack(backURL);
}

renderResponse.setTitle(LanguageUtil.get(request, "add-new-page"));
%>

<portlet:actionURL name="addLayout" var="addLayoutURL">
	<portlet:param name="mvcPath" value="/add_layout.jsp" />
</portlet:actionURL>

<aui:form action="<%= addLayoutURL %>" cssClass="container-fluid-1280" enctype="multipart/form-data" method="post" name="addPageFm">

	<%
	String redirect = ParamUtil.getString(request, "redirect");
	%>

	<c:if test="<%= Validator.isNotNull(redirect) %>">
		<aui:input id="addLayoutRedirect" name="redirect" type="hidden" value="<%= redirect %>" />
	</c:if>

	<aui:input id="addLayoutGroupId" name="groupId" type="hidden" value="<%= String.valueOf(groupId) %>" />
	<aui:input id="addLayoutPrivateLayout" name="privateLayout" type="hidden" value="<%= privateLayout %>" />
	<aui:input id="addLayoutParentPlid" name="parentPlid" type="hidden" value="<%= parentPlid %>" />
	<aui:input id="addLayoutParentLayoutId" name="parentLayoutId" type="hidden" value="<%= parentLayoutId %>" />
	<aui:input id="addLayoutType" name="type" type="hidden" value="portlet" />
	<aui:input id="addLayoutPrototypeId" name="layoutPrototypeId" type="hidden" value="" />
	<aui:input id="addLayoutExplicitCreation" name="explicitCreation" type="hidden" value="<%= true %>" />

	<liferay-ui:error exception="<%= LayoutTypeException.class %>">

		<%
		LayoutTypeException lte = (LayoutTypeException)errorException;

		String type = BeanParamUtil.getString(selLayout, request, "type");
		%>

		<c:if test="<%= lte.getType() == LayoutTypeException.FIRST_LAYOUT %>">
			<liferay-ui:message arguments='<%= Validator.isNull(lte.getLayoutType()) ? type : "layout.types." + lte.getLayoutType() %>' key="the-first-page-cannot-be-of-type-x" />
		</c:if>

		<c:if test="<%= lte.getType() == LayoutTypeException.NOT_INSTANCEABLE %>">
			<liferay-ui:message arguments="<%= type %>" key="pages-of-type-x-cannot-be-selected" />
		</c:if>

		<c:if test="<%= lte.getType() == LayoutTypeException.NOT_PARENTABLE %>">
			<liferay-ui:message arguments="<%= type %>" key="pages-of-type-x-cannot-have-child-pages" />
		</c:if>
	</liferay-ui:error>

	<liferay-ui:error exception="<%= LayoutNameException.class %>" message="please-enter-a-valid-name" />

	<liferay-ui:error exception="<%= RequiredLayoutException.class %>">

		<%
		RequiredLayoutException rle = (RequiredLayoutException)errorException;
		%>

		<c:if test="<%= rle.getType() == RequiredLayoutException.AT_LEAST_ONE %>">
			<liferay-ui:message key="you-must-have-at-least-one-page" />
		</c:if>
	</liferay-ui:error>

	<aui:model-context model="<%= Layout.class %>" />

	<aui:fieldset-group markupView="lexicon">
		<aui:fieldset>
			<aui:input autoFocus="<%= true %>" id="addLayoutName" name="name" />

			<aui:input helpMessage="if-enabled-this-page-does-not-show-up-in-the-navigation-menu" id="addLayoutHidden" label="hide-from-navigation-menu" name="hidden" type="toggle-switch" />

			<h4><liferay-ui:message key="type" /></h4>

			<div id="<portlet:namespace />templateList">
				<c:if test='<%= ArrayUtil.contains(types, "portlet") %>'>
					<div data-search='<%= HtmlUtil.escape(LanguageUtil.get(request, "empty-page")) %>'>
						<div class="active lfr-page-template-title toggler-header toggler-header-expanded" data-type="portlet">
							<aui:input checked="<%= true %>" id="addLayoutSelectedPageTemplateBlank" label="empty-page" name="selectedPageTemplate" type="radio" />
						</div>

						<div class="lfr-page-template-options toggler-content toggler-content-expanded">
							<p class="small text-muted">
								<liferay-ui:message key="empty-page-description" />
							</p>

							<liferay-ui:layout-templates-list
								layoutTemplateId="<%= PropsValues.DEFAULT_LAYOUT_TEMPLATE_ID %>"
								layoutTemplateIdPrefix="addLayout"
								layoutTemplates="<%= LayoutTemplateLocalServiceUtil.getLayoutTemplates(layout.getThemeId()) %>"
							/>
						</div>
					</div>
				</c:if>

				<%
				List<LayoutPrototype> layoutPrototypes = LayoutPrototypeServiceUtil.search(company.getCompanyId(), Boolean.TRUE, null);

				for (LayoutPrototype layoutPrototype : layoutPrototypes) {
					String name = HtmlUtil.escape(layoutPrototype.getName(locale));
				%>

					<div data-search="<%= name %>">
						<div class="lfr-page-template-title toggler-header toggler-header-collapsed" data-prototype-id="<%= layoutPrototype.getLayoutPrototypeId() %>">
							<aui:input id='<%= "addLayoutSelectedPageTemplate" + layoutPrototype.getUuid() %>' label="<%= name %>" name="selectedPageTemplate" type="radio" />
						</div>

						<div class="lfr-page-template-options toggler-content toggler-content-collapsed">
							<p class="small text-muted">
								<%= HtmlUtil.escape(layoutPrototype.getDescription(locale)) %>
							</p>

							<aui:input helpMessage="if-enabled-this-page-will-inherit-changes-made-to-the-page-template" id='<%= "addLayoutLayoutPrototypeLinkEnabled" + layoutPrototype.getUuid() %>' label="inherit-changes" name='<%= "layoutPrototypeLinkEnabled" + layoutPrototype.getUuid() %>' type="toggle-switch" value="<%= PropsValues.LAYOUT_PROTOTYPE_LINK_ENABLED_DEFAULT %>" />
						</div>
					</div>

				<%
				}

				liferayPortletRequest.setAttribute(WebKeys.LAYOUT_DESCRIPTIONS, layoutsAdminDisplayContext.getLayoutDescriptions());

				int layoutsCount = LayoutLocalServiceUtil.getLayoutsCount(layoutsAdminDisplayContext.getGroup(), privateLayout);

				for (String type : types) {
					if (type.equals("portlet")) {
						continue;
					}

					LayoutTypeController layoutTypeController = LayoutTypeControllerTracker.getLayoutTypeController(type);

					if (!layoutTypeController.isInstanceable()) {
						continue;
					}

					ResourceBundle layoutTypeResourceBundle = ResourceBundleUtil.getBundle("content.Language", locale, layoutTypeController.getClass());
				%>

					<div data-search='<%= LanguageUtil.get(request, layoutTypeResourceBundle, "layout.types." + type) %>'>
						<div class="lfr-page-template-title toggler-header toggler-header-collapsed" data-type="<%= type %>">
							<aui:input disabled="<%= (layoutsCount == 0) && !layoutTypeController.isFirstPageable() %>" id='<%= "addLayoutSelectedPageTemplate" + type %>' label='<%= LanguageUtil.get(request, layoutTypeResourceBundle, "layout.types." + type) %>' name="selectedPageTemplate" type="radio" />
						</div>

						<div class="lfr-page-template-options toggler-content toggler-content-collapsed">
							<p class="small text-muted">
								<%= LanguageUtil.get(request, layoutTypeResourceBundle, "layout.types." + type + ".description") %>
							</p>

							<%= layoutTypeController.includeEditContent(request, response, selLayout) %>
						</div>
					</div>

				<%
				}
				%>

				<c:if test='<%= ArrayUtil.contains(types, "portlet") %>'>
					<div data-search="portlet">
						<div class="lfr-page-template-title toggler-header toggler-header-collapsed" data-type="portlet">
							<aui:input id="addLayoutSelectedPageTemplateCopyOfPage" label="copy-of-a-page" name="selectedPageTemplate" type="radio" />
						</div>

						<div class="lfr-page-template-options toggler-content toggler-content-collapsed">
							<p class="small text-muted">
								<liferay-ui:message key="copy-of-a-page-description" />
							</p>

							<liferay-util:include page="/html/portal/layout/edit/portlet_applications.jsp">
								<liferay-util:param name="copyLayoutIdPrefix" value="addLayout" />
							</liferay-util:include>
						</div>
					</div>
				</c:if>
			</div>
		</aui:fieldset>
	</aui:fieldset-group>

	<aui:button-row>
		<aui:button cssClass="btn-lg" type="submit" value="add-page" />

		<c:if test="<%= Validator.isNotNull(backURL) %>">
			<aui:button cssClass="btn-lg" href="<%= backURL %>" type="cancel" />
		</c:if>
	</aui:button-row>
</aui:form>

<aui:script use="aui-toggler">
	A.on(
		'domready',
		function(event) {
			new A.TogglerDelegate(
				{
					animated: true,
					closeAllOnExpand: true,
					container: A.one('#<portlet:namespace />templateList'),
					content: '.lfr-page-template-options',
					expanded: false,
					header: '.lfr-page-template-title',
					on: {
						'toggler:expandedChange': function(event) {
							var nodeList = A.one('#<portlet:namespace />templateList');

							if (event.newVal) {
								if (nodeList) {
									nodeList.all('.active').removeClass('active');
								}

								var header = event.target.get('header');

								if (header) {
									var selectedType = header.attr('data-type');

									var selectedPrototypeId = header.attr('data-prototype-id');

									var selectedPageTemplate = header.one('input');

									var addLayoutType = A.one('#<portlet:namespace />addLayoutType');

									var addLayoutPrototypeId = A.one('#<portlet:namespace />addLayoutPrototypeId');

									selectedPageTemplate.attr('checked', true);

									header.addClass('active');

									addLayoutType.attr('value', selectedType);

									addLayoutPrototypeId.attr('value', selectedPrototypeId);
								}
							}
						}
					}
				}
			);
		}
	);
</aui:script>