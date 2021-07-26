package no.neic.tryggve;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import no.neic.tryggve.constants.ConfigName;


public class App extends AbstractVerticle {

    @Override
    public void start() throws Exception {
        Config.init();
        DeploymentOptions deploymentOptions = new DeploymentOptions();
        deploymentOptions.setInstances(Integer.valueOf(Config.valueOf(ConfigName.HTTP_VERTICLE_INSTANCE_NUMBER)));
        deploymentOptions.setWorkerPoolSize(Integer.valueOf(Config.valueOf(ConfigName.WORKER_THREAD_POOL_SIZE)));
        vertx.deployVerticle(HttpVerticle.class.getName(), deploymentOptions);

        no.neic.ssh.Config.init();
        DeploymentOptions deploymentOptions4SSH = new DeploymentOptions();
        deploymentOptions4SSH.setInstances(Integer.parseInt(Config.valueOf(no.neic.ssh.constants.ConfigName.HTTP_VERTICLE_INSTANCE_NUMBER)));
        deploymentOptions4SSH.setWorkerPoolSize(Integer.parseInt(Config.valueOf(no.neic.ssh.constants.ConfigName.WORKER_THREAD_POOL_SIZE)));
        vertx.deployVerticle(no.neic.ssh.HttpVerticle.class.getName(), deploymentOptions4SSH);
    }

}
