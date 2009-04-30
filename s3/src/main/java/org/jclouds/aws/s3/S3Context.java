/**
 *
 * Copyright (C) 2009 Adrian Cole <adriancole@jclouds.org>
 *
 * ====================================================================
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * ====================================================================
 */
package org.jclouds.aws.s3;

import org.jclouds.aws.s3.domain.S3Bucket;

public interface S3Context {

    /**
     * @return a connection to S3
     */
    S3Connection getConnection();

    /**
     * Creates a <code>Map<String,InputStream</code> view of the specified
     * bucket.
     * 
     * @param bucket
     * @return
     */
    S3ObjectMap createMapView(S3Bucket bucket);

    /**
     * Closes all connections to S3.
     */
    void close();

}