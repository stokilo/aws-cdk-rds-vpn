#!/usr/bin/env bash

function prop {
    grep "${1}" env.properties|cut -d'=' -f2
}

ENV=${1:-dev}

if [[ $# -ge 2 ]]; then
    export ENV=$2
    CDK_DEPLOY_ACCOUNT=$(prop "$ENV.account")
    CDK_DEPLOY_REGION=$(prop "$ENV.region")
    CDK_HOSTED_ZONE_ID=$(prop "$ENV.hosted.zone.id")
    CDK_VPN_HOST_URL=$(prop "$ENV.vpn.host.url")
    export CDK_DEPLOY_ACCOUNT CDK_DEPLOY_REGION CDK_HOSTED_ZONE_ID CDK_VPN_HOST_URL CMD=$1

    shift; shift;
    cdk bootstrap "aws://$CDK_DEPLOY_ACCOUNT/$CDK_DEPLOY_REGION" || exit
    cdk $CMD "$@"
    exit $?
else
    echo 1>&2 "Missing required arguments, sample usage for dev system:"
    echo 1>&2 "./cdk.sh deploy dev"
    echo 1>&2 "./cdk.sh deploy dev Route53Stack"
    echo 1>&2 "./cdk.sh deploy dev --all"

    echo 1>&2 "./cdk.sh destroy dev"
    echo 1>&2 "./cdk.sh destroy dev Route53Stack"
    echo 1>&2 "./cdk.sh destroy dev --all"

    echo 1>&2 "./cdk.sh ls dev"
    echo 1>&2 "./cdk.sh synth dev"
    exit 1
fi