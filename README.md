[![Stand With Ukraine](https://raw.githubusercontent.com/vshymanskyy/StandWithUkraine/main/banner2-direct.svg)](https://vshymanskyy.github.io/StandWithUkraine)
#### What is it?

AWS CDK Java application presented on my personal blog 

https://slawomirstec.com/blog/2021/04/cdk-rds-vpn

It consists of 

1. VPC deployed on 3 AZ with 1 public and 2 private subnets.
2. Aurora RDS in single master mode, 2 AZ deployment
3. Client VPN with single association to private subnet (dev mode) or two associations to private subnets (prod mode)
4. Route53 private hosted zone and two CNAME entries to allow access database reader node as reader.rds.com and writer
as writer.rds.com
5. Route53 public zone update with vpn client url CNAME record.

Upon completion of these steps you will have:

1. VPC configured to deploy your applications
2. RDS in single master mode with reader.rds.com and writer.rds.com DNS names assigned. You can save them in your
   database client and same name will be assigned after redeployment.
3. ClientVpn connection to connect to RDS database. Please note RDS is deployed in private network and SSH is not
included in security group.

This is an app implemented during preparation for aws exam. 
Please be warned, it can be insecure, to expensive or just wrong.

##### Step 1: Manual setup

Prepare env properties and configure minimum one account, below example dev account

    cp templates/env.properties env.properties
    echo env.properties > .gitignore
    vim env.properties

    # you can use any prefix you want i.e. 'dev', this will be later required to be passed to ./cdk.sh shell script
    # example: ./cdk.sh deploy dev --all

    # aws account number
    dev.account=

    # target deployment region
    dev.region=

    # id of public hosted zone where you have your domain configured
    # this zone must exists before deployment i.e. you own domain my-domain.com and you want to connect
    # to the vpn via url vpn.my-domain.com, enter zone id here
    dev.hosted.zone.id=

    # enter vpn host url i.e. *.vpn.my-domain.com (add *. in front of the name)
    dev.vpn.host.url=

    
Original instructions from AWS documentation

https://docs.aws.amazon.com/vpn/latest/clientvpn-admin/cvpn-getting-started.html

https://docs.aws.amazon.com/vpn/latest/clientvpn-admin/client-authentication.html#mutual

    mkdir ~/my-temp-generate-cert-folder
    cd ~/my-temp-generate-cert-folder

    git clone https://github.com/OpenVPN/easy-rsa.git
    cd easy-rsa/easyrsa3

    ./easyrsa init-pki
    ./easyrsa build-ca nopass
    ./easyrsa build-server-full server nopass
    ./easyrsa build-client-full client1.domain.tld nopass

    mkdir ~/my-temp-cert-folder/
    cp pki/ca.crt ~/my-temp-cert-folder/
    cp pki/issued/server.crt ~/my-temp-cert-folder/
    cp pki/private/server.key ~/my-temp-cert-folder/
    cp pki/issued/client1.domain.tld.crt ~/my-temp-cert-folder
    cp pki/private/client1.domain.tld.key ~/my-temp-cert-folder/
    cd ~/my-temp-cert-folder/

    SERVER_CERT=`aws acm import-certificate --certificate fileb://server.crt --private-key fileb://server.key --certificate-chain fileb://ca.crt --query CertificateArn --output text`
    aws ssm put-parameter --name "server-cert-parameter" --value $SERVER_CERT --type "String" --overwrite

    CLIENT_CERT=`aws acm import-certificate --certificate fileb://client1.domain.tld.crt --private-key fileb://client1.domain.tld.key --certificate-chain fileb://ca.crt --query CertificateArn --output text`
    aws ssm put-parameter --name "client-cert-parameter" --value $CLIENT_CERT --type "String" --overwrite

##### Step 2: OpenVpn setup

Under templates folder you have OpenVpn profile file template (aws.ovpn), copy it to your home folder.

    # never store this under version control system
    cp templates/aws.ovpn ~/aws.ovpn

    # Replace my-domain.com with your domain name
    remote vpn.my-domain.com 443

    # Copy ca.crt into
    <ca></ca>

    # Copy client1.domain.tld.crt into
    <cert></cert>

    # Copy client1.domain.tld.key into
    <key></key>

    # Delete all cert and key files. Delete .git repo too ! this repo contains generated keys/certs.
    # No need to keep these files after uploading them into AWS ACM. Next time regenerate them again.
    cd ~
    rm -rf ~/my-temp-generate-cert-folder
    rm -rf ~/my-temp-cert-folder


##### Step 3: AWS CDK deployment

Deploy all stacks in single command run (takes around 15 minutes to complete)

    ./cdk.sh deploy dev --all

##### Step 4: Testing

Open aws.ovpn with OpenVpn client and test connection. In case it is not working wait 5 minutes, it is possible that
post deployment DNS update task is still pending.

Run 'dig' on MacOS and check CNAME for both RDS nodes, should point to the reader and writer endpoints. 

    dig reader.rds.com
    dig writer.rds.com

Download Postgres database client and test connection to the database.

    # Connection 1
    host:port: reader.rds.com:5432 
    username: rdsAdminAccount
    password: login to aws and check Secret Manager secret with name: rdsAdminSecretName

    # Connection 2
    host:port: writer.rds.com:5432
    username: rdsAdminAccount
    password: login to aws and check Secret Manager secret with name: rdsAdminSecretName


##### Step 5: Monthly cost

  In dev mode cost for Vpn starts from 80$ per month, this is absolute minimum because usage and transfer charges apply.

  In prod mode it starts from 160$ per month (two associations).

  In dev mode RDS is minimum 60$ per month.


 
