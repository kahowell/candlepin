package org.candlepin.controller;

import org.candlepin.model.Consumer;
import org.candlepin.model.ConsumerCurator;
import org.candlepin.model.ConsumerInstalledProduct;
import org.candlepin.model.Release;

import com.google.inject.Inject;
import com.google.inject.persist.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class ProfileController {

    private final ConsumerCurator consumerCurator;

    @Inject
    public ProfileController(ConsumerCurator consumerCurator) {
        this.consumerCurator = consumerCurator;
    }

    @Transactional
    public Consumer updateProfile(
        String consumerId,
        Map<String, String> facts,
        List<ConsumerInstalledProduct> installedProducts,
        List<String> installedTags,
        String release,
        List<String> addons,
        String role,
        String sla,
        String usage
    ) {
        Consumer consumer = consumerCurator.getConsumer(consumerId);
        applyProfile(consumer, facts, installedProducts, installedTags, release, addons, role, sla, usage);
        consumerCurator.update(consumer);
        return consumer;
    }

    void applyProfile(
        Consumer consumer,
        Map<String, String> facts,
        List<ConsumerInstalledProduct> installedProducts,
        List<String> installedTags,
        String release,
        List<String> addons,
        String role,
        String sla,
        String usage
    ) {
        consumer.setServiceLevel(sla);
        consumer.setRole(role);
        consumer.setAddOns(new HashSet<>(addons));
        consumer.setUsage(usage);
        consumer.setReleaseVer(new Release(release));
        consumer.setInstalledProducts(new HashSet<>(installedProducts));
        consumer.setContentTags(new HashSet<>(installedTags));
        facts.forEach(consumer::setFact);
    }

    @Transactional
    public void setServiceLevel(String consumerId, String servicelevel) {
        Consumer consumer = consumerCurator.getConsumer(consumerId);
        consumer.setServiceLevel(servicelevel);
        consumerCurator.update(consumer);
    }

    @Transactional
    public void setRelease(String consumerId, String release) {
        Consumer consumer = consumerCurator.getConsumer(consumerId);
        consumer.setReleaseVer(new Release(release));
        consumerCurator.update(consumer);
    }
}
