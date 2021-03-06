/**
 *
 * Copyright (C) 2010 Cloud Conscious, LLC. <info@cloudconscious.com>
 *
 * ====================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ====================================================================
 */

package org.jclouds.aws.ec2.compute.suppliers;

import static com.google.common.collect.Maps.uniqueIndex;
import static com.google.common.collect.Sets.newLinkedHashSet;

import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.jclouds.domain.Location;
import org.jclouds.domain.LocationScope;
import org.jclouds.domain.internal.LocationImpl;
import org.jclouds.rest.annotations.Provider;

import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableMap;

/**
 * 
 * @author Adrian Cole
 */
@Singleton
public class EC2LocationSupplier implements Supplier<Set<? extends Location>> {
   private final Map<String, String> availabilityZoneToRegionMap;
   private final String providerName;

   @Inject
   EC2LocationSupplier(Map<String, String> availabilityZoneToRegionMap, @Provider String providerName) {
      this.availabilityZoneToRegionMap = availabilityZoneToRegionMap;
      this.providerName = providerName;
   }

   @Override
   public Set<? extends Location> get() {
      Location ec2 = new LocationImpl(LocationScope.PROVIDER, providerName, providerName, null);
      Set<Location> locations = newLinkedHashSet();
      for (String region : newLinkedHashSet(availabilityZoneToRegionMap.values())) {
         locations.add(new LocationImpl(LocationScope.REGION, region, region, ec2));
      }
      ImmutableMap<String, Location> idToLocation = uniqueIndex(locations, new Function<Location, String>() {
         @Override
         public String apply(Location from) {
            return from.getId();
         }
      });
      for (String zone : availabilityZoneToRegionMap.keySet()) {
         locations.add(new LocationImpl(LocationScope.ZONE, zone, zone, idToLocation.get(availabilityZoneToRegionMap
                  .get(zone))));
      }
      return locations;
   }

}