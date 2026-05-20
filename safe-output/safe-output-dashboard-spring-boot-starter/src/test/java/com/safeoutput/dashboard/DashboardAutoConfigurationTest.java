package com.safeoutput.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import com.safeoutput.dashboard.web.SafeOutputDashboardController;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

class DashboardAutoConfigurationTest {

    private final WebApplicationContextRunner webContextRunner = new WebApplicationContextRunner()
            .withUserConfiguration(TestDashboardApplication.class);

    @Test
    void dashboardIsDisabledByDefault() {
        webContextRunner.run(context -> assertThat(context).doesNotHaveBean(SafeOutputDashboardController.class));
    }

    @Test
    void dashboardIsNotRegisteredInNonWebApplications() {
        new ApplicationContextRunner()
                .withUserConfiguration(TestDashboardApplication.class)
                .withPropertyValues("safe-output.dashboard.enabled=true")
                .run(context -> assertThat(context).doesNotHaveBean(SafeOutputDashboardController.class));
    }
}
