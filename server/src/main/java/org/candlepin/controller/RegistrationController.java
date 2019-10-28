package org.candlepin.controller;

import org.candlepin.audit.Event;
import org.candlepin.audit.EventBuilder;
import org.candlepin.audit.EventFactory;
import org.candlepin.audit.EventSink;
import org.candlepin.model.Consumer;
import org.candlepin.model.ConsumerCurator;
import org.candlepin.model.ConsumerInstalledProduct;
import org.candlepin.model.ConsumerType;
import org.candlepin.model.ConsumerTypeCurator;
import org.candlepin.model.IdentityCertificate;
import org.candlepin.model.Owner;
import org.candlepin.model.OwnerCurator;
import org.candlepin.service.IdentityCertServiceAdapter;

import com.google.inject.Inject;
import com.google.inject.persist.Transactional;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class RegistrationController {

    private final OwnerCurator ownerCurator;
    private final ConsumerCurator consumerCurator;
    private final ConsumerTypeCurator consumerTypeCurator;
    private final IdentityCertServiceAdapter identityCertService;
    private final SubscriptionController subscriptionController;
    private final ProfileController profileController;
    private final EventFactory eventFactory;
    private final EventSink sink;

    @Inject
    public RegistrationController(OwnerCurator ownerCurator, ConsumerCurator consumerCurator,
        ConsumerTypeCurator consumerTypeCurator, IdentityCertServiceAdapter identityCertService,
        SubscriptionController subscriptionController, ProfileController profileController,
        EventFactory eventFactory, EventSink sink) {

        this.ownerCurator = ownerCurator;
        this.consumerCurator = consumerCurator;
        this.consumerTypeCurator = consumerTypeCurator;
        this.identityCertService = identityCertService;
        this.subscriptionController = subscriptionController;
        this.profileController = profileController;
        this.eventFactory = eventFactory;
        this.sink = sink;
    }

    @Transactional
    public Consumer register(String name, String consumerid, List<String> activationkey, boolean autoattach,
        String servicelevel, String org, String environment, String release, Map<String, String> facts,
        List<ConsumerInstalledProduct> consumerInstalledProducts, List<String> installedTags,
        List<String> addons, String role, String usage)
        throws GeneralSecurityException, IOException {

        Owner owner;
        if (org == null) {
            List<Owner> owners = ownerCurator.listAll().list();
            if (owners.size() != 1) {
                throw new RuntimeException("Too few/too many owners (hint: specify with --org?)");
            }
            owner = owners.get(0);
        }
        else {
            owner = ownerCurator.getByKey(org);
        }

        // TODO activation keys
        // TODO fix for duplicate hypervisor/consumer problem
        if (!activationkey.isEmpty()) {
            throw new UnsupportedOperationException("Not yet implemented");
        }

        Consumer consumer = new Consumer();
        //consumer.setId(UUID.randomUUID().toString());
        ConsumerType ctype = consumerTypeCurator.getByLabel("system");
        consumer.setType(ctype);
        consumer.setUuid(consumerid);
        consumer.setName(name);
        consumer.setOwner(owner);
        consumer.setEnvironmentId(environment);
        consumerCurator.create(consumer);
        profileController.applyProfile(consumer, facts, consumerInstalledProducts, installedTags,
            release, addons, role, servicelevel, usage);
        IdentityCertificate identityCertificate = identityCertService.generateIdentityCert(consumer);
        consumer.setIdCert(identityCertificate);
        consumerCurator.create(consumer);
        if (autoattach) {
            subscriptionController.autoattach(consumer, owner);
        }
        return consumer;
    }

    @Transactional
    public Consumer regenerateIdentity(String consumerId) throws GeneralSecurityException, IOException {
        Consumer consumer = consumerCurator.getConsumer(consumerId);

        EventBuilder eventBuilder = eventFactory
            .getEventBuilder(Event.Target.CONSUMER, Event.Type.MODIFIED)
            .setEventData(consumer);

        IdentityCertificate cert = identityCertService.generateIdentityCert(consumer);
        consumer.setIdCert(cert);
        consumerCurator.update(consumer);
        sink.queueEvent(eventBuilder.setEventData(consumer).buildEvent());
        return consumer;
    }

    @Transactional
    public void unregister(String consumerId) {
        Consumer consumer = consumerCurator.getConsumer(consumerId);
        consumerCurator.delete(consumer);
    }
}
