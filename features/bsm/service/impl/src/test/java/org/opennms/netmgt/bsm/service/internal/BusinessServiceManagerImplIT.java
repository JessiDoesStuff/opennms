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

package org.opennms.netmgt.bsm.service.internal;

import com.google.common.collect.ImmutableSet;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.opennms.core.spring.BeanUtils;
import org.opennms.core.test.OpenNMSJUnit4ClassRunner;
import org.opennms.core.test.db.annotations.JUnitTemporaryDatabase;
import org.opennms.core.utils.InetAddressUtils;
import org.opennms.netmgt.bsm.persistence.api.BusinessService;
import org.opennms.netmgt.bsm.persistence.api.BusinessServiceDao;
import org.opennms.netmgt.bsm.service.BusinessServiceManager;
import org.opennms.netmgt.bsm.service.BusinessServiceStateMachine;
import org.opennms.netmgt.dao.DatabasePopulator;
import org.opennms.netmgt.dao.api.DistPollerDao;
import org.opennms.netmgt.dao.api.MonitoredServiceDao;
import org.opennms.netmgt.dao.api.NodeDao;
import org.opennms.netmgt.events.api.EventConstants;
import org.opennms.netmgt.model.OnmsAlarm;
import org.opennms.netmgt.model.OnmsMonitoredService;
import org.opennms.netmgt.model.OnmsSeverity;
import org.opennms.test.JUnitConfigurationEnvironment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Lists;

import java.util.Collections;

@RunWith(OpenNMSJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:/META-INF/opennms/applicationContext-commonConfigs.xml",
        "classpath:/META-INF/opennms/applicationContext-minimal-conf.xml",
        "classpath:/META-INF/opennms/applicationContext-soa.xml",
        "classpath:/META-INF/opennms/applicationContext-dao.xml",
        "classpath*:/META-INF/opennms/component-dao.xml",
        "classpath*:/META-INF/opennms/component-service.xml",
        "classpath:/META-INF/opennms/mockEventIpcManager.xml",
        "classpath:/META-INF/opennms/applicationContext-setupIpLike-enabled.xml",
        "classpath:/META-INF/opennms/applicationContext-databasePopulator.xml" })
@JUnitConfigurationEnvironment
@JUnitTemporaryDatabase
@Transactional
public class BusinessServiceManagerImplIT {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Autowired
    private BusinessServiceManager businessServiceManager;

    @Autowired
    private BusinessServiceStateMachine businessServiceStateMachine;

    @Autowired
    private BusinessServiceDao businessServiceDao;

    @Autowired
    private MonitoredServiceDao monitoredServiceDao;

    @Autowired
    private DistPollerDao distPollerDao;

    @Autowired
    private NodeDao nodeDao;

    @Autowired
    private DatabasePopulator populator;

    @Before
    public void before() {
        BeanUtils.assertAutowiring(this);
        populator.populateDatabase();
    }

    @After
    public void after() {
        populator.resetDatabase();
        for (BusinessService eachService : businessServiceDao.findAll()) {
            businessServiceDao.delete(eachService);
        }
    }

    @Test
    public void testGetOperationalStatusForBusinessService() {
        BusinessService service = createService("Dummy Business Service");
        BusinessService service2 = createService("Another Dummy Business Service");
        Long serviceId1 = businessServiceDao.save(service);
        Long serviceId2 = businessServiceDao.save(service2);
        businessServiceStateMachine.setBusinessServices(Lists.newArrayList(service, service2));

        // no ip services attached
        Assert.assertEquals(OnmsSeverity.NORMAL, businessServiceManager.getOperationalStatusForBusinessService(serviceId1));
        Assert.assertEquals(OnmsSeverity.NORMAL, businessServiceManager.getOperationalStatusForBusinessService(serviceId2));

        // ip services attached
        businessServiceManager.assignIpInterface(serviceId1, 5);
        businessServiceManager.assignIpInterface(serviceId1, 6);
        businessServiceStateMachine.setBusinessServices(Lists.newArrayList(service, service2));

        // should not have any effect
        Assert.assertEquals(OnmsSeverity.NORMAL, businessServiceManager.getOperationalStatusForBusinessService(serviceId1));
        Assert.assertEquals(OnmsSeverity.NORMAL, businessServiceManager.getOperationalStatusForBusinessService(serviceId2));

        // attach NORMAL alarm to service 1
        businessServiceStateMachine.handleNewOrUpdatedAlarm(createAlarm(monitoredServiceDao.get(5), OnmsSeverity.NORMAL));
        Assert.assertEquals(OnmsSeverity.NORMAL, businessServiceManager.getOperationalStatusForIPService(5));
        Assert.assertEquals(OnmsSeverity.NORMAL, businessServiceManager.getOperationalStatusForBusinessService(serviceId1));
        Assert.assertEquals(OnmsSeverity.NORMAL, businessServiceManager.getOperationalStatusForBusinessService(serviceId2));

        // attach INDETERMINATE alarm to service 1
        businessServiceStateMachine.handleNewOrUpdatedAlarm(createAlarm(monitoredServiceDao.get(5), OnmsSeverity.INDETERMINATE));
        Assert.assertEquals(OnmsSeverity.NORMAL, businessServiceManager.getOperationalStatusForIPService(5));
        Assert.assertEquals(OnmsSeverity.NORMAL, businessServiceManager.getOperationalStatusForBusinessService(serviceId1));
        Assert.assertEquals(OnmsSeverity.NORMAL, businessServiceManager.getOperationalStatusForBusinessService(serviceId2));

        // attach WARNING alarm to service 1
        businessServiceStateMachine.handleNewOrUpdatedAlarm(createAlarm(monitoredServiceDao.get(5), OnmsSeverity.WARNING));
        Assert.assertEquals(OnmsSeverity.WARNING, businessServiceManager.getOperationalStatusForIPService(5));
        Assert.assertEquals(OnmsSeverity.WARNING, businessServiceManager.getOperationalStatusForBusinessService(serviceId1));
        Assert.assertEquals(OnmsSeverity.NORMAL, businessServiceManager.getOperationalStatusForBusinessService(serviceId2));

        // attach CRITICAL alarm to service 1
        businessServiceStateMachine.handleNewOrUpdatedAlarm(createAlarm(monitoredServiceDao.get(5), OnmsSeverity.CRITICAL));
        Assert.assertEquals(OnmsSeverity.CRITICAL, businessServiceManager.getOperationalStatusForIPService(5));
        Assert.assertEquals(OnmsSeverity.CRITICAL, businessServiceManager.getOperationalStatusForBusinessService(serviceId1));
        Assert.assertEquals(OnmsSeverity.NORMAL, businessServiceManager.getOperationalStatusForBusinessService(serviceId2));
    }

    @Test
    public void testChildMapping() {
        BusinessService service_p_1 = createService("Business Service #p1");
        BusinessService service_p_2 = createService("Business Service #p2");
        BusinessService service_c_1 = createService("Business Service #c1");
        BusinessService service_c_2 = createService("Business Service #c2");

        businessServiceDao.save(service_p_1);
        businessServiceDao.save(service_p_2);
        businessServiceDao.save(service_c_1);
        businessServiceDao.save(service_c_2);

        businessServiceManager.assignChildService(service_p_1.getId(), service_c_1.getId());
        businessServiceManager.assignChildService(service_p_1.getId(), service_c_2.getId());

        businessServiceManager.assignChildService(service_p_2.getId(), service_c_1.getId());
        businessServiceManager.assignChildService(service_p_2.getId(), service_c_2.getId());

        Assert.assertEquals(ImmutableSet.of(service_p_1, service_p_2),
                            service_c_1.getParentServices());

        Assert.assertEquals(ImmutableSet.of(service_p_1, service_p_2),
                            service_c_2.getParentServices());
    }

    @Test
    public void testChildDeletion() {
        BusinessService service_p = createService("Business Service #p");
        BusinessService service_c_1 = createService("Business Service #c1");
        BusinessService service_c_2 = createService("Business Service #c2");

        businessServiceDao.save(service_p);
        businessServiceDao.save(service_c_1);
        businessServiceDao.save(service_c_2);

        businessServiceManager.assignChildService(service_p.getId(), service_c_1.getId());
        businessServiceManager.assignChildService(service_p.getId(), service_c_2.getId());

        businessServiceManager.delete(service_c_1.getId());

        Assert.assertEquals(ImmutableSet.of(service_c_2),
                            service_p.getChildServices());
    }

    @Test
    public void testLoopDetection() {
        BusinessService service1 = createService("Business Service #1");
        BusinessService service2 = createService("Business Service #2");
        BusinessService service3 = createService("Business Service #3");

        Long serviceId1 = businessServiceDao.save(service1);
        Long serviceId2 = businessServiceDao.save(service2);
        Long serviceId3 = businessServiceDao.save(service3);

        Assert.assertEquals(ImmutableSet.of(businessServiceManager.getById(serviceId2),
                                            businessServiceManager.getById(serviceId3)),
                            businessServiceManager.getFeasibleChildServices(businessServiceManager.getById(serviceId1)));
        Assert.assertEquals(ImmutableSet.of(businessServiceManager.getById(serviceId1),
                                            businessServiceManager.getById(serviceId3)),
                            businessServiceManager.getFeasibleChildServices(businessServiceManager.getById(serviceId2)));
        Assert.assertEquals(ImmutableSet.of(businessServiceManager.getById(serviceId1),
                                            businessServiceManager.getById(serviceId2)),
                            businessServiceManager.getFeasibleChildServices(businessServiceManager.getById(serviceId3)));

        businessServiceManager.assignChildService(serviceId1, serviceId2);

        Assert.assertEquals(ImmutableSet.of(businessServiceManager.getById(serviceId2),
                                            businessServiceManager.getById(serviceId3)),
                            businessServiceManager.getFeasibleChildServices(businessServiceManager.getById(serviceId1)));
        Assert.assertEquals(ImmutableSet.of(businessServiceManager.getById(serviceId3)),
                            businessServiceManager.getFeasibleChildServices(businessServiceManager.getById(serviceId2)));
        Assert.assertEquals(ImmutableSet.of(businessServiceManager.getById(serviceId1),
                                            businessServiceManager.getById(serviceId2)),
                            businessServiceManager.getFeasibleChildServices(businessServiceManager.getById(serviceId3)));

        businessServiceManager.assignChildService(serviceId2, serviceId3);

        Assert.assertEquals(ImmutableSet.of(businessServiceManager.getById(serviceId2),
                                            businessServiceManager.getById(serviceId3)),
                            businessServiceManager.getFeasibleChildServices(businessServiceManager.getById(serviceId1)));
        Assert.assertEquals(ImmutableSet.of(businessServiceManager.getById(serviceId3)),
                            businessServiceManager.getFeasibleChildServices(businessServiceManager.getById(serviceId2)));
        Assert.assertEquals(ImmutableSet.of(),
                            businessServiceManager.getFeasibleChildServices(businessServiceManager.getById(serviceId3)));
    }

    @Test
    public void testLoopCreation() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Service will form a loop");

        BusinessService service1 = createService("Business Service #1");
        BusinessService service2 = createService("Business Service #2");
        BusinessService service3 = createService("Business Service #3");

        Long serviceId1 = businessServiceDao.save(service1);
        Long serviceId2 = businessServiceDao.save(service2);
        Long serviceId3 = businessServiceDao.save(service3);

        businessServiceManager.assignChildService(serviceId1, serviceId2);
        businessServiceManager.assignChildService(serviceId2, serviceId3);
        businessServiceManager.assignChildService(serviceId3, serviceId1);
    }

    private BusinessService createService(String serviceName) {
        BusinessService service = new BusinessService();
        service.setName(serviceName);
        return service;
    }

    private OnmsAlarm createAlarm(OnmsMonitoredService monitoredService, OnmsSeverity severity) {
        OnmsAlarm alarm = new OnmsAlarm();
        alarm.setUei(EventConstants.NODE_LOST_SERVICE_EVENT_UEI);
        alarm.setCounter(1);
        alarm.setDistPoller(distPollerDao.whoami());
        alarm.setNode(nodeDao.get(monitoredService.getNodeId()));
        alarm.setServiceType(monitoredService.getServiceType());
        alarm.setIpAddr(monitoredService.getIpAddress());
        alarm.setSeverity(severity);
        alarm.setReductionKey(String.format("%s::%d:%s:%s",
                        EventConstants.NODE_LOST_SERVICE_EVENT_UEI, monitoredService.getNodeId(),
                        InetAddressUtils.toIpAddrString(monitoredService.getIpAddress()),
                        monitoredService.getServiceName()));
        return alarm;
    }
}