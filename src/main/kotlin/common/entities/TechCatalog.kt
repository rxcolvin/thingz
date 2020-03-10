package common.entities.techcatalog

import common.entities.*
import common.lang.Ref
import common.meta.*


val idField = Field("id", StringType)
val nameField = Field("name", StringType)

interface Element {
    val id: String
}


private interface App__ : Element {
    val name: String
}


data class App(
    override val id: String,
    override val name: String
) : Element, App__

data class App_(
    override var id: String,
    override var name: String
) : App__


private interface Assoc__<FROM : Element, TO : Element> : Element {
    val from: FROM?
    val to: TO?
}

data class Cluster (
    override val id: String

) : Element

data class DeployedApp (
    override val id: String,
    override val from: Cluster?,
    override val to: App?
) : Element, Assoc__<Cluster, App>

data class DeployedApp_ (
    override var id: String,
    override var from: Cluster?,
    override var to: App?
) : Element, Assoc__<Cluster, App>



