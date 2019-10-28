package org.candlepin.controller;

import org.candlepin.model.Consumer;
import org.candlepin.model.ConsumerContentOverrideCurator;
import org.candlepin.model.ConsumerCurator;
import org.candlepin.model.ContentOverride;
import org.candlepin.model.ContentOverrideCurator;
import org.candlepin.model.Entitlement;
import org.candlepin.model.EntitlementCurator;

import com.google.inject.Inject;
import com.google.inject.persist.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ContentController {

    private final ConsumerContentOverrideCurator contentOverrideCurator;
    private final EntitlementCurator entitlementCurator;
    private final ConsumerCurator consumerCurator;

    @Inject
    public ContentController(ConsumerContentOverrideCurator contentOverrideCurator,
        EntitlementCurator entitlementCurator, ConsumerCurator consumerCurator) {

        this.contentOverrideCurator = contentOverrideCurator;
        this.entitlementCurator = entitlementCurator;
        this.consumerCurator = consumerCurator;
    }

    @Transactional
    public void removeAllOverrides(String consumerId, List<String> repos) {
        // TODO
    }

    public void updateOverrides(String consumerId, List<String> repos, List<String> add, List<String> remove) {
        // TODO
    }

    public void toggleRepos(String consumerId, List<String> enable, List<String> disable) {
        // TODO
    }

    public Collection<ContentOverride> getOverrides(String consumerId, List<String> repo) {
        return null; // TODO
    }

    public List<Entitlement> getEntitlements(String consumerId, Boolean enabled, Boolean disabled) {
        // TODO handle enabled, disabled
        Consumer consumer = consumerCurator.getConsumer(consumerId);
        return new ArrayList<>(consumer.getEntitlements());
    }
}
