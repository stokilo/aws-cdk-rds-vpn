package com.sstec.cdk.rdsvpn.stacks;

import com.sstec.cdk.rdsvpn.MultiStageStackProps;
import com.sstec.cdk.rdsvpn.Tagging;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.services.s3.BucketEncryption;
import software.amazon.awscdk.services.s3.BucketProps;


public class S3Stack extends Stack {

    public S3Stack(final Construct scope, final String id, final MultiStageStackProps props) {
        super(scope, id, props.props);

        Bucket bucket = new Bucket(this, "aws-training-s3-bucket", new BucketProps.Builder()
                .versioned(true)
                .encryption(BucketEncryption.KMS_MANAGED)
                .build());
        Tagging.addEnvironmentTag(bucket, props);

    }
}
