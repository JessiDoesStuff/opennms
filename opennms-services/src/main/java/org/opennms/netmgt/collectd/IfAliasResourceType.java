//
// This file is part of the OpenNMS(R) Application.
//
// OpenNMS(R) is Copyright (C) 2006 The OpenNMS Group, Inc.  All rights reserved.
// OpenNMS(R) is a derivative work, containing both original code, included code and modified
// code that was published under the GNU General Public License. Copyrights for modified 
// and included code are below.
//
// OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
//
// Modifications:
//
// 2006 Aug 15: Use generics for collections.
//
// Original code base Copyright (C) 1999-2001 Oculan Corp.  All rights reserved.
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
//
// For more information contact:
// OpenNMS Licensing       <license@opennms.org>
//     http://www.opennms.org/
//     http://www.opennms.com/
//

package org.opennms.netmgt.collectd;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.opennms.netmgt.snmp.SnmpInstId;

public class IfAliasResourceType extends ResourceType {

    private IfResourceType m_ifResourceType;
    private Map m_aliasedIfs = new HashMap();
    private ServiceParameters m_params;

    public IfAliasResourceType(CollectionAgent agent, OnmsSnmpCollection snmpCollection, ServiceParameters params, IfResourceType ifResourceType) {
        super(agent, snmpCollection);
        m_ifResourceType = ifResourceType;
        m_params = params;
    }

    public CollectionResource findResource(SnmpInstId inst) {
        Integer key = new Integer(inst.toInt());
        AliasedResource resource = (AliasedResource) m_aliasedIfs.get(key);
        if (resource == null) {
            IfInfo ifInfo = (IfInfo)m_ifResourceType.findResource(inst);
            
            if(ifInfo == null) {
            	log().info("Not creating an aliased resource for ifInfo = null");
            } else {
                log().info("Creating an aliased resource for "+ifInfo);
            
                resource = new AliasedResource(this, m_params.getDomain(), ifInfo, m_params.getIfAliasComment());
            
                m_aliasedIfs.put(key, resource);
            }
        }
        return resource;
    }

    public Collection<AttributeType> getAttributeTypes() {
        MibObject ifAliasMibObject = new MibObject();
        ifAliasMibObject.setOid(".1.3.6.1.2.1.31.1.1.1.18");
        ifAliasMibObject.setAlias("ifAlias");
        ifAliasMibObject.setType("string");
        ifAliasMibObject.setInstance("ifIndex");
        
        ifAliasMibObject.setGroupName("aliasedResource");
        ifAliasMibObject.setGroupIfType("all");

        AttributeGroupType groupType = new AttributeGroupType(ifAliasMibObject.getGroupName(), ifAliasMibObject.getGroupIfType());

        AttributeType type = AttributeType.create(this, getCollectionName(), ifAliasMibObject, groupType);
        return Collections.singleton(type);
   }

    public Collection getResources() {
        return m_aliasedIfs.values();
    }
    

}
