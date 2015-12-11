/*******************************************************************************
 * This file is part of OpenNMS(R).
 * <p>
 * Copyright (C) 2015 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2015 The OpenNMS Group, Inc.
 * <p>
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 * <p>
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 * <p>
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * <p>
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 * http://www.gnu.org/licenses/
 * <p>
 * For more information contact:
 * OpenNMS(R) Licensing <license@opennms.org>
 * http://www.opennms.org/
 * http://www.opennms.com/
 *******************************************************************************/

package org.opennms.netmgt.bsm.service.model;

import java.text.ParseException;
import java.util.Arrays;
import java.util.Collection;

import org.junit.runners.Parameterized;
import org.opennms.core.test.xml.XmlTestNoCastor;
import org.opennms.web.rest.api.model.ApiVersion;
import org.opennms.web.rest.api.model.ResourceLocation;

public class IpServiceDTOJaxbTest extends XmlTestNoCastor<IpServiceDTO> {

    public IpServiceDTOJaxbTest(IpServiceDTO sampleObject, Object sampleXml, String schemaFile) {
        super(sampleObject, sampleXml, schemaFile);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() throws ParseException {
        IpServiceDTO ipService = new IpServiceDTO();
        ipService.setId("1");
        ipService.setServiceName("my service name");
        ipService.setNodeLabel("my node label");
        ipService.setIpAddress("127.0.0.1");
        ipService.setLocation(new ResourceLocation(ApiVersion.Version1, "ifservices", "1"));

        return Arrays.asList(new Object[][]{{
                ipService,
                "<ip-service>\n" +
                "   <id>1</id>\n" +
                "   <location>/rest/ifservices/1</location>\n" +
                "   <serviceName>my service name</serviceName>\n" +
                "   <nodeLabel>my node label</nodeLabel>\n" +
                "   <ipAddress>127.0.0.1</ipAddress>\n" +
                "</ip-service>",
                null
        }});
    }
}