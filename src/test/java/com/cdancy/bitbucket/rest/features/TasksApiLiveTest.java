/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cdancy.bitbucket.rest.features;

import static org.assertj.core.api.Assertions.assertThat;

import com.cdancy.bitbucket.rest.BaseBitbucketApiLiveTest;
import com.cdancy.bitbucket.rest.GeneratedTestContents;
import com.cdancy.bitbucket.rest.domain.branch.Branch;
import com.cdancy.bitbucket.rest.domain.branch.BranchPage;
import com.cdancy.bitbucket.rest.domain.comment.Comments;
import com.cdancy.bitbucket.rest.domain.comment.Task;
import com.cdancy.bitbucket.rest.domain.pullrequest.MinimalRepository;
import com.cdancy.bitbucket.rest.domain.pullrequest.ProjectKey;
import com.cdancy.bitbucket.rest.domain.pullrequest.PullRequest;
import com.cdancy.bitbucket.rest.domain.pullrequest.Reference;

import com.cdancy.bitbucket.rest.options.CreatePullRequest;
import com.cdancy.bitbucket.rest.options.CreateTask;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test(groups = "live", testName = "TasksApiLiveTest", singleThreaded = true)
public class TasksApiLiveTest extends BaseBitbucketApiLiveTest {
    
    private GeneratedTestContents generatedTestContents;

    private String projectKey;
    private String repoKey;
    private final String commentText = randomString();
    private final String taskComment = randomString();
    private int commentId = -1;
    private int taskId = -1;
    
    @BeforeClass
    public void init() {
        generatedTestContents = initGeneratedTestContents();
        this.projectKey = generatedTestContents.project.key();
        this.repoKey = generatedTestContents.repository.name();
        
        final BranchPage branchPage = api.branchApi().list(projectKey, repoKey, null, null, null, null, null, null);
        assertThat(branchPage).isNotNull();
        assertThat(branchPage.errors().isEmpty()).isTrue();
        assertThat(branchPage.values().size()).isEqualTo(2);
        
        String branchToMerge = null;
        for (final Branch branch : branchPage.values()) {
            if (!branch.id().endsWith("master")) {
                branchToMerge = branch.id();
                break;
            }
        }
        assertThat(branchToMerge).isNotNull();
        
        final String randomChars = randomString();
        final ProjectKey proj = ProjectKey.create(projectKey);
        final MinimalRepository repository = MinimalRepository.create(repoKey, null, proj);
        final Reference fromRef = Reference.create(branchToMerge, repository, branchToMerge);
        final Reference toRef = Reference.create(null, repository);
        final CreatePullRequest cpr = CreatePullRequest.create(randomChars, "Fix for issue " + randomChars, fromRef, toRef, null, null);
        final PullRequest pr = api.pullRequestApi().create(projectKey, repoKey, cpr);
        
        assertThat(pr).isNotNull();
        assertThat(projectKey).isEqualTo(pr.fromRef().repository().project().key());
        assertThat(repoKey).isEqualTo(pr.fromRef().repository().name());
        
        final Comments comm = this.api.commentsApi().comment(projectKey, repoKey, pr.id(), commentText);
        assertThat(comm).isNotNull();
        assertThat(comm.errors().isEmpty()).isTrue();
        assertThat(comm.text()).isEqualTo(commentText);
        commentId = comm.id();
    }
    
    @Test
    public void testCreateTask() {
        final CreateTask createTask = CreateTask.create(commentId, taskComment);
        final Task createdTask = api().create(createTask);
        assertThat(createdTask).isNotNull();
        assertThat(createdTask.errors().isEmpty()).isTrue();
        assertThat(createTask.text()).isEqualTo(taskComment);
        this.taskId = createdTask.id();
    }
    
    @Test
    public void testCreateTaskOnError() {
        final CreateTask createTask = CreateTask.create(9999999, taskComment);
        final Task createdTask = api().create(createTask);
        assertThat(createdTask).isNotNull();
        assertThat(createdTask.errors().isEmpty()).isFalse();
    }
    
    @Test (dependsOnMethods = "testCreateTask")
    public void testGetTask() {
        final Task getTask = api().get(this.taskId);
        assertThat(getTask).isNotNull();
        assertThat(getTask.errors().isEmpty()).isTrue();
        assertThat(getTask.text()).isEqualTo(taskComment);
    }
    
    @Test
    public void testGetTaskOnError() {
        final Task getTask = api().get(99999);
        assertThat(getTask).isNotNull();
        assertThat(getTask.errors().isEmpty()).isFalse();
    }
    
    @AfterClass
    public void fin() {
        terminateGeneratedTestContents(generatedTestContents);
    }

    private TasksApi api() {
        return api.tasksApi();
    }
}
