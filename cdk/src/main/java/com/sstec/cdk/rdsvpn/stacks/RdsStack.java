package com.sstec.cdk.rdsvpn.stacks;

import com.sstec.cdk.rdsvpn.MultiStageStackProps;
import com.sstec.cdk.rdsvpn.Tagging;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.RemovalPolicy;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.rds.*;
import software.amazon.awscdk.services.rds.InstanceProps;

import java.util.Collections;

public class RdsStack extends Stack {

    public static final String TRAINING_SUBNET_GROUP = "TrainingVpcRdsSubnetGroup";
    public static final String TRAINING_DATABASE_SECRET = "TrainingRdsSecret";
    public static final String TRAINING_DATABASE_CLUSTER = "TrainingRdsCluster";

    public static final String TRAINING_DATABASE_NAME = "trainingRds";
    public static final String TRAINING_ADMIN_USERNAME = "rdsAdminAccount";
    public static final String TRAINING_ADMIN_SECRET_NAME = "rdsAdminSecretName";

    public RdsStack(final Construct scope, final String id, final MultiStageStackProps props) {
        super(scope, id, props.props);

        SubnetGroup subnetGroup = SubnetGroup.Builder.create(this, RdsStack.TRAINING_SUBNET_GROUP)
                .vpc(props.vpc)
                .description("Rds subnet group")
                .vpcSubnets(SubnetSelection.builder()
                        .subnetGroupName(VPCStack.SUBNET_DB_NAME)
                        .build())
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();
        Tagging.addEnvironmentTag(subnetGroup, props);

        DatabaseSecret databaseSecret = new DatabaseSecret(this, RdsStack.TRAINING_DATABASE_SECRET,
                DatabaseSecretProps.builder()
                        .username(RdsStack.TRAINING_ADMIN_USERNAME)
                        .secretName(RdsStack.TRAINING_ADMIN_SECRET_NAME)
                        .build());
        Tagging.addEnvironmentTag(databaseSecret, props);

        props.databaseCluster = new DatabaseCluster(this, RdsStack.TRAINING_DATABASE_CLUSTER,
                DatabaseClusterProps.builder()
                        .clusterIdentifier("cluster-spring-aws")
                        .instanceIdentifierBase("instance-spring-aws")


                        .engine(DatabaseClusterEngine.auroraPostgres(
                                AuroraPostgresClusterEngineProps.builder()
                                        .version(
                                                AuroraPostgresEngineVersion.VER_12_4
                                        )
                                        .build()))

                        .instanceProps(
                                InstanceProps.builder()
                                        .instanceType(
                                                InstanceType.of(InstanceClass.BURSTABLE3, InstanceSize.MEDIUM)
                                        )
                                        .vpc(props.vpc)
                                        .securityGroups(Collections.singletonList(props.vpnRdsSecurityGroup))
                                        .deleteAutomatedBackups(true)
                                        .publiclyAccessible(false)
                                        .enablePerformanceInsights(false)
                                        .allowMajorVersionUpgrade(true)
                                        .build())

                        .removalPolicy(RemovalPolicy.DESTROY)
                        .defaultDatabaseName(RdsStack.TRAINING_DATABASE_NAME)
                        .deletionProtection(false)
                        .subnetGroup(subnetGroup)
                        .credentials(Credentials.fromSecret(databaseSecret))
                        .build());
        Tagging.addEnvironmentTag(props.databaseCluster, props);

    }
}
