package org.openstack.atlas.service.domain.pojo;

import org.openstack.atlas.service.domain.entity.LoadBalancerJoinVip;
import org.openstack.atlas.service.domain.entity.LoadBalancerJoinVip6;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public final class VirtualIpDozerWrapper implements Serializable {
    private final static long serialVersionUID = 532512316L;

    private Set<LoadBalancerJoinVip> loadBalancerJoinVipSet = new HashSet<LoadBalancerJoinVip>();
    private Set<LoadBalancerJoinVip6> loadBalancerJoinVip6Set = new HashSet<LoadBalancerJoinVip6>();

    public VirtualIpDozerWrapper() {
    }

    public VirtualIpDozerWrapper(Set<LoadBalancerJoinVip> loadBalancerJoinVipSet, Set<LoadBalancerJoinVip6> loadBalancerJoinVip6Set) {
        this.loadBalancerJoinVipSet = loadBalancerJoinVipSet;
        this.loadBalancerJoinVip6Set = loadBalancerJoinVip6Set;
    }

    public Set<LoadBalancerJoinVip> getLoadBalancerJoinVipSet() {
        return loadBalancerJoinVipSet;
    }

/*    public Set<LoadBalancerJoinVip> getLoadBalancerJoinVipSet() {
        Set<LoadBalancerJoinVip> returnSet = new HashSet<LoadBalancerJoinVip>();

        for (LoadBalancerJoinVip loadBalancerJoinVip : loadBalancerJoinVipSet) {
            returnSet.add((LoadBalancerJoinVip) loadBalancerJoinVip);
        }

        return returnSet;
    }*/

    public void setLoadBalancerJoinVipSet(Set<LoadBalancerJoinVip> loadBalancerJoinVipSet) {
        this.loadBalancerJoinVipSet = loadBalancerJoinVipSet;
    }

/*    public void setLoadBalancerJoinVipSet(Set<LoadBalancerJoinVip> loadBalancerJoinVipSet) {
        this.loadBalancerJoinVipSet = new HashSet<LoadBalancerJoinVip>(loadBalancerJoinVipSet);
    }*/

    public Set<LoadBalancerJoinVip6> getLoadBalancerJoinVip6Set() {
        return loadBalancerJoinVip6Set;
    }

/*    public Set<LoadBalancerJoinVip6> getLoadBalancerJoinVip6Set() {
        Set<LoadBalancerJoinVip6> returnSet = new HashSet<LoadBalancerJoinVip6>();

        for (LoadBalancerJoinVip6 loadBalancerJoinVip6 : loadBalancerJoinVip6Set) {
            returnSet.add((LoadBalancerJoinVip6) loadBalancerJoinVip6);
        }

        return returnSet;
    }*/

    public void setLoadBalancerJoinVip6Set(Set<LoadBalancerJoinVip6> loadBalancerJoinVip6Set) {
        this.loadBalancerJoinVip6Set = loadBalancerJoinVip6Set;
    }

/*    public void setLoadBalancerJoinVip6Set(Set<LoadBalancerJoinVip6> loadBalancerJoinVip6Set) {
        this.loadBalancerJoinVip6Set = new HashSet<LoadBalancerJoinVip6>(loadBalancerJoinVip6Set);
    }*/
}