package com.socialpath.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * How much of a feed is loaded at a time. The comment tree itself has no
 * depth limit; what a single request returns does.
 * @param pageSize posts in one feed portion
 * @param repliesPageSize direct replies of one post or comment in one portion
 * @param repliesPreloadDepth reply levels loaded together with a feed portion;
 *                            deeper levels open on request, one level at a time
 */
@ConfigurationProperties("app.feed")
public record FeedProperties(@DefaultValue("20") int pageSize,
                             @DefaultValue("10") int repliesPageSize,
                             @DefaultValue("1") int repliesPreloadDepth) {

    public FeedProperties {
        if (pageSize < 1 || repliesPageSize < 1) {
            throw new IllegalArgumentException("app.feed page sizes must be at least 1");
        }
        if (repliesPreloadDepth < 0) {
            throw new IllegalArgumentException("app.feed.replies-preload-depth must not be negative");
        }
    }
}
