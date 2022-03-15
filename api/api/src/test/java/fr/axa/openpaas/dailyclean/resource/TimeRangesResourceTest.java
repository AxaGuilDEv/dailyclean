package fr.axa.openpaas.dailyclean.resource;

import fr.axa.openpaas.dailyclean.model.TimeRange;
import fr.axa.openpaas.dailyclean.util.KubernetesUtils;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.batch.v1beta1.CronJob;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.KubernetesTestServer;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;
import org.apache.http.HttpStatus;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import java.io.InputStream;
import java.util.List;

import static fr.axa.openpaas.dailyclean.service.KubernetesArgument.START;
import static fr.axa.openpaas.dailyclean.service.KubernetesArgument.STOP;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@WithKubernetesTestServer
@QuarkusTest
public class TimeRangesResourceTest {

    private static final String OLD_IMAGE_NAME = "imagename:1.0.0";
    private static final String TIMERANGES_URI = "/timeranges";
    private static final String CRON_START = "cron_start";
    private static final String CRON_STOP = "cron_stop";
    private static final String CRON_9_00 = "0 9 * * *";
    private static final String CRON_19_00 = "0 19 * * *";
    private static final String CRON_10_00 = "0 10 * * *";
    private static final String CRON_20_00 = "0 20 * * *";
    private static final String IMG_NAME = "imgName";
    private static final String SERVICE_ACCOUNT_NAME = "default";

    @KubernetesTestServer
    KubernetesServer mockServer;

    @Inject
    TimeRangesResource resource;

    @ConfigProperty(name = "service.job.imageName")
    String imgName;

    @BeforeEach
    public void before() {
        KubernetesClient client = mockServer.getClient();
        final String namespace = client.getNamespace();
        client.batch().cronjobs().inNamespace(namespace).delete();
    }

    @Test
    public void shouldCreateCronJobs() {
        String cronStart = CRON_9_00;
        String cronStop = CRON_19_00;

        TimeRange timeRange = new TimeRange();
        timeRange.setCronStart(cronStart);
        timeRange.setCronStop(cronStop);
        given()
                .contentType(MediaType.APPLICATION_JSON)
                .when().body(timeRange)
                .post(TIMERANGES_URI)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(CRON_START, is(cronStart))
                .body(CRON_STOP, is(cronStop));

        assertThatCronJobsExist(cronStart, cronStop);
    }

    @Test
    public void shouldReturn400WhenCronAreNotSet() {
        given()
                .contentType(MediaType.APPLICATION_JSON)
                .when()
                .post(TIMERANGES_URI)
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    @Test
    public void shouldGetTimeRanges() {
        String cronStart = CRON_10_00;
        String cronStop = CRON_20_00;

        initializeExistingCronJobs(cronStart, cronStop);

        given()
                .contentType(MediaType.APPLICATION_JSON)
                .when()
                .get(TIMERANGES_URI)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(CRON_START, is(cronStart))
                .body(CRON_STOP, is(cronStop));
    }

    @Test
    public void shouldReturn200WithEmptyTimerangesWhenNoTimeRangeIsSet() {
        given()
                .contentType(MediaType.APPLICATION_JSON)
                .when()
                .get(TIMERANGES_URI)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(CRON_START, is(nullValue()))
                .body(CRON_STOP, is(nullValue()));
    }

    @Test
    public void shouldNotDeleteCronJobsIfTimeRangesAreNotSet() {
        String cronStart = CRON_10_00;
        String cronStop = CRON_20_00;

        initializeExistingCronJobs(cronStart, cronStop);

        given()
                .contentType(MediaType.APPLICATION_JSON)
                .when()
                .post(TIMERANGES_URI)
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST);

        assertThatCronJobsExist(cronStart, cronStop);
    }

    @Test
    public void shouldUpsertExistingTimerangesWithNewOnes() {
        String cronStart = CRON_10_00;
        String cronStop = CRON_20_00;

        initializeExistingCronJobs(cronStart, cronStop);

        String newCronStart = CRON_9_00;

        TimeRange timeRange = new TimeRange();
        timeRange.setCronStart(newCronStart);
        given()
                .contentType(MediaType.APPLICATION_JSON)
                .when().body(timeRange)
                .post(TIMERANGES_URI)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(CRON_START, is(newCronStart))
                .body(CRON_STOP, is(nullValue()));

        KubernetesClient client = mockServer.getClient();

        final String namespace = client.getNamespace();
        List<CronJob> cronJobs = client.batch().cronjobs().inNamespace(namespace).list().getItems();
        assertThat(cronJobs.size(), is(1));

        CronJob cronJob = cronJobs.get(0);
        assertThat(cronJob.getMetadata().getName(), is(KubernetesUtils.getCronName(START)));
        assertThat(cronJob.getSpec().getSchedule(), is(newCronStart));
    }

    @Test
    public void shouldUpsertExistingTimerangeWithNewOneAndCreate() {
        String cronStart = CRON_10_00;

        InputStream cronJobStart = KubernetesUtils.createCronJobAsInputStream(START, cronStart, IMG_NAME, SERVICE_ACCOUNT_NAME);

        KubernetesClient client = mockServer.getClient();
        final String namespace = client.getNamespace();
        client.load(cronJobStart).inNamespace(namespace).createOrReplace();

        String newCronStart = CRON_9_00;
        String newCronStop = CRON_20_00;

        TimeRange timeRange = new TimeRange();
        timeRange.setCronStart(newCronStart);
        timeRange.setCronStop(newCronStop);
        given()
                .contentType(MediaType.APPLICATION_JSON)
                .when().body(timeRange)
                .post(TIMERANGES_URI)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(CRON_START, is(newCronStart))
                .body(CRON_STOP, is(newCronStop));

        assertThatCronJobsExist(newCronStart, newCronStop);
    }

    @Test
    public void shouldUpdateTheImageVersionOnStartup() {
        KubernetesClient client = mockServer.getClient();
        final String namespace = client.getNamespace();

        // Create cron jobs with old versions
        InputStream cronJobStart =
                KubernetesUtils.createCronJobAsInputStream(START, CRON_10_00, OLD_IMAGE_NAME, SERVICE_ACCOUNT_NAME);
        InputStream cronJobStop =
                KubernetesUtils.createCronJobAsInputStream(STOP, CRON_10_00, OLD_IMAGE_NAME, SERVICE_ACCOUNT_NAME);

        client.load(cronJobStart).inNamespace(namespace).createOrReplace();
        client.load(cronJobStop).inNamespace(namespace).createOrReplace();

        resource.onStart(null);

        List<CronJob> cronJobs = client.batch().cronjobs().inNamespace(namespace).list().getItems();
        assertThat(cronJobs.size(), is(2));

        cronJobs.forEach(cronJob -> {
            Container container = cronJob.getSpec()
                    .getJobTemplate()
                    .getSpec()
                    .getTemplate()
                    .getSpec()
                    .getContainers()
                    .stream().findFirst().orElseThrow();

            assertThat(container.getImage(), is(imgName));
        });
    }

    @Test
    public void shouldUpdateTheImageVersionOnStartupOnlyIfStartExisted() {
        KubernetesClient client = mockServer.getClient();
        final String namespace = client.getNamespace();

        String startCronJobName = KubernetesUtils.getCronName(START);
        String cronStart = CRON_10_00;


        // Create cron jobs with old versions
        InputStream cronJobStart =
                KubernetesUtils.createCronJobAsInputStream(START, cronStart, OLD_IMAGE_NAME, SERVICE_ACCOUNT_NAME);

        client.load(cronJobStart).inNamespace(namespace).createOrReplace();

        resource.onStart(null);

        List<CronJob> cronJobs = client.batch().cronjobs().inNamespace(namespace).list().getItems();
        assertThat(cronJobs.size(), is(1));

        cronJobs.forEach(cronJob -> {
            Container container = cronJob.getSpec()
                    .getJobTemplate()
                    .getSpec()
                    .getTemplate()
                    .getSpec()
                    .getContainers()
                    .stream().findFirst().orElseThrow();

            assertThat(container.getImage(), is(imgName));
            assertThat(cronJob.getSpec().getSchedule(), is(cronStart));
            assertThat(cronJob.getMetadata().getName(), is(startCronJobName));
        });
    }

    @Test
    public void shouldUpdateTheImageVersionOnStartupOnlyIfStopExisted() {
        KubernetesClient client = mockServer.getClient();
        final String namespace = client.getNamespace();

        String stopCronJobName = KubernetesUtils.getCronName(STOP);
        String cronStop = CRON_10_00;


        // Create cron jobs with old versions
        InputStream cronJobStop =
                KubernetesUtils.createCronJobAsInputStream(STOP, cronStop, OLD_IMAGE_NAME, SERVICE_ACCOUNT_NAME);

        client.load(cronJobStop).inNamespace(namespace).createOrReplace();

        resource.onStart(null);

        List<CronJob> cronJobs = client.batch().cronjobs().inNamespace(namespace).list().getItems();
        assertThat(cronJobs.size(), is(1));

        cronJobs.forEach(cronJob -> {
            Container container = cronJob.getSpec()
                    .getJobTemplate()
                    .getSpec()
                    .getTemplate()
                    .getSpec()
                    .getContainers()
                    .stream().findFirst().orElseThrow();

            assertThat(container.getImage(), is(imgName));
            assertThat(cronJob.getSpec().getSchedule(), is(cronStop));
            assertThat(cronJob.getMetadata().getName(), is(stopCronJobName));
        });
    }

    private void initializeExistingCronJobs(String cronStart, String cronStop) {
        InputStream cronJobStart = KubernetesUtils.createCronJobAsInputStream(START, cronStart, IMG_NAME, SERVICE_ACCOUNT_NAME);
        InputStream cronJobStop = KubernetesUtils.createCronJobAsInputStream(STOP, cronStop, IMG_NAME, SERVICE_ACCOUNT_NAME);

        KubernetesClient client = mockServer.getClient();
        final String namespace = client.getNamespace();
        client.load(cronJobStart).inNamespace(namespace).createOrReplace();
        client.load(cronJobStop).inNamespace(namespace).createOrReplace();
    }

    private void assertThatCronJobsExist(String cronStart, String cronStop) {
        KubernetesClient client = mockServer.getClient();

        final String namespace = client.getNamespace();
        List<CronJob> cronJobs = client.batch().cronjobs().inNamespace(namespace).list().getItems();
        assertThat(cronJobs.size(), is(2));

        String startCronJobName = KubernetesUtils.getCronName(START);
        String stopCronJobName = KubernetesUtils.getCronName(STOP);

        cronJobs.forEach(cronJob -> {
            assertThat(cronJob.getSpec().getSchedule(), anyOf(is(cronStart), is(cronStop)));
            assertThat(cronJob.getMetadata().getName(), anyOf(is(startCronJobName), is(stopCronJobName)));
        });
    }
}
