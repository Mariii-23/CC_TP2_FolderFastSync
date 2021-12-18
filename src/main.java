import module.Communication;
import module.Information;
import module.HTTP.Listening;

import java.io.IOException;

public class main {

  public static void main(String[] args) throws IOException {
    String ip = args[0];
    String path = args[1];
    //TODO confirmar argumentos

    // verificar se o argumento 0 é um ip valido , verificar o formato
    // InetAddress.getByName( ... ); -> basta ver se isto nao lanca excecao

    // verificar se o argumnto 1 é uma pasta valida... isto é se o path é valido e exite

    // senoa for... terminar programa e printar o erro

    Information status = new Information();

    Communication c = new Communication(status ,ip, path);
    Listening l = new Listening(status, path);

    Thread[] t = new Thread[2];
    t[0] = new Thread(c);
    t[1] = new Thread(l);

    t[0].start();
    t[1].start();

    try {
      t[0].join();
      t[1].join();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

}
