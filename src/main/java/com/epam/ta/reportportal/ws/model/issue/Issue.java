/*
 * Copyright 2025 EPAM Systems
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

package com.epam.ta.reportportal.ws.model.issue;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;

/**
 * Test item issue
 */
@JsonInclude(Include.NON_NULL)
public class Issue {

	@JsonProperty(value = "issueType", required = true)
	@JsonAlias({ "issueType", "issue_type" })
	private String issueType;

	@JsonProperty(value = "comment")
	private String comment;

	@JsonProperty(value = "autoAnalyzed")
	private boolean autoAnalyzed;

	@JsonProperty(value = "ignoreAnalyzer")
	private boolean ignoreAnalyzer;

	@JsonProperty(value = "externalSystemIssues")
	private Set<ExternalSystemIssue> externalSystemIssues;

	@JsonInclude(Include.NON_NULL)
	public static class ExternalSystemIssue {

		@JsonProperty(value = "ticketId")
		private String ticketId;

		@JsonProperty(value = "submitDate")
		private Comparable<? extends Comparable<?>> submitDate;

		@JsonProperty(value = "btsUrl")
		private String btsUrl;

		@JsonProperty(value = "btsProject")
		private String btsProject;

		@JsonProperty(value = "url")
		private String url;

		public void setTicketId(String ticketId) {
			this.ticketId = ticketId;
		}

		public String getTicketId() {
			return ticketId;
		}

		public Object getSubmitDate() {
			return submitDate;
		}

		public void setSubmitDate(Comparable<? extends Comparable<?>> submitDate) {
			this.submitDate = submitDate;
		}

		public String getBtsUrl() {
			return btsUrl;
		}

		public void setBtsUrl(String btsUrl) {
			this.btsUrl = btsUrl;
		}

		public String getBtsProject() {
			return btsProject;
		}

		public void setBtsProject(String btsProject) {
			this.btsProject = btsProject;
		}

		public void setUrl(String value) {
			this.url = value;
		}

		public String getUrl() {
			return url;
		}
	}

	public void setExternalSystemIssues(Set<ExternalSystemIssue> externalSystemIssues) {
		this.externalSystemIssues = externalSystemIssues;
	}

	public Set<ExternalSystemIssue> getExternalSystemIssues() {
		return externalSystemIssues;
	}

	public String getIssueType() {
		return issueType;
	}

	public void setIssueType(String issueType) {
		this.issueType = issueType;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public boolean getAutoAnalyzed() {
		return autoAnalyzed;
	}

	public void setAutoAnalyzed(boolean autoAnalyzed) {
		this.autoAnalyzed = autoAnalyzed;
	}

	public boolean getIgnoreAnalyzer() {
		return ignoreAnalyzer;
	}

	public void setIgnoreAnalyzer(boolean ignoreAnalyzer) {
		this.ignoreAnalyzer = ignoreAnalyzer;
	}
} 