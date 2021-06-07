package com.sstec.cdk.rdsvpn;

import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.*;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.route53.AmazonRoute53ClientBuilder;
import com.amazonaws.services.route53.model.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.List;

/**
 * Execute post deploy steps. Add CNAME record to Route53 public hosted zone allowing to connect via vpn.my-domain.com
 * to private subnets.
 */
public class PostDeploy implements RequestHandler<Object, String> {

    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILED = "FAILED";

    Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public String handleRequest(Object input, Context context) {
        LambdaLogger logger = context.getLogger();
        logger.log("PostDeploy lambda invoked");

        try {
            DescribeClientVpnEndpointsRequest describeEndpointRequest = new DescribeClientVpnEndpointsRequest();
            DescribeClientVpnEndpointsResult vpnEndpoints = AmazonEC2ClientBuilder.defaultClient()
                    .describeClientVpnEndpoints(describeEndpointRequest);

            List<ClientVpnEndpoint> vpnEndpointList = vpnEndpoints.getClientVpnEndpoints();

            ClientVpnEndpoint endpoint = vpnEndpointList.get(0);
            String vpnDnsName = endpoint.getDnsName();
            String randomPrefix = String.format("random-prefix-%s", System.currentTimeMillis());
            vpnDnsName = randomPrefix + vpnDnsName.substring(1);
            logger.log(String.format("Dns name %s", vpnDnsName));

            ChangeBatch changeBatch = new ChangeBatch();
            changeBatch.withChanges(new Change().withAction(ChangeAction.UPSERT).withResourceRecordSet(
                    new ResourceRecordSet().withName(System.getenv("VPN_HOST_URL"))
                            .withType(RRType.CNAME)
                            .withTTL(300L)
                            .withResourceRecords(new ResourceRecord().withValue(vpnDnsName))
            ));
            ChangeResourceRecordSetsRequest recordSet = new ChangeResourceRecordSetsRequest()
                    .withHostedZoneId(System.getenv("HOSTED_ZONE_ID"));
            recordSet.setChangeBatch(changeBatch);
            AmazonRoute53ClientBuilder.defaultClient().changeResourceRecordSets(recordSet);


        } catch (Exception e) {
            logger.log(e.toString());
            return PostDeploy.STATUS_FAILED;
        }

        return PostDeploy.STATUS_SUCCESS;
    }
}
