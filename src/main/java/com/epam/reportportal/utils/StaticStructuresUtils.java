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

import com.epam.reportportal.listeners.LogLevel;
import com.epam.ta.reportportal.ws.model.issue.Issue;
import com.epam.ta.reportportal.ws.model.log.SaveLogRQ;

import java.util.Calendar;
import java.util.Set;

public class StaticStructuresUtils {

	public static final Issue REDUNDANT_ISSUE = new Issue() {
		public static final String AUTOMATION_BUG_ISSUE_TYPE = "ab001";
		public static final String ISSUE_NOT_REMOVED = "Invalid Issue parameter for Passed Test Item. "
				+ "Did you forgot to remove Issue mark? Ignore this message with \"rp.bts.issue.fail=false\" property.";

		@Override
		public String getIssueType() {
			return AUTOMATION_BUG_ISSUE_TYPE;
		}

		@Override
		public String getComment() {
			return ISSUE_NOT_REMOVED;
		}

		@Override
		public void setComment(String comment) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setIssueType(String type) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setAutoAnalyzed(boolean autoAnalyzed) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setIgnoreAnalyzer(boolean ignoreAnalyzer) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setExternalSystemIssues(Set<ExternalSystemIssue> externalSystemIssues) {
			throw new UnsupportedOperationException();
		}
	};

	public static final Issue NOT_ISSUE = new Issue() {
		public static final String NOT_ISSUE = "NOT_ISSUE";

		@Override
		public String getIssueType() {
			return NOT_ISSUE;
		}

		@Override
		public void setComment(String comment) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setIssueType(String type) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setAutoAnalyzed(boolean autoAnalyzed) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setIgnoreAnalyzer(boolean ignoreAnalyzer) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setExternalSystemIssues(Set<ExternalSystemIssue> externalSystemIssues) {
			throw new UnsupportedOperationException();
		}
	};

	public static SaveLogRQ getLastLogRQ(String launchUuid) {
		SaveLogRQ rq = new SaveLogRQ();
		rq.setLogTime(Calendar.getInstance().getTime());
		rq.setMessage("Launch finished");
		rq.setLevel(LogLevel.TRACE.name());
		rq.setLaunchUuid(launchUuid);
		return rq;
	}
}
