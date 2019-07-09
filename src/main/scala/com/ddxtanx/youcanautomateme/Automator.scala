package com.ddxtanx.youcanautomateme

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import cats.data.Chain
import cats.{Monad, MonadError}
import cats.implicits._
import org.http4s.{EntityDecoder, Headers, UrlForm}
import org.http4s.client.Client
import org.http4s.headers.{AgentProduct, Host, `User-Agent`}
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.model.Element

class Automator[F[_]: Monad](link: String, client: Client[F], nextButtonClass: String = ".gridNext")
                            (implicit d: EntityDecoder[F, String],
                             e: MonadError[F, Throwable]){
  private val browser: Browser[F] = new Browser[F](client)
  val defaultHeaders: Headers = Headers.of(
    Host(link.replace("https://", "").replace("/", "")),
    `User-Agent`(AgentProduct("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.14; rv:67.0) Gecko/20100101 Firefox/67.0"))
  )
  val defDateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
  val bookUrlPart = "service/jsps/book.jsp"
  val calUrlPart = "service/jsps/cal.jsp"
  private def dropNone[A](l: List[Option[A]]): List[A] = {
    def accumulat(acc: List[A], cl: List[Option[A]]): List[A] = {
      if(cl.isEmpty) acc
      else cl.head match{
        case None => accumulat(acc, cl.tail)
        case Some(a) => accumulat(acc ++ List(a), cl.tail)
      }
    }
    accumulat(List(), l)
  }
  private def getSlotSpecWeek(startDate: LocalDate, cal: String, ini: String, time: Time): F[Option[String]] = {
    val url = s"$link/$calUrlPart?cal=$cal&ini=$ini&jumpDate=${defDateFormat.format(startDate)}"
    val mondayMillis: Long = startDate.toEpochDay * 24 * 60 * 60 * 1000
    val correction: Long = 18000000
    val youcanbookMondayMillis = mondayMillis + correction
    // Youcanbook seems to be 18,000,000 milliseconds ahead for some reason
    val timeMillis: Long = {
      val ampmMillis = if(!time.ampm) 12 * 60 * 60 * 1000 else 0
      val dayMillis = time.dotw * 24 * 60 * 60 * 1000
      val hrMillis = time.hr * 60 * 60 * 1000
      val minMillis = time.min * 60 * 1000
      dayMillis + youcanbookMondayMillis + ampmMillis + hrMillis + minMillis
    }
    for{
      page <- browser.getPage(url, defaultHeaders, Map())
    } yield{
      //println(page)
      val htmlId: String = s"#grid$timeMillis"
      //println(htmlId)
      val elem: Element = page >> element(htmlId)
      val busy = elem.attr("class").contains("gridBusy")
      //println(s"$startDate $time $busy")
      if(busy) None else Some(page >> attr("href")(s"#button-$timeMillis"))
    }
  }
  private def getTimeSlotLinks(t: Time, weeksToDo: Int = 4): F[List[String]] = {
    for{
      page <- browser.getPage(link, defaultHeaders, Map())
      cal = page >> attr("value")("input[name=cal]")
      ini = page >> attr("value")("input[name=ini]")
      startNextWeek: String = {
        val a: Element = (page >> elementList(nextButtonClass)).head
        a.attr("href").replaceAll(".*&jumpDate=", "")
      }
      shifts = Stream.from(-7, 7).take(weeksToDo).toList
      nextWeekDate: LocalDate = LocalDate.parse(startNextWeek, defDateFormat)
      dates: List[LocalDate] = shifts.map(nextWeekDate.plusDays(_))
      listSlots: List[F[Option[String]]] = dates.map(getSlotSpecWeek(_, cal, ini, t))
      slotList <- listSlots.sequence
    } yield{
      dropNone(slotList).map(endLink => s"$link$endLink")
    }
  }

  private def reserveGuestForSlots(guest: Guest, t: Time): F[Unit] = {
    def reserveForSlot(book_link: String): F[Unit] = {
      for{
        page <- browser.getPage(book_link, defaultHeaders, Map())
        hiddenInputs: List[Element] = page >> elementList("input[type=hidden]")
        inputsMap: Map[String, Chain[String]] = hiddenInputs.map(
          e => e.attr("name") -> Chain(e.attr("value"))
        ).toMap
        guestDataMap: Map[String, Chain[String]] = guest.toMap.mapValues(Chain(_))
        allDataMap: Map[String, Chain[String]] = inputsMap ++ guestDataMap
        allData: UrlForm = UrlForm(allDataMap)
        _ <- browser.post(link + "/Book.do", defaultHeaders, Map(), allData)
      } yield ()
    }
    for{
      reqs <- getTimeSlotLinks(t)
      resps <- reqs.traverse(reserveForSlot)
    } yield ()
  }

  def automateReservation(r: Reservation): F[Unit] = {
    val times: List[Time] = r.times
    for{
      resps <- times.traverse(reserveGuestForSlots(r.guest, _))
    } yield ()
  }

  def automateReservations(lr: List[Reservation]): F[Unit] = {
    for{
      resps <- lr.traverse(automateReservation)
    } yield ()
  }
}
