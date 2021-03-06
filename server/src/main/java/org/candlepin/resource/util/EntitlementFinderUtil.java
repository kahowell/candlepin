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
package org.candlepin.resource.util;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.candlepin.model.EntitlementFilterBuilder;
import org.candlepin.resteasy.parameter.KeyValueParameter;

/**
 * EntitlementFinderUtil
 */
public class EntitlementFinderUtil {

    private EntitlementFinderUtil() {
    }

    public static EntitlementFilterBuilder createFilter(String matches, List<KeyValueParameter> attrFilters) {
        EntitlementFilterBuilder filters = new EntitlementFilterBuilder();
        if (attrFilters != null) {
            for (KeyValueParameter filterParam : attrFilters) {
                filters.addAttributeFilter(filterParam.getKey(), filterParam.getValue());
            }
        }
        if (!StringUtils.isEmpty(matches)) {
            filters.addMatchesFilter(matches);
        }
        return filters;
    }

}
