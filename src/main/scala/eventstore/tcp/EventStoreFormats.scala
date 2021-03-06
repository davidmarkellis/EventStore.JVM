package eventstore
package tcp

import util.{ BytesWriter, BytesReader, BytesFormat }
import akka.util.{ ByteStringBuilder, ByteIterator }

object EventStoreFormats extends EventStoreFormats

trait EventStoreFormats extends EventStoreProtoFormats {
  abstract class EmptyFormat[T](obj: T) extends BytesFormat[T] {
    def read(bi: ByteIterator) = obj
    def write(x: T, builder: ByteStringBuilder) {}
  }

  implicit object HeartbeatRequestFormat extends EmptyFormat(HeartbeatRequest)
  implicit object HeartbeatResponseFormat extends EmptyFormat(HeartbeatResponse)
  implicit object PingFormat extends EmptyFormat(Ping)
  implicit object PongFormat extends EmptyFormat(Pong)
  implicit object UnsubscribeFromStreamFormat extends EmptyFormat(Unsubscribe)
  implicit object ScavengeDatabaseFormat extends EmptyFormat(ScavengeDatabase)
  implicit object AuthenticateFormat extends EmptyFormat(Authenticate)
  implicit object AuthenticatedFormat extends EmptyFormat(Authenticated)

  implicit object UserCredentialsFormat extends BytesFormat[UserCredentials] {
    def write(x: UserCredentials, builder: ByteStringBuilder) {
      def putString(s: String, name: String) {
        val bs = ByteString(s)
        val length = bs.length
        require(length < 256, s"$name serialized length should be less than 256 bytes, but is $length:$x")
        builder.putByte(bs.size.toByte)
        builder.append(bs)
      }
      putString(x.login, "login")
      putString(x.password, "password")
    }

    def read(bi: ByteIterator) = {
      def getString = {
        val length = bi.getByte
        val bytes = new Bytes(length)
        bi.getBytes(bytes)
        new String(bytes, "UTF-8")
      }
      val login = getString
      val password = getString
      UserCredentials(login, password)
    }
  }

  implicit object FlagsFormat extends BytesFormat[Flags] {
    def write(x: Flags, builder: ByteStringBuilder) {
      builder.putByte(x)
    }

    def read(bi: ByteIterator) = bi.getByte
  }

  implicit object PackOutOutWriter extends BytesWriter[PackOut] {
    def write(x: PackOut, builder: ByteStringBuilder) {
      val (writeMarker, writeMessage) = MarkerByte.writeMessage(x.message)
      writeMarker(builder)
      val credentials = x.credentials
      BytesWriter[Flags].write(Flags(credentials), builder)
      BytesWriter[Uuid].write(x.correlationId, builder)
      credentials.foreach(x => BytesWriter[UserCredentials].write(x, builder))
      writeMessage(builder)
    }
  }

  implicit object PackInReader extends BytesReader[PackIn] {
    def read(bi: ByteIterator): PackIn = {
      val readMessage = MarkerByte.readMessage(bi)

      val flags = BytesReader[Flags].read(bi)
      val correlationId = BytesReader[Uuid].read(bi)
      val message = readMessage(bi)

      PackIn(message, correlationId)
    }
  }
}