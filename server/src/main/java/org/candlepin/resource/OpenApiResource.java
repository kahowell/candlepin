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

import org.candlepin.api.v3.resources.OpenapiJsonApi;
import org.candlepin.common.auth.SecurityHole;

import com.google.common.io.Resources;

import java.io.IOException;
import java.nio.charset.StandardCharsets;


public class OpenApiResource implements OpenapiJsonApi {

    @Override
    @SecurityHole(noAuth = true)
    public String getOpenApiJson() {
        try {
            return Resources.toString(Resources.getResource("openapi.json"), StandardCharsets.UTF_8);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
