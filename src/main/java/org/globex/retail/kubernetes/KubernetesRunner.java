package org.globex.retail.kubernetes;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.regex.Pattern;

@ApplicationScoped
public class KubernetesRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(KubernetesRunner.class);

    @Inject
    KubernetesClient client;

    public int run() {

        String threescaleNamespace = System.getenv().getOrDefault("THREESCALE_NAMESPACE", "3scale");

        String namespace = System.getenv("NAMESPACE");
        if (namespace == null || namespace.isBlank()) {
            LOGGER.error("Environment variable 'NAMESPACE' for namespace not set. Exiting...");
            return -1;
        }
        String user = "";
        if (namespace.matches(".*-user[0-9]+")) {
            String[] split = namespace.split(Pattern.quote("-"));
            user = split[split.length -1];
        }

        String threescaleTenantSecretName = System.getenv().getOrDefault("THREESCALE_SECRET", "3scale-" + user + "-tenant-secret");

        Secret threescaleTenantSecret = client.secrets().inNamespace(threescaleNamespace).withName(threescaleTenantSecretName).get();
        if (threescaleTenantSecret == null) {
            LOGGER.error("Secret " + threescaleTenantSecretName + " not found in namespace " + threescaleNamespace + ". Exiting");
            return -1;
        }

        Secret newSecret = new SecretBuilder().withNewMetadata().withName(threescaleTenantSecretName).endMetadata()
                .addToData(threescaleTenantSecret.getData()).build();
        client.secrets().inNamespace(namespace).resource(newSecret).createOrReplace();

        LOGGER.info("Secret " + threescaleTenantSecretName + " created in namespace " + namespace + ".");

        return 0;
    }

}
