package module.MsgType;

import control.SeqPedido;
import module.Constantes;
import module.Exceptions.AckErrorException;
import module.Exceptions.PackageErrorException;
import module.Exceptions.TimeOutMsgException;
import module.MSG_interface;

import java.io.IOException;
import java.net.*;

public class HI implements MSG_interface {

  private int port;

  InetAddress clientIP; // coisas que nao estao a ser bem usadas

  Type type = Type.Hi;
  DatagramPacket packet;
  DatagramSocket socket;

  Byte seq = (byte) 0;
  SeqPedido seqPedido;

  boolean know_port;

  public HI(InetAddress clientIp, int port,DatagramSocket socket, SeqPedido seq) throws SocketException {
    //this.serverIP = serverIP;
    this.clientIP = clientIp;
    this.port = port;
    this.socket = socket;
    this.packet = null;
    this.seqPedido = seq;
    know_port = true;
    //this.serverSocket = new DatagramSocket();
  }

  public HI(DatagramPacket packet,int port,DatagramSocket socket, SeqPedido seq) throws SocketException {
    this.port = port;
    this.clientIP = packet.getAddress();
    this.packet = packet;
    this.socket = socket;
    this.seqPedido = seq;
    know_port = false;
    //this.serverSocket = new DatagramSocket();
  }

  public void setPort(int port) {
    this.port = port;
  }
  public void setSocket(DatagramSocket socket) {
    this.socket = socket;
  }
  @Override
  public DatagramPacket getPacket() {
    return packet;
  }

  @Override
  public int getPort() {
    return port;
  }

  @Override
  public Type getType() {
    return type;
  }

  public void createTailPacket(byte[] buff) {
    String msg = "HI";
    byte[] msgByte = msg.getBytes();

    for(int i= Constantes.CONFIG.HEAD_SIZE; i<msgByte.length;i++)
      buff[i] = msgByte[i-2];
  }

  //@Override
  public DatagramPacket createPacket(byte seq,byte seqSeg) {
    byte[] msg = createMsg(seq, seqSeg);
    return this.packet = new DatagramPacket(msg, msg.length, clientIP, port);
  }

  public boolean validType(DatagramPacket packet){
    byte[] msg = packet.getData();
    return  msg[0] == Type.Hi.getBytes();
  }

  public static boolean valid(DatagramPacket packet){
    byte[] msg = packet.getData();
    return  msg[0] == Type.Hi.getBytes();
  }

  public void send() throws IOException, PackageErrorException {

    var sendPackage = createPacket(seqPedido.getSeq(),seq);
    socket.send(sendPackage);
    seq++;

    byte[] buff = new byte[Constantes.CONFIG.BUFFER_SIZE];
    DatagramPacket receivedPacket = new DatagramPacket(buff, Constantes.CONFIG.BUFFER_SIZE);
    socket.setSoTimeout(2000); // prob eliminar e por isso no Communication

    boolean receveidPackage = false;
    while (!receveidPackage) {
      try {
        socket.receive(receivedPacket);
        if (!know_port)
          port = receivedPacket.getPort();

      } catch (IOException e) {
        throw new SocketTimeoutException("Não recebeu nada");
      }

      if (!validType(receivedPacket)){
        //TODO chamar controlo de fluxo
        // mais q 3 vezes e ele manda um package error
      } else {
        System.out.println("RECEBI: " + HI.toString(receivedPacket));
        ACK ack = new ACK(receivedPacket, port, socket, clientIP, seqPedido.getSeq());
        ack.send();
        receveidPackage = true;
      }
    }
  }

  public void send(DatagramSocket socket) throws IOException, SocketTimeoutException, PackageErrorException {

    var sendPackage = createPacket(seqPedido.getSeq(),seq);
    System.out.println("Vou mandar por aqui -> " +port);
    this.socket.send(sendPackage);
    seq++;

    byte[] buff = new byte[Constantes.CONFIG.BUFFER_SIZE];
    DatagramPacket receivedPacket = new DatagramPacket(buff, Constantes.CONFIG.BUFFER_SIZE);
    //TODO
    this.socket.setSoTimeout(2000);

    boolean receveidPackage = false;
    while (!receveidPackage) {
      try {
        this.socket.receive(receivedPacket);
        if (!know_port)
          port = receivedPacket.getPort();

      } catch (IOException e) {
        throw new SocketTimeoutException("Não recebeu nada");
      }

      if (!validType(receivedPacket)){
        //TODO chamar controlo de fluxo
        // mais q 3 vezes e ele manda um package error
      } else {
        System.out.println("RECEBI: " + HI.toString(receivedPacket));
        ACK ack = new ACK(receivedPacket, port, socket, clientIP, seqPedido.getSeq());
        ack.send();
        receveidPackage = true;
      }
    }
  }

  public void received() throws IOException {

      boolean hiReceved = false;

      byte[] buff = new byte[Constantes.CONFIG.BUFFER_SIZE];
      DatagramPacket receivedPacket = new DatagramPacket(buff, Constantes.CONFIG.BUFFER_SIZE);
      while (!hiReceved) {
        try {
          socket.receive(receivedPacket);
          if (!know_port)
            port = receivedPacket.getPort();

          hiReceved = validType(receivedPacket);
          //TODO se for falso varias vezes temos q fazer algo
          // FLUXO de congestao
          if (hiReceved) System.out.println("RECEBI: " + HI.toString(receivedPacket));

        } catch (SocketTimeoutException e) {
          // TODO fluxo de congestao
          continue;
        }
      }

      boolean hiMsgReceveid = false;
      while (!hiMsgReceveid) {
        DatagramPacket hiPacket = createPacket(seqPedido.getSeq(),seq);
        seq++;
        socket.send(hiPacket);

        ACK ack = new ACK(hiPacket,port,socket,clientIP,seq);
        boolean ackFail = false;
        while (!ackFail){
          try {
            ack.received();
            ackFail = true;
            hiMsgReceveid = true;
          } catch (TimeOutMsgException e){
            // TODO controlo de fluxo
            // vamos diminuindo o tempo de receber cenas
            continue;
          } catch (PackageErrorException e1) {
            // TODO controlo de fluxo
            // a partir de x pacotes errados, fechamos a conecao
            continue;
          } catch (AckErrorException e2) {
            break;
          }
        }
      }
  }

  public String toString() {
    if (packet!=null)
      return HI.toString(packet);
    else
      return "Packet Invalid";
  }

  public static String toString(DatagramPacket packet) {
    byte[] msg = packet.getData();
    return  "[HI]   -> SEQ: " + msg[1] + "; SEG: " +msg[2]  + "; MSG:  HI;";
  }
}
