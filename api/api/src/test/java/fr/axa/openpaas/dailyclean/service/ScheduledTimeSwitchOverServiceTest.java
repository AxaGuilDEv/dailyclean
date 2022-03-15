package fr.axa.openpaas.dailyclean.service;

import fr.axa.openpaas.dailyclean.util.KubernetesUtils;
import fr.axa.openpaas.dailyclean.util.MockedDateUtils;
import io.fabric8.kubernetes.api.model.batch.v1beta1.CronJob;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.KubernetesTestServer;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;

import static fr.axa.openpaas.dailyclean.service.KubernetesArgument.START;
import static fr.axa.openpaas.dailyclean.service.KubernetesArgument.STOP;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@WithKubernetesTestServer
@QuarkusTest
public class ScheduledTimeSwitchOverServiceTest {

    private static final String CRON_10_00 = "0 10 * * *";
    private static final String CRON_11_00 = "0 11 * * *";
    private static final String CRON_9_00 = "0 9 * * *";
    private static final String CRON_20_00 = "0 20 * * *";

    @KubernetesTestServer
    KubernetesServer mockServer;

    @Inject
    ScheduledTimeSwitchOverService service;

    @Inject
    MockedDateUtils dateUtils;

    private static final LocalDate WINTER_TIME_DATE = LocalDate.of(2021, 10, 31);
    private static final LocalDate SUMMER_TIME_DATE = LocalDate.of(2021, 3, 28);
    private static final LocalDate NOT_SUNDAY_DATE = LocalDate.of(2021, 3, 27);
    private static final LocalDate NOT_LAST_SUNDAY_OF_MONTH_DATE = LocalDate.of(2021, 10, 24);

    @BeforeEach
    public void before() {
        KubernetesClient client = mockServer.getClient();
        final String namespace = client.getNamespace();
        client.batch().cronjobs().inNamespace(namespace).delete();
    }

    @Test
    public void shouldChangeSchedulesToSummerTime() {
        dateUtils.setCurrentNow(SUMMER_TIME_DATE);

        String cronStart = CRON_10_00;
        String cronStop = CRON_20_00;

        String summerCronStart = CRON_11_00;
        String summerCronStop = "0 21 * * *";

        initializeExistingCronJobs(cronStart, cronStop);
        service.switchToSummerTime();

        assertThatCronJobsExist(summerCronStart, summerCronStop);
    }

    @Test
    public void shouldNotChangeSchedulesToSummerTimeIfItIsNotSunday() {
        dateUtils.setCurrentNow(NOT_SUNDAY_DATE);

        String cronStart = CRON_10_00;
        String cronStop = CRON_20_00;

        initializeExistingCronJobs(cronStart, cronStop);
        service.switchToSummerTime();

        assertThatCronJobsExist(cronStart, cronStop);
    }

    @Test
    public void shouldNotChangeSchedulesToSummerTimeIfItIsNotLastSundayOfMonth() {
        dateUtils.setCurrentNow(NOT_LAST_SUNDAY_OF_MONTH_DATE);

        String cronStart = CRON_10_00;
        String cronStop = CRON_20_00;

        initializeExistingCronJobs(cronStart, cronStop);
        service.switchToSummerTime();

        assertThatCronJobsExist(cronStart, cronStop);
    }

    @Test
    public void shouldChangeSchedulesToSummerTimeMidnight() {
        dateUtils.setCurrentNow(SUMMER_TIME_DATE);

        String cronStart = CRON_10_00;
        String cronStop = "0 23 * * *";

        String summerCronStart = CRON_11_00;
        String summerCronStop = "0 0 * * *";

        initializeExistingCronJobs(cronStart, cronStop);
        service.switchToSummerTime();

        assertThatCronJobsExist(summerCronStart, summerCronStop);
    }

    @Test
    public void shouldChangeSchedulesToSummerTimeWithDoW() {
        dateUtils.setCurrentNow(SUMMER_TIME_DATE);

        String cronStart = CRON_10_00;
        String cronStop = "0 20 21 10 1-5";

        String summerCronStart = CRON_11_00;
        String summerCronStop = "0 21 21 10 1-5";

        initializeExistingCronJobs(cronStart, cronStop);
        service.switchToSummerTime();

        assertThatCronJobsExist(summerCronStart, summerCronStop);
    }

    @Test
    public void shouldChangeSchedulesToWinterTime() {
        dateUtils.setCurrentNow(WINTER_TIME_DATE);

        String cronStart = CRON_10_00;
        String cronStop = CRON_20_00;

        String winterCronStart = CRON_9_00;
        String winterCronStop = "0 19 * * *";

        initializeExistingCronJobs(cronStart, cronStop);
        service.switchToWinterTime();

        assertThatCronJobsExist(winterCronStart, winterCronStop);
    }

    @Test
    public void shouldNotChangeSchedulesToWinterTimeIfItIsNotSunday() {
        dateUtils.setCurrentNow(NOT_SUNDAY_DATE);

        String cronStart = CRON_10_00;
        String cronStop = CRON_20_00;

        initializeExistingCronJobs(cronStart, cronStop);
        service.switchToWinterTime();

        assertThatCronJobsExist(cronStart, cronStop);
    }

    @Test
    public void shouldNotChangeSchedulesToWinterTimeIfItIsNotLastSundayOfMonth() {
        dateUtils.setCurrentNow(NOT_LAST_SUNDAY_OF_MONTH_DATE);

        String cronStart = CRON_10_00;
        String cronStop = CRON_20_00;

        initializeExistingCronJobs(cronStart, cronStop);
        service.switchToWinterTime();

        assertThatCronJobsExist(cronStart, cronStop);
    }

    @Test
    public void shouldChangeSchedulesToWinterTimeMidnight() {
        dateUtils.setCurrentNow(WINTER_TIME_DATE);

        String cronStart = CRON_10_00;
        String cronStop = "0 0 * * *";

        String winterCronStart = CRON_9_00;
        String winterCronStop = "0 23 * * *";

        initializeExistingCronJobs(cronStart, cronStop);
        service.switchToWinterTime();

        assertThatCronJobsExist(winterCronStart, winterCronStop);
    }

    @Test
    public void shouldChangeSchedulesToWinterTimeWithDow() {
        dateUtils.setCurrentNow(WINTER_TIME_DATE);

        String cronStart = CRON_10_00;
        String cronStop = "0 20 * * 1-5";

        String winterCronStart = CRON_9_00;
        String winterCronStop = "0 19 * * 1-5";

        initializeExistingCronJobs(cronStart, cronStop);
        service.switchToWinterTime();

        assertThatCronJobsExist(winterCronStart, winterCronStop);
    }

    @Test
    public void shouldWorkWithOnlyOneCronSet() {
        dateUtils.setCurrentNow(WINTER_TIME_DATE);

        String cronStart = CRON_10_00;

        String summerCronStart = CRON_11_00;

        initializeExistingCronJobs(cronStart, null);
        service.switchToSummerTime();

        assertThatCronJobsExist(summerCronStart, null);
    }

    @Test
    public void shouldWorkWithNoCronSet() {
        dateUtils.setCurrentNow(WINTER_TIME_DATE);

        service.switchToSummerTime();
        service.switchToWinterTime();
        Assertions.assertTrue(true);
    }


    private void initializeExistingCronJobs(String cronStart, String cronStop) {
        KubernetesClient client = mockServer.getClient();
        final String namespace = client.getNamespace();
        if(cronStart != null) {
            InputStream cronJobStart = KubernetesUtils.createCronJobAsInputStream(START, cronStart, "imgName", "default");
            client.load(cronJobStart).inNamespace(namespace).createOrReplace();
        }
        if(cronStop != null) {
            InputStream cronJobStop = KubernetesUtils.createCronJobAsInputStream(STOP, cronStop, "imgName", "default");
            client.load(cronJobStop).inNamespace(namespace).createOrReplace();
        }
    }

    private void assertThatCronJobsExist(String cronStart, String cronStop) {
        KubernetesClient client = mockServer.getClient();

        final String namespace = client.getNamespace();
        List<CronJob> cronJobs = client.batch().cronjobs().inNamespace(namespace).list().getItems();

        if(cronStart == null || cronStop == null) {
            assertThat(cronJobs.size(), is(1));
        } else {
            assertThat(cronJobs.size(), is(2));
        }


        String startCronJobName = KubernetesUtils.getCronName(START);
        String stopCronJobName = KubernetesUtils.getCronName(STOP);

        cronJobs.forEach(cronJob -> {
            if(cronJob.getMetadata().getName().equals(startCronJobName)) {
                assertThat(cronJob.getSpec().getSchedule(), is(cronStart));
            } else if(cronJob.getMetadata().getName().equals(stopCronJobName)) {
                assertThat(cronJob.getSpec().getSchedule(), is(cronStop));
            } else {
                Assertions.fail("Incorrect name of cron");
            }
        });
    }
}
