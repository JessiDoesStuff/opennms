/*******************************************************************************
 * This file is part of the OpenNMS(R) Application.
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * Copyright (C) 2002-2006, 2008-2009 The OpenNMS Group, Inc.  All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc.:
 *
 *      51 Franklin Street
 *      5th Floor
 *      Boston, MA 02110-1301
 *      USA
 *
 * For more information contact:
 *
 *      OpenNMS Licensing <license@opennms.org>
 *      http://www.opennms.org/
 *      http://www.opennms.com/
 *
 *******************************************************************************/


package org.opennms.netmgt.collectd;

import java.net.InetAddress;
import java.util.Collection;
import org.apache.log4j.Category;
import org.opennms.core.utils.ThreadCategory;
import org.opennms.netmgt.snmp.AggregateTracker;
import org.opennms.netmgt.snmp.SnmpResult;

public class SnmpNodeCollector extends AggregateTracker {
    /**
     * Used to store the collected MIB data.
     */
    private SNMPCollectorEntry m_collectorEntry;

    /**
     * Holds the IP Address of the primary SNMP iterface.
     */
    private String m_primaryIf;

    private SnmpCollectionSet m_collectionSet;

    /**
     * The class constructor is used to initialize the collector and send out
     * the initial SNMP packet requesting data. The data is then received and
     * store by the object. When all the data has been collected the passed
     * signaler object is <EM>notified</EM> using the notifyAll() method.
     * @param address TODO
     * @param objList
     *            The list of object id's to be collected.
     * @param collectionSet TODO
     */
    public SnmpNodeCollector(InetAddress address, Collection<SnmpAttributeType> objList, SnmpCollectionSet collectionSet) {
        super(SnmpAttributeType.getCollectionTrackers(objList));
        
        m_primaryIf = address.getHostAddress();
        m_collectionSet = collectionSet;
        m_collectorEntry = new SNMPCollectorEntry(objList, m_collectionSet);


    }


    protected Category log() {
        return ThreadCategory.getInstance(getClass());
    }

    /**
     * Returns the list of all entry maps that can be used to access all the
     * information from the service polling.
     */

    public SNMPCollectorEntry getEntry() {
        return m_collectorEntry;
    }
    
    protected void reportGenErr(String msg) {
        log().warn("genErr collecting data for node "+m_primaryIf+": "+msg);
    }

    protected void reportNoSuchNameErr(String msg) {
        log().info("noSuchName collecting data for node "+m_primaryIf+": "+msg);
    }

    protected void storeResult(SnmpResult res) {
        m_collectorEntry.storeResult(res);
    }


    public SnmpCollectionSet getCollectionSet() {
        return m_collectionSet;
    }
}
