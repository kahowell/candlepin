package org.candlepin.controller;

import org.candlepin.model.Consumer;

import com.google.inject.Inject;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class EnvironmentController {

    private final PoolManager poolManager;

    @Inject
    public EnvironmentController(PoolManager poolManager) {
        this.poolManager = poolManager;
    }

    public List<String> getSlas(Consumer consumer) {
        boolean exempt = false; // TODO parameter?
        Set<String> slas = poolManager.retrieveServiceLevelsForOwner(consumer.getOwner().getId(), exempt);
        return new ArrayList<>(slas);
    }
}
