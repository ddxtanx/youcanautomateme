package com.ddxtanx.youcanautomateme

import cats.{Monad, MonadError}
import cats.data.NonEmptyList
import cats.implicits._
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.model.Document
import org.http4s._
import org.http4s.client._
import org.http4s.headers._
import org.http4s.Method._

protected class Browser[F[_]: Monad](client: Client[F])
                          (implicit d: EntityDecoder[F, String],
                           e: MonadError[F, Throwable]){
  case class Resp(body: String, headers: Headers)
  private def cookiesWithHeaders(headers: Headers, cookies: Map[String, String]): Headers = {
    val newHeaders: Headers = {
      if(cookies.isEmpty) headers
      else{
        val cookieList: List[RequestCookie] = cookies.map{case (k,v) => RequestCookie(name=k,content=v)}(collection.breakOut)
        val cookieObj: Header = new Cookie(NonEmptyList.fromListUnsafe(cookieList)) //Guarenteed to be safe bec of non empty check above
        headers ++ Headers.of(cookieObj)
      }
    }
    newHeaders
  }

  private def getReq(url: String, headers: Headers, cookies: Map[String, String]): Request[F] = {
    val newHeaders = cookiesWithHeaders(headers, cookies)
    Request[F](GET, Uri.unsafeFromString(url)).withHeaders(newHeaders)
  }

  private def perfReq(r: Request[F]): F[Resp] = {
    //println(s"performing request $r")
    client.fetch(r)(
      (resp: Response[F]) => resp.as[String].map(
        body => Resp(body, resp.headers)
      )
    )
  }
  def get(url: String, headers: Headers, cookies: Map[String, String]): F[Resp] = {
    val req = getReq(url, headers, cookies)
    perfReq(req)
  }

  def getPage(url: String, headers: Headers, cookies: Map[String, String]): F[Document] = {
    for{
      resp <- get(url, headers, cookies)
    } yield {
      new JsoupBrowser().parseString(resp.body)
    }
  }

  def post(url: String, headers: Headers, cookies: Map[String, String], data: UrlForm): F[Resp] = {
    val cookieHead = cookiesWithHeaders(headers, cookies)
    val req: Request[F] = Request[F](POST, Uri.unsafeFromString(url)).withHeaders(cookieHead).withEntity(data)
    perfReq(req)
  }
}
