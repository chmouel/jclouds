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

package org.jclouds.aws.ec2.compute.functions;

import static org.jclouds.aws.ec2.compute.domain.EC2HardwareBuilder.m1_small;
import static org.testng.Assert.assertEquals;

import java.net.UnknownHostException;
import java.util.Map;
import java.util.Set;

import org.jclouds.aws.ec2.compute.config.EC2ComputeServiceDependenciesModule;
import org.jclouds.aws.ec2.compute.domain.RegionAndName;
import org.jclouds.aws.ec2.domain.InstanceState;
import org.jclouds.aws.ec2.domain.RunningInstance;
import org.jclouds.aws.ec2.xml.DescribeInstancesResponseHandlerTest;
import org.jclouds.compute.domain.Hardware;
import org.jclouds.compute.domain.Image;
import org.jclouds.compute.domain.NodeMetadataBuilder;
import org.jclouds.compute.domain.NodeState;
import org.jclouds.compute.domain.OperatingSystemBuilder;
import org.jclouds.compute.domain.OsFamily;
import org.jclouds.date.DateService;
import org.jclouds.date.internal.SimpleDateFormatDateService;
import org.jclouds.domain.Credentials;
import org.jclouds.domain.Location;
import org.jclouds.domain.LocationScope;
import org.jclouds.domain.internal.LocationImpl;
import org.testng.annotations.Test;

import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.MapMaker;

import javax.annotation.Nullable;

/**
 * @author Adrian Cole
 */
@Test(groups = "unit", testName = "ec2.RunningInstanceToNodeMetadataTest")
public class RunningInstanceToNodeMetadataTest {

   public void testAllStatesCovered() {

      for (InstanceState state : InstanceState.values()) {
         assert EC2ComputeServiceDependenciesModule.instanceToNodeState.containsKey(state) : state;
      }

   }

   Location provider = new LocationImpl(LocationScope.REGION, "us-east-1", "description", null);

   @Test
   public void testApplyWhereTagDoesntMatchAndImageHardwareAndLocationNotFoundButCredentialsFound()
            throws UnknownHostException {
      Credentials creds = new Credentials("root", "abdce");

      RunningInstanceToNodeMetadata parser = createNodeParser(ImmutableSet.<Hardware> of(), ImmutableSet
               .<Location> of(), ImmutableSet.<Image> of(), ImmutableMap.<String, Credentials> of(
               "node#us-east-1/i-9slweygo", creds));

      RunningInstance server = firstInstanceFromResource("/ec2/describe_instances_nova.xml");

      assertEquals(parser.apply(server), new NodeMetadataBuilder().state(NodeState.TERMINATED).publicAddresses(
               ImmutableSet.<String> of()).privateAddresses(ImmutableSet.of("10.128.207.5")).tag("NOTAG-i-9slweygo")
               .credentials(creds).imageId("us-east-1/ami-25CB1213").id("us-east-1/i-9slweygo")
               .providerId("i-9slweygo").build());
   }

   @Test
   public void testApplyWhereTagDoesntMatchAndImageHardwareAndLocationNotFound() throws UnknownHostException {
      RunningInstanceToNodeMetadata parser = createNodeParser(ImmutableSet.<Hardware> of(), ImmutableSet
               .<Location> of(), ImmutableSet.<Image> of(), ImmutableMap.<String, Credentials> of());

      RunningInstance server = firstInstanceFromResource("/ec2/describe_instances_nova.xml");

      assertEquals(parser.apply(server), new NodeMetadataBuilder().state(NodeState.TERMINATED).publicAddresses(
               ImmutableSet.<String> of()).privateAddresses(ImmutableSet.of("10.128.207.5")).tag("NOTAG-i-9slweygo")
               .imageId("us-east-1/ami-25CB1213").id("us-east-1/i-9slweygo").providerId("i-9slweygo").build());
   }

   @Test
   public void testApplyWhereTagDoesntMatchAndLocationFoundAndImageAndHardwareNotFound() throws UnknownHostException {
      RunningInstanceToNodeMetadata parser = createNodeParser(ImmutableSet.<Hardware> of(), ImmutableSet.of(provider),
               ImmutableSet.<Image> of(), ImmutableMap.<String, Credentials> of());

      RunningInstance server = firstInstanceFromResource("/ec2/describe_instances_nova.xml");

      assertEquals(parser.apply(server), new NodeMetadataBuilder().state(NodeState.TERMINATED).privateAddresses(
               ImmutableSet.of("10.128.207.5")).tag("NOTAG-i-9slweygo").imageId("us-east-1/ami-25CB1213").id(
               "us-east-1/i-9slweygo").providerId("i-9slweygo").location(provider).build());
   }

   @Test
   public void testApplyWhereTagDoesntMatchAndImageAndLocationFoundAndHardwareNotFound() throws UnknownHostException {
      RunningInstanceToNodeMetadata parser = createNodeParser(ImmutableSet.<Hardware> of(), ImmutableSet.of(provider),
               ImageParserTest.convertImages("/ec2/nova_images.xml"), ImmutableMap.<String, Credentials> of());

      RunningInstance server = firstInstanceFromResource("/ec2/describe_instances_nova.xml");

      assertEquals(parser.apply(server), new NodeMetadataBuilder().state(NodeState.TERMINATED).privateAddresses(
               ImmutableSet.of("10.128.207.5")).tag("NOTAG-i-9slweygo").imageId("us-east-1/ami-25CB1213")
               .operatingSystem(
                        new OperatingSystemBuilder().family(OsFamily.UBUNTU).version("9.10").arch("paravirtual")
                                 .description("nebula/ubuntu-karmic").is64Bit(true).build()).id("us-east-1/i-9slweygo")
               .providerId("i-9slweygo").location(provider).build());
   }

   @Test
   public void testApplyWhereTagDoesntMatchAndImageHardwareAndLocationFound() throws UnknownHostException {
      RunningInstanceToNodeMetadata parser = createNodeParser(ImmutableSet.of(m1_small().build()), ImmutableSet
               .of(provider), ImageParserTest.convertImages("/ec2/nova_images.xml"), ImmutableMap
               .<String, Credentials> of());

      RunningInstance server = firstInstanceFromResource("/ec2/describe_instances_nova.xml");

      assertEquals(parser.apply(server), new NodeMetadataBuilder().state(NodeState.TERMINATED).privateAddresses(
               ImmutableSet.of("10.128.207.5")).tag("NOTAG-i-9slweygo").imageId("us-east-1/ami-25CB1213").hardware(
               m1_small().build()).operatingSystem(
               new OperatingSystemBuilder().family(OsFamily.UBUNTU).version("9.10").arch("paravirtual").description(
                        "nebula/ubuntu-karmic").is64Bit(true).build()).id("us-east-1/i-9slweygo").providerId(
               "i-9slweygo").location(provider).build());
   }

   @Test
   public void testHandleMissingAMIs() {

      // Handle the case when the installed AMI no longer can be found in AWS.

      // Create a null-returning function to simulate that the AMI can't be found.
      Function<RegionAndName, Image> nullReturningFunction = new Function<RegionAndName, Image>() {

         @Override
         public Image apply(@Nullable RegionAndName from) {
            return null;
         }
      };
      Map<RegionAndName, Image> instanceToImage = new MapMaker().makeComputingMap(nullReturningFunction);

      RunningInstanceToNodeMetadata parser = createNodeParser(ImmutableSet.of(m1_small().build()), ImmutableSet
              .of(provider), ImmutableMap
              .<String, Credentials>of(), EC2ComputeServiceDependenciesModule.instanceToNodeState, instanceToImage);

      RunningInstance server = firstInstanceFromResource("/ec2/describe_instances_nova.xml");

      assertEquals(parser.apply(server), new NodeMetadataBuilder().state(NodeState.TERMINATED).privateAddresses(
              ImmutableSet.of("10.128.207.5")).tag("NOTAG-i-9slweygo").imageId("us-east-1/ami-25CB1213").id(
              "us-east-1/i-9slweygo").providerId("i-9slweygo").hardware(m1_small().build()).location(
              provider).build());
   }


   protected RunningInstance firstInstanceFromResource(String resource) {
      RunningInstance server = Iterables.get(Iterables.get(DescribeInstancesResponseHandlerTest
              .parseRunningInstances(resource), 0), 0);
      return server;
   }

   protected RunningInstanceToNodeMetadata createNodeParser(final ImmutableSet<Hardware> hardware,
                                                            final ImmutableSet<Location> locations,
                                                            Set<org.jclouds.compute.domain.Image> images,
                                                            Map<String, Credentials> credentialStore) {
      Map<InstanceState, NodeState> instanceToNodeState = EC2ComputeServiceDependenciesModule.instanceToNodeState;

      Map<RegionAndName, Image> instanceToImage = Maps.uniqueIndex(images, new Function<Image, RegionAndName>() {

         @Override
         public RegionAndName apply(Image from) {
            return new RegionAndName(from.getLocation().getId(), from.getProviderId());
         }

      });

      return createNodeParser(hardware, locations, credentialStore, instanceToNodeState, instanceToImage);
   }

   private RunningInstanceToNodeMetadata createNodeParser(final ImmutableSet<Hardware> hardware, final
   ImmutableSet<Location> locations, Map<String, Credentials> credentialStore, Map<InstanceState, NodeState>
           instanceToNodeState, Map<RegionAndName, Image> instanceToImage) {
      Supplier<Set<? extends Location>> locationSupplier = new Supplier<Set<? extends Location>>() {

         @Override
         public Set<? extends Location> get() {
            return locations;
         }

      };
      Supplier<Set<? extends Hardware>> hardwareSupplier = new Supplier<Set<? extends Hardware>>() {

         @Override
         public Set<? extends Hardware> get() {
            return hardware;
         }

      };
      RunningInstanceToNodeMetadata parser = new RunningInstanceToNodeMetadata(instanceToNodeState, credentialStore,
               instanceToImage, locationSupplier, hardwareSupplier);
      return parser;
   }

   DateService dateService = new SimpleDateFormatDateService();
   //
   // @SuppressWarnings({ "unchecked" })
   // @Test
   // public void testApplyWithEBSWhenBootIsInstanceStoreAndAvailabilityZoneNotFound() throws
   // UnknownHostException {
   // EC2Client client = createMock(EC2Client.class);
   // AMIClient amiClient = createMock(AMIClient.class);
   // expect(client.getAMIServices()).andReturn(amiClient).atLeastOnce();
   // Map<RegionAndName, KeyPair> credentialsMap = createMock(Map.class);
   // ConcurrentMap<RegionAndName, org.jclouds.compute.domain.Image> imageMap =
   // createMock(ConcurrentMap.class);
   // @Memoized Supplier<Set<? extends Hardware>> hardwares = Suppliers.<Set<? extends Hardware>>
   // ofInstance(ImmutableSet
   // .<Hardware> of(m1_small().build()));
   // PopulateDefaultLoginCredentialsForImageStrategy credentialProvider =
   // createMock(PopulateDefaultLoginCredentialsForImageStrategy.class);
   // RunningInstance instance = createMock(RunningInstance.class);
   // Image image = createMock(Image.class);
   //
   // expect(instance.getId()).andReturn("i-3d640055").atLeastOnce();
   // expect(instance.getGroupIds()).andReturn(ImmutableSet.of("default")).atLeastOnce();
   // expect(instance.getKeyName()).andReturn("jclouds#tag#us-east-1#50").atLeastOnce();
   // expect(instance.getInstanceState()).andReturn(InstanceState.RUNNING);
   //
   // Location location = new LocationImpl(LocationScope.ZONE, "us-east-1d", "description", null);
   // @Memoized Supplier<Set<? extends Location>> locations = Suppliers.<Set<? extends Location>>
   // ofInstance(ImmutableSet
   // .<Location> of(location));
   // org.jclouds.compute.domain.Image jcImage = createMock(org.jclouds.compute.domain.Image.class);
   //
   // expect(instance.getIpAddress()).andReturn("174.129.1.50");
   // expect(instance.getPrivateIpAddress()).andReturn("10.202.117.241");
   //
   // expect(instance.getRegion()).andReturn(Region.US_EAST_1).atLeastOnce();
   //
   // expect(jcImage.getOperatingSystem()).andReturn(createMock(OperatingSystem.class)).atLeastOnce();
   //
   // expect(instance.getImageId()).andReturn("ami-1515f07c").atLeastOnce();
   // expect(imageMap.get(new RegionAndName(Region.US_EAST_1, "ami-1515f07c"))).andReturn(jcImage);
   //
   // expect(amiClient.describeImagesInRegion(Region.US_EAST_1,
   // imageIds("ami-1515f07c"))).andReturn(
   // (Set) ImmutableSet.<Image> of(image));
   //
   // expect(credentialProvider.execute(image)).andReturn(new Credentials("user", "pass"));
   //
   // expect(credentialsMap.get(new RegionAndName(Region.US_EAST_1,
   // "jclouds#tag#us-east-1#50"))).andReturn(
   // new KeyPair(Region.US_EAST_1, "jclouds#tag#us-east-1#50", "keyFingerprint", "pass"));
   //
   // expect(instance.getAvailabilityZone()).andReturn(AvailabilityZone.US_EAST_1A).atLeastOnce();
   //
   // expect(instance.getInstanceType()).andReturn(InstanceType.M1_SMALL).atLeastOnce();
   // expect(instance.getEbsBlockDevices()).andReturn(
   // ImmutableMap.<String, EbsBlockDevice> of(
   // "/dev/sdg",
   // new EbsBlockDevice("vol-1f20d376", Attachment.Status.ATTACHED, dateService
   // .iso8601DateParse("2009-12-11T16:32:46.000Z"), false),
   // "/dev/sdj",
   // new EbsBlockDevice("vol-c0eb78aa", Attachment.Status.ATTACHED, dateService
   // .iso8601DateParse("2010-06-17T10:43:28.000Z"), false)));
   // expect(instance.getRootDeviceType()).andReturn(RootDeviceType.INSTANCE_STORE);
   // expect(instance.getRootDeviceName()).andReturn(null).atLeastOnce();
   //
   // replay(imageMap);
   // replay(client);
   // replay(amiClient);
   // replay(credentialsMap);
   // replay(credentialProvider);
   // replay(instance);
   // replay(jcImage);
   //
   // Function<RunningInstance, NodeMetadata> parser = new RunningInstanceToNodeMetadata(client,
   // credentialsMap,
   // credentialProvider, imageMap, locations, hardwares);
   //
   // NodeMetadata metadata = parser.apply(instance);
   //
   // assertEquals(metadata.getTag(), "NOTAG-i-3d640055");
   // assertEquals(metadata.getLocation(), null);
   // assertEquals(metadata.getImageId(), "us-east-1/ami-1515f07c");
   // assertEquals(metadata.getHardware().getId(), "m1.small");
   // assertEquals(metadata.getHardware().getName(), "m1.small");
   // assertEquals(metadata.getHardware().getProviderId(), "m1.small");
   // assertEquals(metadata.getHardware().getProcessors(), ImmutableList.<Processor> of(new
   // Processor(1.0, 1.0)));
   // assertEquals(metadata.getHardware().getRam(), 1740);
   // assertEquals(metadata.getHardware().getVolumes(),
   // ImmutableList.<Volume> of(new VolumeImpl(null, Volume.Type.LOCAL, 10.0f, "/dev/sda1", true,
   // false),//
   // new VolumeImpl(null, Volume.Type.LOCAL, 150.0f, "/dev/sda2", false, false),//
   // new VolumeImpl("vol-1f20d376", Volume.Type.SAN, null, "/dev/sdg", false, true),//
   // new VolumeImpl("vol-c0eb78aa", Volume.Type.SAN, null, "/dev/sdj", false, true)));
   //
   // assertEquals(metadata.getCredentials(), new Credentials("user", "pass"));
   //
   // verify(imageMap);
   // verify(jcImage);
   // verify(client);
   // verify(amiClient);
   // verify(credentialsMap);
   // verify(credentialProvider);
   // verify(instance);
   //
   // }
   //
   // @SuppressWarnings({ "unchecked" })
   // @Test
   // public void testApplyForNovaWhereTagDoesntMatchAndNullAvailabilityZoneIpAddressNoGroups()
   // throws
   // UnknownHostException {
   // EC2Client client = createMock(EC2Client.class);
   // AMIClient amiClient = createMock(AMIClient.class);
   // expect(client.getAMIServices()).andReturn(amiClient).atLeastOnce();
   // Map<RegionAndName, KeyPair> credentialsMap = createMock(Map.class);
   // ConcurrentMap<RegionAndName, org.jclouds.compute.domain.Image> imageMap =
   // createMock(ConcurrentMap.class);
   // @Memoized Supplier<Set<? extends Hardware>> hardwares = Suppliers.<Set<? extends Hardware>>
   // ofInstance(ImmutableSet
   // .<Hardware> of(m1_small().build()));
   // PopulateDefaultLoginCredentialsForImageStrategy credentialProvider =
   // createMock(PopulateDefaultLoginCredentialsForImageStrategy.class);
   // RunningInstance instance = createMock(RunningInstance.class);
   // Image image = createMock(Image.class);
   //
   // expect(instance.getId()).andReturn("i-3d640055").atLeastOnce();
   // expect(instance.getGroupIds()).andReturn(ImmutableSet.<String> of()).atLeastOnce();
   // expect(instance.getKeyName()).andReturn("nebulatanimislam").atLeastOnce();
   // expect(instance.getInstanceState()).andReturn(InstanceState.RUNNING);
   //
   // Location region = new LocationImpl(LocationScope.REGION, "us-east-1", "description", null);
   // @Memoized Supplier<Set<? extends Location>> locations = Suppliers.<Set<? extends Location>>
   // ofInstance(ImmutableSet
   // .<Location> of(region));
   // org.jclouds.compute.domain.Image jcImage = createMock(org.jclouds.compute.domain.Image.class);
   //
   // expect(instance.getIpAddress()).andReturn(null);
   // expect(instance.getPrivateIpAddress()).andReturn("10.202.117.241");
   //
   // expect(instance.getRegion()).andReturn(Region.US_EAST_1).atLeastOnce();
   //
   // expect(jcImage.getOperatingSystem()).andReturn(createMock(OperatingSystem.class)).atLeastOnce();
   //
   // expect(instance.getImageId()).andReturn("ami-1515f07c").atLeastOnce();
   // expect(imageMap.get(new RegionAndName(Region.US_EAST_1, "ami-1515f07c"))).andReturn(jcImage);
   //
   // expect(amiClient.describeImagesInRegion(Region.US_EAST_1,
   // imageIds("ami-1515f07c"))).andReturn(
   // (Set) ImmutableSet.<Image> of(image));
   //
   // expect(credentialProvider.execute(image)).andReturn(new Credentials("user", "pass"));
   //
   // expect(credentialsMap.get(new RegionAndName(Region.US_EAST_1,
   // "nebulatanimislam"))).andReturn(null);
   //
   // expect(instance.getAvailabilityZone()).andReturn(null).atLeastOnce();
   //
   // expect(instance.getInstanceType()).andReturn(InstanceType.M1_SMALL).atLeastOnce();
   // expect(instance.getEbsBlockDevices()).andReturn(Maps.<String, EbsBlockDevice> newHashMap());
   // expect(instance.getRootDeviceType()).andReturn(RootDeviceType.INSTANCE_STORE);
   //
   // replay(imageMap);
   // replay(client);
   // replay(amiClient);
   // replay(credentialsMap);
   // replay(credentialProvider);
   // replay(instance);
   // replay(jcImage);
   //
   // Function<RunningInstance, NodeMetadata> parser = new RunningInstanceToNodeMetadata(client,
   // credentialsMap,
   // credentialProvider, imageMap, locations, hardwares);
   //
   // NodeMetadata metadata = parser.apply(instance);
   //
   // assertEquals(metadata.getTag(), "NOTAG-i-3d640055");
   // assertEquals(metadata.getLocation(), region);
   // assertEquals(metadata.getImageId(), "us-east-1/ami-1515f07c");
   // assertEquals(metadata.getHardware().getId(), "m1.small");
   // assertEquals(metadata.getHardware().getName(), "m1.small");
   // assertEquals(metadata.getHardware().getProviderId(), "m1.small");
   // assertEquals(metadata.getHardware().getProcessors(), ImmutableList.<Processor> of(new
   // Processor(1.0, 1.0)));
   // assertEquals(metadata.getHardware().getRam(), 1740);
   // assertEquals(metadata.getHardware().getVolumes(),
   // ImmutableList.<Volume> of(new VolumeImpl(null, Volume.Type.LOCAL, 10.0f, "/dev/sda1", true,
   // false),//
   // new VolumeImpl(null, Volume.Type.LOCAL, 150.0f, "/dev/sda2", false, false)));
   //
   // assertEquals(metadata.getCredentials(), new Credentials("user", null));
   //
   // verify(imageMap);
   // verify(jcImage);
   // verify(client);
   // verify(amiClient);
   // verify(credentialsMap);
   // verify(credentialProvider);
   // verify(instance);
   //
   // }
   //
   // @SuppressWarnings({ "unchecked" })
   // @Test
   // public void testApplyWhereTagDoesntMatchAndUnknownInstanceType() throws UnknownHostException {
   // EC2Client client = createMock(EC2Client.class);
   // AMIClient amiClient = createMock(AMIClient.class);
   // expect(client.getAMIServices()).andReturn(amiClient).atLeastOnce();
   // Map<RegionAndName, KeyPair> credentialsMap = createMock(Map.class);
   // ConcurrentMap<RegionAndName, org.jclouds.compute.domain.Image> imageMap =
   // createMock(ConcurrentMap.class);
   // @Memoized Supplier<Set<? extends Hardware>> hardwares = Suppliers.<Set<? extends Hardware>>
   // ofInstance(ImmutableSet
   // .<Hardware> of(m1_small().build()));
   // PopulateDefaultLoginCredentialsForImageStrategy credentialProvider =
   // createMock(PopulateDefaultLoginCredentialsForImageStrategy.class);
   // RunningInstance instance = createMock(RunningInstance.class);
   // Image image = createMock(Image.class);
   //
   // expect(instance.getId()).andReturn("i-3d640055").atLeastOnce();
   // expect(instance.getGroupIds()).andReturn(ImmutableSet.<String> of()).atLeastOnce();
   // expect(instance.getKeyName()).andReturn("nebulatanimislam").atLeastOnce();
   // expect(instance.getInstanceState()).andReturn(InstanceState.RUNNING);
   //
   // Location region = new LocationImpl(LocationScope.REGION, "us-east-1", "description", null);
   // @Memoized Supplier<Set<? extends Location>> locations = Suppliers.<Set<? extends Location>>
   // ofInstance(ImmutableSet
   // .<Location> of(region));
   // org.jclouds.compute.domain.Image jcImage = createMock(org.jclouds.compute.domain.Image.class);
   //
   // expect(instance.getIpAddress()).andReturn(null);
   // expect(instance.getPrivateIpAddress()).andReturn("10.202.117.241");
   //
   // expect(instance.getRegion()).andReturn(Region.US_EAST_1).atLeastOnce();
   //
   // expect(jcImage.getOperatingSystem()).andReturn(createMock(OperatingSystem.class)).atLeastOnce();
   //
   // expect(instance.getImageId()).andReturn("ami-1515f07c").atLeastOnce();
   // expect(imageMap.get(new RegionAndName(Region.US_EAST_1, "ami-1515f07c"))).andReturn(jcImage);
   //
   // expect(amiClient.describeImagesInRegion(Region.US_EAST_1,
   // imageIds("ami-1515f07c"))).andReturn(
   // (Set) ImmutableSet.<Image> of(image));
   //
   // expect(credentialProvider.execute(image)).andReturn(new Credentials("user", "pass"));
   //
   // expect(credentialsMap.get(new RegionAndName(Region.US_EAST_1,
   // "nebulatanimislam"))).andReturn(null);
   //
   // expect(instance.getAvailabilityZone()).andReturn(null).atLeastOnce();
   //
   // expect(instance.getInstanceType()).andReturn("hhttpp").atLeastOnce();
   //
   // replay(imageMap);
   // replay(client);
   // replay(amiClient);
   // replay(credentialsMap);
   // replay(credentialProvider);
   // replay(instance);
   // replay(jcImage);
   //
   // Function<RunningInstance, NodeMetadata> parser = new RunningInstanceToNodeMetadata(client,
   // credentialsMap,
   // credentialProvider, imageMap, locations, hardwares);
   //
   // NodeMetadata metadata = parser.apply(instance);
   //
   // assertEquals(metadata.getTag(), "NOTAG-i-3d640055");
   // assertEquals(metadata.getLocation(), region);
   // assertEquals(metadata.getImageId(), "us-east-1/ami-1515f07c");
   // assertEquals(metadata.getHardware(), null);
   //
   // assertEquals(metadata.getCredentials(), new Credentials("user", null));
   //
   // verify(imageMap);
   // verify(jcImage);
   // verify(client);
   // verify(amiClient);
   // verify(credentialsMap);
   // verify(credentialProvider);
   // verify(instance);
   //
   // }
   //
   // @SuppressWarnings({ "unchecked" })
   // @Test
   // public void testApplyForNovaWhereTagDoesntMatchAndImageNotFound() throws UnknownHostException
   // {
   // EC2Client client = createMock(EC2Client.class);
   // AMIClient amiClient = createMock(AMIClient.class);
   // expect(client.getAMIServices()).andReturn(amiClient).atLeastOnce();
   // Map<RegionAndName, KeyPair> credentialsMap = createMock(Map.class);
   // ConcurrentMap<RegionAndName, org.jclouds.compute.domain.Image> imageMap =
   // createMock(ConcurrentMap.class);
   // @Memoized Supplier<Set<? extends Hardware>> hardwares = Suppliers.<Set<? extends Hardware>>
   // ofInstance(ImmutableSet
   // .<Hardware> of(m1_small().build()));
   // PopulateDefaultLoginCredentialsForImageStrategy credentialProvider =
   // createMock(PopulateDefaultLoginCredentialsForImageStrategy.class);
   // RunningInstance instance = createMock(RunningInstance.class);
   //
   // expect(instance.getId()).andReturn("i-3d640055").atLeastOnce();
   // expect(instance.getGroupIds()).andReturn(ImmutableSet.<String> of()).atLeastOnce();
   // expect(instance.getKeyName()).andReturn("nebulatanimislam").atLeastOnce();
   // expect(instance.getInstanceState()).andReturn(InstanceState.RUNNING);
   //
   // Location region = new LocationImpl(LocationScope.REGION, "us-east-1", "description", null);
   // @Memoized Supplier<Set<? extends Location>> locations = Suppliers.<Set<? extends Location>>
   // ofInstance(ImmutableSet
   // .<Location> of(region));
   // org.jclouds.compute.domain.Image jcImage = createMock(org.jclouds.compute.domain.Image.class);
   //
   // expect(instance.getIpAddress()).andReturn(null);
   // expect(instance.getPrivateIpAddress()).andReturn("10.202.117.241");
   //
   // expect(instance.getRegion()).andReturn(Region.US_EAST_1).atLeastOnce();
   //
   // expect(jcImage.getOperatingSystem()).andReturn(createMock(OperatingSystem.class)).atLeastOnce();
   //
   // expect(instance.getImageId()).andReturn("ami-1515f07c").atLeastOnce();
   // expect(imageMap.get(new RegionAndName(Region.US_EAST_1, "ami-1515f07c"))).andReturn(jcImage);
   //
   // expect(amiClient.describeImagesInRegion(Region.US_EAST_1,
   // imageIds("ami-1515f07c"))).andReturn(
   // (Set) ImmutableSet.<Image> of());
   //
   // expect(credentialProvider.execute(null)).andReturn(new Credentials("root", null));
   //
   // expect(credentialsMap.get(new RegionAndName(Region.US_EAST_1,
   // "nebulatanimislam"))).andReturn(null);
   //
   // expect(instance.getAvailabilityZone()).andReturn(null).atLeastOnce();
   //
   // expect(instance.getInstanceType()).andReturn(InstanceType.M1_SMALL).atLeastOnce();
   // expect(instance.getEbsBlockDevices()).andReturn(Maps.<String, EbsBlockDevice> newHashMap());
   // expect(instance.getRootDeviceType()).andReturn(RootDeviceType.INSTANCE_STORE);
   //
   // replay(imageMap);
   // replay(client);
   // replay(amiClient);
   // replay(credentialsMap);
   // replay(credentialProvider);
   // replay(instance);
   // replay(jcImage);
   //
   // Function<RunningInstance, NodeMetadata> parser = new RunningInstanceToNodeMetadata(client,
   // credentialsMap,
   // credentialProvider, imageMap, locations, hardwares);
   //
   // NodeMetadata metadata = parser.apply(instance);
   //
   // assertEquals(metadata.getTag(), "NOTAG-i-3d640055");
   // assertEquals(metadata.getLocation(), region);
   // assertEquals(metadata.getImageId(), "us-east-1/ami-1515f07c");
   // assertEquals(metadata.getHardware().getId(), "m1.small");
   // assertEquals(metadata.getHardware().getName(), "m1.small");
   // assertEquals(metadata.getHardware().getProviderId(), "m1.small");
   // assertEquals(metadata.getHardware().getProcessors(), ImmutableList.<Processor> of(new
   // Processor(1.0, 1.0)));
   // assertEquals(metadata.getHardware().getRam(), 1740);
   // assertEquals(metadata.getHardware().getVolumes(),
   // ImmutableList.<Volume> of(new VolumeImpl(null, Volume.Type.LOCAL, 10.0f, "/dev/sda1", true,
   // false),//
   // new VolumeImpl(null, Volume.Type.LOCAL, 150.0f, "/dev/sda2", false, false)));
   //
   // assertEquals(metadata.getCredentials(), new Credentials("root", null));
   //
   // verify(imageMap);
   // verify(jcImage);
   // verify(client);
   // verify(amiClient);
   // verify(credentialsMap);
   // verify(credentialProvider);
   // verify(instance);
   //
   // }
   //
   // @SuppressWarnings("unchecked")
   // @Test
   // public void testImageNotFoundAndLazyReturnsNull() throws UnknownHostException {
   // EC2Client client = createMock(EC2Client.class);
   // AMIClient amiClient = createMock(AMIClient.class);
   //
   // Map<RegionAndName, KeyPair> credentialsMap = createMock(Map.class);
   // org.jclouds.compute.domain.Image jcImage = createMock(org.jclouds.compute.domain.Image.class);
   //
   // ConcurrentMap<RegionAndName, org.jclouds.compute.domain.Image> imageMap =
   // createMock(ConcurrentMap.class);
   //
   // Location location = new LocationImpl(LocationScope.ZONE, "us-east-1a", "description", null);
   // @Memoized Supplier<Set<? extends Location>> locations = Suppliers.<Set<? extends Location>>
   // ofInstance(ImmutableSet
   // .<Location> of(location));
   // @Memoized Supplier<Set<? extends Hardware>> hardwares = Suppliers.<Set<? extends Hardware>>
   // ofInstance(ImmutableSet
   // .<Hardware> of(m2_4xlarge().build()));
   // PopulateDefaultLoginCredentialsForImageStrategy credentialProvider =
   // createMock(PopulateDefaultLoginCredentialsForImageStrategy.class);
   // RunningInstance instance = createMock(RunningInstance.class);
   //
   // expect(instance.getId()).andReturn("id").atLeastOnce();
   // expect(instance.getGroupIds()).andReturn(ImmutableSet.<String> of()).atLeastOnce();
   // expect(instance.getKeyName()).andReturn(null).atLeastOnce();
   // expect(instance.getInstanceState()).andReturn(InstanceState.RUNNING);
   //
   // expect(instance.getIpAddress()).andReturn("127.0.0.1");
   // expect(instance.getPrivateIpAddress()).andReturn("127.0.0.1");
   //
   // expect(instance.getAvailabilityZone()).andReturn(AvailabilityZone.US_EAST_1A).atLeastOnce();
   //
   // expect(instance.getImageId()).andReturn("imageId").atLeastOnce();
   // expect(instance.getRegion()).andReturn("us-east-1").atLeastOnce();
   //
   // expect(imageMap.get(new RegionAndName("us-east-1", "imageId"))).andReturn(null);
   //
   // expect(instance.getInstanceType()).andReturn(InstanceType.C1_XLARGE).atLeastOnce();
   //
   // replay(imageMap);
   // replay(jcImage);
   // replay(client);
   // replay(amiClient);
   // replay(credentialsMap);
   // replay(credentialProvider);
   // replay(instance);
   //
   // Function<RunningInstance, NodeMetadata> parser = new RunningInstanceToNodeMetadata(client,
   // credentialsMap,
   // credentialProvider, imageMap, locations, hardwares);
   //
   // NodeMetadata metadata = parser.apply(instance);
   // assertEquals(metadata.getLocation(), locations.get().iterator().next());
   // assertEquals(metadata.getImageId(), "us-east-1/imageId");
   // assertEquals(metadata.getTag(), "NOTAG-id");
   // assertEquals(metadata.getCredentials(), null);
   //
   // verify(imageMap);
   // verify(jcImage);
   // verify(client);
   // verify(amiClient);
   // verify(credentialsMap);
   // verify(credentialProvider);
   // verify(instance);
   // }
   //
   // @SuppressWarnings("unchecked")
   // @Test
   // public void testImageNotFoundStillSetsImageId() throws UnknownHostException {
   // EC2Client client = createMock(EC2Client.class);
   // AMIClient amiClient = createMock(AMIClient.class);
   // Map<RegionAndName, KeyPair> credentialsMap = createMock(Map.class);
   // org.jclouds.compute.domain.Image jcImage = createMock(org.jclouds.compute.domain.Image.class);
   //
   // ConcurrentMap<RegionAndName, org.jclouds.compute.domain.Image> imageMap =
   // createMock(ConcurrentMap.class);
   //
   // Location location = new LocationImpl(LocationScope.ZONE, "us-east-1a", "description", null);
   // @Memoized Supplier<Set<? extends Location>> locations = Suppliers.<Set<? extends Location>>
   // ofInstance(ImmutableSet
   // .<Location> of(location));
   // @Memoized Supplier<Set<? extends Hardware>> hardwares = Suppliers.<Set<? extends Hardware>>
   // ofInstance(ImmutableSet
   // .<Hardware> of(m2_4xlarge().build()));
   // PopulateDefaultLoginCredentialsForImageStrategy credentialProvider =
   // createMock(PopulateDefaultLoginCredentialsForImageStrategy.class);
   // RunningInstance instance = createMock(RunningInstance.class);
   //
   // expect(instance.getId()).andReturn("id").atLeastOnce();
   // expect(instance.getGroupIds()).andReturn(ImmutableSet.<String> of()).atLeastOnce();
   // expect(instance.getKeyName()).andReturn(null).atLeastOnce();
   // expect(instance.getInstanceState()).andReturn(InstanceState.RUNNING);
   //
   // expect(instance.getIpAddress()).andReturn("127.0.0.1");
   // expect(instance.getPrivateIpAddress()).andReturn("127.0.0.1");
   //
   // expect(instance.getAvailabilityZone()).andReturn(AvailabilityZone.US_EAST_1A).atLeastOnce();
   //
   // expect(instance.getImageId()).andReturn("imageId").atLeastOnce();
   // expect(instance.getRegion()).andReturn("us-east-1").atLeastOnce();
   //
   // expect(imageMap.get(new RegionAndName("us-east-1", "imageId"))).andThrow(new
   // NullPointerException())
   // .atLeastOnce();
   //
   // expect(instance.getInstanceType()).andReturn(InstanceType.C1_XLARGE).atLeastOnce();
   //
   // replay(imageMap);
   // replay(jcImage);
   // replay(client);
   // replay(amiClient);
   // replay(credentialsMap);
   // replay(credentialProvider);
   // replay(instance);
   //
   // Function<RunningInstance, NodeMetadata> parser = new RunningInstanceToNodeMetadata(client,
   // credentialsMap,
   // credentialProvider, imageMap, locations, hardwares);
   //
   // NodeMetadata metadata = parser.apply(instance);
   // assertEquals(metadata.getLocation(), locations.get().iterator().next());
   // assertEquals(metadata.getImageId(), "us-east-1/imageId");
   // assertEquals(metadata.getTag(), "NOTAG-id");
   // assertEquals(metadata.getCredentials(), null);
   //
   // verify(imageMap);
   // verify(jcImage);
   // verify(client);
   // verify(amiClient);
   // verify(credentialsMap);
   // verify(credentialProvider);
   // verify(instance);
   // }
   //
   // @SuppressWarnings("unchecked")
   // @Test
   // public void testImageNotFoundAndLazySucceeds() throws UnknownHostException {
   // EC2Client client = createMock(EC2Client.class);
   // AMIClient amiClient = createMock(AMIClient.class);
   // Map<RegionAndName, KeyPair> credentialsMap = createMock(Map.class);
   // org.jclouds.compute.domain.Image jcImage = createMock(org.jclouds.compute.domain.Image.class);
   //
   // ConcurrentMap<RegionAndName, org.jclouds.compute.domain.Image> imageMap =
   // createMock(ConcurrentMap.class);
   //
   // Location location = new LocationImpl(LocationScope.ZONE, "us-east-1a", "description", null);
   // @Memoized Supplier<Set<? extends Location>> locations = Suppliers.<Set<? extends Location>>
   // ofInstance(ImmutableSet
   // .<Location> of(location));
   // @Memoized Supplier<Set<? extends Hardware>> hardwares = Suppliers.<Set<? extends Hardware>>
   // ofInstance(ImmutableSet
   // .<Hardware> of(m2_4xlarge().build()));
   // PopulateDefaultLoginCredentialsForImageStrategy credentialProvider =
   // createMock(PopulateDefaultLoginCredentialsForImageStrategy.class);
   // RunningInstance instance = createMock(RunningInstance.class);
   //
   // expect(instance.getId()).andReturn("id").atLeastOnce();
   // expect(instance.getGroupIds()).andReturn(ImmutableSet.<String> of()).atLeastOnce();
   // expect(instance.getKeyName()).andReturn(null).atLeastOnce();
   // expect(instance.getInstanceState()).andReturn(InstanceState.RUNNING);
   //
   // expect(instance.getIpAddress()).andReturn("127.0.0.1");
   // expect(instance.getPrivateIpAddress()).andReturn("127.0.0.1");
   //
   // expect(instance.getAvailabilityZone()).andReturn(AvailabilityZone.US_EAST_1A).atLeastOnce();
   //
   // expect(instance.getImageId()).andReturn("imageId").atLeastOnce();
   // expect(instance.getRegion()).andReturn("us-east-1").atLeastOnce();
   //
   // org.jclouds.compute.domain.Image lateImage =
   // createMock(org.jclouds.compute.domain.Image.class);
   //
   // expect(imageMap.get(new RegionAndName("us-east-1",
   // "imageId"))).andReturn(lateImage).atLeastOnce();
   // expect(lateImage.getId()).andReturn("us-east-1/imageId").atLeastOnce();
   // expect(lateImage.getOperatingSystem()).andReturn(createMock(OperatingSystem.class)).atLeastOnce();
   //
   // expect(instance.getInstanceType()).andReturn(InstanceType.C1_XLARGE).atLeastOnce();
   //
   // replay(lateImage);
   // replay(imageMap);
   // replay(jcImage);
   // replay(client);
   // replay(amiClient);
   // replay(credentialsMap);
   // replay(credentialProvider);
   // replay(instance);
   //
   // Function<RunningInstance, NodeMetadata> parser = new RunningInstanceToNodeMetadata(client,
   // credentialsMap,
   // credentialProvider, imageMap, locations, hardwares);
   //
   // NodeMetadata metadata = parser.apply(instance);
   // assertEquals(metadata.getLocation(), locations.get().iterator().next());
   // assertEquals(metadata.getImageId(), lateImage.getId());
   // assertEquals(metadata.getTag(), "NOTAG-id");
   // assertEquals(metadata.getCredentials(), null);
   //
   // verify(lateImage);
   // verify(imageMap);
   // verify(jcImage);
   // verify(client);
   // verify(amiClient);
   // verify(credentialsMap);
   // verify(credentialProvider);
   // verify(instance);
   // }
   //
   // @SuppressWarnings("unchecked")
   // @Test
   // public void testApplyWithNoSecurityGroupCreatesTagOfIdPrefixedByTagAndNullCredentials() throws
   // UnknownHostException {
   // EC2Client client = createMock(EC2Client.class);
   // AMIClient amiClient = createMock(AMIClient.class);
   // Map<RegionAndName, KeyPair> credentialsMap = createMock(Map.class);
   // org.jclouds.compute.domain.Image jcImage = createMock(org.jclouds.compute.domain.Image.class);
   //
   // ConcurrentMap<RegionAndName, org.jclouds.compute.domain.Image> imageMap =
   // createMock(ConcurrentMap.class);
   //
   // Location location = new LocationImpl(LocationScope.ZONE, "us-east-1a", "description", null);
   // @Memoized Supplier<Set<? extends Location>> locations = Suppliers.<Set<? extends Location>>
   // ofInstance(ImmutableSet
   // .<Location> of(location));
   // @Memoized Supplier<Set<? extends Hardware>> hardwares = Suppliers.<Set<? extends Hardware>>
   // ofInstance(ImmutableSet
   // .<Hardware> of(m2_4xlarge().build()));
   // PopulateDefaultLoginCredentialsForImageStrategy credentialProvider =
   // createMock(PopulateDefaultLoginCredentialsForImageStrategy.class);
   // RunningInstance instance = createMock(RunningInstance.class);
   //
   // expect(instance.getId()).andReturn("id").atLeastOnce();
   // expect(instance.getRegion()).andReturn("us-east-1").atLeastOnce();
   // expect(instance.getGroupIds()).andReturn(ImmutableSet.<String> of()).atLeastOnce();
   // expect(instance.getKeyName()).andReturn(null).atLeastOnce();
   // expect(instance.getInstanceState()).andReturn(InstanceState.RUNNING);
   //
   // expect(instance.getIpAddress()).andReturn("127.0.0.1");
   // expect(instance.getPrivateIpAddress()).andReturn("127.0.0.1");
   //
   // expect(instance.getAvailabilityZone()).andReturn(AvailabilityZone.US_EAST_1A).atLeastOnce();
   //
   // expect(jcImage.getOperatingSystem()).andReturn(createMock(OperatingSystem.class)).atLeastOnce();
   //
   // expect(instance.getImageId()).andReturn("imageId").atLeastOnce();
   // expect(imageMap.get(new RegionAndName(Region.US_EAST_1, "imageId"))).andReturn(jcImage);
   //
   // expect(instance.getInstanceType()).andReturn(InstanceType.C1_XLARGE).atLeastOnce();
   //
   // replay(imageMap);
   // replay(jcImage);
   // replay(client);
   // replay(amiClient);
   // replay(credentialsMap);
   // replay(credentialProvider);
   // replay(instance);
   //
   // Function<RunningInstance, NodeMetadata> parser = new RunningInstanceToNodeMetadata(client,
   // credentialsMap,
   // credentialProvider, imageMap, locations, hardwares);
   //
   // NodeMetadata metadata = parser.apply(instance);
   // assertEquals(metadata.getLocation(), locations.get().iterator().next());
   // assertEquals(metadata.getImageId(), "us-east-1/imageId");
   // assertEquals(metadata.getTag(), "NOTAG-id");
   // assertEquals(metadata.getCredentials(), null);
   //
   // verify(imageMap);
   // verify(jcImage);
   // verify(client);
   // verify(amiClient);
   // verify(credentialsMap);
   // verify(credentialProvider);
   // verify(instance);
   // }
   //
   // @SuppressWarnings("unchecked")
   // @Test
   // public void testApplyWithNoKeyPairCreatesTagOfParsedSecurityGroupAndNullCredentials() throws
   // UnknownHostException {
   // EC2Client client = createMock(EC2Client.class);
   // AMIClient amiClient = createMock(AMIClient.class);
   // Map<RegionAndName, KeyPair> credentialsMap = createMock(Map.class);
   // org.jclouds.compute.domain.Image jcImage = createMock(org.jclouds.compute.domain.Image.class);
   // ConcurrentMap<RegionAndName, org.jclouds.compute.domain.Image> imageMap =
   // createMock(ConcurrentMap.class);
   //
   // Location location = new LocationImpl(LocationScope.ZONE, "us-east-1a", "description", null);
   // @Memoized Supplier<Set<? extends Location>> locations = Suppliers.<Set<? extends Location>>
   // ofInstance(ImmutableSet
   // .<Location> of(location));
   // @Memoized Supplier<Set<? extends Hardware>> hardwares = Suppliers.<Set<? extends Hardware>>
   // ofInstance(ImmutableSet
   // .<Hardware> of(m2_4xlarge().build()));
   // PopulateDefaultLoginCredentialsForImageStrategy credentialProvider =
   // createMock(PopulateDefaultLoginCredentialsForImageStrategy.class);
   // RunningInstance instance = createMock(RunningInstance.class);
   //
   // expect(instance.getId()).andReturn("id").atLeastOnce();
   // expect(instance.getRegion()).andReturn("us-east-1").atLeastOnce();
   // expect(instance.getGroupIds()).andReturn(ImmutableSet.of("jclouds#tag#us-east-1")).atLeastOnce();
   // expect(instance.getKeyName()).andReturn(null).atLeastOnce();
   // expect(instance.getInstanceState()).andReturn(InstanceState.RUNNING);
   //
   // expect(instance.getIpAddress()).andReturn("127.0.0.1");
   // expect(instance.getPrivateIpAddress()).andReturn("127.0.0.1");
   //
   // expect(instance.getAvailabilityZone()).andReturn(AvailabilityZone.US_EAST_1A).atLeastOnce();
   //
   // expect(jcImage.getOperatingSystem()).andReturn(createMock(OperatingSystem.class)).atLeastOnce();
   //
   // expect(instance.getImageId()).andReturn("imageId").atLeastOnce();
   // expect(imageMap.get(new RegionAndName(Region.US_EAST_1, "imageId"))).andReturn(jcImage);
   //
   // expect(instance.getInstanceType()).andReturn(InstanceType.C1_XLARGE).atLeastOnce();
   //
   // replay(imageMap);
   // replay(jcImage);
   // replay(client);
   // replay(amiClient);
   // replay(credentialsMap);
   // replay(credentialProvider);
   // replay(instance);
   //
   // Function<RunningInstance, NodeMetadata> parser = new RunningInstanceToNodeMetadata(client,
   // credentialsMap,
   // credentialProvider, imageMap, locations, hardwares);
   //
   // NodeMetadata metadata = parser.apply(instance);
   // assertEquals(metadata.getLocation(), locations.get().iterator().next());
   // assertEquals(metadata.getImageId(), "us-east-1/imageId");
   // assertEquals(metadata.getTag(), "tag");
   // assertEquals(metadata.getCredentials(), null);
   //
   // verify(imageMap);
   // verify(jcImage);
   // verify(client);
   // verify(amiClient);
   // verify(credentialsMap);
   // verify(credentialProvider);
   // verify(instance);
   // }
   //
   // @SuppressWarnings({ "unchecked" })
   // @Test
   // public void testApplyWithKeyPairCreatesTagOfParsedSecurityGroupAndCredentialsBasedOnIt()
   // throws UnknownHostException {
   // EC2Client client = createMock(EC2Client.class);
   // AMIClient amiClient = createMock(AMIClient.class);
   // expect(client.getAMIServices()).andReturn(amiClient).atLeastOnce();
   // Map<RegionAndName, KeyPair> credentialsMap = createMock(Map.class);
   // ConcurrentMap<RegionAndName, org.jclouds.compute.domain.Image> imageMap =
   // createMock(ConcurrentMap.class);
   // @Memoized Supplier<Set<? extends Hardware>> hardwares = Suppliers.<Set<? extends Hardware>>
   // ofInstance(ImmutableSet
   // .<Hardware> of(m2_4xlarge().build()));
   // PopulateDefaultLoginCredentialsForImageStrategy credentialProvider =
   // createMock(PopulateDefaultLoginCredentialsForImageStrategy.class);
   // RunningInstance instance = createMock(RunningInstance.class);
   // Image image = createMock(Image.class);
   //
   // expect(instance.getId()).andReturn("id").atLeastOnce();
   // expect(instance.getGroupIds()).andReturn(ImmutableSet.of("jclouds#tag#us-east-1")).atLeastOnce();
   // expect(instance.getKeyName()).andReturn("jclouds#tag#us-east-1#50").atLeastOnce();
   // expect(instance.getInstanceState()).andReturn(InstanceState.RUNNING);
   //
   // Location location = new LocationImpl(LocationScope.ZONE, "us-east-1a", "description", null);
   // @Memoized Supplier<Set<? extends Location>> locations = Suppliers.<Set<? extends Location>>
   // ofInstance(ImmutableSet
   // .<Location> of(location));
   // org.jclouds.compute.domain.Image jcImage = createMock(org.jclouds.compute.domain.Image.class);
   //
   // expect(instance.getIpAddress()).andReturn("127.0.0.1");
   // expect(instance.getPrivateIpAddress()).andReturn("127.0.0.1");
   //
   // expect(instance.getRegion()).andReturn(Region.US_EAST_1).atLeastOnce();
   //
   // expect(jcImage.getOperatingSystem()).andReturn(createMock(OperatingSystem.class)).atLeastOnce();
   //
   // expect(instance.getImageId()).andReturn("imageId").atLeastOnce();
   // expect(imageMap.get(new RegionAndName(Region.US_EAST_1, "imageId"))).andReturn(jcImage);
   //
   // expect(amiClient.describeImagesInRegion(Region.US_EAST_1, imageIds("imageId"))).andReturn(
   // (Set) ImmutableSet.<Image> of(image));
   //
   // expect(credentialProvider.execute(image)).andReturn(new Credentials("user", "pass"));
   //
   // expect(credentialsMap.get(new RegionAndName(Region.US_EAST_1,
   // "jclouds#tag#us-east-1#50"))).andReturn(
   // new KeyPair(Region.US_EAST_1, "jclouds#tag#us-east-1#50", "keyFingerprint", "pass"));
   //
   // expect(instance.getAvailabilityZone()).andReturn(AvailabilityZone.US_EAST_1A).atLeastOnce();
   //
   // expect(instance.getInstanceType()).andReturn(InstanceType.C1_XLARGE).atLeastOnce();
   //
   // replay(imageMap);
   // replay(client);
   // replay(amiClient);
   // replay(credentialsMap);
   // replay(credentialProvider);
   // replay(instance);
   // replay(jcImage);
   //
   // Function<RunningInstance, NodeMetadata> parser = new RunningInstanceToNodeMetadata(client,
   // credentialsMap,
   // credentialProvider, imageMap, locations, hardwares);
   // NodeMetadata metadata = parser.apply(instance);
   //
   // assertEquals(metadata.getTag(), "tag");
   // assertEquals(metadata.getLocation(), location);
   // assertEquals(metadata.getImageId(), "us-east-1/imageId");
   //
   // assertEquals(metadata.getCredentials(), new Credentials("user", "pass"));
   //
   // verify(imageMap);
   // verify(jcImage);
   // verify(client);
   // verify(amiClient);
   // verify(credentialsMap);
   // verify(credentialProvider);
   // verify(instance);
   //
   // }
   //
   // @SuppressWarnings({ "unchecked" })
   // @Test
   // public void testApplyWithTwoSecurityGroups() throws UnknownHostException {
   // EC2Client client = createMock(EC2Client.class);
   // AMIClient amiClient = createMock(AMIClient.class);
   // expect(client.getAMIServices()).andReturn(amiClient).atLeastOnce();
   // Map<RegionAndName, KeyPair> credentialsMap = createMock(Map.class);
   // ConcurrentMap<RegionAndName, org.jclouds.compute.domain.Image> imageMap =
   // createMock(ConcurrentMap.class);
   // @Memoized Supplier<Set<? extends Hardware>> hardwares = Suppliers.<Set<? extends Hardware>>
   // ofInstance(ImmutableSet
   // .<Hardware> of(m2_4xlarge().build()));
   // PopulateDefaultLoginCredentialsForImageStrategy credentialProvider =
   // createMock(PopulateDefaultLoginCredentialsForImageStrategy.class);
   // RunningInstance instance = createMock(RunningInstance.class);
   // Image image = createMock(Image.class);
   //
   // expect(instance.getId()).andReturn("id").atLeastOnce();
   // expect(instance.getGroupIds()).andReturn(ImmutableSet.of("jclouds1",
   // "jclouds2")).atLeastOnce();
   // expect(instance.getKeyName()).andReturn("jclouds#tag#us-east-1#50").atLeastOnce();
   // expect(instance.getInstanceState()).andReturn(InstanceState.RUNNING);
   //
   // Location location = new LocationImpl(LocationScope.ZONE, "us-east-1a", "description", null);
   // @Memoized Supplier<Set<? extends Location>> locations = Suppliers.<Set<? extends Location>>
   // ofInstance(ImmutableSet
   // .<Location> of(location));
   // org.jclouds.compute.domain.Image jcImage = createMock(org.jclouds.compute.domain.Image.class);
   //
   // expect(instance.getIpAddress()).andReturn("127.0.0.1");
   // expect(instance.getPrivateIpAddress()).andReturn("127.0.0.1");
   //
   // expect(instance.getRegion()).andReturn(Region.US_EAST_1).atLeastOnce();
   //
   // expect(jcImage.getOperatingSystem()).andReturn(createMock(OperatingSystem.class)).atLeastOnce();
   //
   // expect(instance.getImageId()).andReturn("imageId").atLeastOnce();
   // expect(imageMap.get(new RegionAndName(Region.US_EAST_1, "imageId"))).andReturn(jcImage);
   //
   // expect(amiClient.describeImagesInRegion(Region.US_EAST_1, imageIds("imageId"))).andReturn(
   // (Set) ImmutableSet.<Image> of(image));
   //
   // expect(credentialProvider.execute(image)).andReturn(new Credentials("user", "pass"));
   //
   // expect(credentialsMap.get(new RegionAndName(Region.US_EAST_1,
   // "jclouds#tag#us-east-1#50"))).andReturn(
   // new KeyPair(Region.US_EAST_1, "jclouds#tag#us-east-1#50", "keyFingerprint", "pass"));
   //
   // expect(instance.getAvailabilityZone()).andReturn(AvailabilityZone.US_EAST_1A).atLeastOnce();
   //
   // expect(instance.getInstanceType()).andReturn(InstanceType.C1_XLARGE).atLeastOnce();
   //
   // replay(imageMap);
   // replay(client);
   // replay(amiClient);
   // replay(credentialsMap);
   // replay(credentialProvider);
   // replay(instance);
   // replay(jcImage);
   //
   // Function<RunningInstance, NodeMetadata> parser = new RunningInstanceToNodeMetadata(client,
   // credentialsMap,
   // credentialProvider, imageMap, locations, hardwares);
   //
   // NodeMetadata metadata = parser.apply(instance);
   //
   // assertEquals(metadata.getTag(), "NOTAG-id");
   // assertEquals(metadata.getLocation(), location);
   // assertEquals(metadata.getImageId(), "us-east-1/imageId");
   //
   // assertEquals(metadata.getCredentials(), new Credentials("user", "pass"));
   //
   // verify(imageMap);
   // verify(jcImage);
   // verify(client);
   // verify(amiClient);
   // verify(credentialsMap);
   // verify(credentialProvider);
   // verify(instance);
   //
   // }
}
