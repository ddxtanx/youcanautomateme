package com.ddxtanx.youcanautomateme

trait Guest{
  val toMap: Map[String, String]
  //Guests must have some way of turning into a map so a UrlForm can be constructed
  //out of them.
}

case class Time(dotw: Int, hr: Int, min: Int, ampm: Boolean) {
  private val timeStr: String = {
    val padMin =
      if(min < 10) "0" + min.toString
      else min.toString
    val ampms = if(ampm) "AM" else "PM"
    s"$hr:$padMin $ampms"
  }

  private val dateMap: Map[Int, String] = Map(
    0 -> "Monday",
    1 -> "Tuesday",
    2 -> "Wednesday",
    3 -> "Thursday",
    4 -> "Friday",
    5 -> "Saturday",
    6 -> "Sunday"
  )
  override def toString: String = s"${dateMap.getOrElse(dotw, "Bad Date")} $timeStr"
}
//dotw is day of the week, Monday = 0, Tues = 1, etc
//ampm is true for am, false for pm

case class Reservation(guest: Guest, times: List[Time])

