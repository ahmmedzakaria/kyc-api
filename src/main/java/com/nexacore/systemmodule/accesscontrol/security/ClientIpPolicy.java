package com.nexacore.systemmodule.accesscontrol.security;

import com.nexacore.systemmodule.accesscontrol.config.AccessControlProperties;
import com.nexacore.systemmodule.accesscontrol.entity.SysPrivClientApplication;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class ClientIpPolicy {
    private final AccessControlProperties properties;

    public boolean isAllowed(SysPrivClientApplication application, HttpServletRequest request) {
        if (application == null || !StringUtils.hasText(application.getAllowedIps())) {
            return true;
        }
        try {
            InetAddress clientAddress = resolveClientAddress(request);
            return parseNetworks(application.getAllowedIps()).stream()
                    .anyMatch(network -> network.contains(clientAddress));
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    public String normalizeConfiguredIps(String configuredIps) {
        if (!StringUtils.hasText(configuredIps)) return null;
        Set<String> normalized = new LinkedHashSet<>();
        for (IpNetwork network : parseNetworks(configuredIps)) normalized.add(network.canonical());
        return normalized.isEmpty() ? null : String.join(",", normalized);
    }

    InetAddress resolveClientAddress(HttpServletRequest request) {
        InetAddress peer = parseAddress(request.getRemoteAddr());
        List<IpNetwork> trustedProxies = parseNetworks(properties.getTrustedProxyCidrs());
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (!isInAny(peer, trustedProxies) || !StringUtils.hasText(forwardedFor)) return peer;

        List<InetAddress> chain = new ArrayList<>();
        for (String value : forwardedFor.split(",")) chain.add(parseAddress(value.trim()));
        for (int index = chain.size() - 1; index >= 0; index--) {
            InetAddress candidate = chain.get(index);
            if (!isInAny(candidate, trustedProxies)) return candidate;
        }
        return chain.getFirst();
    }

    private boolean isInAny(InetAddress address, List<IpNetwork> networks) {
        return networks.stream().anyMatch(network -> network.contains(address));
    }

    private List<IpNetwork> parseNetworks(String configured) {
        if (!StringUtils.hasText(configured)) return List.of();
        List<IpNetwork> networks = new ArrayList<>();
        for (String value : configured.split(",")) {
            if (StringUtils.hasText(value)) networks.add(IpNetwork.parse(value.trim()));
        }
        return networks;
    }

    private static InetAddress parseAddress(String value) {
        if (!StringUtils.hasText(value) || value.contains("%") || value.contains("/")) {
            throw new IllegalArgumentException("Invalid IP address");
        }
        String candidate = value.trim();
        if (candidate.startsWith("[") && candidate.endsWith("]")) {
            candidate = candidate.substring(1, candidate.length() - 1);
        }
        if (candidate.contains(".")) validateIpv4(candidate);
        else if (!candidate.contains(":")) throw new IllegalArgumentException("Hostnames are not IP addresses");
        try {
            return InetAddress.getByName(candidate);
        } catch (UnknownHostException ex) {
            throw new IllegalArgumentException("Invalid IP address", ex);
        }
    }

    private static void validateIpv4(String address) {
        String[] octets = address.split("\\.", -1);
        if (octets.length != 4) throw new IllegalArgumentException("Invalid IPv4 address");
        for (String octet : octets) {
            if (!octet.matches("[0-9]{1,3}") || Integer.parseInt(octet) > 255) {
                throw new IllegalArgumentException("Invalid IPv4 address");
            }
        }
    }

    private record IpNetwork(byte[] network, int prefix, boolean explicitPrefix) {
        static IpNetwork parse(String value) {
            String[] parts = value.split("/", -1);
            if (parts.length > 2) throw new IllegalArgumentException("Invalid CIDR");
            InetAddress address = parseAddress(parts[0]);
            int bits = address.getAddress().length * 8;
            int prefix = parts.length == 1 ? bits : parsePrefix(parts[1], bits);
            byte[] network = address.getAddress().clone();
            applyMask(network, prefix);
            return new IpNetwork(network, prefix, parts.length == 2);
        }

        boolean contains(InetAddress candidate) {
            byte[] bytes = candidate.getAddress().clone();
            if (bytes.length != network.length) return false;
            applyMask(bytes, prefix);
            return java.util.Arrays.equals(network, bytes);
        }

        String canonical() {
            try {
                String address = InetAddress.getByAddress(network).getHostAddress();
                return explicitPrefix ? address + "/" + prefix : address;
            } catch (UnknownHostException impossible) {
                throw new IllegalStateException(impossible);
            }
        }

        private static int parsePrefix(String value, int bits) {
            try {
                int prefix = Integer.parseInt(value);
                if (prefix < 0 || prefix > bits) throw new IllegalArgumentException("Invalid CIDR prefix");
                return prefix;
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Invalid CIDR prefix", ex);
            }
        }

        private static void applyMask(byte[] bytes, int prefix) {
            for (int bit = prefix; bit < bytes.length * 8; bit++) {
                bytes[bit / 8] &= (byte) ~(1 << (7 - bit % 8));
            }
        }
    }
}
