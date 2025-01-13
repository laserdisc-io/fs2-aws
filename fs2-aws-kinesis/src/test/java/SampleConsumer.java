import org.apache.http.impl.client.BasicCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.kinesis.common.ConfigsBuilder;
import software.amazon.kinesis.common.KinesisClientUtil;
import software.amazon.kinesis.coordinator.Scheduler;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

public class SampleConsumer {

    private static final Logger log = LoggerFactory.getLogger(SampleRecordProcessor.class);

    private final String streamName;
    private final Region region;
    private final KinesisAsyncClient kinesisClient;

    private final AwsCredentialsProvider creds = StaticCredentialsProvider.create(
            AwsBasicCredentials.create("test", "test"));

    private final URI endpoint;

    {
        try {
            endpoint = new URI("http://localhost:4566/");
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }




    public SampleConsumer(String streamName, Region region) {
        this.streamName = streamName;
        this.region = region;
        this.kinesisClient = KinesisClientUtil.createKinesisAsyncClient(KinesisAsyncClient.builder().region(this.region).credentialsProvider(creds).endpointOverride(endpoint));
    }

    public void run() {
        DynamoDbAsyncClient dynamoDbAsyncClient = DynamoDbAsyncClient.builder().region(region).endpointOverride(endpoint).credentialsProvider(creds).build();
        CloudWatchAsyncClient cloudWatchClient = CloudWatchAsyncClient.builder().region(region).endpointOverride(endpoint).credentialsProvider(creds).build();

        ConfigsBuilder configsBuilder = new ConfigsBuilder(
                streamName,
                streamName,
                kinesisClient,
                dynamoDbAsyncClient,
                cloudWatchClient,
                UUID.randomUUID().toString(),
                new SampleRecordProcessorFactory()
        );

        Scheduler scheduler = new Scheduler(
                configsBuilder.checkpointConfig(),
                configsBuilder.coordinatorConfig(),
                configsBuilder.leaseManagementConfig(),
                configsBuilder.lifecycleConfig(),
                configsBuilder.metricsConfig(),
                configsBuilder.processorConfig(),
                configsBuilder.retrievalConfig()
        );

        Thread schedulerThread = new Thread(scheduler);
        schedulerThread.setDaemon(true);
        schedulerThread.start();
    }

    public static void main(String[] args) {
        String streamName = "test";
        Region region = Region.US_EAST_1;
        log.error("Starting SampleConsumer...");
        new SampleConsumer(streamName, region).run();
    }
}