/*
 * The MIT License
 *
 * Copyright (c) 2013-2016, CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.cloudbees.workflow.rest.endpoints;

import java.util.Collection;

import com.cloudbees.workflow.rest.external.RunExt;
import com.cloudbees.workflow.rest.external.StageNodeExt;
import com.cloudbees.workflow.util.JSONReadWrite;
import com.gargoylesoftware.htmlunit.Page;

import hudson.model.Action;
import hudson.model.queue.QueueTaskFuture;
import hudson.util.RunList;
import jenkins.model.TransientActionFactory;

import org.jenkinsci.plugins.pipeline.modeldefinition.actions.RestartDeclarativePipelineAction;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;


/**
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class StageSequenceTest {

    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

    @Test
    public void testSimpleStageSequence() throws Exception {
        WorkflowJob job = jenkinsRule.jenkins.createProject(WorkflowJob.class, "SimpleStageSequenceJob");

        String script = "" +
                "node {" +
                "   stage ('Stage 1') { " +  // id=6
                "     echo ('echo 1'); " +   // id=7
                "   } \n" +
                "   stage ('Stage 2') { " +  // id=11
                "     echo ('echo 2'); " +   // id=12
                "   } \n" +
                "   stage ('Stage 3') { " +  // id=16
                "     echo ('echo 3'); " +   // id=17
                "   } \n" +
                "}";

        // System.out.println(script);
        job.setDefinition(new CpsFlowDefinition(script, true));

        QueueTaskFuture<WorkflowRun> build = job.scheduleBuild2(0);
        build.waitForStart();
        jenkinsRule.assertBuildStatusSuccess(build);

        JenkinsRule.WebClient webClient = jenkinsRule.createWebClient();

        String jobRunsUrl = job.getUrl() + "wfapi/runs/";
        Page runsPage = webClient.goTo(jobRunsUrl, "application/json");
        String jsonResponse = runsPage.getWebResponse().getContentAsString();

        //System.out.println(jsonResponse);
        JSONReadWrite jsonReadWrite = new JSONReadWrite();
        RunExt[] workflowRuns = jsonReadWrite.fromString(jsonResponse, RunExt[].class);

        // Check the run response... should be 3 stages
        Assert.assertEquals(1, workflowRuns.length);
        Assert.assertEquals(3, workflowRuns[0].getStages().size());
        Assert.assertEquals("Stage 1", workflowRuns[0].getStages().get(0).getName());
        Assert.assertEquals("Stage 2", workflowRuns[0].getStages().get(1).getName());
        Assert.assertEquals("Stage 3", workflowRuns[0].getStages().get(2).getName());

        Assert.assertEquals("/jenkins/job/SimpleStageSequenceJob/1/execution/node/6/wfapi/describe", workflowRuns[0].getStages().get(0).get_links().self.href);
        Assert.assertEquals("/jenkins/job/SimpleStageSequenceJob/1/execution/node/11/wfapi/describe", workflowRuns[0].getStages().get(1).get_links().self.href);
        Assert.assertEquals("/jenkins/job/SimpleStageSequenceJob/1/execution/node/16/wfapi/describe", workflowRuns[0].getStages().get(2).get_links().self.href);

        // Stage 1
        Page stageDescription = webClient.goTo("job/SimpleStageSequenceJob/1/execution/node/6/wfapi/describe", "application/json");
        jsonResponse = stageDescription.getWebResponse().getContentAsString();

        StageNodeExt stage1Desc = jsonReadWrite.fromString(jsonResponse, StageNodeExt.class);
        Assert.assertEquals(1, stage1Desc.getStageFlowNodes().size());
        Assert.assertEquals("7", stage1Desc.getStageFlowNodes().get(0).getId());
        Assert.assertEquals("Print Message", stage1Desc.getStageFlowNodes().get(0).getName());

        // Stage 2
        stageDescription = webClient.goTo("job/SimpleStageSequenceJob/1/execution/node/11/wfapi/describe", "application/json");
        jsonResponse = stageDescription.getWebResponse().getContentAsString();

        StageNodeExt stage2Desc = jsonReadWrite.fromString(jsonResponse, StageNodeExt.class);
        Assert.assertEquals(1, stage2Desc.getStageFlowNodes().size());
        Assert.assertEquals("12", stage2Desc.getStageFlowNodes().get(0).getId());
        Assert.assertEquals("Print Message", stage2Desc.getStageFlowNodes().get(0).getName());

        // Stage 3
        stageDescription = webClient.goTo("job/SimpleStageSequenceJob/1/execution/node/16/wfapi/describe", "application/json");
        jsonResponse = stageDescription.getWebResponse().getContentAsString();

        StageNodeExt stage3Desc = jsonReadWrite.fromString(jsonResponse, StageNodeExt.class);
        //System.out.println(stage3Desc);
        Assert.assertEquals(1, stage3Desc.getStageFlowNodes().size());
        Assert.assertEquals("17", stage3Desc.getStageFlowNodes().get(0).getId());
        Assert.assertEquals("Print Message", stage3Desc.getStageFlowNodes().get(0).getName());

    }

    @Test
    public void testNestedStageSequence() throws Exception {
      WorkflowJob job = jenkinsRule.jenkins.createProject(WorkflowJob.class, "testNestedStageSequence");

      String script = "" +
              "node {" +
              "   stage ('Stage 1') { " +  // id=6
              "     stage ('Stage 1a') { " +  // id=8
              "       echo ('echo 1a'); " +   // id=9
              "     } \n" +
              "     stage ('Stage 1b') { " +  // id=13
              "       echo ('echo 1b'); " +   // id=14
              "     } \n" +
              "   } \n" +
              "   stage ('Stage 2') { " +  // id=20
              "     echo ('echo 2'); " +   // id=21
              "   } \n" +
              "   stage ('Stage 3') { " +  // id=25
              "     echo ('echo 3'); " +   // id=26
              "   } \n" +
              "}";

      // System.out.println(script);
      job.setDefinition(new CpsFlowDefinition(script, true));

      QueueTaskFuture<WorkflowRun> build = job.scheduleBuild2(0);
      build.waitForStart();
      jenkinsRule.assertBuildStatusSuccess(build);

      JenkinsRule.WebClient webClient = jenkinsRule.createWebClient();

      String jobRunsUrl = job.getUrl() + "wfapi/runs/";
      Page runsPage = webClient.goTo(jobRunsUrl, "application/json");
      String jsonResponse = runsPage.getWebResponse().getContentAsString();

      //System.out.println(jsonResponse);
      JSONReadWrite jsonReadWrite = new JSONReadWrite();
      RunExt[] workflowRuns = jsonReadWrite.fromString(jsonResponse, RunExt[].class);

      // Check the run response... should be 5 stages
      Assert.assertEquals(1, workflowRuns.length);
      Assert.assertEquals(5, workflowRuns[0].getStages().size());
      Assert.assertEquals("Stage 1", workflowRuns[0].getStages().get(0).getName());
      Assert.assertEquals("Stage 1a", workflowRuns[0].getStages().get(1).getName());
      Assert.assertEquals("Stage 1b", workflowRuns[0].getStages().get(2).getName());
      Assert.assertEquals("Stage 2", workflowRuns[0].getStages().get(3).getName());
      Assert.assertEquals("Stage 3", workflowRuns[0].getStages().get(4).getName());

      Assert.assertEquals("/jenkins/job/testNestedStageSequence/1/execution/node/6/wfapi/describe", workflowRuns[0].getStages().get(0).get_links().self.href);
      Assert.assertEquals("/jenkins/job/testNestedStageSequence/1/execution/node/8/wfapi/describe", workflowRuns[0].getStages().get(1).get_links().self.href);
      Assert.assertEquals("/jenkins/job/testNestedStageSequence/1/execution/node/13/wfapi/describe", workflowRuns[0].getStages().get(2).get_links().self.href);
      Assert.assertEquals("/jenkins/job/testNestedStageSequence/1/execution/node/20/wfapi/describe", workflowRuns[0].getStages().get(3).get_links().self.href);
      Assert.assertEquals("/jenkins/job/testNestedStageSequence/1/execution/node/25/wfapi/describe", workflowRuns[0].getStages().get(4).get_links().self.href);

      // Stage 1
      Page stageDescription = webClient.goTo("job/testNestedStageSequence/1/execution/node/6/wfapi/describe", "application/json");
      jsonResponse = stageDescription.getWebResponse().getContentAsString();

      StageNodeExt stage1Desc = jsonReadWrite.fromString(jsonResponse, StageNodeExt.class);
      Assert.assertEquals(0, stage1Desc.getStageFlowNodes().size());

      // Stage 1a
      stageDescription = webClient.goTo("job/testNestedStageSequence/1/execution/node/8/wfapi/describe", "application/json");
      jsonResponse = stageDescription.getWebResponse().getContentAsString();

      stage1Desc = jsonReadWrite.fromString(jsonResponse, StageNodeExt.class);
      Assert.assertEquals(1, stage1Desc.getStageFlowNodes().size());
      Assert.assertEquals("9", stage1Desc.getStageFlowNodes().get(0).getId());
      Assert.assertEquals("Print Message", stage1Desc.getStageFlowNodes().get(0).getName());

      // Stage 1b
      stageDescription = webClient.goTo("job/testNestedStageSequence/1/execution/node/13/wfapi/describe", "application/json");
      jsonResponse = stageDescription.getWebResponse().getContentAsString();

      stage1Desc = jsonReadWrite.fromString(jsonResponse, StageNodeExt.class);
      Assert.assertEquals(1, stage1Desc.getStageFlowNodes().size());
      Assert.assertEquals("14", stage1Desc.getStageFlowNodes().get(0).getId());
      Assert.assertEquals("Print Message", stage1Desc.getStageFlowNodes().get(0).getName());

      // Stage 2
      stageDescription = webClient.goTo("job/testNestedStageSequence/1/execution/node/20/wfapi/describe", "application/json");
      jsonResponse = stageDescription.getWebResponse().getContentAsString();

      StageNodeExt stage2Desc = jsonReadWrite.fromString(jsonResponse, StageNodeExt.class);
      Assert.assertEquals(1, stage2Desc.getStageFlowNodes().size());
      Assert.assertEquals("21", stage2Desc.getStageFlowNodes().get(0).getId());
      Assert.assertEquals("Print Message", stage2Desc.getStageFlowNodes().get(0).getName());

      // Stage 3
      stageDescription = webClient.goTo("job/testNestedStageSequence/1/execution/node/25/wfapi/describe", "application/json");
      jsonResponse = stageDescription.getWebResponse().getContentAsString();

      StageNodeExt stage3Desc = jsonReadWrite.fromString(jsonResponse, StageNodeExt.class);
      Assert.assertEquals(1, stage3Desc.getStageFlowNodes().size());
      Assert.assertEquals("26", stage3Desc.getStageFlowNodes().get(0).getId());
      Assert.assertEquals("Print Message", stage3Desc.getStageFlowNodes().get(0).getName());
    }

    
    /**
     * This is to make sure when we restart for a later stage, the sequence of all stages including the nested stages
     * should stay the same.
     *
     * @throws Exception
     */
    @Test
     public void testNestedStageSequenceRestartLaterStage() throws Exception {
      WorkflowJob job = jenkinsRule.jenkins.createProject(WorkflowJob.class, "testNestedStageSequenceRestartLaterStage");

      String script = "" +
              "node {" +
              "   stage ('Stage 1') { " +  // id=6
              "     stage ('Stage 1a') { " +  // id=8
              "       echo ('echo 1a'); " +   // id=9
              "     } \n" +
              "     stage ('Stage 1b') { " +  // id=13
              "       echo ('echo 1b'); " +   // id=14
              "     } \n" +
              "   } \n" +
              "   stage ('Stage 2') { " +  // id=20
              "     echo ('echo 2'); " +   // id=21
              "   } \n" +
              "   stage ('Stage 3') { " +  // id=25
              "     echo ('echo 3'); " +   // id=26
              "   } \n" +
              "}";

      // System.out.println(script);
      job.setDefinition(new CpsFlowDefinition(script, true));

      // First run
      QueueTaskFuture<WorkflowRun> build = job.scheduleBuild2(0);
      build.waitForStart();
      jenkinsRule.assertBuildStatusSuccess(build);

      Thread.sleep(6000000);

      JenkinsRule.WebClient webClient = jenkinsRule.createWebClient();

      String jobRunsUrl = job.getUrl() + "wfapi/runs/";
      Page runsPage = webClient.goTo(jobRunsUrl, "application/json");
      String jsonResponse = runsPage.getWebResponse().getContentAsString();

      //System.out.println(jsonResponse);
      JSONReadWrite jsonReadWrite = new JSONReadWrite();
      RunExt[] workflowRuns = jsonReadWrite.fromString(jsonResponse, RunExt[].class);

      // Check the run response... should be 5 stages
      Assert.assertEquals(1, workflowRuns.length);
      Assert.assertEquals(5, workflowRuns[0].getStages().size());
      Assert.assertEquals("Stage 1", workflowRuns[0].getStages().get(0).getName());
      Assert.assertEquals("Stage 1a", workflowRuns[0].getStages().get(1).getName());
      Assert.assertEquals("Stage 1b", workflowRuns[0].getStages().get(2).getName());
      Assert.assertEquals("Stage 2", workflowRuns[0].getStages().get(3).getName());
      Assert.assertEquals("Stage 3", workflowRuns[0].getStages().get(4).getName());

      
      // Second run, restart from stage 3
      // The sequence of stages should still be the same as first one
      
      // List<Action> actions = new ArrayList<>();
      // //actions.add(new RestartFlowFactoryAction(run.getExternalizableId()));
      // actions.add(new CauseAction(new Cause.UserIdCause(), new RestartDeclarativePipelineCause(job.getBuildByNumber(0), "Stage 3")));

      RunList<WorkflowRun> runs = job.getBuilds();
      System.out.println("Runs available");
      System.out.println(runs);

      // Restart build 1 from Stage 3
      //Action restartFromStageAction = new CauseAction(new Cause.UserIdCause(), new RestartDeclarativePipelineCause(job.getBuildByNumber(1), "Stage 3"));
      //new RestartDeclarativePipelineAction(job.getBuildByNumber(1));
      Iterable<? extends TransientActionFactory<?>> factories = RestartDeclarativePipelineAction.Factory
          .factoriesFor(WorkflowRun.class, RestartDeclarativePipelineAction.class);

      Collection<? extends Action> restartActions = null;
      for (TransientActionFactory<?> factory : factories)
      {
        System.out.println("Factory Action type is "+ factory.getClass());
        //if (factory.actionType().equals(RestartDeclarativePipelineAction.class)) {
          if (factory.getClass().equals(RestartDeclarativePipelineAction.class) )
          System.out.println("Factory match found");
          RestartDeclarativePipelineAction.Factory restartFactory = (RestartDeclarativePipelineAction.Factory) factory;
          restartActions = restartFactory.createFor(job.getBuildByNumber(1));
      }
      
      // factoriesFor.forEach((factory) -> {
      //   System.out.println("Factory Action type is "+ factory.actionType());
      //   if (factory.actionType().equals(RestartDeclarativePipelineAction.class)) {
      //     System.out.println("Factory match found");
      //     RestartDeclarativePipelineAction.Factory restartFactory = (RestartDeclarativePipelineAction.Factory) factory;
      //     restartActions = restartFactory.createFor(job.getBuildByNumber(1));
      //   }
      // });


      QueueTaskFuture<WorkflowRun> buildRestart = job.scheduleBuild2(0, (Action[]) restartActions.toArray());
      buildRestart.waitForStart();

      //RestartDeclarativePipelineAction action = restartWorkflow.getAction(RestartDeclarativePipelineAction.class);
      //assertNotNull(action);
      
      
      //QueueTaskFuture<WorkflowRun> buildRestart = job.scheduleBuild2(0);
      //WorkflowRun restartWorkflow = buildRestart.waitForStart();
      //RestartDeclarativePipelineAction action = restartWorkflow.getAction(RestartDeclarativePipelineAction.class);
      //action.run("Stage 3");


      // QueueTaskFuture<WorkflowRun> buildRestart = job.scheduleBuild2(0);
      // buildRestart.waitForStart();

      // restart
      jenkinsRule.assertBuildStatusSuccess(buildRestart);

      runsPage = webClient.goTo(jobRunsUrl, "application/json");
      jsonResponse = runsPage.getWebResponse().getContentAsString();

      //System.out.println(jsonResponse);
      jsonReadWrite = new JSONReadWrite();
      workflowRuns = jsonReadWrite.fromString(jsonResponse, RunExt[].class);

      // Check the run response... should be 5 stages, with the same sequence
      Assert.assertEquals(2, workflowRuns.length);

      Assert.assertEquals(5, workflowRuns[0].getStages().size());
      Assert.assertEquals("Stage 1", workflowRuns[0].getStages().get(0).getName());
      Assert.assertEquals("Stage 1a", workflowRuns[0].getStages().get(1).getName());
      Assert.assertEquals("Stage 1b", workflowRuns[0].getStages().get(2).getName());
      Assert.assertEquals("Stage 2", workflowRuns[0].getStages().get(3).getName());
      Assert.assertEquals("Stage 3", workflowRuns[0].getStages().get(4).getName());

      Assert.assertEquals(5, workflowRuns[1].getStages().size());
      Assert.assertEquals("Stage 1", workflowRuns[1].getStages().get(0).getName());
      Assert.assertEquals("Stage 1a", workflowRuns[1].getStages().get(1).getName());
      Assert.assertEquals("Stage 1b", workflowRuns[1].getStages().get(2).getName());
      Assert.assertEquals("Stage 2", workflowRuns[1].getStages().get(3).getName());
      Assert.assertEquals("Stage 3", workflowRuns[1].getStages().get(4).getName());


    }
}

// class RestartDeclarativePipelineCause extends Cause {
//   private int originRunNumber;
//   private String originStage;
//   private transient Run<?, ?> run;

//   public RestartDeclarativePipelineCause(@Nonnull Run<?, ?> original, @Nonnull String originStage) {
//       this.originRunNumber = original.getNumber();
//       this.originStage = originStage;
//   }

//   @Override public void onAddedTo(Run run) {
//       super.onAddedTo(run);
//       this.run = run;
//   }

//   @Override public void onLoad(Run<?,?> run) {
//       super.onLoad(run);
//       this.run = run;
//   }

//   public int getOriginRunNumber() {
//       return originRunNumber;
//   }

//   @Nonnull
//   public String getOriginStage() {
//       return originStage;
//   }

//   @CheckForNull
//   public Run<?,?> getOriginal() {
//       return run.getParent().getBuildByNumber(originRunNumber);
//   }

//   @Override
//   public String getShortDescription() {
//       return String.format("Restarting from stage %s of build %s", originRunNumber, originStage);
//   }
// }