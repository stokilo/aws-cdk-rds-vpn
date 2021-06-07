package com.sstec.cdk.rdsvpn.stacks;

import com.sstec.cdk.rdsvpn.MultiStageStackProps;
import com.sstec.cdk.rdsvpn.Tagging;
import software.amazon.awscdk.core.*;
import software.amazon.awscdk.services.ec2.*;

import java.util.Arrays;

public class VPCStack extends Stack {

    public static final String VPC_CIDR = "10.16.0.0/16";
    public static final String VPC_DNS = "10.16.0.2";

    public static final String TRAINING_VPC_NAME = "TrainingVpc";
    public static final String TRAINING_SG_NAME = "TrainingSG";
    public static final String SUBNET_WEB_NAME = "web";
    public static final String SUBNET_DB_NAME = "db";
    public static final String SUBNET_APP_NAME = "app";
    

    public VPCStack(final Construct scope, final String id, final MultiStageStackProps props) {
        super(scope, id, props.props);

        SubnetConfiguration subnetWeb = newSubnet(VPCStack.SUBNET_WEB_NAME, SubnetType.PUBLIC);
        SubnetConfiguration subnetDb = newSubnet(VPCStack.SUBNET_DB_NAME, SubnetType.ISOLATED);
        SubnetConfiguration subnetApp = newSubnet(VPCStack.SUBNET_APP_NAME, SubnetType.ISOLATED);

        props.vpc = Vpc.Builder.create(this, VPCStack.TRAINING_VPC_NAME)
                .cidr(VPCStack.VPC_CIDR)
                .natGateways(0)
                .maxAzs(3)
                .enableDnsHostnames(true)
                .enableDnsSupport(true)
                .subnetConfiguration(Arrays.asList(subnetWeb, subnetDb, subnetApp))
                .build();
        Tagging.addEnvironmentTag(props.vpc, props);

        props.vpnRdsSecurityGroup = SecurityGroup.Builder.create(this, VPCStack.TRAINING_SG_NAME)
                .vpc(props.vpc)
                .securityGroupName(VPCStack.TRAINING_SG_NAME)
                .allowAllOutbound(true)
                .build();
        props.vpnRdsSecurityGroup.addIngressRule(Peer.ipv4(VPCStack.VPC_CIDR), Port.tcp(5432), "PostgreSQL from VPC");
        props.vpnRdsSecurityGroup.addIngressRule(Peer.ipv4(VPCStack.VPC_CIDR), Port.tcp(53), "DNS TCP from VPC");
        props.vpnRdsSecurityGroup.addIngressRule(Peer.ipv4(VPCStack.VPC_CIDR), Port.udp(53), "DNS UDP from VPC");
        Tagging.addEnvironmentTag(props.vpnRdsSecurityGroup, props);

    }

    private SubnetConfiguration newSubnet(String subnetName, SubnetType subnetType) {
       return new SubnetConfiguration.Builder()
                .name(subnetName)
                .cidrMask(20)
                .subnetType(subnetType)
                .build();
    }

}
