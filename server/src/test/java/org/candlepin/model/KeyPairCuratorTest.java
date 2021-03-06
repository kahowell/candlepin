/**
 * Copyright (c) 2009 - 2012 Red Hat, Inc.
 *
 * This software is licensed to you under the GNU General Public License,
 * version 2 (GPLv2). There is NO WARRANTY for this software, express or
 * implied, including the implied warranties of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. You should have received a copy of GPLv2
 * along with this software; if not, see
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt.
 *
 * Red Hat trademarks are not licensed under GPLv2. No permission is
 * granted to use or replicate Red Hat trademarks that are incorporated
 * in this software or its documentation.
 */
package org.candlepin.model;

import static org.junit.jupiter.api.Assertions.*;

import org.candlepin.test.DatabaseTestFixture;

import org.junit.jupiter.api.Test;

import java.security.KeyPair;

import javax.inject.Inject;

/**
 * KeyPairCuratorTest
 */
public class KeyPairCuratorTest extends DatabaseTestFixture {
    @Inject private KeyPairCurator keyPairCurator;

    @Test
    public void testSameConsumerGetsSameKey() {
        Owner owner = createOwner();

        Consumer consumer = createConsumer(owner);

        KeyPair keyPair1 = keyPairCurator.getConsumerKeyPair(consumer);
        KeyPair keyPair2 = keyPairCurator.getConsumerKeyPair(consumer);

        assertEquals(keyPair1.getPrivate(), keyPair2.getPrivate());
    }

    @Test
    public void testTwoConsumersGetDifferentKeys() {
        Owner owner = createOwner();

        Consumer consumer1 = createConsumer(owner);
        Consumer consumer2 = createConsumer(owner);

        KeyPair keyPair1 = keyPairCurator.getConsumerKeyPair(consumer1);
        KeyPair keyPair2 = keyPairCurator.getConsumerKeyPair(consumer2);

        assertFalse(keyPair1.getPrivate().equals(keyPair2.getPrivate()));
    }

}
