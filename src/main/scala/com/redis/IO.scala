package com.redis

import java.io._
import java.net.Socket

trait IO extends Log {
  val host: String
  val port: Int

  var socket: Socket = null
  var out: OutputStream = null
  var in: BufferedReader = null
  var db: Int = 0

  def getOutputStream = out
  def getInputStream = in
  def getSocket = socket

  def connected = { getSocket != null }
  def reconnect = { disconnect && connect }

  // Connects the socket, and sets the input and output streams.
  def connect: Boolean = {
    try {
      socket = new Socket(host, port)
      socket.setKeepAlive(true)
      out = getSocket.getOutputStream
      in = new BufferedReader(
             new InputStreamReader(getSocket.getInputStream))
      true
    } catch {
      case x => 
        clearFd
        throw new RuntimeException(x)
    }
  }
  
  // Disconnects the socket.
  def disconnect: Boolean = {
    try {
      socket.close
      out.close
      in.close
      clearFd
      true
    } catch {
      case x => 
        false
    }
  }
  
  def clearFd = {
    socket = null
    out = null
    in = null
  }

   // Wraper for the socket write operation.
  def write_to_socket(data: String)(op: OutputStream => Unit) = op(getOutputStream)
  
  // Writes data to a socket using the specified block.
  def write(data: String) = {
    debug("C: " + data)
    if(!connected) connect;
    write_to_socket(data){
      getSocket =>
        try {
          getSocket.write(data.getBytes)
        } catch {
          case x => 
            reconnect;
        }
    }
  }

  def readLine: String = {
    try {
      if(!connected) connect;
      val str = getInputStream.readLine
      debug("S: " + str)
      str
    } catch {
      case x => throw new RuntimeException(x)
    }
  }

  def readCounted(count: Int): String = {
    try {
      if(!connected) connect;
      val car = new Array[Char](count)
      getInputStream.read(car, 0, count)
      car.mkString
    } catch {
      case x => throw new RuntimeException(x)
    }
  }
}
