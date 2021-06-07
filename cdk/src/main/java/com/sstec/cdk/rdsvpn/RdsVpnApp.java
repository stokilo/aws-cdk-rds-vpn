package com.sstec.cdk.rdsvpn;

import com.sstec.cdk.rdsvpn.stacks.*;
import software.amazon.awscdk.core.App;
import software.amazon.awscdk.core.Environment;
import software.amazon.awscdk.core.StackProps;

/**
 * Sample application with 3 AZ VPC (2 private subnets, one public), single master Aurora RDS, Vpn Client
 * associated with private subnet, private hosted zone with RDS DNS CNAME entries.
 *
 * Target of this project is provisioning a dev environment where a developer can access AWS Aurora from his local
 * machine without the need of reconfiguring DNS entries or running bastion hosts.
 *
 */
public class RdsVpnApp {

    public static void main(final String[] args) throws Exception{
        App app = new App();
        MultiStageStackProps stackProps = new MultiStageStackProps();

        new VPCStack(app, "VPCStack", stackProps);
        new RdsStack(app, "RdsStack", stackProps);
        if (stackProps.isDev) {
            new VpnClientStack(app, "VpnClientStack", stackProps);
        }
        new Route53Stack(app, "Route53Stack", stackProps);
        new S3Stack(app, "S3Stack", stackProps);

        if (stackProps.isDev) {
            new PostDeployStack(app, "PostDeployStack", stackProps);
        }

        app.synth();
    }
}
