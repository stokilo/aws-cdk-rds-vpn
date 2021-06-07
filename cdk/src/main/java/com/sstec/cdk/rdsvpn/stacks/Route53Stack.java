package com.sstec.cdk.rdsvpn.stacks;

import com.sstec.cdk.rdsvpn.MultiStageStackProps;
import com.sstec.cdk.rdsvpn.Tagging;
import software.amazon.awscdk.core.*;
import software.amazon.awscdk.services.route53.*;


public class Route53Stack extends Stack {

    public static final String PRIVATE_RDS_HOSTED_ZONE = "TrainingRdsPrivateHostedZone";

    public Route53Stack(final Construct scope, final String id, final MultiStageStackProps props) {
        super(scope, id, props.props);

        PrivateHostedZone hostedZone = PrivateHostedZone.Builder
                .create(this, Route53Stack.PRIVATE_RDS_HOSTED_ZONE)
                .zoneName("rds.com")
                .vpc(props.vpc).build();

        CnameRecord readerRecord = CnameRecord.Builder.create(this, "reader.rds.com")
                .recordName("reader")
                .domainName(props.databaseCluster.getClusterReadEndpoint().getHostname())
                .ttl(Duration.seconds(300))
                .zone(hostedZone).build();
        Tagging.addEnvironmentTag(readerRecord, props);

        CnameRecord writerRecord = CnameRecord.Builder.create(this, "writer.rds.com")
                .recordName("writer")
                .domainName(props.databaseCluster.getClusterEndpoint().getHostname())
                .ttl(Duration.seconds(300))
                .zone(hostedZone).build();
        Tagging.addEnvironmentTag(writerRecord, props);

    }
}
