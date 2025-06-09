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

package com.epam.reportportal.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * POJO class representing the response from /api/info endpoint
 */
public class ApiInfo {
    
    private Build build;
    private Map<String, String> environment;
    private Extensions extensions;
    private Jobs jobs;
    
    @JsonProperty("jobsInfo")
    private JobsInfo jobsInfo;
    
    private Metadata metadata;

    public Build getBuild() {
        return build;
    }

    public void setBuild(Build build) {
        this.build = build;
    }

    public Map<String, String> getEnvironment() {
        return environment;
    }

    public void setEnvironment(Map<String, String> environment) {
        this.environment = environment;
    }

    public Extensions getExtensions() {
        return extensions;
    }

    public void setExtensions(Extensions extensions) {
        this.extensions = extensions;
    }

    public Jobs getJobs() {
        return jobs;
    }

    public void setJobs(Jobs jobs) {
        this.jobs = jobs;
    }

    public JobsInfo getJobsInfo() {
        return jobsInfo;
    }

    public void setJobsInfo(JobsInfo jobsInfo) {
        this.jobsInfo = jobsInfo;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }

    public static class Build {
        private String repo;
        private String name;
        private String description;
        private String version;
        private String branch;

        public String getRepo() {
            return repo;
        }

        public void setRepo(String repo) {
            this.repo = repo;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String getBranch() {
            return branch;
        }

        public void setBranch(String branch) {
            this.branch = branch;
        }
    }

    public static class Extensions {
        private ExtensionResult result;
        private List<String> extension;
        private List<Analyzer> analyzers;

        public ExtensionResult getResult() {
            return result;
        }

        public void setResult(ExtensionResult result) {
            this.result = result;
        }

        public List<String> getExtension() {
            return extension;
        }

        public void setExtension(List<String> extension) {
            this.extension = extension;
        }

        public List<Analyzer> getAnalyzers() {
            return analyzers;
        }

        public void setAnalyzers(List<Analyzer> analyzers) {
            this.analyzers = analyzers;
        }
    }

    public static class ExtensionResult {
        @JsonProperty("server.details.instance")
        private String serverDetailsInstance;
        
        @JsonProperty("server.session.expiration")
        private String serverSessionExpiration;
        
        @JsonProperty("server.footer.links")
        private String serverFooterLinks;
        
        @JsonProperty("server.users.sso")
        private String serverUsersSso;
        
        @JsonProperty("server.analytics.all")
        private String serverAnalyticsAll;

        public String getServerDetailsInstance() {
            return serverDetailsInstance;
        }

        public void setServerDetailsInstance(String serverDetailsInstance) {
            this.serverDetailsInstance = serverDetailsInstance;
        }

        public String getServerSessionExpiration() {
            return serverSessionExpiration;
        }

        public void setServerSessionExpiration(String serverSessionExpiration) {
            this.serverSessionExpiration = serverSessionExpiration;
        }

        public String getServerFooterLinks() {
            return serverFooterLinks;
        }

        public void setServerFooterLinks(String serverFooterLinks) {
            this.serverFooterLinks = serverFooterLinks;
        }

        public String getServerUsersSso() {
            return serverUsersSso;
        }

        public void setServerUsersSso(String serverUsersSso) {
            this.serverUsersSso = serverUsersSso;
        }

        public String getServerAnalyticsAll() {
            return serverAnalyticsAll;
        }

        public void setServerAnalyticsAll(String serverAnalyticsAll) {
            this.serverAnalyticsAll = serverAnalyticsAll;
        }
    }

    public static class Analyzer {
        private String analyzer;
        
        @JsonProperty("analyzer_cluster")
        private Boolean analyzerCluster;
        
        @JsonProperty("analyzer_index")
        private Boolean analyzerIndex;
        
        @JsonProperty("analyzer_log_search")
        private Boolean analyzerLogSearch;
        
        @JsonProperty("analyzer_priority")
        private Integer analyzerPriority;
        
        @JsonProperty("analyzer_suggest")
        private Boolean analyzerSuggest;
        
        private String version;

        public String getAnalyzer() {
            return analyzer;
        }

        public void setAnalyzer(String analyzer) {
            this.analyzer = analyzer;
        }

        public Boolean getAnalyzerCluster() {
            return analyzerCluster;
        }

        public void setAnalyzerCluster(Boolean analyzerCluster) {
            this.analyzerCluster = analyzerCluster;
        }

        public Boolean getAnalyzerIndex() {
            return analyzerIndex;
        }

        public void setAnalyzerIndex(Boolean analyzerIndex) {
            this.analyzerIndex = analyzerIndex;
        }

        public Boolean getAnalyzerLogSearch() {
            return analyzerLogSearch;
        }

        public void setAnalyzerLogSearch(Boolean analyzerLogSearch) {
            this.analyzerLogSearch = analyzerLogSearch;
        }

        public Integer getAnalyzerPriority() {
            return analyzerPriority;
        }

        public void setAnalyzerPriority(Integer analyzerPriority) {
            this.analyzerPriority = analyzerPriority;
        }

        public Boolean getAnalyzerSuggest() {
            return analyzerSuggest;
        }

        public void setAnalyzerSuggest(Boolean analyzerSuggest) {
            this.analyzerSuggest = analyzerSuggest;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }
    }

    public static class Jobs {
        @JsonProperty("cleanExpiredCreationBidsTrigger")
        private JobTrigger cleanExpiredCreationBidsTrigger;
        
        @JsonProperty("interruptLaunchesTrigger")
        private JobTrigger interruptLaunchesTrigger;

        public JobTrigger getCleanExpiredCreationBidsTrigger() {
            return cleanExpiredCreationBidsTrigger;
        }

        public void setCleanExpiredCreationBidsTrigger(JobTrigger cleanExpiredCreationBidsTrigger) {
            this.cleanExpiredCreationBidsTrigger = cleanExpiredCreationBidsTrigger;
        }

        public JobTrigger getInterruptLaunchesTrigger() {
            return interruptLaunchesTrigger;
        }

        public void setInterruptLaunchesTrigger(JobTrigger interruptLaunchesTrigger) {
            this.interruptLaunchesTrigger = interruptLaunchesTrigger;
        }
    }

    public static class JobTrigger {
        @JsonProperty("triggersIn")
        private Long triggersIn;

        public Long getTriggersIn() {
            return triggersIn;
        }

        public void setTriggersIn(Long triggersIn) {
            this.triggersIn = triggersIn;
        }
    }

    public static class JobsInfo {
        private Build build;

        public Build getBuild() {
            return build;
        }

        public void setBuild(Build build) {
            this.build = build;
        }
    }

    public static class Metadata {
        @JsonProperty("activityAction")
        private List<String> activityAction;
        
        @JsonProperty("activityObjectEvent")
        private List<String> activityObjectEvent;

        public List<String> getActivityAction() {
            return activityAction;
        }

        public void setActivityAction(List<String> activityAction) {
            this.activityAction = activityAction;
        }

        public List<String> getActivityObjectEvent() {
            return activityObjectEvent;
        }

        public void setActivityObjectEvent(List<String> activityObjectEvent) {
            this.activityObjectEvent = activityObjectEvent;
        }
    }
}
