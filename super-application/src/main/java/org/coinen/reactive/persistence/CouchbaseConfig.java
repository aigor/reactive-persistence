/**
 * Copyright (C) Zoomdata, Inc. 2012-2018. All rights reserved.
 */
package org.coinen.reactive.persistence;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.couchbase.config.AbstractCouchbaseConfiguration;
import org.springframework.data.couchbase.repository.config.EnableCouchbaseRepositories;

import java.util.List;

import static java.util.Collections.singletonList;

@Configuration
@EnableCouchbaseRepositories
public class CouchbaseConfig extends AbstractCouchbaseConfiguration {
    private final String clusterHost;
    private final String bucketName;
    private final String user;
    private final String password;

    public CouchbaseConfig(
        @Value("${couchbase.cluster.host}") String clusterHost,
        @Value("${couchbase.cluster.bucket}") String bucketName,
        @Value("${couchbase.cluster.user}") String user,
        @Value("${couchbase.cluster.password}") String password
    ) {
        this.clusterHost = clusterHost;
        this.bucketName = bucketName;
        this.user = user;
        this.password = password;
    }

    @Override
    protected List<String> getBootstrapHosts() {
        return singletonList(clusterHost);
    }

    @Override
    protected String getBucketName() {
        return bucketName;
    }

    @Override
    protected String getUsername() {
        return user;
    }

    @Override
    protected String getBucketPassword() {
        return password;
    }
}