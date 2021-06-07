package com.sstec.cdk.rdsvpn.stacks;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.HashMap;
import java.util.UUID;

import com.sstec.cdk.rdsvpn.MultiStageStackProps;
import com.sstec.cdk.rdsvpn.Tagging;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Duration;
import software.amazon.awscdk.core.RemovalPolicy;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.services.events.*;
import software.amazon.awscdk.services.events.targets.SfnStateMachine;
import software.amazon.awscdk.services.iam.Effect;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.lambda.SingletonFunction;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.stepfunctions.*;
import software.amazon.awscdk.services.stepfunctions.tasks.LambdaInvoke;


/**
 * CDK PostDeploy lambda.
 */
public class PostDeployStack extends Stack {
    public PostDeployStack(final Construct parent, final String name, final MultiStageStackProps props) {
        super(parent, name, props.props);

        SingletonFunction lambdaFunction =
                SingletonFunction.Builder.create(this, "cdk-post-deploy-lambda")
                        .description("CDK Post deploy lambda")
                        .environment(new HashMap<String, String>() {{
                            put("HOSTED_ZONE_ID", props.hostedZoneId);
                            put("VPN_HOST_URL", props.vpnHostUrl);
                        }})
                        .code(Code.fromAsset("./lambda/out/rds-vpn-lambda.jar"))
                        .handler("com.sstec.cdk.rdsvpn.PostDeploy")
                        .logRetention(RetentionDays.ONE_DAY)
                        .timeout(Duration.seconds(15))
                        .memorySize(512)
                        .runtime(Runtime.JAVA_8)
                        .uuid(UUID.randomUUID().toString())
                        .build();
        Tagging.addEnvironmentTag(lambdaFunction, props);

        lambdaFunction.addToRolePolicy(PolicyStatement.Builder.create()
                .effect(Effect.ALLOW)
                .actions(Collections.singletonList("ec2:DescribeClientVpnEndpoints"))
                .resources(Collections.singletonList("*"))
                .build());

        lambdaFunction.addToRolePolicy(PolicyStatement.Builder.create()
                .effect(Effect.ALLOW)
                .actions(Collections.singletonList("route53:ChangeResourceRecordSets"))
                .resources(Collections.singletonList(String.format("arn:aws:route53:::hostedzone/%s", props.hostedZoneId)))
                .build());

        LambdaInvoke taskLambda = LambdaInvoke.Builder.create(this, "lambdaInvokeStep")
                .lambdaFunction(lambdaFunction)
                .build();

        Wait waitOneMinute = Wait.Builder.create(this, "waitCondition").time(
                WaitTime.duration(Duration.minutes(1))).build();

        Fail fail = Fail.Builder.create(this, "fail").error("This is final step").build();
        Pass pass = Pass.Builder.create(this, "pass").build();

        Chain steps = waitOneMinute
                .next(taskLambda)
                .next(Choice.Builder.create(this, "Choice")
                        .build()
                        .when(Condition.stringEquals("$.Payload", "SUCCESS"), pass)
                        .when(Condition.stringEquals("$.Payload", "FAILED"), waitOneMinute)
                        .otherwise(waitOneMinute)
                );

        StateMachine stateMachine = StateMachine.Builder
                .create(this, "cdkPostDeployStateMachine")
                .stateMachineName("CdkPostDeployStateMachine")
                .logs(LogOptions.builder().includeExecutionData(true).destination(
                        LogGroup.Builder.create(this, "stepFunctionGroup")
                                .removalPolicy(RemovalPolicy.DESTROY)
                                .retention(RetentionDays.ONE_DAY)
                                .logGroupName("PostDeployStepFunction").build()).build())
                .stateMachineType(StateMachineType.STANDARD)
                .definition(steps)
                .timeout(Duration.hours(1))
                .build();
        Tagging.addEnvironmentTag(stateMachine, props);


        // trigger state machine execution 5 minutes from now, state machine will attempt to update public
        // hosted zone CNAME entry for VPN Connection endpoint
        LocalDateTime nowPlus5 = LocalDateTime.now(ZoneOffset.UTC).plusMinutes(5);
        Schedule inNext5Min = Schedule.cron(CronOptions.builder()
                .day(String.valueOf(nowPlus5.getDayOfMonth()))
                .month(String.valueOf(nowPlus5.getMonth().getValue()))
                .year(String.valueOf(nowPlus5.getYear()))
                .hour(String.valueOf(nowPlus5.getHour()))
                .minute(String.valueOf(nowPlus5.getMinute())).build());

        Rule.Builder.create(this, "startCdkPostDeployStateMachine5MinAfterDeployment")
                .ruleName("run-state-machine-5min-after-deployment")
                .targets(Collections.singletonList(SfnStateMachine.Builder.create(stateMachine).build()))
                .schedule(inNext5Min)
                .build();
    }
}