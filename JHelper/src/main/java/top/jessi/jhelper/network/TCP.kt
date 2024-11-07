package top.jessi.jhelper.network

import android.util.Log
import top.jessi.jhelper.util.Functions
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket
import java.net.UnknownHostException
import java.nio.charset.StandardCharsets

/**
 * Created by Jessi on 2023/3/15 11:37
 * Email：17324719944@189.cn
 * Describe：TCP协议
 */
class TCP {

    private var mIsRunning = false

    /**
     * 使用TCP发送数据
     *
     * @param ip                 接收端ip
     * @param port               接收端端口
     * @param data               发送数据
     * @param replyListener 服务端回复监听
     */
    fun send(ip: String, port: Int, data: String, replyListener: Listener) {
        if (Functions.inMainThread()) {
            throw UnsupportedOperationException("This method does not support operation on the main thread!")
        }
        var socket: Socket? = null
        var reader: BufferedReader? = null
        try {
            socket = Socket(ip, port)
            // 设置15秒之后即认为是超时
            socket.soTimeout = 15000
            /*发送数据给服务端*/
            socket.getOutputStream().write(data.toByteArray(StandardCharsets.UTF_8))
            socket.shutdownOutput()
            // 读取服务端响应数据
            reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val line = reader.readLine()
            replyListener.onData(line)
        } catch (e: UnknownHostException) {
            e.printStackTrace()
            Log.w("TCP", "UnknownHost --> $ip:$port")
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            // 当停止接收数据的时候一定要关闭socket
            if (socket != null && !socket.isClosed) socket.close()
            reader?.close()
        }
    }

    fun receive(port: Int, receiveListener: Listener) {
        if (Functions.inMainThread()) {
            throw java.lang.UnsupportedOperationException("This method does not support operation on the main thread!")
        }
        mIsRunning = true
        var serverSocket: ServerSocket? = null
        var socket: Socket? = null
        try {
            // 创建一个ServerSocket,用于监听客户端socket的连接请求
            serverSocket = ServerSocket(port)
            /*采用循环不断接受来自客户端的请求,服务器端也对应产生一个Socket*/
            while (mIsRunning) {
                socket = serverSocket.accept()
                // 接收客户端消息
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                val dataFromClient = reader.readLine()
                receiveListener.onData(dataFromClient)
                val serverCallbackData = "TCP Server Callback your data --> $dataFromClient"
                socket.getOutputStream().write(serverCallbackData.toByteArray(StandardCharsets.UTF_8))
                socket.shutdownOutput()
                socket.close()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            // 当停止接收数据的时候一定要关闭socket
            if (serverSocket != null && !serverSocket.isClosed) serverSocket.close()
            if (socket != null && !socket.isClosed) socket.close()
        }
    }

    fun destroy() {
        mIsRunning = false
    }

    interface Listener {
        fun onData(data: String)
    }

}