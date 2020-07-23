package org.slack


final case class Challenge(token:String,challenge:String,typeu:String)
final case class ResponseChallenge(challenge:String)

final case class Event(text:String)
final case class EventPayload(
  //token:String,
  //teamId:String,
  //apiAppId:String,
  //typee:String,
  //eventId:String,
  //eventTime:Long,
  event:Event
  //  ,authedUsers:Seq[String]
)
