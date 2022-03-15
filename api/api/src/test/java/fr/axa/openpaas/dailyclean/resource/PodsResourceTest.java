package fr.axa.openpaas.dailyclean.resource;

import fr.axa.openpaas.dailyclean.service.KubernetesArgument;
import fr.axa.openpaas.dailyclean.util.KubernetesUtils;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.KubernetesTestServer;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@WithKubernetesTestServer
@QuarkusTest
public class PodsResourceTest {

    private static final String MESSAGE = "message";

    @KubernetesTestServer
    KubernetesServer mockServer;

    @BeforeEach
    public void before() {
        KubernetesClient client = mockServer.getClient();
        client.batch().jobs().delete();
    }

    @Test
    public void shouldStartPods() {
        given()
                .when().post("/pods/start")
                .then()
                    .statusCode(HttpStatus.SC_OK)
                    .body(MESSAGE, is(PodsResource.START_MESSAGE));

        KubernetesClient client = mockServer.getClient();
        final String namespace = client.getNamespace();
        List<Job> jobs = client.batch().jobs().inNamespace(namespace).list().getItems();
        assertThat(jobs.size(), is(1));

        Job job = jobs.get(0);
        assertThat(job.getMetadata().getName(), is(KubernetesUtils.getJobName(KubernetesArgument.START)));
    }

    @Test
    public void shouldStopPods() {
        given()
                .when().post("/pods/stop")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(MESSAGE, is(PodsResource.STOP_MESSAGE));

        KubernetesClient client = mockServer.getClient();
        final String namespace = client.getNamespace();
        List<Job> jobs = client.batch().jobs().inNamespace(namespace).list().getItems();
        assertThat(jobs.size(), is(1));

        Job job = jobs.get(0);
        assertThat(job.getMetadata().getName(), is(KubernetesUtils.getJobName(KubernetesArgument.STOP)));
    }

    @Test
    public void shouldDeleteExistingJob() {
        KubernetesClient client = mockServer.getClient();
        final String namespace = client.getNamespace();

        InputStream is = KubernetesUtils.createJobAsInputStream(KubernetesArgument.START, "imgName", "default");
        client.batch().jobs().load(is).createOrReplace();

        given()
                .when().post("/pods/start")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(MESSAGE, is(PodsResource.START_MESSAGE));

        List<Job> jobs = client.batch().jobs().inNamespace(namespace).list().getItems();
        assertThat(jobs.size(), is(1));

        Job job = jobs.get(0);
        assertThat(job.getMetadata().getName(), is(KubernetesUtils.getJobName(KubernetesArgument.START)));
    }
}
