package io.fatkhuranonym.nuchainnode.data

import android.os.Parcel
import android.os.Parcelable

enum class ServiceAction {
    START,
    STOP
}

enum class NodeAction: Parcelable {
    START,
    STOP,
    RESTART;

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(ordinal)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR: Parcelable.Creator<NodeStatus?> {
        override fun createFromParcel(input: Parcel): NodeStatus? {
            return NodeStatus.values()[input.readInt()]
        }

        override fun newArray(size: Int): Array<NodeStatus?> {
            return arrayOfNulls(size)
        }
    }
}

enum class NodeStatus : Parcelable {
    IDLE,
    RUNNING,
    STOPPED;

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(ordinal)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR: Parcelable.Creator<NodeStatus?> {
        override fun createFromParcel(input: Parcel): NodeStatus? {
            return values()[input.readInt()]
        }

        override fun newArray(size: Int): Array<NodeStatus?> {
            return arrayOfNulls(size)
        }
    }
}

data class Node(
    val status: NodeStatus = NodeStatus.IDLE,
    val name: String = "",
    val output: String = ""
) :
    Parcelable {
    constructor(parcel: Parcel) : this(
        status = parcel.readParcelable(NodeStatus::class.java.classLoader)!!,
        name = parcel.readString()!!,
        output = parcel.readString()!!,
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(status, flags)
        parcel.writeString(name)
        parcel.writeString(output)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Node> {
        override fun createFromParcel(parcel: Parcel): Node {
            return Node(parcel)
        }

        override fun newArray(size: Int): Array<Node?> {
            return arrayOfNulls(size)
        }
    }

}