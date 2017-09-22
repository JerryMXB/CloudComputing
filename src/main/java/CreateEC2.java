
import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by chaoqunhuang on 9/20/17.
 */

public class CreateEC2 {

    public static void main(String[] args) {
        //============================================================================================//
        //=============================== Submitting a Request =======================================//
        //============================================================================================//
        AWSCredentialsProvider credentials;
        try {
            credentials = new ProfileCredentialsProvider("default");
        } catch (Exception e) {
            throw new AmazonClientException(
                    "Cannot load the credentials from the credential profiles file. " +
                            "Please make sure that your credentials file is at the correct " +
                            "location, and is in valid format.", e);
        }

        // Create the AmazonEC2Client
        AmazonEC2 ec2 = AmazonEC2ClientBuilder.standard()
                .withCredentials(credentials)
                .withRegion(Regions.US_EAST_1)
                .build();

        // Create a key pair
        CreateKeyPairRequest createKeyPairRequest = new CreateKeyPairRequest();
        createKeyPairRequest.withKeyName("HW2KeyPair");
        CreateKeyPairResult createKeyPairResult = ec2.createKeyPair(createKeyPairRequest);
        KeyPair keyPair = createKeyPairResult.getKeyPair();
        String privateKey = keyPair.getKeyMaterial();
        System.out.println("Private key is:\n" + privateKey + "\n");

        //Create a Security Group
        CreateSecurityGroupRequest csgr = new CreateSecurityGroupRequest();
        csgr.withGroupName("HW2Group").withDescription("My security group");

        CreateSecurityGroupResult createSecurityGroupResult =
                ec2.createSecurityGroup(csgr);

        //Add 22 port inbound permission
        IpPermission ipPermission =
                new IpPermission();

        IpRange ipRange1 = new IpRange().withCidrIp("0.0.0.0/0");

        ipPermission.withIpv4Ranges(Arrays.asList(new IpRange[] {ipRange1}))
                .withIpProtocol("tcp")
                .withFromPort(22)
                .withToPort(22);

        AuthorizeSecurityGroupIngressRequest authorizeSecurityGroupIngressRequest =
                new AuthorizeSecurityGroupIngressRequest();

        authorizeSecurityGroupIngressRequest.withGroupName("HW2Group")
                .withIpPermissions(ipPermission);

        ec2.authorizeSecurityGroupIngress(authorizeSecurityGroupIngressRequest);

        //Initializes a Run Instance Request
        RunInstancesRequest runInstancesRequest = new RunInstancesRequest();

        runInstancesRequest.withImageId("ami-4fffc834")
                .withInstanceType("t2.micro")
                .withMinCount(1)
                .withMaxCount(1)
                .withKeyName("HW2KeyPair")
                .withSecurityGroups("HW2Group");

        //Send the Request
        RunInstancesResult runInstancesResult = ec2.runInstances(runInstancesRequest);

        //Constantly trying to get the ip address
        Instance instance = runInstancesResult.getReservation().getInstances().get(0);
        String instanceId = instance.getInstanceId();
        DescribeInstancesRequest request =  new DescribeInstancesRequest();
        List<String> instanceIds = new ArrayList<>();
        instanceIds.add(instanceId);
        request.setInstanceIds(instanceIds);
        String publicIp = null;
        int count = 0;

        while (publicIp == null) {
            DescribeInstancesResult result = ec2.describeInstances(request);
            List<Reservation> reservations = result.getReservations();
            publicIp = reservations.get(0).getInstances().get(0).getPublicIpAddress();
            count++;
            System.out.println("Trying to get Public Ip Address:" + count);
            try {
                Thread.sleep(1000);
            } catch (Exception e){
                System.out.println("Failed to sleep");
            }
        }

        System.out.println("\nThe public ip is :" + publicIp + "\nThe instanceId is:" +
                instanceId + "\nThe imageId is:" + instance.getImageId());

    }
}
