package balbucio.browser4j.network.proxy.pool;

import balbucio.browser4j.network.proxy.ProxyConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class ProxyPool {
    private final List<ProxyNode> nodes;
    private final Map<String, ProxyNode> stickyAssignments;
    private final AtomicInteger roundRobinIndex;

    private ProxyPool() {
        this.nodes = new ArrayList<>();
        this.stickyAssignments = new ConcurrentHashMap<>();
        this.roundRobinIndex = new AtomicInteger(0);
    }

    public static ProxyPool create() {
        return new ProxyPool();
    }

    public synchronized void add(ProxyConfig proxyConfig) {
        if (proxyConfig != null) {
            this.nodes.add(new ProxyNode(proxyConfig));
        }
    }

    public synchronized ProxyConfig next() {
        List<ProxyNode> activeNodes = nodes.stream()
                .filter(n -> n.getStatus() == ProxyStatus.ACTIVE)
                .collect(Collectors.toList());

        if (activeNodes.isEmpty()) {
            return null; // Fallback ou Excepção
        }

        int index = roundRobinIndex.getAndIncrement() % activeNodes.size();
        return activeNodes.get(index).getConfig();
    }

    public synchronized ProxyConfig assignToSession(String sessionId) {
        if (stickyAssignments.containsKey(sessionId)) {
            ProxyNode existing = stickyAssignments.get(sessionId);
            if (existing.getStatus() == ProxyStatus.ACTIVE) {
                return existing.getConfig();
            }
        }

        ProxyConfig newAssigned = next();
        if (newAssigned != null) {
            nodes.stream()
                    .filter(n -> n.getConfig().equals(newAssigned))
                    .findFirst()
                    .ifPresent(node -> stickyAssignments.put(sessionId, node));
        }

        return newAssigned;
    }

    public synchronized void reportFailure(ProxyConfig config) {
        nodes.stream()
                .filter(n -> n.getConfig().equals(config))
                .forEach(ProxyNode::fail);
    }
}
