package com.ddxtanx.youcanautomateme.instances

import com.ddxtanx.youcanautomateme.{Automator, Guest}
import org.http4s.client.Client

trait Booking[F[_]] {
  val apiBuilder: Client[F] => Automator[F]
  //A Booking Instance must be able to build an automator from a client
  abstract class GuestInst extends Guest
  //And it must have a suitable definition of a 'Guest' or entity that books appointments.
}
