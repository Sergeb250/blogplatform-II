package com.amali.blogPlatform.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Logging and performance monitoring for EVERY service method, written once, in one place.
 * <p>
 * Without AOP, each of the ~40 service methods would need its own "log start / start timer /
 * try-finally / log end" code. Here an <b>aspect</b> adds that code around the methods automatically:
 * Spring wraps every {@code @Service} bean in a <b>proxy</b>; when a controller calls a service method,
 * the proxy runs the <b>advice</b> methods below before, around and after the real method.
 *
 * <pre>
 * controller ──► proxy ──► @Around (start timer)
 *                           ├─► @Before   "Executing: PostService.createPost"
 *                           ├─► real PostService.createPost(...)
 *                           ├─► @AfterThrowing (only on error) "... failed: ..."
 *                           └─► @After    "Finished: PostService.createPost"
 *                         @Around (stop timer) "PostService.createPost took 12.34 ms"
 * </pre>
 *
 * The aspect never catches an exception for good: errors still reach the GlobalExceptionHandler.
 */
@Aspect
@Component
public class LoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(LoggingAspect.class);

    private final PerformanceMonitor monitor;
    private final long slowThresholdMillis;

    public LoggingAspect(PerformanceMonitor monitor,
                         @Value("${blog.monitoring.slow-threshold-ms:500}") long slowThresholdMillis) {
        this.monitor = monitor;
        this.slowThresholdMillis = slowThresholdMillis;
    }

    /**
     * The <b>pointcut</b>: WHICH methods the advice applies to. In words: "the execution of any method
     * (*), with any arguments (..), of any class in the package com.amali.blogPlatform.service".
     */
    @Pointcut("execution(* com.amali.blogPlatform.service.*.*(..))")
    public void serviceMethods() {
    }

    /** Runs BEFORE the service method. */
    @Before("serviceMethods()")
    public void beforeMethod(JoinPoint joinPoint) {
        log.info("Executing: {} with {}", methodName(joinPoint), maskPasswords(Arrays.toString(joinPoint.getArgs())));
    }

    /** Runs AFTER the service method, whether it returned normally or threw (like a finally block). */
    @After("serviceMethods()")
    public void afterMethod(JoinPoint joinPoint) {
        log.info("Finished: {}", methodName(joinPoint));
    }

    /** Runs only when the service method threw an exception. It logs it; it does not stop it. */
    @AfterThrowing(pointcut = "serviceMethods()", throwing = "error")
    public void afterThrowing(JoinPoint joinPoint, Throwable error) {
        log.warn("{} failed: {} - {}", methodName(joinPoint), error.getClass().getSimpleName(), error.getMessage());
    }

    /**
     * Runs AROUND the service method: it decides when the real method runs ({@code proceed()}), so it
     * can measure the time. The {@code finally} block records the time even when the method throws,
     * and the exception is re-thrown automatically because it is not caught here.
     */
    @Around("serviceMethods()")
    public Object measureTime(ProceedingJoinPoint joinPoint) throws Throwable {
        String method = methodName(joinPoint);
        long start = System.nanoTime();
        boolean failed = false;
        try {
            return joinPoint.proceed();             // run the real service method
        } catch (Throwable error) {
            failed = true;
            throw error;                            // never swallow the error
        } finally {
            double millis = (System.nanoTime() - start) / 1_000_000.0;
            monitor.record(method, millis, failed);
            if (millis > slowThresholdMillis) {
                log.warn("SLOW: {} took {} ms (threshold {} ms)", method, String.format("%.2f", millis), slowThresholdMillis);
            } else {
                log.info("{} took {} ms", method, String.format("%.2f", millis));
            }
        }
    }

    /** e.g. "PostService.createPost" */
    private static String methodName(JoinPoint joinPoint) {
        return joinPoint.getSignature().getDeclaringType().getSimpleName() + "." + joinPoint.getSignature().getName();
    }

    /**
     * Request records print their fields, e.g. {@code CreateUserRequest[username=alice, password=secret123]}.
     * Passwords must never be written to a log file, so their value is replaced by ***.
     */
    static String maskPasswords(String text) {
        return text.replaceAll("(?i)(password=)[^,\\]]*", "$1***");
    }
}
