/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2017-2017 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2017 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.web.rest.support;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import org.opennms.web.utils.ServiceTemporarilyUnavailableException;

/**
 * Maps an Exception of type {@link ServiceTemporarilyUnavailableException} to a Response HTTP Status of 503 (service unavailable).
 */
public class ServiceTemporarilyUnavailableProvider implements ExceptionMapper<ServiceTemporarilyUnavailableException> {

    @Override
    public Response toResponse(ServiceTemporarilyUnavailableException exception) {
        if (exception.getUnavilableService() != null) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity("No service for type " + exception.getUnavilableService() + " registered to handle your query. This is a temporary issue. Please try again later")
                    .build();
        }
        return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                .entity("No service registered to handle your query. This is a temporary issue. Please try again later.")
                .build();
    }
}