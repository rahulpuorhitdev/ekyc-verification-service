package org.example.sdk.retry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RetryExecutor {
    private static Logger log = LoggerFactory.getLogger(RetryExecutor.class);

    public interface Op<T> { T run(); }
    public interface Sleeper{ void sleep(long millis) throws InterruptedException; }
    public interface RetryPredicate{
        boolean shouldRetry(Throwable throwable);
    }

    private final int maxRetries;
    private final Sleeper sleeper;
    private final RetryPredicate retryPredicate;

    public RetryExecutor() {
        this(3, Thread::sleep, t -> false);
    }

    public RetryExecutor(int maxRetries, Sleeper sleeper, RetryPredicate retryPredicate) {
        this.maxRetries = maxRetries;
        this.sleeper = sleeper;
        this.retryPredicate = retryPredicate;
    }

    public <T> T run(Op<T> op) {
        for(int i = 1; i <= maxRetries; i++) {
            try {
                return op.run();
            } catch (RuntimeException e) {
                if (i == maxRetries || !retryPredicate.shouldRetry(e)) {
                    throw e;
                }

                long backoff = (long) Math.pow(2, i-1) * 1000;
                log.warn("Retry attempt #{} failed, retrying in {} ms", i, backoff);

                try {
                    sleeper.sleep(backoff);
                } catch (InterruptedException e1) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        throw new IllegalStateException();
    }
}
