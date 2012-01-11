package org.openstack.atlas.service.domain.event.entity;

import java.io.Serializable;

public enum AlertStatus implements Serializable {
    ACKNOWLEDGED,
    UNACKNOWLEDGED;
    
    private final static long serialVersionUID = 532512316L;
}                            