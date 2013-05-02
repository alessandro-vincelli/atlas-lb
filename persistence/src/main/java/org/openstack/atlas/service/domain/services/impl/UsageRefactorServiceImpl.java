package org.openstack.atlas.service.domain.services.impl;

import org.openstack.atlas.service.domain.services.UsageRefactorService;
import org.openstack.atlas.service.domain.usage.entities.LoadBalancerHostUsage;
import org.openstack.atlas.service.domain.usage.entities.LoadBalancerMergedHostUsage;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class UsageRefactorServiceImpl extends BaseService implements UsageRefactorService {

    @Override
    public void createUsageEvent(LoadBalancerHostUsage loadBalancerHostUsage) {
        hostUsageRefactorRepository.create(loadBalancerHostUsage);
    }

    @Override
    public LoadBalancerHostUsage getRecentHostUsageRecord(int lbId) {
        return hostUsageRefactorRepository.getMostRecentUsageRecordForLbId(lbId);
    }

    @Override
    public Map<Integer, Map<Integer, List<LoadBalancerHostUsage>>> getAllLoadBalancerHostUsages() {
        List<LoadBalancerHostUsage> lbHostUsages = hostUsageRefactorRepository.getAllLoadBalancerHostUsageRecords();
        Map<Integer, Map<Integer, List<LoadBalancerHostUsage>>> lbMap = new HashMap<Integer, Map<Integer, List<LoadBalancerHostUsage>>>();
        for (LoadBalancerHostUsage lbHostUsage : lbHostUsages) {
            if (!lbMap.containsKey(lbHostUsage.getLoadbalancerId())){
                lbMap.put(lbHostUsage.getLoadbalancerId(), new HashMap<Integer, List<LoadBalancerHostUsage>>());
            }
            if (!lbMap.get(lbHostUsage.getLoadbalancerId()).containsKey(lbHostUsage.getHostId())) {
                lbMap.get(lbHostUsage.getLoadbalancerId()).put(lbHostUsage.getHostId(), new ArrayList<LoadBalancerHostUsage>());
            }
            lbMap.get(lbHostUsage.getLoadbalancerId()).get(lbHostUsage.getHostId()).add(lbHostUsage);
        }
        return lbMap;
    }

    @Override
    public void batchCreateLoadBalancerHostUsages(List<LoadBalancerHostUsage> usages) {
        hostUsageRefactorRepository.batchCreate(usages);
    }

    @Override
    public void deleteOldLoadBalancerHostUsages(Calendar deleteTimeMarker) {
        hostUsageRefactorRepository.deleteOldHostUsage(deleteTimeMarker);
    }

    @Override
    public void batchCreateLoadBalancerMergedHostUsages(List<LoadBalancerMergedHostUsage> usages) {
        loadBalancerMergedHostUsageRepository.batchCreate(usages);
    }

    @Override
    public void batchDeleteLoadBalancerMergedHostUsages(Collection<LoadBalancerMergedHostUsage> usages) {
        loadBalancerMergedHostUsageRepository.batchDelete(usages);
    }
}
