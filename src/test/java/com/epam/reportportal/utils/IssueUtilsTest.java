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
	public void method_with_contains_filter_issue_annotation() {
	}

	@com.epam.reportportal.annotations.Issue(value = "ISSUE-2", filter = {
			@com.epam.reportportal.annotations.TestFilter(param = @com.epam.reportportal.annotations.TestParamFilter(paramIndex = 0, valueContains = "amVal")) })
	public void method_with_param_index_and_contains_filter_issue_annotation() {
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

	public static Iterable<Object[]> filterIssueAnnotation() throws NoSuchMethodException {
		return Arrays.asList(
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
						IssueUtilsTest.class.getMethod("method_with_contains_filter_issue_annotation").getAnnotation(com.epam.reportportal.annotations.Issue.class),
						"testName", Arrays.asList(uniqueParameter(), simpleParameter()), true },
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
						IssueUtilsTest.class.getMethod("method_with_contains_filter_issue_annotation").getAnnotation(com.epam.reportportal.annotations.Issue.class),
						"testName", Arrays.asList(uniqueParameter(), longerParameter()), false },
				new Object[] {
						IssueUtilsTest.class.getMethod("method_with_param_index_and_contains_filter_issue_annotation").getAnnotation(com.epam.reportportal.annotations.Issue.class),
						"testName", Arrays.asList(uniqueParameter(), simpleParameter()), false }
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
		} else {
			assertThat(issue, nullValue());
		}
	}
}
