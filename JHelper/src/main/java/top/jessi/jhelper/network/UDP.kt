package top.jessi.jhelper.network

import top.jessi.jhelper.util.Functions
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.charset.StandardCharsets

/**
 * Created by Jessi on 2023/3/14 16:03
 * Email：17324719944@189.cn
 * Describe：UDP协议
 */
class UDP {

    private var mIsRunning = false

    /**
     * 使用udp发送数据
     *
     * @param ip   发送ip
     * @param port 发送端口
     * @param data 数据
     */
    fun send(ip: String, port: Int, data: String) {
        if (Functions.inMainThread()) {
            throw UnsupportedOperationException("This method does not support operation on the main thread!")
        }
        var socket: DatagramSocket? = null
        try {
            val buffer = data.toByteArray()
            val inetAddress = InetAddress.getByName(ip)
            val datagramPacket = DatagramPacket(buffer, buffer.size, inetAddress, port)
            socket = DatagramSocket()
            socket.sendBufferSize = 4096
            socket.send(datagramPacket)
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            socket?.apply {
                disconnect()
                if (!isClosed) close()
            }
        }
    }

    /**
     * 从udp获取数据
     *
     * @param port     接收端口
     * @param listener 数据接收监听
     */
    fun receive(port: Int, listener: Listener) {
        if (Functions.inMainThread()) {
            throw UnsupportedOperationException("This method does not support operation on the main thread!")
        }
        mIsRunning = true
        var receiveData: String
        var socket: DatagramSocket? = null
        try {
            // 创建并绑定到指定端口的 DatagramSocket
            socket = DatagramSocket(port)
            // 端口复用 -- 当服务关掉立马重启时，很多时候会提示端口仍被占用（因端口上有处于TIME_WAIT的连接
            // 设置true 可以使得服务在关闭后重启时能够立即使用该端口，而不是等待60秒的TIME_WAIT时间‌
            socket.reuseAddress = true
            // 创建一个缓冲区来接收数据
            val buffer = ByteArray(4096)
            socket.receiveBufferSize = buffer.size
            while (mIsRunning) {
                val packet = DatagramPacket(buffer, buffer.size)
                // 接收数据
                socket.receive(packet)
                // 获取接收到的数据
                receiveData = String(packet.data, 0, packet.length, StandardCharsets.UTF_8)
                packet.address.hostAddress?.let { listener.onData(it, packet.port, receiveData) }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            // 当停止接收数据的时候一定要关闭socket
            socket?.apply {
                disconnect()
                if (!isClosed) close()
            }
        }
    }

    fun destroy() {
        mIsRunning = false
    }

    interface Listener {
        fun onData(ip: String, port: Int, receiveData: String)
    }

}