package org.candlepin.controller;

import org.candlepin.api.v3.model.SyncResult;
import org.candlepin.model.CertificateSerial;
import org.candlepin.model.Consumer;
import org.candlepin.model.ConsumerCurator;
import org.candlepin.model.Entitlement;
import org.candlepin.model.EntitlementCurator;
import org.candlepin.model.Owner;
import org.candlepin.model.Pool;
import org.candlepin.model.PoolCurator;
import org.candlepin.model.RevocableCertificate;
import org.candlepin.policy.EntitlementRefusedException;
import org.candlepin.policy.js.compliance.ComplianceRules;
import org.candlepin.policy.js.compliance.ComplianceStatus;
import org.candlepin.resource.dto.AutobindData;

import com.google.inject.Inject;
import com.google.inject.persist.Transactional;

import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class SubscriptionController {

    private final ConsumerCurator consumerCurator;
    private final Entitler entitler;
    private final ComplianceRules complianceRules;
    private final EntitlementCurator entitlementCurator;
    private final PoolCurator poolCurator;

    @Inject
    public SubscriptionController(ConsumerCurator consumerCurator, Entitler entitler,
        ComplianceRules complianceRules, EntitlementCurator entitlementCurator, PoolCurator poolCurator) {

        this.consumerCurator = consumerCurator;
        this.entitler = entitler;
        this.complianceRules = complianceRules;
        this.entitlementCurator = entitlementCurator;
        this.poolCurator = poolCurator;
    }

    @Transactional
    public Collection<Entitlement> attach(String consumerId, List<String> pools, Integer quantity)
        throws EntitlementRefusedException {

        return entitler.bindByPoolQuantities(consumerId,
            pools.stream().collect(Collectors.toMap(p -> p, p -> quantity)));
    }

    @Transactional
    public Collection<Entitlement> autoattach(String consumerId, String servicelevel) {
        Consumer consumer = consumerCurator.getConsumer(consumerId);
        if (servicelevel != null) {
            consumer.setServiceLevel(servicelevel);
            consumerCurator.update(consumer);
        }
        Owner owner = consumer.getOwner();
        return autoattach(consumer, owner);
    }

    @Transactional
    public Collection<Entitlement> autoattach(Consumer consumer, Owner owner) {
        AutobindData autobindData = AutobindData.create(consumer, owner);
        try {
            return entitler.bindByProducts(autobindData);
        }
        catch (AutobindDisabledForOwnerException e) {
            throw new RuntimeException(e);
        }
    }

    public ComplianceStatus getStatus(Consumer consumer) {
        return complianceRules.getStatus(consumer, null);
    }

    public SyncResult remove(String consumerId, List<String> serials, List<String> pools, boolean all) {
        Collection<Entitlement> entitlements;
        Consumer consumer = consumerCurator.getConsumer(consumerId);
        if (Boolean.TRUE.equals(all)) {
            entitlements = consumer.getEntitlements();
        }
        else if (pools != null && !pools.isEmpty()) {
            entitlements = pools.stream().map(
                pool -> entitlementCurator.listByConsumerAndPoolId(consumer, pool)
            ).flatMap(List::stream).collect(Collectors.toList());
        }
        else if (serials != null && !serials.isEmpty()) {
            entitlements = serials.stream().map(Long::parseLong)
                .map(entitlementCurator::findByCertificateSerial).collect(Collectors.toList());
        }
        else {
            throw new RuntimeException("Bad arguments, nothing asked to remove...");
        }
        entitlementCurator.batchDelete(entitlements);
        List<String> removedCertIds = entitlements.stream().map(Entitlement::getCertificates)
            .flatMap(Set::stream).map(RevocableCertificate::getSerial).map(CertificateSerial::getSerial)
            .map(BigInteger::toString).collect(Collectors.toList());
        return new SyncResult()
            .removedEntitlementCertificateIds(removedCertIds);
    }

    public void redeem(String consumerId, String email, String locale, String org) {
        // TODO implement
    }

    public List<String> getPoolIds(String consumerId, Boolean all, Boolean available, Boolean consumed, String ondate, Boolean nooverlap, Boolean matchinstalled, String matches) {
        Consumer consumer = consumerCurator.getConsumer(consumerId);
        List<Pool> pools = poolCurator.listByConsumer(consumer); // TODO optimize
        return pools.stream().map(Pool::getId).collect(Collectors.toList());
    }

    public Collection<Pool> getPools(String consumerId, String afterdate, Boolean all, Boolean available, Boolean consumed, String ondate, Boolean nooverlap, Boolean matchinstalled, String matches) {
        Consumer consumer = consumerCurator.getConsumer(consumerId);
        return poolCurator.listByConsumer(consumer);
    }
}
