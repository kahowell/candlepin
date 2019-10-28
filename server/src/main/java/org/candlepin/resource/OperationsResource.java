/*
 *  Copyright (c) 2009 - 2019 Red Hat, Inc.
 *
 *  This software is licensed to you under the GNU General Public License,
 *  version 2 (GPLv2). There is NO WARRANTY for this software, express or
 *  implied, including the implied warranties of MERCHANTABILITY or FITNESS
 *  FOR A PARTICULAR PURPOSE. You should have received a copy of GPLv2
 *  along with this software; if not, see
 *  http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt.
 *
 *  Red Hat trademarks are not licensed under GPLv2. No permission is
 *  granted to use or replicate Red Hat trademarks that are incorporated
 *  in this software or its documentation.
 */
package org.candlepin.resource;

import org.candlepin.api.v3.model.CandlepinVersion;
import org.candlepin.api.v3.model.Certificate;
import org.candlepin.api.v3.model.InstalledProduct;
import org.candlepin.api.v3.model.ListReleasesResult;
import org.candlepin.api.v3.model.ListServiceLevelsResult;
import org.candlepin.api.v3.model.Pool;
import org.candlepin.api.v3.model.ProductStatus;
import org.candlepin.api.v3.model.Repo;
import org.candlepin.api.v3.model.SubscriptionStatus;
import org.candlepin.api.v3.model.SyncResult;
import org.candlepin.api.v3.model.SystemProfile;
import org.candlepin.api.v3.model.SystemPurpose;
import org.candlepin.api.v3.resources.OperationsApi;
import org.candlepin.auth.Principal;
import org.candlepin.common.util.VersionUtil;
import org.candlepin.controller.ContentController;
import org.candlepin.controller.EnvironmentController;
import org.candlepin.controller.ProfileController;
import org.candlepin.controller.RegistrationController;
import org.candlepin.controller.SubscriptionController;
import org.candlepin.model.Consumer;
import org.candlepin.model.ConsumerCurator;
import org.candlepin.model.ConsumerInstalledProduct;
import org.candlepin.model.ContentOverride;
import org.candlepin.model.Entitlement;
import org.candlepin.model.Owner;
import org.candlepin.model.OwnerCurator;
import org.candlepin.model.ProductContent;
import org.candlepin.policy.EntitlementRefusedException;
import org.candlepin.policy.js.JsRunnerProvider;
import org.candlepin.policy.js.compliance.ComplianceReason;
import org.candlepin.policy.js.compliance.ComplianceStatus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.core.Context;

public class OperationsResource implements OperationsApi {

    private static final Logger log = LoggerFactory.getLogger(OperationsResource.class);

    @Context Principal principal;

    private final RegistrationController registrationController;
    private final SubscriptionController subscriptionController;
    private final JsRunnerProvider jsProvider;
    private final String version;
    private final String release;
    private final ConsumerCurator consumerCurator;
    private final OwnerCurator ownerCurator;
    private final EnvironmentController environmentController;
    private final ProfileController profileController;
    private final ContentController contentController;

    @Inject
    public OperationsResource(RegistrationController registrationController,
        SubscriptionController subscriptionController, JsRunnerProvider jsProvider,
        ConsumerCurator consumerCurator, OwnerCurator ownerCurator,
        EnvironmentController environmentController, ProfileController profileController,
        ContentController contentController) {

        this.registrationController = registrationController;
        this.subscriptionController = subscriptionController;
        this.jsProvider = jsProvider;
        this.consumerCurator = consumerCurator;
        this.ownerCurator = ownerCurator;
        this.environmentController = environmentController;
        this.profileController = profileController;
        this.contentController = contentController;

        Map<String, String> map = VersionUtil.getVersionMap();
        version = map.get("version");
        release = map.get("release");
    }

    @Override
    public SyncResult attach(String consumerId, List<String> pools, Integer quantity) {
        try {
            Collection<Entitlement> entitlements = subscriptionController.attach(consumerId, pools, quantity);
            return new SyncResult()
                .addedEntitlementCertificates(tranformEntitlementCerts(entitlements))
                .addedRepos(transformRepos(entitlements));
        }
        catch (EntitlementRefusedException e) {
            throw new RuntimeException(e);
        }
    }

    private List<Repo> transformRepos(Collection<Entitlement> entitlements) {
        // TODO filter to reported product tags/ids, apply repo overrides
        return entitlements.stream().map(this::transformRepo).flatMap(List::stream)
            .collect(Collectors.toList());
    }

    private List<Repo> transformRepo(Entitlement entitlement) {
        return entitlement.getPool().getProduct().getProductContent().stream().map(ProductContent::getContent)
            .map(content -> {
                List<String> entitlementSerials =
                    entitlement.getCertificates().stream().map(e -> e.getSerial().getSerial())
                        .map(BigInteger::toString).collect(Collectors.toList());
                Map<String, String> attributes = new HashMap<>();
                attributes.put("arches", content.getArches());
                attributes.put("url", content.getContentUrl());
                attributes.put("gpgurl", content.getGpgUrl());
                attributes.put("id", content.getId());
                attributes.put("entitlements", String.join(",", entitlementSerials));
                return new Repo()
                    .id(content.getLabel())
                    .attributes(attributes);
            })
            .collect(Collectors.toList());
    }

    private List<Certificate> tranformEntitlementCerts(Collection<Entitlement> entitlements) {
        return entitlements.stream().map(Entitlement::getCertificates)
            .flatMap(Set::stream).map(cert -> new Certificate()
                .certificate(cert.getCertificate())
                .key(cert.getKey())
            ).collect(Collectors.toList());
    }

    @Override
    public SyncResult autoattach(String consumerId, String servicelevel) {
        Collection<Entitlement> entitlements = subscriptionController.autoattach(consumerId, servicelevel);
        return new SyncResult()
            .addedRepos(transformRepos(entitlements))
            .addedEntitlementCertificates(tranformEntitlementCerts(entitlements));
    }

    @Override
    public ListServiceLevelsResult getServiceLevel(String consumerId) {
        Consumer consumer = consumerCurator.getConsumer(consumerId);
        List<String> slas = environmentController.getSlas(consumer);
        return new ListServiceLevelsResult()
            .currentServiceLevel(consumer.getServiceLevel())
            .availableServiceLevels(slas);
    }

    @Override
    public SubscriptionStatus getStatus(String consumerId) {
        Consumer consumer = consumerCurator.getConsumer(consumerId);
        ComplianceStatus status = subscriptionController.getStatus(consumer);
        return new SubscriptionStatus()
            .overallStatus(status.getStatus())
            .products(mapProductStatuses(consumer, status));
    }

    private List<ProductStatus> mapProductStatuses(Consumer consumer, ComplianceStatus status) {
        // TODO handle multiple reasons
        Map<String, String> reasonsMap = status.getReasons().stream()
            .collect(Collectors.toMap(ComplianceReason::getKey, ComplianceReason::getMessage));
        return consumer.getInstalledProducts().stream()
            .map(p -> new ProductStatus().product(p.getProductName()).status(reasonsMap.get(p.getProductId())))
            .collect(Collectors.toList());
    }

    @Override
    public CandlepinVersion getVersion() {
        return new CandlepinVersion()
            .rulesVersion(jsProvider.getRulesVersion())
            .serverVersion(version)
            .serverRelease(release);
    }

    @Override
    public List<String> listEnvironments(String org) {
        throw new UnsupportedOperationException("Not implemented in Candlepin.");
        // N.B. implemented in Katello
    }

    @Override
    public List<String> listOrgs() {
        return ownerCurator.listAll().transform(Owner::getKey).list();
    }

    @Override
    public List<String> listPoolIds(String consumerId, String afterdate, Boolean all, Boolean available,
        Boolean consumed, String ondate, Boolean nooverlap, Boolean matchinstalled, String matches) {

        return subscriptionController.getPoolIds(consumerId, all, available, consumed, ondate, nooverlap,
                    matchinstalled, matches);
    }

    @Override
    public ListReleasesResult listReleases(String consumerId) {
        Consumer consumer = consumerCurator.getConsumer(consumerId);
        return new ListReleasesResult()
            .currentRelease(consumer.getReleaseVer().getReleaseVer());
        // N.B. Katello implements available releases
    }

    @Override
    public List<String> listRepoOverrides(String consumerId, List<String> repo) {
        Collection<ContentOverride> overrides = contentController.getOverrides(consumerId, repo);
        return overrides.stream().map(override -> String.format("%s:%s:%s", override.getContentLabel(),
            override.getName(), override.getValue())).collect(Collectors.toList());
    }

    @Override
    public List<Repo> listRepos(String consumerId, Boolean enabled, Boolean disabled) {
        List<Entitlement> entitlements = contentController.getEntitlements(consumerId, enabled, disabled);
        return entitlements.stream().map(this::transformRepo).flatMap(List::stream)
            .collect(Collectors.toList());
    }

    @Override
    public List<Pool> listSubscriptions(String consumerId, String afterdate, Boolean all, Boolean available,
        Boolean consumed, String ondate, Boolean nooverlap, Boolean matchinstalled, String matches) {

        return subscriptionController.getPools(consumerId, afterdate, all, available, consumed, ondate,
            nooverlap, matchinstalled, matches).stream().map(this::transformPool)
            .collect(Collectors.toList());
    }

    private Pool transformPool(org.candlepin.model.Pool pool) {
        return new Pool()
            .id(pool.getId()); // TODO other fields
    }

    @Override
    public void redeem(String consumerId, String email, String locale, String org) {
        subscriptionController.redeem(consumerId, email, locale, org);
    }

    @Override
    public SyncResult refresh(String consumerId, String lastStateToken) {
        return new SyncResult(); // TODO fancy logic!
    }

    @Override
    public SyncResult regenerateIdentity(String consumerId) {
        try {
            Consumer consumer = registrationController.regenerateIdentity(consumerId);

            return new SyncResult().identityCertificate(
                new Certificate().certificate(consumer.getIdCert().getCertificate())
                    .key(consumer.getIdCert().getKey()));
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public SyncResult register(String name, String consumerid, List<String> activationkey, Boolean autoattach,
        String servicelevel, String org, String environment, String release, SystemProfile systemProfile) {

        try {
            SystemPurpose sysPurpose = systemProfile.getSystemPurpose();
            if (sysPurpose == null) {
                sysPurpose = new SystemPurpose();
            }
            Consumer consumer = registrationController
                .register(name, consumerid, activationkey, autoattach, servicelevel, org, environment,
                    release, systemProfile.getFacts(),
                    transformInstalledProducts(systemProfile.getInstalledProducts()),
                    systemProfile.getInstalledTags(), sysPurpose.getAddons(), sysPurpose.getRole(),
                    sysPurpose.getUsage());

            return new SyncResult()
                .identityCertificate(
                    new Certificate()
                        .certificate(consumer.getIdCert().getCertificate())
                        .key(consumer.getIdCert().getKey())
                )
                .addedEntitlementCertificates(tranformEntitlementCerts(consumer.getEntitlements()))
                .addedRepos(transformRepos(consumer.getEntitlements()));
        }
        catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public SyncResult remove(String consumerId, List<String> serial, List<String> pool, Boolean all) {
        return subscriptionController.remove(consumerId, serial, pool, all);
    }

    @Override
    public void setRelease(String consumerId, String release) {
        profileController.setRelease(consumerId, release);
    }

    @Override
    public void setServiceLevel(String consumerId, String release) {
        profileController.setServiceLevel(consumerId, release);
    }

    @Override
    public void unregister(String consumerId) {
        registrationController.unregister(consumerId);
    }

    @Override
    public SyncResult updateProfile(String consumerId, SystemProfile systemProfile) {
        SystemPurpose sysPurpose = systemProfile.getSystemPurpose();
        profileController.updateProfile(consumerId, systemProfile.getFacts(),
            transformInstalledProducts(systemProfile.getInstalledProducts()),
            systemProfile.getInstalledTags(), systemProfile.getRelease(),
            sysPurpose.getAddons(), sysPurpose.getRole(), sysPurpose.getSla(), sysPurpose.getUsage());
        return new SyncResult(); // TODO handle entitlement changes resulting from check-in
    }

    private List<ConsumerInstalledProduct> transformInstalledProducts(
        List<InstalledProduct> installedProducts) {

        return installedProducts.stream().map(p -> {
            ConsumerInstalledProduct consumerInstalledProduct = new ConsumerInstalledProduct();
            consumerInstalledProduct.setArch(p.getArch());
            consumerInstalledProduct.setProductId(p.getId());
            consumerInstalledProduct.setProductName(p.getName());
            consumerInstalledProduct.setVersion(p.getVersion());
            return consumerInstalledProduct;
        }).collect(Collectors.toList());
    }

    @Override
    public void updateRepoOverrides(String consumerId, List<String> repos, List<String> add,
        List<String> remove, Boolean removeall) {
        if (Boolean.TRUE.equals(removeall)) {
            contentController.removeAllOverrides(consumerId, repos);
        }
        else {
            contentController.updateOverrides(consumerId, repos, add, remove);
        }
    }

    @Override
    public void updateRepos(String consumerId, List<String> enable, List<String> disable) {
        contentController.toggleRepos(consumerId, enable, disable);
    }
}
