/*
 * Copyright 2024 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.reportportal.utils;

import com.epam.reportportal.annotations.ExternalIssue;
import com.epam.reportportal.annotations.Issues;
import com.epam.ta.reportportal.ws.model.ParameterResource;
import com.epam.ta.reportportal.ws.model.issue.Issue;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.platform.commons.util.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class IssueUtilsTest {

	@com.epam.reportportal.annotations.Issue("")
	public void method_with_empty_issue_annotation() {
	}

	@com.epam.reportportal.annotations.Issue("   ")
	public void method_with_blank_issue_annotation() {
	}

	public static Iterable<Object[]> emptyIssueAnnotation() throws NoSuchMethodException {
		return Arrays.asList(
				new Object[] {
						IssueUtilsTest.class.getMethod("method_with_empty_issue_annotation").getAnnotation(com.epam.reportportal.annotations.Issue.class) },
				new Object[] {
						IssueUtilsTest.class.getMethod("method_with_blank_issue_annotation").getAnnotation(com.epam.reportportal.annotations.Issue.class) },
				new Object[] { null }
		);
	}

	@ParameterizedTest
	@MethodSource("emptyIssueAnnotation")
	public void test_create_issue_with_empty_or_null_annotation(com.epam.reportportal.annotations.Issue issueAnnotation) {
		Issue issue = IssueUtils.createIssue(issueAnnotation, "testName", Collections.emptyList());
		assertThat(issue, nullValue());
	}

	@com.epam.reportportal.annotations.Issues({})
	public static void method_with_empty_issues_annotation() {
	}

	@com.epam.reportportal.annotations.Issues({ @com.epam.reportportal.annotations.Issue("") })
	public static void method_with_one_empty_issue_in_issues_annotation() {
	}

	public static Iterable<Object[]> emptyIssuesAnnotation() throws NoSuchMethodException {
		return Arrays.asList(
				new Object[] {
						IssueUtilsTest.class.getMethod("method_with_empty_issues_annotation").getAnnotation(com.epam.reportportal.annotations.Issues.class) },
				new Object[] {
						IssueUtilsTest.class.getMethod("method_with_one_empty_issue_in_issues_annotation").getAnnotation(com.epam.reportportal.annotations.Issues.class) },
				new Object[] { null }
		);
	}

	@ParameterizedTest
	@MethodSource("emptyIssuesAnnotation")
	public void test_create_issue_with_empty_or_null_issues_annotation(com.epam.reportportal.annotations.Issues issuesAnnotation) {
		Issue issue = IssueUtils.createIssue(issuesAnnotation, "testName", Collections.emptyList());
		assertThat(issue, nullValue());
	}

	@com.epam.reportportal.annotations.Issue(value = "ISSUE-1", comment = "Test issue")
	public void method_with_valid_issue_annotation_and_comment() {
	}

	@com.epam.reportportal.annotations.Issue(value = "ISSUE-1")
	public void method_with_valid_issue_annotation() {
	}

	public static Iterable<Object[]> validIssueAnnotation() throws NoSuchMethodException {
		return Arrays.asList(
				new Object[] {
						IssueUtilsTest.class.getMethod("method_with_valid_issue_annotation_and_comment").getAnnotation(com.epam.reportportal.annotations.Issue.class) },
				new Object[] {
						IssueUtilsTest.class.getMethod("method_with_valid_issue_annotation").getAnnotation(com.epam.reportportal.annotations.Issue.class) }
		);
	}

	@ParameterizedTest
	@MethodSource("validIssueAnnotation")
	public void test_create_issue_with_valid_annotation(com.epam.reportportal.annotations.Issue issueAnnotation) {
		Issue issue = IssueUtils.createIssue(issueAnnotation, "testName", Collections.emptyList());
		assertThat(issue, notNullValue());
		assertThat(issue.getIssueType(), equalTo(issueAnnotation.value()));
		assertThat(issue.getComment(), equalTo(StringUtils.isBlank(issueAnnotation.comment()) ? null : issueAnnotation.comment()));
		assertThat(issue.getExternalSystemIssues(), nullValue());
	}

	@Issues(@com.epam.reportportal.annotations.Issue(value = "ISSUE-1"))
	public void method_with_valid_issues_annotation() {
	}

	@com.epam.reportportal.annotations.Issue(value = "ISSUE-1")
	@com.epam.reportportal.annotations.Issue(value = "ISSUE-2")
	public void method_with_two_valid_issue_annotation() {
	}

	public static Iterable<Object[]> validIssuesAnnotation() throws NoSuchMethodException {
		return Arrays.asList(
				new Object[] {
						IssueUtilsTest.class.getMethod("method_with_valid_issues_annotation").getAnnotation(com.epam.reportportal.annotations.Issues.class) },
				new Object[] {
						IssueUtilsTest.class.getMethod("method_with_two_valid_issue_annotation").getAnnotation(com.epam.reportportal.annotations.Issues.class) }
		);
	}

	@ParameterizedTest
	@MethodSource("validIssuesAnnotation")
	public void test_create_issue_with_valid_issues_annotation(com.epam.reportportal.annotations.Issues issuesAnnotation) {
		Issue issue = IssueUtils.createIssue(issuesAnnotation, "testName", Collections.emptyList());
		assertThat(issue, notNullValue());
		assertThat(issue.getIssueType(), equalTo(issuesAnnotation.value()[0].value()));
		assertThat(issue.getExternalSystemIssues(), nullValue());
	}

	@com.epam.reportportal.annotations.Issue(value = "ISSUE-2", filter = {
			@com.epam.reportportal.annotations.TestFilter(name = @com.epam.reportportal.annotations.TestNameFilter(startsWith = "test")) })
	public void method_with_name_starts_filter_issue_annotation() {
	}

	@com.epam.reportportal.annotations.Issue(value = "ISSUE-2", filter = {
			@com.epam.reportportal.annotations.TestFilter(name = @com.epam.reportportal.annotations.TestNameFilter(contains = "stNa")) })
	public void method_with_name_contains_filter_issue_annotation() {
	}

	@com.epam.reportportal.annotations.Issue(value = "ISSUE-2", filter = {
			@com.epam.reportportal.annotations.TestFilter(name = @com.epam.reportportal.annotations.TestNameFilter(endsWith = "Name")) })
	public void method_with_name_ends_filter_issue_annotation() {
	}

	@com.epam.reportportal.annotations.Issue(value = "ISSUE-2", filter = {
			@com.epam.reportportal.annotations.TestFilter(name = @com.epam.reportportal.annotations.TestNameFilter(startsWith = "test", endsWith = "Name")) })
	public void method_with_name_starts_and_ends_filter_issue_annotation() {
	}

	@com.epam.reportportal.annotations.Issue(value = "ISSUE-2", filter = {
			@com.epam.reportportal.annotations.TestFilter(name = @com.epam.reportportal.annotations.TestNameFilter(startsWith = "test", contains = "stNa")) })
	public void method_with_name_starts_and_contains_filter_issue_annotation() {
	}

	@com.epam.reportportal.annotations.Issue(value = "ISSUE-2", filter = {
			@com.epam.reportportal.annotations.TestFilter(param = @com.epam.reportportal.annotations.TestParamFilter(valueContains = "amVal")) })
	public void method_with_value_contains_filter_issue_annotation() {
	}

	@com.epam.reportportal.annotations.Issue(value = "ISSUE-2", filter = {
			@com.epam.reportportal.annotations.TestFilter(param = @com.epam.reportportal.annotations.TestParamFilter(paramIndex = 0, valueContains = "amVal")) })
	public void method_with_param_index_and_contains_filter_issue_annotation() {
	}

	@com.epam.reportportal.annotations.Issue(value = "ISSUE-2", filter = {
			@com.epam.reportportal.annotations.TestFilter(param = @com.epam.reportportal.annotations.TestParamFilter(nameStartsWith = "param")) })
	public void method_with_param_name_starts_filter_issue_annotation() {
	}

	@com.epam.reportportal.annotations.Issue(value = "ISSUE-2", filter = {
			@com.epam.reportportal.annotations.TestFilter(param = @com.epam.reportportal.annotations.TestParamFilter(nameEndsWith = "Key")) })
	public void method_with_param_name_ends_filter_issue_annotation() {
	}

	@com.epam.reportportal.annotations.Issue(value = "ISSUE-2", filter = {
			@com.epam.reportportal.annotations.TestFilter(param = @com.epam.reportportal.annotations.TestParamFilter(nameStartsWith = "param", nameEndsWith = "Key")) })
	public void method_with_param_name_starts_ends_filter_issue_annotation() {
	}

	@com.epam.reportportal.annotations.Issue(value = "ISSUE-2", filter = {
			@com.epam.reportportal.annotations.TestFilter(param = @com.epam.reportportal.annotations.TestParamFilter(nameContains = "amKe")) })
	public void method_with_param_name_contains_filter_issue_annotation() {
	}

	@com.epam.reportportal.annotations.Issue(value = "ISSUE-2", filter = {
			@com.epam.reportportal.annotations.TestFilter(param = @com.epam.reportportal.annotations.TestParamFilter(paramIndex = 1, valueStartsWith = "param")) })
	public void method_with_param_index_and_value_starts_filter_issue_annotation() {
	}

	@com.epam.reportportal.annotations.Issue(value = "ISSUE-2", filter = {
			@com.epam.reportportal.annotations.TestFilter(param = @com.epam.reportportal.annotations.TestParamFilter(valueStartsWith = "param")) })
	public void method_with_value_starts_filter_issue_annotation() {
	}

	@com.epam.reportportal.annotations.Issue(value = "ISSUE-2", filter = {
			@com.epam.reportportal.annotations.TestFilter(param = @com.epam.reportportal.annotations.TestParamFilter(valueEndsWith = "Value")) })
	public void method_with_value_ends_filter_issue_annotation() {
	}

	private static ParameterResource simpleParameter() {
		ParameterResource param = new ParameterResource();
		param.setKey("paramKey");
		param.setValue("paramValue");
		return param;
	}

	private static ParameterResource longerParameter() {
		ParameterResource param = new ParameterResource();
		param.setKey("paramSomethingInTheMiddleKey");
		param.setValue("paramSomethingInTheMiddleValue");
		return param;
	}

	private static ParameterResource uniqueParameter() {
		ParameterResource param = new ParameterResource();
		param.setKey("uniqueKey");
		param.setValue("uniqueValue");
		return param;
	}

	private static ParameterResource fullUniqueParameter() {
		ParameterResource param = new ParameterResource();
		param.setKey("uniqueName");
		param.setValue("uniqueRate");
		return param;
	}

	public static Iterable<Object[]> filterIssueAnnotation() throws NoSuchMethodException {
		return Arrays.asList(
				// Positive cases
				new Object[] {
						IssueUtilsTest.class.getMethod("method_with_name_starts_filter_issue_annotation").getAnnotation(com.epam.reportportal.annotations.Issue.class),
						"testName", Collections.emptyList(), true },
				new Object[] {
						IssueUtilsTest.class.getMethod("method_with_name_contains_filter_issue_annotation").getAnnotation(com.epam.reportportal.annotations.Issue.class),
						"testName", Collections.emptyList(), true },
				new Object[] {
						IssueUtilsTest.class.getMethod("method_with_name_ends_filter_issue_annotation").getAnnotation(com.epam.reportportal.annotations.Issue.class),
						"testName", Collections.emptyList(), true },
				new Object[] {
						IssueUtilsTest.class.getMethod("method_with_name_starts_and_ends_filter_issue_annotation").getAnnotation(com.epam.reportportal.annotations.Issue.class),
						"testSomethingInTheMiddleName", Collections.emptyList(), true },
				new Object[] {
						IssueUtilsTest.class.getMethod("method_with_name_starts_and_contains_filter_issue_annotation").getAnnotation(com.epam.reportportal.annotations.Issue.class),
						"testName", Collections.emptyList(), true },
				new Object[] {
						IssueUtilsTest.class.getMethod("method_with_name_starts_and_contains_filter_issue_annotation").getAnnotation(com.epam.reportportal.annotations.Issue.class),
						"testName", Collections.singletonList(simpleParameter()), true },
				new Object[] {
						IssueUtilsTest.class.getMethod("method_with_param_index_and_contains_filter_issue_annotation").getAnnotation(com.epam.reportportal.annotations.Issue.class),
						"testName", Collections.singletonList(simpleParameter()), true },
				new Object[] {
						IssueUtilsTest.class.getMethod("method_with_value_contains_filter_issue_annotation").getAnnotation(com.epam.reportportal.annotations.Issue.class),
						"testName", Arrays.asList(uniqueParameter(), simpleParameter()), true },
				new Object[] {
						IssueUtilsTest.class.getMethod("method_with_param_name_starts_filter_issue_annotation").getAnnotation(com.epam.reportportal.annotations.Issue.class),
						"testName", Arrays.asList(uniqueParameter(), simpleParameter()), true },
				new Object[] {
						IssueUtilsTest.class.getMethod("method_with_param_name_ends_filter_issue_annotation").getAnnotation(com.epam.reportportal.annotations.Issue.class),
						"testName", Arrays.asList(uniqueParameter(), simpleParameter()), true },
				new Object[] {
						IssueUtilsTest.class.getMethod("method_with_param_name_starts_ends_filter_issue_annotation").getAnnotation(com.epam.reportportal.annotations.Issue.class),
						"testName", Arrays.asList(uniqueParameter(), simpleParameter()), true },
				new Object[] {
						IssueUtilsTest.class.getMethod("method_with_param_name_contains_filter_issue_annotation").getAnnotation(com.epam.reportportal.annotations.Issue.class),
						"testName", Arrays.asList(uniqueParameter(), simpleParameter()), true },
				new Object[] {
						IssueUtilsTest.class.getMethod("method_with_param_index_and_value_starts_filter_issue_annotation").getAnnotation(com.epam.reportportal.annotations.Issue.class),
						"testName", Arrays.asList(uniqueParameter(), simpleParameter()), true },
				new Object[] {
						IssueUtilsTest.class.getMethod("method_with_value_starts_filter_issue_annotation").getAnnotation(com.epam.reportportal.annotations.Issue.class),
						"testName", Arrays.asList(uniqueParameter(), simpleParameter()), true },
				new Object[] {
						IssueUtilsTest.class.getMethod("method_with_value_ends_filter_issue_annotation").getAnnotation(com.epam.reportportal.annotations.Issue.class),
						"testName", Arrays.asList(uniqueParameter(), simpleParameter()), true },

				// Negative cases
				new Object[] {
						IssueUtilsTest.class.getMethod("method_with_name_starts_filter_issue_annotation").getAnnotation(com.epam.reportportal.annotations.Issue.class),
						"nameTest", Collections.emptyList(), false },
				new Object[] {
						IssueUtilsTest.class.getMethod("method_with_name_contains_filter_issue_annotation").getAnnotation(com.epam.reportportal.annotations.Issue.class),
						"nameTest", Collections.emptyList(), false },
				new Object[] {
						IssueUtilsTest.class.getMethod("method_with_name_ends_filter_issue_annotation").getAnnotation(com.epam.reportportal.annotations.Issue.class),
						"nameTest", Collections.emptyList(), false },
				new Object[] {
						IssueUtilsTest.class.getMethod("method_with_name_starts_and_ends_filter_issue_annotation").getAnnotation(com.epam.reportportal.annotations.Issue.class),
						"nameSomethingInTheMiddleTest", Collections.emptyList(), false },
				new Object[] {
						IssueUtilsTest.class.getMethod("method_with_name_starts_and_ends_filter_issue_annotation").getAnnotation(com.epam.reportportal.annotations.Issue.class),
						"testSomethingInTheMiddle", Collections.emptyList(), false },
				new Object[] {
						IssueUtilsTest.class.getMethod("method_with_name_starts_and_contains_filter_issue_annotation").getAnnotation(com.epam.reportportal.annotations.Issue.class),
						"testSomethingInTheMiddle", Collections.emptyList(), false },
				new Object[] {
						IssueUtilsTest.class.getMethod("method_with_name_starts_and_contains_filter_issue_annotation").getAnnotation(com.epam.reportportal.annotations.Issue.class),
						"testSomethingInTheMiddle", Collections.singletonList(simpleParameter()), false },
				new Object[] {
						IssueUtilsTest.class.getMethod("method_with_param_index_and_contains_filter_issue_annotation").getAnnotation(com.epam.reportportal.annotations.Issue.class),
						"testName", Collections.singletonList(uniqueParameter()), false },
				new Object[] {
						IssueUtilsTest.class.getMethod("method_with_value_contains_filter_issue_annotation").getAnnotation(com.epam.reportportal.annotations.Issue.class),
						"testName", Arrays.asList(uniqueParameter(), longerParameter()), false },
				new Object[] {
						IssueUtilsTest.class.getMethod("method_with_param_index_and_contains_filter_issue_annotation").getAnnotation(com.epam.reportportal.annotations.Issue.class),
						"testName", Arrays.asList(uniqueParameter(), simpleParameter()), false },
				new Object[] {
						IssueUtilsTest.class.getMethod("method_with_param_name_starts_filter_issue_annotation").getAnnotation(com.epam.reportportal.annotations.Issue.class),
						"testName", Arrays.asList(uniqueParameter(), fullUniqueParameter()), false },
				new Object[] {
						IssueUtilsTest.class.getMethod("method_with_param_name_ends_filter_issue_annotation").getAnnotation(com.epam.reportportal.annotations.Issue.class),
						"testName", Collections.singletonList(fullUniqueParameter()), false },
				new Object[] {
						IssueUtilsTest.class.getMethod("method_with_param_name_starts_ends_filter_issue_annotation").getAnnotation(com.epam.reportportal.annotations.Issue.class),
						"testName", Arrays.asList(uniqueParameter(), fullUniqueParameter()), false },
				new Object[] {
						IssueUtilsTest.class.getMethod("method_with_param_name_contains_filter_issue_annotation").getAnnotation(com.epam.reportportal.annotations.Issue.class),
						"testName", Arrays.asList(uniqueParameter(), fullUniqueParameter()), false },
				new Object[] {
						IssueUtilsTest.class.getMethod("method_with_param_index_and_value_starts_filter_issue_annotation").getAnnotation(com.epam.reportportal.annotations.Issue.class),
						"testName", Arrays.asList(simpleParameter(), fullUniqueParameter()), false },
				new Object[] {
						IssueUtilsTest.class.getMethod("method_with_param_index_and_value_starts_filter_issue_annotation").getAnnotation(com.epam.reportportal.annotations.Issue.class),
						"testName", Arrays.asList(uniqueParameter(), fullUniqueParameter()), false },
				new Object[] {
						IssueUtilsTest.class.getMethod("method_with_value_ends_filter_issue_annotation").getAnnotation(com.epam.reportportal.annotations.Issue.class),
						"testName", Collections.singletonList(fullUniqueParameter()), false }

		);
	}

	@ParameterizedTest
	@MethodSource("filterIssueAnnotation")
	public void test_create_issue_with_filter_annotation(com.epam.reportportal.annotations.Issue issueAnnotation, String testName,
			List<ParameterResource> parameters, boolean expected) {
		Issue issue = IssueUtils.createIssue(issueAnnotation, testName, parameters);
		if (expected) {
			assertThat(issue, notNullValue());
			assertThat(issue.getIssueType(), equalTo("ISSUE-2"));
			assertThat(issue.getComment(), nullValue());
			assertThat(issue.getExternalSystemIssues(), nullValue());
		} else {
			assertThat(issue, nullValue());
		}
	}

	@com.epam.reportportal.annotations.Issue(value = "ISSUE-3", external = { @ExternalIssue("JIRA-123") })
	public void method_with_simple_external_issue_annotation() {
	}

	@com.epam.reportportal.annotations.Issue(value = "ISSUE-3", external = {
			@ExternalIssue(value = "JIRA-123", btsUrl = "https://example.com") })
	public void method_with_external_issue_bts_url_annotation() {
	}

	@com.epam.reportportal.annotations.Issue(value = "ISSUE-3", external = { @ExternalIssue(value = "JIRA-123", btsProject = "RPP") })
	public void method_with_external_issue_bts_project_annotation() {
	}

	@com.epam.reportportal.annotations.Issue(value = "ISSUE-3", external = {
			@ExternalIssue(value = "JIRA-123", urlPattern = "https://example.com/{bts_project}/{issue_id}") })
	public void method_with_external_issue_url_pattern_annotation() {
	}

	@com.epam.reportportal.annotations.Issue(value = "ISSUE-3", external = {
			@ExternalIssue(value = "JIRA-123", btsUrl = "https://example.com", btsProject = "RPP") })
	public void method_with_external_issue_bts_url_project_annotation() {
	}

	public static Iterable<Object[]> externalIssueAnnotation() throws NoSuchMethodException {
		return Arrays.asList(
				new Object[] {
						IssueUtilsTest.class.getMethod("method_with_simple_external_issue_annotation").getAnnotation(com.epam.reportportal.annotations.Issue.class), null, null, null },
				new Object[] {
						IssueUtilsTest.class.getMethod("method_with_external_issue_bts_url_annotation").getAnnotation(com.epam.reportportal.annotations.Issue.class), "https://example.com", null, null },
				new Object[] {
						IssueUtilsTest.class.getMethod("method_with_external_issue_bts_project_annotation").getAnnotation(com.epam.reportportal.annotations.Issue.class), null, "RPP", null },
				new Object[] {
						IssueUtilsTest.class.getMethod("method_with_external_issue_url_pattern_annotation").getAnnotation(com.epam.reportportal.annotations.Issue.class), null, null, "https://example.com/{bts_project}/{issue_id}" },
				new Object[] {
						IssueUtilsTest.class.getMethod("method_with_external_issue_bts_url_project_annotation").getAnnotation(com.epam.reportportal.annotations.Issue.class), "https://example.com", "RPP", null }
		);
	}

	@ParameterizedTest
	@MethodSource("externalIssueAnnotation")
	public void test_create_issue_with_filter_annotation(com.epam.reportportal.annotations.Issue issueAnnotation, String btsUrl, String btsProject, String urlPattern) {
		Issue issue = IssueUtils.createIssue(issueAnnotation, "testName", Collections.emptyList());
		assertThat(issue, notNullValue());
		assertThat(issue.getExternalSystemIssues(), allOf(notNullValue(), hasSize(1)));
		Issue.ExternalSystemIssue externalIssue = issue.getExternalSystemIssues().iterator().next();
		assertThat(externalIssue.getTicketId(), equalTo("JIRA-123"));
		assertThat(externalIssue.getBtsUrl(), equalTo(btsUrl));
		assertThat(externalIssue.getBtsProject(), equalTo(btsProject));
		assertThat(externalIssue.getUrl(), equalTo(urlPattern));
	}
}
