package org.zstack.header.vo;

import javax.persistence.*;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ForeignKey.ReferenceOption;

@Entity
@Table
public class ResourceVO {

    @Id
    @Column
    private String uuid;
    
    @Column
    private String name;
    
    public String getUuid() {
        return uuid;
    }
    
    public void setUuid(String v) {
        uuid = v;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String v) {
        name = v;
    }
    
    @Column
    private String portForwardingRuleUuid;

    public String getPortForwardingRuleUuid() {
        return portForwardingRuleUuid;
    }
    
    public void setPortForwardingRuleUuid(String v) {
        portForwardingRuleUuid = v;
    }

    @Column
    private String applianceVmUuid;

    public String getApplianceVmUuid() {
        return applianceVmUuid;
    }
    
    public void setApplianceVmUuid(String v) {
        applianceVmUuid = v;
    }

    @Column
    private String securityGroupUuid;

    public String getSecurityGroupUuid() {
        return securityGroupUuid;
    }
    
    public void setSecurityGroupUuid(String v) {
        securityGroupUuid = v;
    }

    @Column
    private String loadBalancerListenerUuid;

    public String getLoadBalancerListenerUuid() {
        return loadBalancerListenerUuid;
    }
    
    public void setLoadBalancerListenerUuid(String v) {
        loadBalancerListenerUuid = v;
    }

    @Column
    private String imageUuid;

    public String getImageUuid() {
        return imageUuid;
    }
    
    public void setImageUuid(String v) {
        imageUuid = v;
    }

    @Column
    private String vmInstanceUuid;

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }
    
    public void setVmInstanceUuid(String v) {
        vmInstanceUuid = v;
    }

    @Column
    private String diskOfferingUuid;

    public String getDiskOfferingUuid() {
        return diskOfferingUuid;
    }
    
    public void setDiskOfferingUuid(String v) {
        diskOfferingUuid = v;
    }

    @Column
    private String quotaUuid;

    public String getQuotaUuid() {
        return quotaUuid;
    }
    
    public void setQuotaUuid(String v) {
        quotaUuid = v;
    }

    @Column
    private String l3NetworkUuid;

    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }
    
    public void setL3NetworkUuid(String v) {
        l3NetworkUuid = v;
    }

    @Column
    private String vmNicUuid;

    public String getVmNicUuid() {
        return vmNicUuid;
    }
    
    public void setVmNicUuid(String v) {
        vmNicUuid = v;
    }

    @Column
    private String schedulerUuid;

    public String getSchedulerUuid() {
        return schedulerUuid;
    }
    
    public void setSchedulerUuid(String v) {
        schedulerUuid = v;
    }

    @Column
    private String ipRangeUuid;

    public String getIpRangeUuid() {
        return ipRangeUuid;
    }
    
    public void setIpRangeUuid(String v) {
        ipRangeUuid = v;
    }

    @Column
    private String virtualRouterVmUuid;

    public String getVirtualRouterVmUuid() {
        return virtualRouterVmUuid;
    }
    
    public void setVirtualRouterVmUuid(String v) {
        virtualRouterVmUuid = v;
    }

    @Column
    private String policyUuid;

    public String getPolicyUuid() {
        return policyUuid;
    }
    
    public void setPolicyUuid(String v) {
        policyUuid = v;
    }

    @Column
    private String volumeSnapshotUuid;

    public String getVolumeSnapshotUuid() {
        return volumeSnapshotUuid;
    }
    
    public void setVolumeSnapshotUuid(String v) {
        volumeSnapshotUuid = v;
    }

    @Column
    private String userGroupUuid;

    public String getUserGroupUuid() {
        return userGroupUuid;
    }
    
    public void setUserGroupUuid(String v) {
        userGroupUuid = v;
    }

    @Column
    private String vipUuid;

    public String getVipUuid() {
        return vipUuid;
    }
    
    public void setVipUuid(String v) {
        vipUuid = v;
    }

    @Column
    private String userUuid;

    public String getUserUuid() {
        return userUuid;
    }
    
    public void setUserUuid(String v) {
        userUuid = v;
    }

    @Column
    private String eipUuid;

    public String getEipUuid() {
        return eipUuid;
    }
    
    public void setEipUuid(String v) {
        eipUuid = v;
    }

    @Column
    private String volumeUuid;

    public String getVolumeUuid() {
        return volumeUuid;
    }
    
    public void setVolumeUuid(String v) {
        volumeUuid = v;
    }

    @Column
    private String loadBalancerUuid;

    public String getLoadBalancerUuid() {
        return loadBalancerUuid;
    }
    
    public void setLoadBalancerUuid(String v) {
        loadBalancerUuid = v;
    }

    
}
