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

package org.opennms.web.rest.v2.topology;

import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.opennms.features.topology.api.TopologyService;
import org.opennms.features.topology.api.topo.GraphProvider;
import org.opennms.web.rest.v2.topology.model.GraphDTO;
import org.opennms.web.rest.v2.topology.model.MetaTopologyDTO;
import org.opennms.web.utils.ServiceLocator;
import org.opennms.web.utils.ServiceTemporarilyUnavailableException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("topologyRestService")
@Path("topologies")
@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
public class TopologyRestService {


    @Autowired
    private ServiceLocator serviceLocator;

    @GET
    public List<MetaTopologyDTO> getMetaTopologies() throws ServiceTemporarilyUnavailableException {
        List<MetaTopologyDTO> metaTopologyList = getTopologyService().getMetaTopologyProviders().stream().map(metaTopologyProvider -> {
            MetaTopologyDTO dto = new MetaTopologyDTO();
            dto.setBreadcrumbStrategy(metaTopologyProvider.getBreadcrumbStrategy().name());
            dto.setDefaultNamespace(metaTopologyProvider.getDefaultGraphProvider().getNamespace());
            dto.setId(metaTopologyProvider.getId());
            dto.setNamespaces(metaTopologyProvider.getGraphProviders().stream().map(p -> p.getNamespace()).collect(Collectors.toList()));
            return dto;
        }).collect(Collectors.toList());
        return metaTopologyList;
    }

    @GET
    @Path("{metaTopologyId}/{namespace}")
    public GraphDTO getGraph(@PathParam("metaTopologyId") String metaTopologyId,
                             @PathParam("namespace") String namespace) throws ServiceTemporarilyUnavailableException {
        GraphProvider graphProvider = getTopologyService().getGraphProvider(metaTopologyId, namespace);
        serviceLocator.findService(GraphConverterRepository).getConverter(metaTopologyId, namespace).convert(graphProvider);


    }

    private TopologyService getTopologyService() throws ServiceTemporarilyUnavailableException {
        return serviceLocator.findService(TopologyService.class);
    }
}
