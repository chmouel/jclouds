package org.jclouds.nirvanix.sdn;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.testng.Assert.*;

import java.net.URI;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jclouds.blobstore.domain.Blob;
import org.jclouds.blobstore.domain.BlobMetadata;
import org.jclouds.blobstore.integration.internal.BaseBlobStoreIntegrationTest;
import org.jclouds.logging.log4j.config.Log4JLoggingModule;
import org.jclouds.nirvanix.sdn.domain.UploadInfo;
import org.testng.annotations.BeforeGroups;
import org.testng.annotations.Test;

/**
 * Tests behavior of {@code SDNConnection}
 * 
 * @author Adrian Cole
 */
@Test(groups = "live", sequential = true, testName = "sdn.SDNConnectionLiveTest")
public class SDNConnectionLiveTest {

   protected SDNConnection connection;
   private String containerPrefix = BaseBlobStoreIntegrationTest.CONTAINER_PREFIX;

   URI container1;
   URI container2;

   @BeforeGroups(groups = { "live" })
   public void setupConnection() {
      String appname = checkNotNull(System.getProperty("jclouds.test.appname"),
               "jclouds.test.appname");
      String appid = checkNotNull(System.getProperty("jclouds.test.appid"), "jclouds.test.appid");
      String user = checkNotNull(System.getProperty("jclouds.test.user"), "jclouds.test.user");
      String password = checkNotNull(System.getProperty("jclouds.test.key"), "jclouds.test.key");

      connection = new SDNContextBuilder(appid, appname, user, password).withModules(
               new Log4JLoggingModule()).buildContext().getApi();
   }

   public void testUploadToken() throws InterruptedException, ExecutionException, TimeoutException {
      String containerName = containerPrefix + ".testObjectOperations";
      long size = 1024;
      UploadInfo uploadInfo = connection.getStorageNode(containerName, size);
      assertNotNull(uploadInfo.getHost());
      assertNotNull(uploadInfo.getToken());

      connection.upload(uploadInfo.getHost(), uploadInfo.getToken(), containerName,
               new Blob<BlobMetadata>("test.txt", "value")).get(30, TimeUnit.SECONDS);

      String metadataS = connection.getMetadata(containerName + "/test.txt").get(30,
               TimeUnit.SECONDS);
      System.err.println(metadataS);

      String content = connection.getFile(containerName + "/test.txt").get(30, TimeUnit.SECONDS);
      assertEquals(content, "value");

      // Multimap<String, String> metadata = ImmutableMultimap.of("chef", "sushi", "foo", "bar");
      // connection.setMetadata(containerName+"/test.txt", metadata).get(30, TimeUnit.SECONDS);

   }
}