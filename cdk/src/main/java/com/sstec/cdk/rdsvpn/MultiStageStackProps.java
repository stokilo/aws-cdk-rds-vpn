package com.sstec.cdk.rdsvpn;

import software.amazon.awscdk.core.Environment;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.ec2.ClientVpnEndpoint;
import software.amazon.awscdk.services.ec2.SecurityGroup;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.rds.DatabaseCluster;

public class MultiStageStackProps {

    public String appName = "rds-vpn-cdk-app";

    public String hostedZoneId = "";

    public String vpnHostUrl = "";

    public String env = "";

    public Boolean isDev = false;

    public Boolean isProd = false;

    public Boolean isTraining = false;

    public StackProps props;

    public Vpc vpc;

    public SecurityGroup vpnRdsSecurityGroup;

    public DatabaseCluster databaseCluster;

    public ClientVpnEndpoint clientVpnEndpoint;

    public MultiStageStackProps() throws Exception{
        this.env = System.getenv("ENV");

        this.props = StackProps.builder().env(makeEnv()).build();

        this.isDev = this.env.equalsIgnoreCase("dev");
        this.isProd = this.env.equalsIgnoreCase("prod");
        this.isTraining = this.env.equalsIgnoreCase("training");

        this.hostedZoneId = System.getenv("CDK_HOSTED_ZONE_ID");
        this.vpnHostUrl = System.getenv("CDK_VPN_HOST_URL");
    }

    private Environment makeEnv() throws Exception {
//        if (!System.getenv("CDK_DEFAULT_ACCOUNT").equals(System.getenv("CDK_DEPLOY_ACCOUNT"))) {
//            throw new Exception("AWS CLI profile account is different than the one specified for CDK run.");
//        }
        return Environment.builder()
                .account(System.getenv("CDK_DEPLOY_ACCOUNT"))
                .region(System.getenv("CDK_DEPLOY_REGION"))
                .build();
    }

}
