package eventstore
package operations

import eventstore.OperationError._
import eventstore.operations.Inspection.Decision.{ Retry, Stop, Fail }
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import scala.util.{ Failure, Success }

class TransactionCommitInspectionSpec extends Specification with Mockito {
  val inspection = TransactionCommitInspection(mock[TransactionCommit]).pf

  "TransactionCommitInspection" should {

    "handle TransactionCommitCompleted" in {
      inspection(Success(mock[TransactionCommitCompleted])) mustEqual Stop
    }

    "handle PrepareTimeout" in {
      inspection(Failure(PrepareTimeout)) mustEqual Retry
    }

    "handle CommitTimeout" in {
      inspection(Failure(CommitTimeout)) mustEqual Retry
    }

    "handle ForwardTimeout" in {
      inspection(Failure(ForwardTimeout)) mustEqual Retry
    }

    "handle WrongExpectedVersion" in {
      inspection(Failure(WrongExpectedVersion)) must beLike {
        case Fail(_: WrongExpectedVersionException) => ok
      }
    }

    "handle StreamDeleted" in {
      inspection(Failure(StreamDeleted)) must beLike {
        case Fail(_: StreamDeletedException) => ok
      }
    }

    "handle InvalidTransaction" in {
      inspection(Failure(InvalidTransaction)) mustEqual Fail(InvalidTransactionException)
    }

    "handle AccessDenied" in {
      inspection(Failure(AccessDenied)) must beLike {
        case Fail(_: AccessDeniedException) => ok
      }
    }
  }
}
