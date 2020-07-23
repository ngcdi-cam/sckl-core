package org.ngcdi.serializer

import akka.serialization.SerializerWithStringManifest
import org.ngcdi.sckl.msgs._


class Serializer extends SerializerWithStringManifest{

  def identifier: Int = 101110116

  override def manifest(o: AnyRef): String = o.getClass.getName
  final val SMRegistrationManifest = classOf[SMRegistration].getName
  final val NewInfrastructureManifest = classOf[NewInfrastructure].getName
  final val SenseManifest = classOf[Sense].getName
  final val InfrastructureReadyManifest = classOf[InfrastructureReady].getName
  final val MeasurementManifest = classOf[Measurement].getName
  final val AggregateLocalViewManifest = classOf[AggregateLocalView].getName

  override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef = {

    manifest match {
      case SMRegistrationManifest => SMRegistration.parseFrom(bytes)
      case NewInfrastructureManifest => NewInfrastructure.parseFrom(bytes)
      case SenseManifest => Sense.parseFrom(bytes)
      case InfrastructureReadyManifest => InfrastructureReady.parseFrom(bytes)
      case MeasurementManifest => Measurement.parseFrom(bytes)
      case AggregateLocalViewManifest => AggregateLocalView.parseFrom(bytes)
     }
  }

  override def toBinary(o: AnyRef): Array[Byte] = {
    o match {
      case a: SMRegistration => a.toByteArray
      case a: NewInfrastructure => a.toByteArray
      case a: Sense => a.toByteArray
      case a: InfrastructureReady => a.toByteArray
      //case a: Measurement => a.toByteArray
      case a: AggregateLocalView => a.toByteArray
    }
  }
}
