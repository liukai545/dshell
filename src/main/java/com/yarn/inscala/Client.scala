package com.yarn.inscala

import java.util.Collections

import Utils._
import org.apache.hadoop.fs.Path
import org.apache.hadoop.yarn.api.ApplicationConstants
import org.apache.hadoop.yarn.api.records.{ContainerLaunchContext, _}
import org.apache.hadoop.yarn.client.api.YarnClient
import org.apache.hadoop.yarn.conf.YarnConfiguration
import org.apache.hadoop.yarn.util.Records

import scala.collection.JavaConverters._

/**
  * Created by liukai on 2016/1/8.
  */
object Client {

  def main(args: Array[String]) {

    implicit val conf = new YarnConfiguration()
    val jarPath = args(0)
    val numberOfInstances = args(1).toInt

    // start a yarn client
    val client = YarnClient.createYarnClient()
    client.init(conf)
    client.start()

    // application creation
    val app = client.createApplication()
    val amContainer = Records.newRecord(classOf[ContainerLaunchContext])
    //application master is a just java program with given commands
    amContainer.setCommands(List(
      "$JAVA_HOME/bin/java" +
        " -Xmx256M" +
        " com.liu.simplejava.ApplicationMaster" +
        "  " + jarPath + "   " + numberOfInstances + " " +
        " 1>" + ApplicationConstants.LOG_DIR_EXPANSION_VAR + "/stdout" +
        " 2>" + ApplicationConstants.LOG_DIR_EXPANSION_VAR + "/stderr"
    ).asJava)


    //add the jar which contains the Application master code to classpath
    val appMasterJar = Records.newRecord(classOf[LocalResource])
    setUpLocalResource(new Path(jarPath), appMasterJar)
    amContainer.setLocalResources(Collections.singletonMap("helloworld.jar", appMasterJar))

    //setup env to get all yarn and hadoop classes in classpath
    val env = collection.mutable.Map[String, String]()
    setUpEnv(env)
    amContainer.setEnvironment(env.asJava)

    //specify resource requirements
    val resource = Records.newRecord(classOf[Resource])
    resource.setMemory(300)
    resource.setVirtualCores(1)

    //context to launch
    val appContext = app.getApplicationSubmissionContext
    appContext.setApplicationName("helloworld")
    appContext.setAMContainerSpec(amContainer)
    appContext.setResource(resource)
    appContext.setQueue("default")

    //submit the application
    val appId = appContext.getApplicationId
    println("submitting application id" + appId)
    client.submitApplication(appContext)
  }

}