package eventstore
package cluster

import java.net.{ InetSocketAddress, Inet6Address }
import java.util.Date
import akka.actor.ActorSystem

import scala.concurrent._

case class ClusterInfo(serverAddress: InetSocketAddress, members: List[MemberInfo] /*WOULD BE NICE TO HAVE IT SORTED*/ ) {
  // TODO test
  lazy val bestNode: Option[MemberInfo] = {
    val xs = members.filter(x => x.isAlive && x.state.isAllowedToConnect)
    if (xs.isEmpty) None else Some(xs.maxBy(_.state))
  }
}

object ClusterInfo {

  type FutureFunc = InetSocketAddress => Future[ClusterInfo]

  def futureFunc(implicit system: ActorSystem): FutureFunc = {
    import ClusterProtocol._
    import system.dispatcher
    import spray.client.pipelining._
    import spray.http._
    import spray.httpx.SprayJsonSupport._

    val pipeline: HttpRequest => Future[ClusterInfo] = sendReceive ~> unmarshal[ClusterInfo]
    (address: InetSocketAddress) => {
      // TODO what is a proper way to convert InetSocketAddress to Uri
      val host = address.getAddress match {
        case address: Inet6Address => s"[${address.getHostAddress}]"
        case address               => address.getHostAddress
      }
      val port = address.getPort
      val uri = Uri(s"http://$host:$port/gossip?format=json")

      pipeline(Get(uri))
    }
  }
}

case class MemberInfo(
    instanceId: Uuid,
    timestamp: Date,
    state: NodeState,
    isAlive: Boolean,
    internalTcp: InetSocketAddress,
    externalTcp: InetSocketAddress,
    internalSecureTcp: InetSocketAddress,
    externalSecureTcp: InetSocketAddress,
    internalHttp: InetSocketAddress,
    externalHttp: InetSocketAddress,
    lastCommitPosition: Long,
    writerCheckpoint: Long,
    chaserCheckpoint: Long,
    epochPosition: Long,
    epochNumber: Int,
    epochId: Uuid,
    nodePriority: Int) extends Ordered[MemberInfo] {

  def compare(that: MemberInfo) = this.state compare that.state

  // TODO discuss with @gregoryyoung
  def like(other: MemberInfo): Boolean = this.instanceId == other.instanceId

  // TODO
  override def toString = s"MemberInfo($instanceId, $state)"
}