package com.amali.blogPlatform.aspect;

import com.amali.blogPlatform.dto.request.LoginRequest;
import com.amali.blogPlatform.exception.InvalidCredentialsException;
import com.amali.blogPlatform.exception.PostNotFoundException;
import com.amali.blogPlatform.service.AuthService;
import com.amali.blogPlatform.service.PostService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Phase 8: the aspect logs service calls, measures time, records metrics and never swallows errors. */
@SpringBootTest
@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)     // captures what is printed to the console (the log)
class LoggingAspectTest {

    @Autowired
    private PostService postService;
    @Autowired
    private AuthService authService;
    @Autowired
    private PerformanceMonitor monitor;

    @BeforeEach
    void resetMetrics() {
        monitor.reset();
    }

    @Test
    void servicesAreWrappedInAProxy() {
        assertThat(AopUtils.isAopProxy(postService)).isTrue();
    }

    @Test
    void beforeAfterAndAroundAdviceAreLogged(CapturedOutput output) {
        postService.getPostById(1);

        assertThat(output).contains("Executing: PostService.getPostById with [1]");
        assertThat(output).contains("Finished: PostService.getPostById");
        assertThat(output).containsPattern("PostService\\.getPostById took \\d+\\.\\d{2} ms");
    }

    @Test
    void errorsAreLoggedAndStillThrown(CapturedOutput output) {
        assertThatThrownBy(() -> postService.getPostById(999))
                .isInstanceOf(PostNotFoundException.class);           // the aspect did not swallow it

        assertThat(output).contains("PostService.getPostById failed: PostNotFoundException - Post with id 999 not found");
        assertThat(output).contains("Finished: PostService.getPostById");   // @After runs even on errors
    }

    @Test
    void timingsAreRecordedPerMethod() {
        postService.getPostById(1);
        postService.getPostById(2);
        assertThatThrownBy(() -> postService.getPostById(999)).isInstanceOf(PostNotFoundException.class);

        assertThat(monitor.snapshot())
                .filteredOn(m -> m.method().equals("PostService.getPostById"))
                .singleElement()
                .satisfies(m -> {
                    assertThat(m.calls()).isEqualTo(3);
                    assertThat(m.errors()).isEqualTo(1);
                    assertThat(m.maxMillis()).isGreaterThanOrEqualTo(m.averageMillis());
                });
    }

    @Test
    void passwordsNeverAppearInTheLog(CapturedOutput output) {
        assertThatThrownBy(() -> authService.login(new LoginRequest("alice", "mySecretPassword")))
                .isInstanceOf(InvalidCredentialsException.class);

        assertThat(output).contains("Executing: AuthService.login with [LoginRequest[username=alice, password=***]]");
        assertThat(output).doesNotContain("mySecretPassword");
    }

    @Test
    void maskPasswordsReplacesEveryPasswordValue() {
        assertThat(LoggingAspect.maskPasswords("[CreateUserRequest[username=a, email=b, password=x1, role=null]]"))
                .isEqualTo("[CreateUserRequest[username=a, email=b, password=***, role=null]]");
    }
}
