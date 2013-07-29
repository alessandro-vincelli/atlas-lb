package org.openstack.atlas.adapter.itest;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openstack.atlas.adapter.exceptions.InsufficientRequestException;
import org.openstack.atlas.adapter.helpers.ResourceTranslator;
import org.openstack.atlas.adapter.helpers.StmConstants;
import org.openstack.atlas.adapter.helpers.ZeusNodePriorityContainer;
import org.openstack.atlas.service.domain.entities.*;
import org.openstack.atlas.util.ip.exception.IPStringConversionException;
import org.rackspace.stingray.client.StingrayRestClient;
import org.rackspace.stingray.client.exception.StingrayRestClientException;
import org.rackspace.stingray.client.exception.StingrayRestClientObjectNotFoundException;
import org.rackspace.stingray.client.monitor.Monitor;
import org.rackspace.stingray.client.monitor.MonitorHttp;
import org.rackspace.stingray.client.pool.Pool;
import org.rackspace.stingray.client.pool.PoolNodeWeight;
import org.rackspace.stingray.client.protection.Protection;
import org.rackspace.stingray.client.protection.ProtectionAccessRestriction;
import org.rackspace.stingray.client.protection.ProtectionConnectionLimiting;
import org.rackspace.stingray.client.traffic.ip.TrafficIp;
import org.rackspace.stingray.client.virtualserver.VirtualServer;

import java.io.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.openstack.atlas.service.domain.entities.AccessListType.ALLOW;
import static org.openstack.atlas.service.domain.entities.AccessListType.DENY;
import static org.openstack.atlas.service.domain.entities.SessionPersistence.HTTP_COOKIE;

public class FullConfigIntegrationTest extends STMTestBase {
    Node n;
    Node n2;
    Node n3;

    @BeforeClass
    public static void setupClass() throws InterruptedException {
        Thread.sleep(SLEEP_TIME_BETWEEN_TESTS);
        setupIvars();
//        createSimpleLoadBalancer();
    }

    @AfterClass
    public static void tearDownClass() {
        removeLoadBalancer();
    }

    @Test
    public void createFullyConfiguredLoadBalancer() {
        try {
            stmAdapter.createLoadBalancer(config, buildHydratedLb());
            Thread.sleep(3000);
            StingrayRestClient tclient = new StingrayRestClient();
            verifyVS(tclient, lb);
            verifyPool(tclient, lb);
            verifyMonitor(tclient, lb);
            verifyProtection(tclient, lb);
            verifyVips(tclient, lb);
            removeLoadBalancer();
        } catch (Exception e) {
            e.printStackTrace();
            removeLoadBalancer();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void updateFullyConfiguredLoadBalancer() {
        StingrayRestClient tclient = new StingrayRestClient();

        try {
            Thread.sleep(15000);
            stmAdapter.createLoadBalancer(config, buildHydratedLb());
            Thread.sleep(3000);
            verifyVS(tclient, lb);
            verifyPool(tclient, lb);
            verifyMonitor(tclient, lb);
            verifyProtection(tclient, lb);
            verifyVips(tclient, lb);

            LoadBalancer nlb = new LoadBalancer();
            UserPages up = new UserPages();
            up.setErrorpage("iError");
            nlb.setUserPages(up);
            nlb.setProtocol(LoadBalancerProtocol.FTP);
            HealthMonitor mon = new HealthMonitor();
            mon.setType(HealthMonitorType.HTTP);
            mon.setStatusRegex("202");
            mon.setBodyRegex("");
            mon.setAttemptsBeforeDeactivation(4);
            mon.setDelay(3);
            mon.setPath("/");
            mon.setTimeout(2);
            mon.setHostHeader("");
            nlb.setHealthMonitor(mon);

            buildHydratedLb();
            lb.setUserPages(up);
            lb.setHealthMonitor(mon);

            stmAdapter.updateLoadBalancer(config, buildHydratedLb(), nlb);
            Thread.sleep(3000);
            verifyVS(tclient, lb);
            verifyPool(tclient, lb);
            verifyMonitor(tclient, lb);
            verifyProtection(tclient, lb);
            verifyVips(tclient, lb);

        } catch (Exception e) {
            e.printStackTrace();
            removeLoadBalancer();
            Assert.fail(e.getMessage());
        }
    }

    private LoadBalancer buildHydratedLb() {
        lb.setAlgorithm(LoadBalancerAlgorithm.WEIGHTED_ROUND_ROBIN);
        lb.setSessionPersistence(HTTP_COOKIE);
        lb.setTimeout(99);
        lb.setConnectionLogging(true);
        lb.setSessionPersistence(SessionPersistence.HTTP_COOKIE);

        HealthMonitor monitor = new HealthMonitor();
        monitor.setType(HealthMonitorType.CONNECT);
        monitor.setDelay(10);
        monitor.setTimeout(20);
        monitor.setAttemptsBeforeDeactivation(3);
        lb.setHealthMonitor(monitor);

        ConnectionLimit limit = new ConnectionLimit();
        limit.setMaxConnections(50);
        limit.setRateInterval(10);
        limit.setMaxConnectionRate(10);
        limit.setMinConnections(1);
        lb.setConnectionLimit(limit);

        Set<AccessList> networkItems = new HashSet<AccessList>();
        AccessList item1 = new AccessList();
        AccessList item2 = new AccessList();
        item1.setIpAddress("0.0.0.0/0");
        item2.setIpAddress("127.0.0.1");
        item1.setType(DENY);
        item2.setType(ALLOW);
        networkItems.add(item1);
        networkItems.add(item2);

        lb.setAccessLists(networkItems);

        n = new Node();
        n.setWeight(1);
        n.setType(NodeType.SECONDARY);
        n.setIpAddress("10.3.3.3");
        n.setCondition(NodeCondition.ENABLED);
        n.setPort(8080);
        n.setStatus(NodeStatus.ONLINE);

        n2 = new Node();
        n2.setWeight(3);
        n2.setType(NodeType.SECONDARY);
        n2.setIpAddress("10.3.3.5");
        n2.setCondition(NodeCondition.DISABLED);
        n2.setPort(7070);
        n2.setStatus(NodeStatus.OFFLINE);

        n3 = new Node();
        n3.setWeight(3);
        n3.setType(NodeType.SECONDARY);
        n3.setIpAddress("10.3.3.9");
        n3.setCondition(NodeCondition.DRAINING);
        n3.setPort(9090);
        n3.setStatus(NodeStatus.OFFLINE);

        lb.getNodes().add(n);
        lb.getNodes().add(n2);
        lb.getNodes().add(n3);

        return lb;
    }

    private VirtualServer verifyVS(StingrayRestClient client, LoadBalancer lb) throws InsufficientRequestException, StingrayRestClientException, StingrayRestClientObjectNotFoundException, IOException {
        VirtualServer vs = client.getVirtualServer(loadBalancerName());
        ResourceTranslator translator = new ResourceTranslator();

        Assert.assertNotNull(vs);
        Assert.assertEquals(true, vs.getProperties().getBasic().getEnabled());
        Assert.assertEquals(lb.getPort(), vs.getProperties().getBasic().getPort());
        Assert.assertEquals(poolName(), vs.getProperties().getBasic().getPool());

        if (lb.getUserPages() != null) {
            Assert.assertEquals(errorFileName(), vs.getProperties().getConnection_errors().getError_file());
            File ef = client.getExtraFile(errorFileName());
            BufferedReader br = new BufferedReader(new FileReader(ef));

            Assert.assertEquals(lb.getUserPages().getErrorpage(), br.readLine());
            ef.delete();
        } else {
            Assert.assertEquals("Default", vs.getProperties().getConnection_errors().getError_file());
        }

        if (lb.getProtocol() == LoadBalancerProtocol.HTTP) {
            Assert.assertTrue(vs.getProperties().getBasic().getRequest_rules().contains(StmConstants.XFF));
            Assert.assertTrue(vs.getProperties().getBasic().getRequest_rules().contains(StmConstants.XFP));
            Assert.assertTrue(vs.getProperties().getBasic().getRequest_rules().contains(StmConstants.RATE_LIMIT_HTTP));
        } else {
            Assert.assertFalse(vs.getProperties().getBasic().getRequest_rules().contains(StmConstants.XFF));
            Assert.assertFalse(vs.getProperties().getBasic().getRequest_rules().contains(StmConstants.XFP));
            Assert.assertTrue(vs.getProperties().getBasic().getRequest_rules().contains(StmConstants.RATE_LIMIT_NON_HTTP));
        }

        Assert.assertEquals(false, vs.getProperties().getBasic().getListen_on_any());
        Assert.assertEquals(false, vs.getProperties().getTcp().getProxy_close());
        Assert.assertEquals(vs.getProperties().getBasic().getListen_on_traffic_ips(), translator.genGroupNameSet(lb));

        Assert.assertEquals(protectionClassName(), vs.getProperties().getBasic().getProtection_class());


        return vs;
    }

    private Pool verifyPool(StingrayRestClient client, LoadBalancer lb) throws InsufficientRequestException, StingrayRestClientException, StingrayRestClientObjectNotFoundException {
        Pool pool = client.getPool(loadBalancerName());
        Assert.assertNotNull(pool);
        Assert.assertEquals(1, pool.getProperties().getBasic().getMonitors().size());
        Assert.assertEquals(lb.getAlgorithm().name().toLowerCase(), pool.getProperties().getLoad_balancing().getAlgorithm());
        Assert.assertEquals(3, pool.getProperties().getBasic().getNodes().size());
        Assert.assertEquals(2, pool.getProperties().getBasic().getDisabled().size());
        Assert.assertEquals(1, pool.getProperties().getBasic().getDraining().size());

        List<PoolNodeWeight> pws = pool.getProperties().getLoad_balancing().getNode_weighting();
        for (PoolNodeWeight pnw : pws) {
            Node lbn = lb.getNodes().iterator().next();
            if (lbn.getIpAddress().equals(pnw.getNode().split(":")[0])) {
                Assert.assertEquals(lbn.getIpAddress() + ":" + lbn.getPort(), pnw.getNode());
                Assert.assertEquals(lbn.getWeight(), pnw.getWeight());
            }
        }

        ZeusNodePriorityContainer znpc = new ZeusNodePriorityContainer(lb.getNodes());
        Assert.assertEquals(znpc.getPriorityValuesSet(), pool.getProperties().getLoad_balancing().getPriority_values());
        Assert.assertEquals(znpc.hasSecondary(), pool.getProperties().getLoad_balancing().getPriority_enabled());

        Assert.assertEquals(lb.getTimeout(), pool.getProperties().getConnection().getMax_reply_time());

        Assert.assertEquals(new HashSet<String>(Arrays.asList(loadBalancerName())), pool.getProperties().getBasic().getMonitors());

        Assert.assertEquals(lb.getSessionPersistence().name(), pool.getProperties().getBasic().getPersistence_class());
        Assert.assertEquals("", pool.getProperties().getBasic().getBandwidth_class());

        return pool;
    }

    private Monitor verifyMonitor(StingrayRestClient client, LoadBalancer lb) throws InsufficientRequestException, StingrayRestClientException, StingrayRestClientObjectNotFoundException {
        Monitor monitor = client.getMonitor(loadBalancerName());
        Assert.assertEquals(lb.getHealthMonitor().getType().name(), monitor.getProperties().getBasic().getType().toUpperCase());
        Assert.assertEquals(lb.getHealthMonitor().getTimeout(), monitor.getProperties().getBasic().getTimeout());
        Assert.assertEquals(lb.getHealthMonitor().getAttemptsBeforeDeactivation(), monitor.getProperties().getBasic().getFailures());

        if (lb.getHealthMonitor().getType().equals(HealthMonitorType.HTTP) || lb.getHealthMonitor().equals(HealthMonitorType.HTTPS)) {
            MonitorHttp http = new MonitorHttp();
            HealthMonitor hm = lb.getHealthMonitor();
            Assert.assertEquals(hm.getPath(), http.getPath());
            Assert.assertEquals(hm.getStatusRegex(), http.getStatus_regex());
            Assert.assertEquals(hm.getBodyRegex(), http.getBody_regex());
            Assert.assertEquals(hm.getHostHeader(), http.getHost_header());
        }

        return monitor;
    }

    private Protection verifyProtection(StingrayRestClient client, LoadBalancer lb) throws InsufficientRequestException, StingrayRestClientException, StingrayRestClientObjectNotFoundException {
        Protection protection = client.getProtection(loadBalancerName());
        if (lb.getAccessLists() != null && !lb.getAccessLists().isEmpty()) {
            ProtectionAccessRestriction pal = protection.getProperties().getAccess_restriction();
            for (AccessList al : lb.getAccessLists()) {
                if (al.getType().equals(AccessListType.ALLOW)) {
                    Assert.assertTrue(pal.getAllowed().contains(al.getIpAddress()));
                } else {
                    Assert.assertTrue(pal.getBanned().contains(al.getIpAddress()));
                }
            }
        }

        if (lb.getConnectionLimit() != null) {
            ProtectionConnectionLimiting cl = protection.getProperties().getConnection_limiting();
            Assert.assertEquals(lb.getConnectionLimit().getMinConnections(), cl.getMin_connections());
            Assert.assertEquals(lb.getConnectionLimit().getMaxConnectionRate(), cl.getMax_connection_rate());
            Assert.assertEquals(lb.getConnectionLimit().getMaxConnections(), cl.getMax_1_connections());
            Assert.assertEquals(lb.getConnectionLimit().getRateInterval(), cl.getRate_timer());
        }

        return protection;
    }

    private void verifyVips(StingrayRestClient client, LoadBalancer lb) throws InsufficientRequestException, StingrayRestClientException, StingrayRestClientObjectNotFoundException, IPStringConversionException {
        TrafficIp t;
        for (LoadBalancerJoinVip jv : lb.getLoadBalancerJoinVipSet()) {
            t = client.getTrafficIp(trafficIpGroupName(jv.getVirtualIp()));
            Assert.assertTrue(t.getProperties().getBasic().getIpaddresses().contains(jv.getVirtualIp().getIpAddress()));
            Assert.assertEquals(true, t.getProperties().getBasic().getEnabled());

            Assert.assertTrue(t.getProperties().getBasic().getMachines().contains(config.getFailoverTrafficManagerNames().iterator().next()));
            Assert.assertTrue(t.getProperties().getBasic().getMachines().contains(config.getTrafficManagerName()));
        }

        for (LoadBalancerJoinVip6 jv : lb.getLoadBalancerJoinVip6Set()) {
            t = client.getTrafficIp(trafficIpGroupName(jv.getVirtualIp()));
            Assert.assertTrue(t.getProperties().getBasic().getIpaddresses().contains(jv.getVirtualIp().getDerivedIpString()));
            Assert.assertEquals(true, t.getProperties().getBasic().getEnabled());

            Assert.assertTrue(t.getProperties().getBasic().getMachines().contains(config.getFailoverTrafficManagerNames().iterator().next()));
            Assert.assertTrue(t.getProperties().getBasic().getMachines().contains(config.getTrafficManagerName()));
        }

    }
}
