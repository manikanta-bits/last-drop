package com.lastdrop.service;

import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Demo mode ON  = every auction drops its price every 10 seconds (for judges).
 * Demo mode OFF = real mode: each product drops at its own interval (e.g. every hour).
 * Start value comes from DEMO_MODE; the admin can switch it at any time.
 */
@Service
public class DemoModeService {

    private final AtomicBoolean enabled;

    public DemoModeService(@Value("${app.demo-mode:false}") boolean startEnabled) {
        this.enabled = new AtomicBoolean(startEnabled);
    }

    public boolean isEnabled() {
        return enabled.get();
    }

    public void setEnabled(boolean value) {
        enabled.set(value);
    }
}
