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
import org.apache.commons.lang3.StringUtils;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

/**
 * Utility class for creating {@link Issue} objects from different annotation sources taking into account test name and parameters
 * filtering.
 */
public class IssueUtils {

	private IssueUtils() {
		throw new IllegalStateException("Static only class");
	}

	private static boolean matchName(@Nonnull String testName, @Nonnull com.epam.reportportal.annotations.TestNameFilter filter) {
		boolean startsWith = filter.startsWith().isEmpty() || testName.startsWith(filter.startsWith());
		boolean endsWith = filter.endsWith().isEmpty() || testName.endsWith(filter.endsWith());
		boolean contains = filter.contains().isEmpty() || testName.contains(filter.contains());
		return startsWith && endsWith && contains;
	}

	private static boolean matchParameter(@Nonnull ParameterResource param,
			@Nonnull com.epam.reportportal.annotations.TestParamFilter filter) {
		boolean nameStartsWith = filter.nameStartsWith().isEmpty() || param.getKey().startsWith(filter.nameStartsWith());
		boolean nameEndsWith = filter.nameEndsWith().isEmpty() || param.getKey().endsWith(filter.nameEndsWith());
		boolean nameContains = filter.nameContains().isEmpty() || param.getKey().contains(filter.nameContains());
		boolean valueStartsWith = filter.valueStartsWith().isEmpty() || param.getValue().startsWith(filter.valueStartsWith());
		boolean valueEndsWith = filter.valueEndsWith().isEmpty() || param.getValue().endsWith(filter.valueEndsWith());
		boolean valueContains = filter.valueContains().isEmpty() || param.getValue().contains(filter.valueContains());
		return nameStartsWith && nameEndsWith && nameContains && valueStartsWith && valueEndsWith && valueContains;
	}

	/**
	 * Creates {@link Issue} object from the provided {@link com.epam.reportportal.annotations.Issue} annotation if it is not null and
	 * passed {@link com.epam.reportportal.annotations.TestFilter} filters suit the provided test name and parameters.
	 *
	 * @param issue      the annotation
	 * @param testName   test name
	 * @param parameters test parameters
	 * @return the result object or null if the annotation is null or filters do not suit
	 */
	@Nullable
	public static Issue createIssue(@Nullable com.epam.reportportal.annotations.Issue issue, @Nonnull String testName,
			@Nonnull List<ParameterResource> parameters) {
		if (issue == null || StringUtils.isBlank(issue.value())) {
			return null;
		}

		// Check if the test name and parameters match the filters
		boolean matches = Arrays.stream(issue.filter()).allMatch(filter -> {
			boolean nameMatches = Arrays.stream(filter.name()).allMatch(nameFilter -> matchName(testName, nameFilter));

			boolean paramMatches = Arrays.stream(filter.param()).allMatch(paramFilter -> {
				if (paramFilter.paramIndex() == -1) {
					return parameters.stream().anyMatch(param -> matchParameter(param, paramFilter));
				} else {
					int paramIndex = paramFilter.paramIndex();
					if (paramIndex < 0 || paramIndex >= parameters.size()) {
						return false;
					}
					ParameterResource param = parameters.get(paramIndex);
					return matchParameter(param, paramFilter);
				}
			});
			return nameMatches && paramMatches;
		});

		if (!matches) {
			return null;
		}

		// Create the Issue object
		Issue result = new Issue();
		result.setIssueType(issue.value());
		if (StringUtils.isNotBlank(issue.comment())) {
			result.setComment(issue.comment());
		}
		result.setAutoAnalyzed(false);
		result.setIgnoreAnalyzer(false);

		// Set external issues if any
		Set<Issue.ExternalSystemIssue> externalIssues = Arrays.stream(issue.external())
				.filter(ext -> StringUtils.isNotBlank(ext.value()))
				.map(ext -> {
					Issue.ExternalSystemIssue externalIssue = new Issue.ExternalSystemIssue();
					externalIssue.setTicketId(ext.value());
					if (StringUtils.isNotBlank(ext.btsUrl())) {
						externalIssue.setBtsUrl(ext.btsUrl());
					}
					if (StringUtils.isNotBlank(ext.btsProject())) {
						externalIssue.setBtsProject(ext.btsProject());
					}
					if (StringUtils.isNotBlank(ext.urlPattern())) {
						externalIssue.setUrl(ext.urlPattern());
					}
					return externalIssue;
				})
				.collect(Collectors.toSet());

		if (!externalIssues.isEmpty()) {
			result.setExternalSystemIssues(externalIssues);
		}

		return result;
	}

	/**
	 * Creates {@link Issue} object from the provided list of {@link com.epam.reportportal.annotations.Issue} annotations if it is not
	 * empty and at least one of them suits corresponding {@link com.epam.reportportal.annotations.TestFilter} filters by provided test name
	 * and parameters. The first suitable annotation will be taken to create the result object.
	 *
	 * @param issues     the list of annotations
	 * @param testName   test name
	 * @param parameters test parameters
	 * @return the result object or null if the list is empty or filters do not suit
	 */
	@Nullable
	public static Issue createIssue(@Nullable List<com.epam.reportportal.annotations.Issue> issues, @Nonnull String testName,
			@Nonnull List<ParameterResource> parameters) {
		if (ofNullable(issues).filter(i -> !i.isEmpty()).isPresent()) {
			return issues.stream().map(i -> createIssue(i, testName, parameters)).filter(Objects::nonNull).findFirst().orElse(null);
		}
		return null;
	}

	/**
	 * Creates {@link Issue} object from the provided {@link com.epam.reportportal.annotations.Issue} annotation array if it is not null or
	 * empty and at least one of them suits corresponding {@link com.epam.reportportal.annotations.TestFilter} filters by provided test name
	 * and parameters. The first suitable annotation will be taken to create the result object.
	 *
	 * @param issues     the array of annotations
	 * @param testName   test name
	 * @param parameters test parameters
	 * @return the result object or null if the list is empty or filters do not suit
	 */
	@Nullable
	public static Issue createIssue(@Nullable com.epam.reportportal.annotations.Issue[] issues, @Nonnull String testName,
			@Nonnull List<ParameterResource> parameters) {
		return ofNullable(issues).map(Arrays::asList).filter(i -> !i.isEmpty()).map(i -> createIssue(i, testName, parameters)).orElse(null);
	}

	/**
	 * Creates {@link Issue} object from the provided {@link Issues} annotation if it is not null and at least one of the annotations in the
	 * value array suits corresponding {@link com.epam.reportportal.annotations.TestFilter} filters by provided test name and parameters.
	 * The first suitable annotation will be taken to create the result object.
	 *
	 * @param issues     the annotation
	 * @param testName   test name
	 * @param parameters test parameters
	 * @return the result object or null if the annotation is null or filters do not suit
	 */
	@Nullable
	public static Issue createIssue(@Nullable Issues issues, @Nonnull String testName, @Nonnull List<ParameterResource> parameters) {
		return ofNullable(issues).map(Issues::value)
				.map(Arrays::asList)
				.filter(i -> !i.isEmpty())
				.map(i -> createIssue(i, testName, parameters))
				.orElse(null);
	}
}
