package com.bt.pi.app.common;

import java.security.Security;

import javax.annotation.PostConstruct;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.stereotype.Component;

@Component
public class SecurityProvider {
    public SecurityProvider() {
    }

    @PostConstruct
    public void addProvider() {
        Security.addProvider(new BouncyCastleProvider());
    }
}
