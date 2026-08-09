package com.nexacore.systemmodule.accesscontrol.security;

import com.nexacore.systemmodule.accesscontrol.config.AccessControlProperties;
import com.nexacore.systemmodule.accesscontrol.entity.SysAccClientApplication;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class ClientIpPolicyTest {

    private final AccessControlProperties properties = new AccessControlProperties(new MockEnvironment());
    private final ClientIpPolicy policy = new ClientIpPolicy(properties);

    @Test
    void normalizesIpv4AndIpv6Networks() {
        assertThat(policy.normalizeConfiguredIps(
                "192.168.10.25/24,192.168.10.0/24,2001:db8::12/64,127.0.0.1"))
                .isEqualTo("192.168.10.0/24,2001:db8:0:0:0:0:0:0/64,127.0.0.1");
        assertThatIllegalArgumentException().isThrownBy(() -> policy.normalizeConfiguredIps("example.com"));
        assertThatIllegalArgumentException().isThrownBy(() -> policy.normalizeConfiguredIps("10.0.0.0/33"));
    }

    @Test
    void matchesExactAddressesAndCidrs() {
        SysAccClientApplication client = client("10.20.0.0/16,2001:db8::/32");

        assertThat(policy.isAllowed(client, request("10.20.4.8", null))).isTrue();
        assertThat(policy.isAllowed(client, request("10.21.4.8", null))).isFalse();
        assertThat(policy.isAllowed(client, request("2001:db8::5", null))).isTrue();
    }

    @Test
    void ignoresSpoofedForwardedHeaderFromUntrustedPeer() {
        properties.setTrustedProxyCidrs("10.0.0.0/8");
        SysAccClientApplication client = client("198.51.100.8");

        assertThat(policy.isAllowed(client, request("203.0.113.9", "198.51.100.8"))).isFalse();
    }

    @Test
    void resolvesFirstUntrustedAddressBehindTrustedProxyChain() {
        properties.setTrustedProxyCidrs("10.0.0.0/8,192.168.0.0/16");
        SysAccClientApplication client = client("198.51.100.0/24");
        MockHttpServletRequest request = request(
                "10.0.0.5", "1.2.3.4, 198.51.100.22, 192.168.1.4");

        assertThat(policy.resolveClientAddress(request).getHostAddress()).isEqualTo("198.51.100.22");
        assertThat(policy.isAllowed(client, request)).isTrue();
    }

    @Test
    void emptyClientPolicyAllowsAnyResolvedAddress() {
        assertThat(policy.isAllowed(client(null), request("203.0.113.9", "malformed"))).isTrue();
    }

    private SysAccClientApplication client(String allowedIps) {
        return SysAccClientApplication.builder().allowedIps(allowedIps).build();
    }

    private MockHttpServletRequest request(String remoteAddress, String forwardedFor) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(remoteAddress);
        if (forwardedFor != null) request.addHeader("X-Forwarded-For", forwardedFor);
        return request;
    }
}
