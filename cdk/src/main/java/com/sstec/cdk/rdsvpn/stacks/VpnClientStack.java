package com.sstec.cdk.rdsvpn.stacks;

import com.sstec.cdk.rdsvpn.MultiStageStackProps;
import com.sstec.cdk.rdsvpn.Tagging;
import software.amazon.awscdk.core.*;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.ssm.StringParameter;

import java.util.Collections;


public class VpnClientStack extends Stack {

    public static final String CLIENT_CIDR = "10.16.0.0/22";
    public static final String TRAINING_CLIENT_VPN_ENDPOINT = "trainingClientVpnEndpoint";

    public VpnClientStack(final Construct scope, final String id, final MultiStageStackProps props) {
        super(scope, id, props.props);

        String clientCertToken = StringParameter.valueForStringParameter(
                this, "client-cert-parameter");
        String serverCertToken = StringParameter.valueForStringParameter(
                this, "server-cert-parameter");

        if (props.isProd) {
            // HA, AWS charges for each of the subnet association, our VPS has 2 isolated subnets, it cost 2x for standby
            props.clientVpnEndpoint = new ClientVpnEndpoint(this, VpnClientStack.TRAINING_CLIENT_VPN_ENDPOINT,
                    ClientVpnEndpointProps.builder()
                            .clientCertificateArn(clientCertToken)
                            .serverCertificateArn(serverCertToken)
                            .vpc(props.vpc)
                            .splitTunnel(true)
                            .selfServicePortal(false)
                            .vpcSubnets(SubnetSelection.builder().onePerAz(true).subnetType(SubnetType.ISOLATED).build())
                            .cidr(VpnClientStack.CLIENT_CIDR)
                            .build());

        } else if (props.isDev) {
            props.clientVpnEndpoint = new ClientVpnEndpoint(this, VpnClientStack.TRAINING_CLIENT_VPN_ENDPOINT,
                    ClientVpnEndpointProps.builder()
                            .clientCertificateArn(clientCertToken)
                            .serverCertificateArn(serverCertToken)
                            .vpc(props.vpc)
                            .splitTunnel(true)
                            .logging(false)
                            .selfServicePortal(false)
                            .dnsServers(Collections.singletonList(VPCStack.VPC_DNS))
                            .securityGroups(Collections.singletonList(props.vpnRdsSecurityGroup))
                            .vpcSubnets(SubnetSelection.builder()
                                    .availabilityZones(props.vpc.getAvailabilityZones().subList(1, 2))
                                    .subnetGroupName(VPCStack.SUBNET_DB_NAME)
                                    .build())
                            .cidr(VpnClientStack.CLIENT_CIDR)
                            .build());
        }
        Tagging.addEnvironmentTag(props.clientVpnEndpoint, props);
    }
}
