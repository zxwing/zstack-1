package org.zstack.kvm;

import org.zstack.core.logging.LogLabel;

/**
 * Created by xing5 on 2016/6/3.
 */
public class KVMHostLabel {
    @LogLabel(messages = {
            "en_US = ping following DNS names[{0}] to make sure the network connection is working",
            "zh_CN = ping下列DNS域名：{0}，以确保物理机网络可以连通外部网络"
    })
    public static final String ADD_HOST_CHECK_DNS = "add.host.kvm.checkDNS";

    @LogLabel(messages = {
            "en_US = check if the host can reach the management node's IP",
            "zh_CN = 检查物理机是否可以访问管理节点IP地址"
    })
    public static final String ADD_HOST_CHECK_PING_MGMT_NODE = "add.host.kvm.pingManagementNode";

    @LogLabel(messages = {
            "en_US = call Ansible to prepare environment on the host, it may take a few minutes ...",
            "zh_CN = 调用Ansible安装物理机环境，这可能需要花费数分钟时间"
    })
    public static final String CALL_ANSIBLE = "add.host.kvm.callAnsible";

    @LogLabel(messages = {
            "en_US = the KVM agent is installed, now wait for it connecting",
            "zh_CN = KVM代理安装完成，正在连接代理"
    })
    public static final String ECHO_AGENT = "add.host.kvm.echoAgent";

    @LogLabel(messages = {
            "en_US = collect libraries(e.g. libvirt, qemu) information from the host",
            "zh_CN = 收集物理机系统中依赖包（例如Libvrit，QEMU）信息"
    })
    public static final String COLLECT_HOST_FACTS = "add.host.kvm.collectFacts";

    @LogLabel(messages = {
            "en_US = configuring iptables on the host",
            "zh_CN = 配置物理机防火墙信息"
    })
    public static final String PREPARE_FIREWALL = "add.host.kvm.prepareFirewall";
}
